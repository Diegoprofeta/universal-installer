package app.pwhs.universalinstaller.presentation.promo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.pwhs.universalinstaller.R

fun LazyListScope.promotedAppsSection(
    modifier: Modifier = Modifier,
) {
    val apps = PromotedApps.getApps()
    if (apps.isEmpty()) return

    item {
        Text(
            text = stringResource(R.string.about_section_more_apps),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )
    }

    items(apps, key = { "promo_${it.id}" }) { app ->
        PromotedAppCard(
            app = app,
            modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
}
