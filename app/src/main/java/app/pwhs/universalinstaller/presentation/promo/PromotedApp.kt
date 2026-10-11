package app.pwhs.universalinstaller.presentation.promo

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import app.pwhs.universalinstaller.BuildConfig
import app.pwhs.universalinstaller.R

data class PromotedApp(
    val id: String,
    val name: String,
    @StringRes val taglineRes: Int,
    @StringRes val descRes: Int,
    val packageName: String,
    val githubUrl: String? = null,
    val playStoreUrl: String? = null,
    val releasesUrl: String? = null,
    @DrawableRes val iconRes: Int,
    val accentColor: Color,
    val tags: List<String>,
)

object PromotedApps {

    val BLOCKADS = PromotedApp(
        id = "blockads",
        name = "BlockAds",
        taglineRes = R.string.promo_blockads_tagline,
        descRes = R.string.promo_blockads_desc,
        packageName = "app.pwhs.blockads",
        githubUrl = "https://github.com/pass-with-high-score/blockads-android",
        releasesUrl = "https://github.com/pass-with-high-score/blockads-android/releases",
        iconRes = R.drawable.ic_promo_blockads,
        accentColor = Color(0xFF2E7D32),
        tags = listOf("Open Source", "No Root", "AdBlock"),
    )

    val CRYSTAL_SCAN = PromotedApp(
        id = "crystal_scan",
        name = "Crystal Scan",
        taglineRes = R.string.promo_crystal_scan_tagline,
        descRes = R.string.promo_crystal_scan_desc,
        packageName = "app.pwhs.crystalscan",
        githubUrl = null,
        playStoreUrl = "https://play.google.com/store/apps/details?id=app.pwhs.crystalscan",
        releasesUrl = null,
        iconRes = R.drawable.ic_promo_crystal_scan,
        accentColor = Color(0xFF1976D2),
        tags = listOf("Google Play", "QR & Barcode", "OpenCV AI", "Material 3"),
    )

    val ANEKO = PromotedApp(
        id = "aneko",
        name = "ANeko Reborn",
        taglineRes = R.string.promo_aneko_tagline,
        descRes = R.string.promo_aneko_desc,
        packageName = "org.nqmgaming.aneko",
        githubUrl = "https://github.com/pass-with-high-score/ANeko",
        playStoreUrl = "https://play.google.com/store/apps/details?id=org.nqmgaming.aneko",
        releasesUrl = "https://github.com/pass-with-high-score/ANeko/releases",
        iconRes = R.drawable.ic_promo_aneko,
        accentColor = Color(0xFFE65100),
        tags = listOf("Cute Pet", "Material You", "Open Source"),
    )

    /**
     * Resolves the list of promoted apps based on the build distribution flavor:
     * - `opensource`: BlockAds and ANeko Reborn.
     * - `play` (Store): Crystal Scan and ANeko Reborn.
     */
    fun getApps(flavor: String = BuildConfig.FLAVOR): List<PromotedApp> {
        return if (flavor == "opensource") {
            listOf(BLOCKADS, ANEKO)
        } else {
            listOf(CRYSTAL_SCAN, ANEKO)
        }
    }
}
