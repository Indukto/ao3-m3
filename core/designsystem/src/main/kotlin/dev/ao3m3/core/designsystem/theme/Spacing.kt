package dev.ao3m3.core.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * The only spacing values the app is allowed to use. Keeping the scale tiny is what makes the
 * layout feel deliberate rather than "mostly aligned".
 */
object Spacing {
    val none = 0.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    /** Horizontal page gutter used by every screen. */
    val gutter = 20.dp

    /** Minimum touch target. */
    val touch = 48.dp

    /** Standard list row height. */
    val row = 88.dp

    /** Space reserved under scrollable content for the floating navigation bar. */
    val bottomBarClearance = 96.dp

    /** Cap on a reading column; longer lines are harder to scan on tablets. */
    val proseMaxWidth = 680.dp

    /** Room under reader prose for the floating chapter toolbar. */
    val readerToolbarClearance = 112.dp

    /** Long bottom sheets stop growing here and scroll instead. */
    val sheetMaxHeight = 720.dp
}
