package app.beingmuslim.android.report

/**
 * The report backend seam, so the screen can be exercised without a network.
 * A failure is ordinary: the caller keeps the report on screen and offers a
 * retry rather than losing it (`docs/PRODUCT_REQUIREMENTS.md` §2.1).
 */
interface ProblemReportService {

    /** Sends [report] as encoded by [ProblemReportCodec]; throws when it was not stored. */
    suspend fun send(report: String)
}
