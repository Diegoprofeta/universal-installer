package app.pwhs.universalinstaller.presentation.install.util

import android.content.Context
import android.os.Build
import app.pwhs.universalinstaller.R

object AbiCompatibilityHelper {

    private val ABI_ALIASES = mapOf(
        "arm64-v8a" to setOf("arm64-v8a", "arm64_v8a", "arm64", "aarch64"),
        "armeabi-v7a" to setOf("armeabi-v7a", "armeabi_v7a", "armv7", "armv7a", "armv7l", "armeabi"),
        "x86_64" to setOf("x86_64", "x64", "amd64"),
        "x86" to setOf("x86", "i386", "i686"),
    )

    fun normalize(abi: String): String =
        abi.trim().replace('-', '_').lowercase()

    fun is64Bit(abi: String): Boolean {
        val lower = normalize(abi)
        return lower.contains("64") || lower.contains("aarch64")
    }

    fun matches(apkAbi: String, deviceAbis: Array<String> = Build.SUPPORTED_ABIS): Boolean {
        val normalizedApk = normalize(apkAbi)
        val apkAliases = ABI_ALIASES.entries.firstOrNull { (_, aliases) ->
            aliases.any { normalize(it) == normalizedApk }
        }?.value?.map { normalize(it) }?.toSet() ?: setOf(normalizedApk)

        return deviceAbis.any { deviceAbi ->
            val normalizedDevice = normalize(deviceAbi)
            normalizedDevice in apkAliases
        }
    }

    /**
     * Checks if the APK's native libraries are compatible with the device.
     * Pure bytecode APKs (empty [apkAbis]) run on any architecture and return true.
     */
    fun isCompatible(
        apkAbis: List<String>,
        deviceAbis: Array<String> = Build.SUPPORTED_ABIS,
    ): Boolean {
        if (apkAbis.isEmpty() || deviceAbis.isEmpty()) return true
        return apkAbis.any { apkAbi -> matches(apkAbi, deviceAbis) }
    }

    fun isDevice32BitOnly(deviceAbis: Array<String> = Build.SUPPORTED_ABIS): Boolean {
        return deviceAbis.none { is64Bit(it) }
    }

    fun isDevice64BitOnly(deviceAbis: Array<String> = Build.SUPPORTED_ABIS): Boolean {
        return deviceAbis.none { !is64Bit(it) }
    }

    fun isApk64BitOnly(apkAbis: List<String>): Boolean {
        return apkAbis.isNotEmpty() && apkAbis.all { is64Bit(it) }
    }

    fun isApk32BitOnly(apkAbis: List<String>): Boolean {
        return apkAbis.isNotEmpty() && apkAbis.all { !is64Bit(it) }
    }

    fun getRecommendedArchitecture(
        apkAbis: List<String>,
        deviceAbis: Array<String> = Build.SUPPORTED_ABIS,
    ): String {
        return when {
            isDevice32BitOnly(deviceAbis) -> "32-bit (armeabi-v7a)"
            isDevice64BitOnly(deviceAbis) -> "64-bit (arm64-v8a)"
            else -> "Universal / 32-bit"
        }
    }

    fun isAbiErrorMessage(message: String?): Boolean {
        val upper = message.orEmpty().uppercase()
        return "NO_MATCHING_ABIS" in upper ||
                "CPU_ABI" in upper ||
                "NATIVE_LIBRARIES" in upper ||
                "NATIVE LIBRARIES" in upper ||
                "RES=-113" in upper ||
                "INSTALL_FAILED_CPU_ABI_INCOMPATIBLE" in upper
    }

    fun getDetailedGuidance(
        context: Context,
        apkAbis: List<String>,
        deviceAbis: Array<String> = Build.SUPPORTED_ABIS,
    ): String {
        val apkAbiFormatted = if (apkAbis.isNotEmpty()) {
            val bitness = if (isApk64BitOnly(apkAbis)) "64-bit: " else if (isApk32BitOnly(apkAbis)) "32-bit: " else ""
            bitness + apkAbis.joinToString(", ")
        } else {
            "Unknown"
        }

        val deviceAbiFormatted = if (deviceAbis.isNotEmpty()) {
            val bitness = if (isDevice32BitOnly(deviceAbis)) "32-bit: " else if (isDevice64BitOnly(deviceAbis)) "64-bit: " else ""
            bitness + deviceAbis.joinToString(", ")
        } else {
            "Unknown"
        }

        val recommended = getRecommendedArchitecture(apkAbis, deviceAbis)

        return context.getString(
            R.string.install_error_incompatible_abi_detailed_guidance,
            apkAbiFormatted,
            deviceAbiFormatted,
            recommended,
        )
    }
}
