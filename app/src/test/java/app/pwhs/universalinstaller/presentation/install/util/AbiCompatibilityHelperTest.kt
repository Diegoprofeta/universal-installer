package app.pwhs.universalinstaller.presentation.install.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AbiCompatibilityHelperTest {

    @Test
    fun `isCompatible returns true when apk supportedAbis is empty`() {
        val deviceAbis = arrayOf("arm64-v8a", "armeabi-v7a", "armeabi")
        assertTrue(AbiCompatibilityHelper.isCompatible(emptyList(), deviceAbis))
    }

    @Test
    fun `isCompatible returns true when device supports exact or compatible ABI`() {
        val deviceAbis = arrayOf("arm64-v8a", "armeabi-v7a", "armeabi")

        // 64-bit app on 64-bit device
        assertTrue(AbiCompatibilityHelper.isCompatible(listOf("arm64-v8a"), deviceAbis))
        // 32-bit armv7 app on 64-bit device supporting 32-bit
        assertTrue(AbiCompatibilityHelper.isCompatible(listOf("armeabi-v7a"), deviceAbis))
        // Multi-arch app
        assertTrue(AbiCompatibilityHelper.isCompatible(listOf("arm64-v8a", "x86_64"), deviceAbis))
    }

    @Test
    fun `isCompatible returns false when 64-bit app on 32-bit device`() {
        // e.g. Xiaomi Redmi 9C (angelica)
        val deviceAbis = arrayOf("armeabi-v7a", "armeabi")
        val apkAbis = listOf("arm64-v8a")

        assertFalse(AbiCompatibilityHelper.isCompatible(apkAbis, deviceAbis))
        assertTrue(AbiCompatibilityHelper.isDevice32BitOnly(deviceAbis))
        assertTrue(AbiCompatibilityHelper.isApk64BitOnly(apkAbis))
        assertEquals("32-bit (armeabi-v7a)", AbiCompatibilityHelper.getRecommendedArchitecture(apkAbis, deviceAbis))
    }

    @Test
    fun `isCompatible returns false when 32-bit app on 64-bit-only device`() {
        // Modern 64-bit-only devices (e.g. Pixel 7+)
        val deviceAbis = arrayOf("arm64-v8a")
        val apkAbis = listOf("armeabi-v7a")

        assertFalse(AbiCompatibilityHelper.isCompatible(apkAbis, deviceAbis))
        assertTrue(AbiCompatibilityHelper.isDevice64BitOnly(deviceAbis))
        assertTrue(AbiCompatibilityHelper.isApk32BitOnly(apkAbis))
        assertEquals("64-bit (arm64-v8a)", AbiCompatibilityHelper.getRecommendedArchitecture(apkAbis, deviceAbis))
    }

    @Test
    fun `isCompatible returns false for architecture mismatch e-g x86 on arm`() {
        val deviceAbis = arrayOf("arm64-v8a", "armeabi-v7a")
        val apkAbis = listOf("x86", "x86_64")

        assertFalse(AbiCompatibilityHelper.isCompatible(apkAbis, deviceAbis))
    }

    @Test
    fun `isAbiErrorMessage detects various PackageManager ABI error patterns`() {
        assertTrue(AbiCompatibilityHelper.isAbiErrorMessage("INSTALL_FAILED_NO_MATCHING_ABIS: Failed to extract native libraries, res=-113"))
        assertTrue(AbiCompatibilityHelper.isAbiErrorMessage("INSTALL_FAILED_CPU_ABI_INCOMPATIBLE"))
        assertTrue(AbiCompatibilityHelper.isAbiErrorMessage("Package couldn't be installed in /data/app/... (res=-113)"))
        assertTrue(AbiCompatibilityHelper.isAbiErrorMessage("Failed to extract native libraries"))
        assertFalse(AbiCompatibilityHelper.isAbiErrorMessage("INSTALL_FAILED_INSUFFICIENT_STORAGE"))
        assertFalse(AbiCompatibilityHelper.isAbiErrorMessage(null))
    }
}
