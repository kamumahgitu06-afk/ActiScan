package com.example.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import com.example.model.ExportedActivityItem
import com.example.model.RootStatus
import com.example.model.TestStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScanResult(
    val items: List<ExportedActivityItem>,
    val totalPackagesScanned: Int,
    val scanTimestampMillis: Long
)

/**
 * Local developer auditing repository.
 *
 * Uses Android's official [PackageManager.getInstalledPackages] with [PackageManager.GET_ACTIVITIES]
 * (requiring QUERY_ALL_PACKAGES on Android 11+ strictly for this core developer audit function)
 * to inspect public manifest metadata of Activity components marked `exported = true`.
 *
 * Does not access private app data, strictly respects OS permission and lockscreen enforcement,
 * and does not transmit any device or package metadata to external servers.
 */
class PackageScannerRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    suspend fun scanExportedActivities(): ScanResult = withContext(Dispatchers.IO) {
        val packages: List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledPackages(
                PackageManager.PackageInfoFlags.of(PackageManager.GET_ACTIVITIES.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES)
        }

        val exportedList = ArrayList<ExportedActivityItem>(512)

        for (pkg in packages) {
            val activities = pkg.activities ?: continue
            val appInfo = pkg.applicationInfo
            val appLabel = appInfo?.loadLabel(packageManager)?.toString()?.takeIf { it.isNotBlank() }
                ?: pkg.packageName
            val isSystem = appInfo != null &&
                ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
            val appPerm = appInfo?.permission?.takeIf { it.isNotBlank() }

            for (act in activities) {
                if (act.exported) {
                    val actPerm = act.permission?.takeIf { it.isNotBlank() }
                    exportedList.add(
                        ExportedActivityItem(
                            id = "${pkg.packageName}/${act.name}",
                            appName = appLabel,
                            packageName = pkg.packageName,
                            activityName = act.name,
                            permission = actPerm,
                            appPermission = appPerm,
                            isSystemApp = isSystem,
                            isEnabled = act.enabled,
                            testStatus = TestStatus.NotTested
                        )
                    )
                }
            }
        }

        val sorted = exportedList.sortedWith(
            compareBy<ExportedActivityItem>(
                { it.isSystemApp },
                { it.appName.lowercase(Locale.ROOT) },
                { it.packageName.lowercase(Locale.ROOT) },
                { it.activityName.lowercase(Locale.ROOT) }
            )
        )

        ScanResult(
            items = sorted,
            totalPackagesScanned = packages.size,
            scanTimestampMillis = System.currentTimeMillis()
        )
    }

    /**
     * Launches a single exported Activity using Android's standard public Intent API.
     * Respects OS security enforcement: any permission denial throws [SecurityException]
     * and is recorded as [TestStatus.Denied].
     */
    fun testSingleActivity(item: ExportedActivityItem): TestStatus {
        return try {
            val intent = Intent().apply {
                component = ComponentName(item.packageName, item.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            TestStatus.Launched
        } catch (e: SecurityException) {
            TestStatus.Denied(
                e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Ditolak Sistem (SecurityException)"
            )
        } catch (e: Exception) {
            TestStatus.RequiresParameters(
                e.localizedMessage?.takeIf { it.isNotBlank() }
                    ?: "Membutuhkan parameter tambahan (${e.javaClass.simpleName})"
            )
        }
    }

    fun loadAppIcon(packageName: String): Drawable? {
        return try {
            packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            null
        }
    }

    fun buildTxtReport(
        items: List<ExportedActivityItem>,
        totalPackagesScanned: Int,
        scanTimestampMillis: Long,
        rootStatus: RootStatus = RootStatus.UNKNOWN
    ): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault())
        val scanDate = if (scanTimestampMillis > 0L) {
            dateFormat.format(Date(scanTimestampMillis))
        } else {
            dateFormat.format(Date())
        }

        val noPermCount = items.count { it.hasNoPermission }
        val launchedCount = items.count { it.testStatus is TestStatus.Launched }
        val deniedCount = items.count { it.testStatus is TestStatus.Denied }
        val requiresParamsCount = items.count { it.testStatus is TestStatus.RequiresParameters }
        val notTestedCount = items.count { it.testStatus is TestStatus.NotTested }

        val rootStatusLabel = when (rootStatus) {
            RootStatus.ROOTED -> "Rooted"
            RootStatus.NON_ROOTED -> "Non-Rooted"
            RootStatus.UNKNOWN -> "Tidak Diketahui"
        }

        return buildString {
            appendLine("======================================================================")
            appendLine("ACTISCAN - EXPORTED ACTIVITY AUDIT REPORT FOR DEVELOPERS")
            appendLine("======================================================================")
            appendLine("Waktu Audit      : $scanDate")
            appendLine("Perangkat        : ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})")
            appendLine("Root Status      : $rootStatusLabel")
            appendLine("Catatan Root     : Status ini hanya untuk keperluan audit, aplikasi ini tidak meminta akses root dan tidak menjalankan perintah root.")
            appendLine("Total Paket Scan : $totalPackagesScanned")
            appendLine("Total Exported   : ${items.size}")
            appendLine("Tanpa Izin       : $noPermCount")
            appendLine("Berizin          : ${items.size - noPermCount}")
            appendLine("----------------------------------------------------------------------")
            appendLine("RINGKASAN HASIL PENGUJIAN MANUAL:")
            appendLine("  [LAUNCHED]             : $launchedCount (Berhasil Diluncurkan)")
            appendLine("  [DENIED]               : $deniedCount (Ditolak Sistem / SecurityException)")
            appendLine("  [REQUIRES_PARAMETERS]  : $requiresParamsCount (Membutuhkan Parameter Tambahan)")
            appendLine("  [NOT_TESTED]           : $notTestedCount (Belum Diuji)")
            appendLine("======================================================================")
            appendLine()

            items.forEachIndexed { index, item ->
                val statusLabel = when (item.testStatus) {
                    TestStatus.NotTested -> "NOT_TESTED"
                    TestStatus.Launched -> "LAUNCHED"
                    is TestStatus.Denied -> "DENIED"
                    is TestStatus.RequiresParameters -> "REQUIRES_PARAMETERS"
                }
                val permText = item.permission ?: "TANPA IZIN"
                val typeText = if (item.isSystemApp) "SYSTEM" else "USER"
                val enabledText = if (item.isEnabled) "ENABLED" else "DISABLED"

                appendLine("${index + 1}. [$statusLabel] ${item.appName} (System App: ${item.isSystemApp} | $typeText | $enabledText)")
                appendLine("   Package    : ${item.packageName}")
                appendLine("   Activity   : ${item.activityName}")
                appendLine("   Permission : $permText")
                if (!item.appPermission.isNullOrBlank()) {
                    appendLine("   App Perm   : ${item.appPermission}")
                }
                when (val s = item.testStatus) {
                    is TestStatus.Denied -> appendLine("   Detail     : ${s.reason}")
                    is TestStatus.RequiresParameters -> appendLine("   Detail     : ${s.reason}")
                    else -> Unit
                }
                appendLine()
            }
        }
    }

    suspend fun writeReportToUri(uri: Uri, content: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val outputStream = context.contentResolver.openOutputStream(uri)
                ?: error("Tidak dapat membuka file output")
            outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(content)
            }
        }
    }
}
