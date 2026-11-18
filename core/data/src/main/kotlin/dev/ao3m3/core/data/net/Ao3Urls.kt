package dev.ao3m3.core.data.net

/**
 * Every AO3 path the app touches. Keeping these here means a site change is a one-file fix,
 * and makes it obvious (for review and for AO3's ToS) what the app requests and how often.
 */
object Ao3Urls {
    const val BASE = "https://archiveofourown.org"

    /** Identifies the client honestly in AO3's logs, as the archive asks scrapers to do. */
    const val USER_AGENT =
        "AO3M3Reader/0.1 (Android; personal offline reader; contact: local user)"

    /** AO3's JSON endpoints only answer when the client asks for JSON. */
    const val JSON_ACCEPT = "application/json"

    fun work(id: Long): List<String> = listOf("works", id.toString())

    fun workPage(id: Long): List<String> = work(id)

    fun chapter(workId: Long, chapterId: Long): List<String> =
        listOf("works", workId.toString(), "chapters", chapterId.toString())

    /** AO3's full-page chapter index, used when a work page renders no chapter list. */
    fun navigate(workId: Long): List<String> = work(workId) + "navigate"

    /** The create-bookmark form page for a work. */
    fun newBookmark(workId: Long): List<String> = work(workId) + listOf("bookmarks", "new")

    /** AO3's browse index, used for Discover's popular list. */
    fun browse(): List<String> = listOf("works")

    /** AO3's work-search form. `/works` is the browse index and ignores work_search filters. */
    fun search(): List<String> = listOf("works", "search")

    fun tagWorks(tag: String): List<String> = listOf("tags", tag, "works")

    /**
     * Tag autocomplete. The response is JSON: `[{"id":"Name","name":"Name"}]`, and the
     * endpoint works signed out (AO3's own search forms use it for guests).
     */
    fun autocomplete(type: String): List<String> = listOf("autocomplete", type)

    fun login(): List<String> = listOf("users", "login")

    fun userBookmarks(user: String): List<String> = listOf("users", user, "bookmarks")

    fun userReadings(user: String): List<String> = listOf("users", user, "readings")

    fun userSubscriptions(user: String): List<String> =
        listOf("users", user, "subscriptions")

    fun tagFeed(tag: String): List<String> = listOf("tags", tag, "feed.atom")
}
