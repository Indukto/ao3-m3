package dev.ao3m3.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.ao3m3.core.designsystem.theme.Ao3Theme
import dev.ao3m3.core.designsystem.theme.Spacing
import dev.ao3m3.core.model.Work
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val ShortDate: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

/** How many category/relationship pills a blurb shows before collapsing into "+N". */
private const val MAX_CONTENT_TAGS = 3

/**
 * A work blurb: title, attribution, the content classification (rating, category, relationships),
 * a three-line summary, at most three additional tags and one stats line. No card chrome — the
 * whitespace between rows is the separator.
 */
@Composable
fun WorkCard(
    work: Work,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingLabel: String? = null,
    showSummary: Boolean = true,
    maxTags: Int = 3,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.gutter, vertical = Spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = work.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            work.chapterLabel?.let { label ->
                Spacer(Modifier.width(Spacing.md))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = bypassLine(work),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        val rating = work.rating?.takeIf { it.isNotBlank() }
        val contentTags = contentTags(work)
        if (rating != null || contentTags.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.sm))
            ContentTagRow(
                rating = rating,
                tags = contentTags,
                overflowCount = (contentTags.size - MAX_CONTENT_TAGS).coerceAtLeast(0),
            )
        }

        if (showSummary && work.summary.isNotBlank()) {
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = work.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val additionalTags = work.additionalTags.distinct()
        val tags = additionalTags.take(maxTags)
        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.md))
            TagRow(tags = tags, overflowCount = (additionalTags.size - tags.size).coerceAtLeast(0))
        }

        Spacer(Modifier.height(Spacing.md))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(
                text = statsLine(work),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            supportingLabel?.let {
                Text(
                    text = "· $it",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * The classification a reader checks before opening a work: rating, then AO3's categories and
 * relationships. The rating is tinted so it reads differently from a story tag.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContentTagRow(
    rating: String?,
    tags: List<String>,
    modifier: Modifier = Modifier,
    overflowCount: Int = 0,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        maxLines = 1,
    ) {
        rating?.let { RatingPill(it) }
        tags.take(MAX_CONTENT_TAGS).forEach { TagPill(text = it) }
        if (overflowCount > 0) TagPill(text = "+$overflowCount")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagRow(
    tags: List<String>,
    modifier: Modifier = Modifier,
    overflowCount: Int = 0,
    maxLines: Int = 1,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        maxLines = maxLines,
    ) {
        tags.forEach { tag ->
            TagPill(text = tag)
        }
        if (overflowCount > 0) {
            TagPill(text = "+$overflowCount")
        }
    }
}

@Composable
fun TagPill(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = color,
        contentColor = contentColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = Spacing.md, vertical = 6.dp),
        )
    }
}

/** Categories and relationships, in AO3's display order, without duplicates. */
private fun contentTags(work: Work): List<String> =
    (work.categories + work.relationships).distinct()

/** "by author · Fandom" */
private fun bypassLine(work: Work): String = buildString {
    append("by ").append(work.authorLabel)
    work.fandoms.firstOrNull()?.let { append(" · ").append(it) }
}

/**
 * A rating pill. Mature and Explicit are 18+ content, so they get the error colour with
 * full-contrast text instead of the neutral tint used for the other ratings.
 */
@Composable
fun RatingPill(text: String, modifier: Modifier = Modifier) {
    val mature = isMatureRating(text)
    TagPill(
        text = text,
        modifier = modifier,
        color = if (mature) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (mature) {
            MaterialTheme.colorScheme.onError
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
    )
}

/** AO3's 18+ ratings: Mature and Explicit. */
fun isMatureRating(rating: String?): Boolean {
    val value = rating?.lowercase().orEmpty()
    return "mature" in value || "explicit" in value
}

/** "12,345 words · 1.2k kudos · updated 2 Mar" */
fun statsLine(work: Work): String {
    val parts = buildList {
        work.words?.let { add("%,d words".format(it)) }
        work.kudos?.takeIf { it > 0 }?.let { add("${compact(it)} kudos") }
        work.updated?.let { add("updated ${formatDate(it)}") }
    }
    return if (parts.isEmpty()) "No stats" else parts.joinToString(" · ")
}

fun compact(value: Int): String = when {
    value >= 1_000_000 -> "%.1fm".format(value / 1_000_000f).replace(".0", "")
    value >= 1_000 -> "%.1fk".format(value / 1_000f).replace(".0", "")
    else -> value.toString()
}

fun formatDate(instant: Instant): String =
    instant.atZone(ZoneId.systemDefault()).format(ShortDate)

@Preview(showBackground = true)
@Composable
private fun WorkCardPreview() {
    Ao3Theme {
        Surface(color = MaterialTheme.colorScheme.background) {
            WorkCard(
                work = Work(
                    id = 1L,
                    title = "A Long Winter of Small Kindnesses",
                    authors = listOf("stormandsky"),
                    summary = "Snowed in at a cottage, two strangers learn to share a fire.",
                    rating = "Teen And Up Audiences",
                    categories = listOf("F/M"),
                    fandoms = listOf("Original Work"),
                    relationships = listOf("A/B"),
                    additionalTags = listOf("Fluff", "Slow Burn"),
                    words = 12_345,
                    chaptersPublished = 3,
                    chaptersTotal = 12,
                    kudos = 1_203,
                ),
                onClick = {},
            )
        }
    }
}
