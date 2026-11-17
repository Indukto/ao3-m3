package dev.ao3m3.core.model

/** Sort orders offered by AO3's work search, in the order the app presents them. */
enum class SortOption(val param: String, val label: String) {
    BEST_MATCH("", "Best match"),
    UPDATED("revised_at", "Recently updated"),
    KUDOS("kudos_count", "Kudos"),
    HITS("hits", "Hits"),
    WORDS("word_count", "Word count"),
    PUBLISHED("created_at", "Date posted"),
    ;

    companion object {
        fun fromParam(param: String?): SortOption =
            entries.firstOrNull { it.param == param } ?: BEST_MATCH
    }
}

/**
 * AO3's work-search filters, reduced to the ones that materially change results.
 * Free-text values (fandom/tag) are passed through verbatim so users can use AO3 tag syntax.
 */
data class SearchFilters(
    val rating: Set<String> = emptySet(),
    val warnings: Set<String> = emptySet(),
    val categories: Set<String> = emptySet(),
    val fandom: String = "",
    val relationship: String = "",
    val character: String = "",
    val otherTags: String = "",
    val minWords: Int? = null,
    val maxWords: Int? = null,
    val singleChapterOnly: Boolean = false,
    val completeOnly: Boolean = false,
    val excludeCrossovers: Boolean = false,
) {
    val activeCount: Int
        get() = rating.size + warnings.size + categories.size +
            listOf(fandom, relationship, character, otherTags).count { it.isNotBlank() } +
            listOf(minWords, maxWords).count { it != null } +
            listOf(singleChapterOnly, completeOnly, excludeCrossovers).count { it }

    val isEmpty: Boolean get() = activeCount == 0

    companion object {
        /**
         * AO3 filters ratings by canonical tag id, not by name. These ids are archive-wide and
         * stable; the label is what the filter sheet shows.
         */
        val RATING_IDS: Map<String, String> = linkedMapOf(
            "9" to "Not Rated",
            "10" to "General Audiences",
            "11" to "Teen And Up Audiences",
            "12" to "Mature",
            "13" to "Explicit",
        )

        val RATINGS: List<String> get() = RATING_IDS.values.toList()

        /**
         * Archive warnings and categories are also filtered by canonical tag id. These are AO3's
         * published ids (14/16–20 and 21–24, 116, 2246); the numeric values themselves have not
         * been re-confirmed against a live search form because filtered searches answer 418 during
         * the archive's maintenance window. See README.
         */
        val WARNING_IDS: Map<String, String> = linkedMapOf(
            "16" to "No Archive Warnings Apply",
            "14" to "Creator Chose Not To Use Archive Warnings",
            "17" to "Graphic Depictions Of Violence",
            "18" to "Major Character Death",
            "19" to "Rape/Non-Con",
            "20" to "Underage",
        )

        val CATEGORY_IDS: Map<String, String> = linkedMapOf(
            "116" to "F/F",
            "22" to "F/M",
            "21" to "Gen",
            "23" to "M/M",
            "2246" to "Multi",
            "24" to "Other",
        )

        val SORTS = SortOption.entries
    }
}

/** One browse/search request. [tag] drives tag pages, [query] drives the search page. */
data class WorkQuery(
    val query: String = "",
    val tag: String? = null,
    val filters: SearchFilters = SearchFilters(),
    val sort: SortOption = SortOption.BEST_MATCH,
    val page: Int = 1,
    /** The Search tab uses AO3's work-search form; Discover's popular browse uses `/works`. */
    val useSearchEndpoint: Boolean = false,
) {
    /** Stable identity for caching one page of results. */
    fun cacheKey(): String = buildString {
        append(tag ?: if (useSearchEndpoint) "search" else "browse")
        append('|').append(query)
        append('|').append(sort.param)
        append('|').append(filters.rating.sorted().joinToString(","))
        append('|').append(filters.warnings.sorted().joinToString(","))
        append('|').append(filters.categories.sorted().joinToString(","))
        append('|').append(filters.fandom)
        append('|').append(filters.relationship)
        append('|').append(filters.character)
        append('|').append(filters.otherTags)
        append('|').append(filters.minWords ?: "")
        append('|').append(filters.maxWords ?: "")
        append('|').append(filters.singleChapterOnly)
        append('|').append(filters.completeOnly)
        append('|').append(filters.excludeCrossovers)
        append('|').append(page)
    }

    fun withPage(page: Int): WorkQuery = copy(page = page)

    val isTagPage: Boolean get() = !tag.isNullOrBlank()
}
