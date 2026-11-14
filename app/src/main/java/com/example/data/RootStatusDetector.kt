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
 */
class RootStatusDetector(private val context: Context) {

    private val suBinaryPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/data/local/xbin/su"
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
            val indicators = mutableListOf<String>()

            // 1. Passive file existence check for common su binary paths
            for (path in suBinaryPaths) {
                if (runCatching { File(path).exists() }.getOrDefault(false)) {
                    indicators.add("Binary path ditemukan: $path")
                }
            }

            // 2. Check Build.TAGS for test-keys
            val buildTags = Build.TAGS
            if (!buildTags.isNullOrBlank() && buildTags.contains("test-keys", ignoreCase = true)) {
                indicators.add("Build.TAGS mengandung test-keys")
            }

            // 3. Passive PackageManager check for known root manager apps (no execution)
            val pm = context.packageManager
            for (pkgName in knownRootManagerPackages) {
                if (isPackageInstalled(pm, pkgName)) {
                    indicators.add("Paket manager root terinstall: $pkgName")
                }
            }

            RootDetectionResult(
                status = if (indicators.isNotEmpty()) RootStatus.ROOTED else RootStatus.NON_ROOTED,
                indicators = indicators
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
