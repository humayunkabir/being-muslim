package app.beingmuslim.android.report

import android.content.Context
import app.beingmuslim.android.BuildConfig
import app.beingmuslim.android.appwrite.appwriteClient
import io.appwrite.enums.ExecutionStatus
import io.appwrite.services.Functions
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Reports through the `report-problem` Appwrite Function, which validates the
 * allowlisted inputs and stores them where only the operator can read them
 * (`docs/APPWRITE.md`). No account is required, so a signed-out user can report
 * as well (`docs/PRIVACY.md`).
 *
 * Every failure — transport, timeout, or the function's own refusal — reaches
 * the caller as [IOException], so the screen has one retryable failure to show.
 */
class AppwriteProblemReportService(context: Context) : ProblemReportService {

    private val appContext = context.applicationContext

    // Built lazily inside the IO call: constructing the client reads and
    // decrypts the Keystore-backed cookie store.
    private val client by lazy { appwriteClient(appContext) }
    private val functions by lazy { Functions(client) }

    override suspend fun send(report: String): Unit = withContext(Dispatchers.IO) {
        val execution = try {
            withTimeout(SEND_TIMEOUT_MS) {
                functions.createExecution(
                    BuildConfig.APPWRITE_REPORT_FUNCTION_ID,
                    body = report,
                    async = false,
                )
            }
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
