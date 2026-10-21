package app.beingmuslim.android.report

import android.content.Context
import app.beingmuslim.android.BuildConfig
import app.beingmuslim.android.appwrite.appwriteGuestClient
import io.appwrite.enums.ExecutionStatus
import io.appwrite.services.Functions
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Reports through the `report-problem` Appwrite Function, which validates the
 * allowlisted inputs and stores them where only the operator can read them
 * (`docs/APPWRITE.md`). No account is required, so the send uses a guest client
 * even when the user is signed in: the report cannot be tied to the account
 * (`docs/PRIVACY.md`).
 *
 * Every failure — transport, timeout, or the function's own refusal — reaches
 * the caller as [IOException], so the screen has one retryable failure to show.
 */
class AppwriteProblemReportService internal constructor(
    private val transport: ReportTransport,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ProblemReportService {

    constructor(context: Context) : this(appwriteTransport(context.applicationContext))

    override suspend fun send(report: String): Unit = withContext(ioDispatcher) {
        val execution = try {
            withTimeout(SEND_TIMEOUT_MS) { transport.execute(report) }
        } catch (timeout: TimeoutCancellationException) {
            // Caught before CancellationException: a timed-out send is a
            // retryable failure, not the caller abandoning the screen.
            throw IOException("Report not sent", timeout)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Exception) {
            throw IOException("Report not sent", failure)
        }
        // A function's own non-2xx answer arrives as a completed execution, so
        // the app must check the function's status as well as the transport.
        if (execution.status != ExecutionStatus.COMPLETED ||
            execution.responseStatusCode !in 200L..299L
        ) {
            throw IOException("Report not sent")
        }
    }

    private companion object {
        /** The function's own timeout is 15 s; allow the round trip around it. */
        const val SEND_TIMEOUT_MS = 20_000L
    }
}

/** What one send returned: the function's status and its own HTTP answer. */
internal data class ReportExecution(
    val status: ExecutionStatus,
    val responseStatusCode: Long,
)

/** The one call a send makes, so a failure can be exercised without a network. */
internal fun interface ReportTransport {
    suspend fun execute(body: String): ReportExecution
}

private fun appwriteTransport(context: Context): ReportTransport {
    val client by lazy { appwriteGuestClient(context) }
    val functions by lazy { Functions(client) }
    return ReportTransport { body ->
        val execution = functions.createExecution(
            BuildConfig.APPWRITE_REPORT_FUNCTION_ID,
            body = body,
            async = false,
        )
        ReportExecution(execution.status, execution.responseStatusCode)
    }
}
