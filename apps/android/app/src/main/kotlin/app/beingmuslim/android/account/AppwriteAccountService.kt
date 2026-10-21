package app.beingmuslim.android.account

import android.content.Context
import app.beingmuslim.android.BuildConfig
import app.beingmuslim.android.appwrite.appwriteClient
import app.beingmuslim.android.auth.AuthUser
import app.beingmuslim.android.sync.readSyncRow
import io.appwrite.enums.ExecutionStatus
import io.appwrite.services.Functions
import io.appwrite.services.TablesDB
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The account's data through Appwrite: the sync row is read for export and
 * removed for deletion; the identity is deleted through the trusted server
 * function because the client SDK has no account-deletion call
 * (`docs/APPWRITE.md`).
 */
class AppwriteAccountService(context: Context) : AccountService {

    private val appContext = context.applicationContext

    // Built lazily inside the IO calls: constructing the client reads and
    // decrypts the Keystore-backed cookie store.
    private val client by lazy { appwriteClient(appContext) }
    private val tables by lazy { TablesDB(client) }
    private val functions by lazy { Functions(client) }
    private val accountState = SignedInUserStore(appContext)

    override suspend fun export(user: AuthUser): String = withContext(Dispatchers.IO) {
        // No row is a normal answer: the account simply holds nothing yet.
        AccountExport.document(user, tables.readSyncRow(user.id))
    }

    override suspend fun delete(): Unit = withContext(Dispatchers.IO) {
        val execution = functions.createExecution(
            BuildConfig.APPWRITE_ACCOUNT_DELETE_FUNCTION_ID,
            async = false,
        )
        if (execution.status != ExecutionStatus.COMPLETED ||
            execution.responseStatusCode !in 200L..299L
        ) {
            throw IOException("Account deletion failed")
        }
        // The identity is gone; the session cookie is dead and the next account
        // check clears it. Only this marker records who was signed in.
        accountState.setSignedInUserId(null)
    }
}
