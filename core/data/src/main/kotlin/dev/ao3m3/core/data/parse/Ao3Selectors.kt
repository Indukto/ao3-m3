package dev.ao3m3.core.data.parse

/**
 * Every CSS selector the parsers depend on. When AO3 changes its markup this is the only file
 * that should need editing.
 *
 * NOTE: these were written against AO3's known structure while the archive was in maintenance and
 * have not been verified against live markup. Fixture tests exercise them; re-check against a real
 * page before a release.
 */
object Ao3Selectors {

    // --- Blurbs (browse, search, bookmarks, history, marked for later) ---
    val BLURBS: List<String> = listOf(
        "li.work.blurb.group",
        "li.bookmark.blurb.group",
        "li.subscription.group",
        "li.blurb.group",
    )

    /** Joined once so callers can pass a single CSS query. */
    val BLURB_QUERY: String = BLURBS.joinToString(", ")

    val BLURB_TITLE: List<String> = listOf(".header .heading a", ".heading a", "a.work")
    val BLURB_AUTHOR: List<String> = listOf("a[rel=author]", ".byline a")
    val BLURB_SUMMARY: List<String> = listOf(
        "blockquote.userstuff.summary",
        ".summary blockquote",
        "blockquote.userstuff",
    )
    val BLURB_DATETIME: List<String> = listOf("p.datetime", ".datetime")

    const val BLURB_WARNINGS = ".tags li.warnings a.tag"
    const val BLURB_RELATIONSHIPS = ".tags li.relationships a.tag"
    const val BLURB_CHARACTERS = ".tags li.characters a.tag"
    const val BLURB_FREEFORMS = ".tags li.freeforms a.tag"
    const val BLURB_FANDOMS = ".tags li.fandoms a.tag"
    const val BLURB_CATEGORIES = ".tags li.category a.tag"

    /**
     * Required-tag symbols. A live blurb carries its Category (F/F, F/M, Gen, …) here and nowhere
     * else — [BLURB_CATEGORIES] is only a fallback for pages that list it as a tag.
     */
    const val BLURB_REQUIRED_TAGS = "ul.required-tags"
    const val BLURB_CATEGORY_SYMBOLS = "span[class^=category-]"
    const val BLURB_RATING = "span[class^=rating-]"

    // --- Work page ---
    val WORK_TITLE: List<String> = listOf("h2.title", "#workskin h2.title", ".work .title")
    val WORK_AUTHOR: List<String> = listOf("h3.byline a[rel=author]", ".byline a[rel=author]")
    val WORK_SUMMARY: List<String> = listOf("div.summary blockquote.userstuff", ".summary .userstuff")
    val WORK_NOTES: List<String> = listOf(".preface .notes blockquote.userstuff", ".notes .userstuff")
    val WORK_STATS: List<String> = listOf("dl.stats", ".stats")
    /**
     * AO3's work page lists its chapters in a `<select>` inside `#chapter_index`, one `<option>`
     * per chapter (`<option value="chapterId">1. Title</option>`). There is no per-chapter link
     * list on that page — the `<ol>` only exists on the full-page index.
     */
    const val WORK_CHAPTER_OPTIONS = "#chapter_index select[name=selected_id] option"

    /** Full-page index (/works/:id/navigate), and older markup: one link per chapter. */
    val WORK_CHAPTER_TOC: List<String> = listOf(
        "ol.chapter.index.group li a",
        "#chapter_index ol li a",
    )

    const val WORK_FANDOMS = "dd.fandoms a.tag, dd.fandom a.tag"
    const val WORK_RELATIONSHIPS = "dd.relationships a.tag, dd.relationship a.tag"
    const val WORK_CHARACTERS = "dd.characters a.tag, dd.character a.tag"
    const val WORK_FREEFORMS = "dd.freeforms a.tag, dd.freeform a.tag"
    const val WORK_WARNINGS = "dd.warnings a.tag, dd.warning a.tag"
    const val WORK_CATEGORIES = "dd.category a.tag, dd.categories a.tag"
    const val WORK_RATING = "dd.rating a.tag, dd.rating"

    /**
     * Chapter body only. AO3 marks the body `userstuff module`; notes use `userstuff` without
     * `module`, so anchoring on `.module` avoids returning a note block as the chapter text.
     */
    const val WORK_USERSTUFF = "#chapters .userstuff.module, #workskin .userstuff.module, #chapters div.userstuff"
    const val WORK_END_NOTES = "#chapters .end.notes .userstuff, .end.notes .userstuff"

    const val PAGINATION_LINKS = "ol.pagination li a, .pagination a"

    // --- Stats fields ---
    const val STAT_WORDS = "dd.words"
    const val STAT_KUDOS = "dd.kudos"
    const val STAT_HITS = "dd.hits"
    const val STAT_BOOKMARKS = "dd.bookmarks"
    const val STAT_COMMENTS = "dd.comments"
    const val STAT_CHAPTERS = "dd.chapters"
    const val STAT_LANGUAGE = "dd.language"
    const val STAT_PUBLISHED = "dd.published"
    const val STAT_STATUS = "dd.status"

    // --- Library page ---
    const val HISTORY_CONTAINER = "#user-history"

    // --- Forms ---
    const val AUTHENTICITY_TOKEN = "input[name=authenticity_token]"
    const val CSRF_META = "meta[name=csrf-token]"

    /** The inline bookmark form on a work page, and on /works/:id/bookmarks/new. */
    const val BOOKMARK_FORM = "#bookmark-form"
}
