package com.example.data

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.model.RootDetectionResult
import com.example.model.RootStatus
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Passive, read-only device root indicator detector for developer audit context.
 *
 * Strictly compliant with Google Play policy:
 * - Does NOT execute `su` or any shell command.
 * - Does NOT request root privileges.
 * - Strictly respects OS permission boundaries and never accesses restricted app data.
 *
 * Note: Intentionally does NOT treat `Build.TAGS` (`test-keys`) or bootloader unlock (UBL)
 * properties as root indicators, because devices with only an unlocked bootloader or custom
 * ROM without root binaries would otherwise be falsely flagged as rooted.
 */
class RootStatusDetector(private val context: Context) {

    private val suBinaryPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/debug_ramdisk/su",
        "/vendor/bin/su",
        "/su/bin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su"
    )

    private val knownRootManagerPackages = listOf(
        "com.topjohnwu.magisk",
        "io.github.vvb2060.magisk",
        "me.weishu.kernelsu",
        "me.bmax.apatch",
        "eu.chainfire.supersu",
        "com.koushikdutta.superuser"
    )

    suspend fun detect(): RootDetectionResult = withContext(Dispatchers.IO) {
        runCatching {
            val indicators = LinkedHashSet<String>()

            // 1. Passive file existence check for Android su binary paths
            for (path in suBinaryPaths) {
                val existsAsFile = runCatching {
                    val file = File(path)
                    file.exists() && file.isFile
                }.getOrDefault(false)
                if (existsAsFile) {
                    indicators.add("Binary su ditemukan: $path")
                }
            }

            // 2. Passive PackageManager check for known root manager apps (no execution)
            val pm = context.packageManager
            for (pkgName in knownRootManagerPackages) {
                if (isPackageInstalled(pm, pkgName)) {
                    indicators.add("Paket manager root terinstall: $pkgName")
                }
            }

            val indicatorList = indicators.toList()
            RootDetectionResult(
                status = if (indicatorList.isNotEmpty()) RootStatus.ROOTED else RootStatus.NON_ROOTED,
                indicators = indicatorList
            )
        }.getOrElse {
            RootDetectionResult(status = RootStatus.UNKNOWN, indicators = emptyList())
        }
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
