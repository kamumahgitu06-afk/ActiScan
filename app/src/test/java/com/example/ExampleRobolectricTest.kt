package com.example

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.test.core.app.ApplicationProvider
import com.example.data.PackageScannerRepository
import com.example.data.RootStatusDetector
import com.example.model.ExportedActivityItem
import com.example.model.RootStatus
import com.example.model.TestStatus
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app_name and subtitle from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("ActiScan", context.getString(R.string.app_name))
        assertEquals(
            "Exported Activity Auditor for Android Developers",
            context.getString(R.string.app_subtitle)
        )
    }

    @Test
    fun `scanExportedActivities returns only exported activities`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPm = Shadows.shadowOf(context.packageManager)

        val appInfo = ApplicationInfo().apply {
            packageName = "com.sample.target"
            name = "Sample Target"
            flags = 0
        }
        val exportedAct = ActivityInfo().apply {
            packageName = "com.sample.target"
            name = "com.sample.target.OpenActivity"
            exported = true
            permission = null
            enabled = true
        }
        val privateAct = ActivityInfo().apply {
            packageName = "com.sample.target"
            name = "com.sample.target.InternalActivity"
            exported = false
            permission = null
            enabled = true
        }
        val protectedAct = ActivityInfo().apply {
            packageName = "com.sample.target"
            name = "com.sample.target.ProtectedActivity"
            exported = true
            permission = "com.sample.PERMISSION_ADMIN"
            enabled = true
        }
        val pkgInfo = PackageInfo().apply {
            packageName = "com.sample.target"
            applicationInfo = appInfo
            activities = arrayOf(exportedAct, privateAct, protectedAct)
        }
        shadowPm.installPackage(pkgInfo)

        val repo = PackageScannerRepository(context)
        val result = repo.scanExportedActivities()

        val targetItems = result.items.filter { it.packageName == "com.sample.target" }
        assertEquals(2, targetItems.size)
        assertTrue(targetItems.any { it.activityName == "com.sample.target.OpenActivity" && it.hasNoPermission })
        assertTrue(targetItems.any { it.activityName == "com.sample.target.ProtectedActivity" && !it.hasNoPermission })
        assertFalse(targetItems.any { it.activityName == "com.sample.target.InternalActivity" })
    }

    @Test
    fun `rootStatusDetector detects installed root manager package passively`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val shadowPm = Shadows.shadowOf(context.packageManager)

        val detector = RootStatusDetector(context)
        val initialResult = detector.detect()
        assertEquals(RootStatus.NON_ROOTED, initialResult.status)

        // Simulate unlocked bootloader / custom build tag (test-keys) without su or root manager
        org.robolectric.util.ReflectionHelpers.setStaticField(
            android.os.Build::class.java,
            "TAGS",
            "test-keys"
        )
        val ublOnlyResult = detector.detect()
        assertEquals(RootStatus.NON_ROOTED, ublOnlyResult.status)

        val magiskPkg = PackageInfo().apply {
            packageName = "com.topjohnwu.magisk"
            applicationInfo = ApplicationInfo().apply {
                packageName = "com.topjohnwu.magisk"
            }
        }
        shadowPm.installPackage(magiskPkg)

        val rootedResult = detector.detect()
        assertEquals(RootStatus.ROOTED, rootedResult.status)
        assertTrue(rootedResult.indicators.any { it.contains("com.topjohnwu.magisk") })
    }

    @Test
    fun `buildTxtReport uses neutral developer audit labels and includes root status`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = PackageScannerRepository(context)
        val items = listOf(
            ExportedActivityItem(
                id = "com.a/Act1",
                appName = "App A",
                packageName = "com.a",
                activityName = "com.a.Act1",
                permission = null,
                appPermission = null,
                isSystemApp = false,
                isEnabled = true,
                testStatus = TestStatus.Launched
            ),
            ExportedActivityItem(
                id = "com.b/Act2",
                appName = "App B",
                packageName = "com.b",
                activityName = "com.b.Act2",
                permission = "android.permission.BIND_DEVICE_ADMIN",
                appPermission = null,
                isSystemApp = true,
                isEnabled = false,
                testStatus = TestStatus.Denied("Ditolak Sistem (SecurityException)")
            ),
            ExportedActivityItem(
                id = "com.c/Act3",
                appName = "App C",
                packageName = "com.c",
                activityName = "com.c.Act3",
                permission = null,
                appPermission = null,
                isSystemApp = false,
                isEnabled = true,
                testStatus = TestStatus.RequiresParameters("Missing extra")
            )
        )

        val report = repo.buildTxtReport(
            items = items,
            totalPackagesScanned = 3,
            scanTimestampMillis = 1700000000000L,
            rootStatus = RootStatus.NON_ROOTED
        )
        assertTrue(report.contains("ACTISCAN - EXPORTED ACTIVITY AUDIT REPORT FOR DEVELOPERS"))
        assertTrue(report.contains("Root Status      : Non-Rooted"))
        assertTrue(report.contains("tidak meminta akses root dan tidak menjalankan perintah root"))
        assertTrue(report.contains("TANPA IZIN"))
        assertTrue(report.contains("[LAUNCHED]"))
        assertTrue(report.contains("[DENIED]"))
        assertTrue(report.contains("[REQUIRES_PARAMETERS]"))
        assertTrue(report.contains("android.permission.BIND_DEVICE_ADMIN"))
        assertFalse(report.uppercase().contains("JEBOL"))
        assertFalse(report.uppercase().contains("HACK"))
        assertFalse(report.uppercase().contains("BYPASS"))
    }
}
