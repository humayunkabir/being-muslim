package app.beingmuslim.android.report

import io.appwrite.enums.ExecutionStatus
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A send has one failure for the screen to show, whatever went wrong. The
 * function's own answer and the client-side timeout are the two cases the app
 * cannot see from the response alone, so they are pinned here.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppwriteProblemReportServiceTest {

    private fun TestScope.service(execute: suspend (String) -> ReportExecution) =
        AppwriteProblemReportService(
            transport = ReportTransport { execute(it) },
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )

    private suspend fun failureOf(block: suspend () -> Unit): Throwable? = try {
        block()
        null
    } catch (failure: Throwable) {
        failure
    }

    @Test
    fun aCompletedTwoHundredIsASentReport() = runTest {
        val failure = failureOf {
            service { ReportExecution(ExecutionStatus.COMPLETED, 201) }.send("{}")
        }

        assertTrue("expected no failure, was $failure", failure == null)
    }

    @Test
    fun theFunctionsOwnErrorAnswerIsARetryableFailure() = runTest {
        val failure = failureOf {
            service { ReportExecution(ExecutionStatus.COMPLETED, 400) }.send("{}")
        }

        assertTrue(failure is IOException)
    }

    @Test
    fun anExecutionThatDidNotCompleteIsARetryableFailure() = runTest {
        val failure = failureOf {
            service { ReportExecution(ExecutionStatus.FAILED, 201) }.send("{}")
        }

        assertTrue(failure is IOException)
    }

    @Test
    fun aTransportFailureIsARetryableFailureWithItsCauseKept() = runTest {
        val transport = ReportTransport { throw IllegalStateException("socket closed") }
        val service = AppwriteProblemReportService(
            transport = transport,
            ioDispatcher = StandardTestDispatcher(testScheduler),
        )
        val failure = failureOf { service.send("{}") }

        // Coroutine stack-trace recovery may copy the wrapper at the boundary,
        // so look for the original cause in the chain rather than at one level.
        assertTrue(failure is IOException)
        assertTrue(
            "failure=$failure, causes=${failure?.causes()}",
            failure?.causes()?.any { it is IllegalStateException } == true,
        )
    }

    @Test
    fun aSendThatOutlivesTheTimeoutIsARetryableFailure() = runTest {
        val failure = failureOf {
            service {
                delay(60_000)
                ReportExecution(ExecutionStatus.COMPLETED, 201)
            }.send("{}")
        }

        assertTrue(failure is IOException)
    }

    @Test
    fun leavingTheScreenStillCancelsTheSend() = runTest {
        val failure = failureOf {
            service { throw CancellationException("screen left") }.send("{}")
        }

        assertTrue(failure is CancellationException)
    }
}

/** This failure and every cause, ending if a cause points back at itself. */
private fun Throwable.causes(): Sequence<Throwable> =
    generateSequence(this) { failure -> failure.cause?.takeIf { it !== failure } }
