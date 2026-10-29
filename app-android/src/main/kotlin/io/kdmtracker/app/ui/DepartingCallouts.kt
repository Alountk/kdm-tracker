package io.kdmtracker.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.kdmtracker.app.ui.theme.KdmType

/** The design's armor-verification callout — inert copy. */
@Composable
internal fun ArmorCallout() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.medium)
                .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Symbol("shield", MaterialTheme.colorScheme.secondary, 22)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = PrepareDepartingStepCopy.ARMOR_TITLE,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 14.sp, lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = PrepareDepartingStepCopy.ARMOR_PARAGRAPH,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ArmorChip(PrepareDepartingStepCopy.ARMOR_CHIP_WAIST)
                ArmorChip(PrepareDepartingStepCopy.ARMOR_CHIP_SURVIVAL)
            }
        }
    }
}

/** One checked armor-verification chip — inert design copy. */
@Composable
private fun ArmorChip(label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Symbol("check_box", MaterialTheme.colorScheme.secondary, 14)
        Text(
            text = label,
            style = KdmType.labelCaps.copy(fontSize = 11.sp, lineHeight = 14.sp),
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

/** The campaign-rule error box (step 9's year-1 skip) — inert design copy. */
@Composable
internal fun CampaignRuleBox() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                    MaterialTheme.shapes.medium,
                ).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Symbol("warning", MaterialTheme.colorScheme.error, 18)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = PrepareDepartingStepCopy.CAMPAIGN_RULE_LABEL.uppercase(),
                style = KdmType.labelCaps,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = PrepareDepartingStepCopy.CAMPAIGN_RULE_QUOTE,
                style = KdmType.ruleQuote.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = PrepareDepartingStepCopy.CAMPAIGN_RULE_SOURCE,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, lineHeight = 13.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
