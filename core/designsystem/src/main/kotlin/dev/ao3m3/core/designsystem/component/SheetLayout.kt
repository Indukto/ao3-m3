package dev.ao3m3.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.ao3m3.core.designsystem.theme.Spacing

/**
 * The shared shape of a long bottom sheet: a title, one scrollable body and an optional pinned
 * footer. Keeping the primary action out of the scroll means it stays reachable on small screens
 * and above the keyboard.
 */
@Composable
fun SheetLayout(
    title: String,
    modifier: Modifier = Modifier,
    footer: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = Spacing.sheetMaxHeight)
            .imePadding(),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = Spacing.gutter, vertical = Spacing.md),
        )
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter),
            content = content,
        )
        if (footer != null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.gutter, vertical = Spacing.md),
                content = footer,
            )
        } else {
            Spacer(Modifier.height(Spacing.xxl))
        }
    }
}
