package dev.ao3m3.core.data.net

/**
 * Every network failure the app can hit, modelled explicitly. [Maintenance] is deliberately not
 * an error path: it means "serve what you have and tell the reader quietly".
 */
sealed class Ao3Exception(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** AO3's "Request temporarily denied" / maintenance page (HTTP 418, or a 5xx shell). */
    class Maintenance : Ao3Exception("The Archive is offline for maintenance.")

    /** 429, or a 503 that is not the maintenance page. */
    class RateLimited(val retryAfterSeconds: Long = 30) :
        Ao3Exception("The Archive is rate-limiting this device. Try again shortly.")

    /** The page needs a signed-in session. */
    class Unauthorized : Ao3Exception("Log in to your AO3 account to see this.")

    class NotFound : Ao3Exception("That page doesn't exist any more.")

    class Server(val code: Int) : Ao3Exception("The Archive returned HTTP $code.")

    class Http(val code: Int) : Ao3Exception("The Archive returned HTTP $code.")

    class Network(cause: Throwable) : Ao3Exception("Couldn't reach the Archive.", cause)

    /** AO3 markup changed under us. */
    class Parse(detail: String) : Ao3Exception("Couldn't read the Archive's page ($detail).")

    fun userMessage(): String = message ?: "Something went wrong."
}
