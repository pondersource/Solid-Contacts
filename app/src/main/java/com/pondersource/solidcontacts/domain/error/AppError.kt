package com.pondersource.solidcontacts.domain.error

import com.erfangholami.androidsolidservices.client.sdk.SolidException
import com.pondersource.solidcontacts.R
import java.io.IOException

/**
 * What went wrong, in terms the UI can act on.
 *
 * The SDK throws a typed [SolidException] tree and the network throws [IOException]. The screens
 * do not care about either; they care whether to show a retry button, a sign-in button or an
 * install button. [AppError] carries that decision.
 */
sealed class AppError {

    /** The host app, Solid Share, is not installed. Offer to install it. */
    data object HostMissing : AppError()

    /** The host app is installed but the bound service is not answering. Retrying usually works. */
    data object HostUnreachable : AppError()

    /** Nobody is signed in, or the grant was taken back. Send the user to sign in again. */
    data object NotSignedIn : AppError()

    /** The pod refused the call. The user lacks permission on that resource. */
    data object NotPermitted : AppError()

    /** The device is offline. The write is queued; the read serves the cache. */
    data object Offline : AppError()

    /** Anything else, with the message the layer below gave us. */
    data class Unknown(val detail: String?) : AppError()

    /** The string resource that explains this error to the user. */
    val messageRes: Int
        get() = when (this) {
            HostMissing -> R.string.error_host_missing
            HostUnreachable -> R.string.error_host_unreachable
            NotSignedIn -> R.string.error_not_signed_in
            NotPermitted -> R.string.error_not_permitted
            Offline -> R.string.error_offline
            is Unknown -> R.string.error_unknown
        }

    /** `true` when trying the same call again could plausibly succeed. */
    val isRetryable: Boolean
        get() = this is HostUnreachable || this is Offline || this is Unknown

    /** `true` when the failure means the queued write should stay queued rather than be dropped. */
    val isTransient: Boolean
        get() = this is HostUnreachable || this is Offline

    companion object {

        /** Classifies [throwable] into the error the UI knows how to present. */
        fun from(throwable: Throwable): AppError = when (throwable) {
            is SolidException.SolidAppNotFoundException -> HostMissing
            is SolidException.SolidServiceConnectionException -> HostUnreachable
            is SolidException.SolidNotLoggedInException -> NotSignedIn
            is SolidException.SolidResourceException.NullWebIdException -> NotSignedIn
            is SolidException.SolidResourceException.NotPermissionException -> NotPermitted
            is IOException -> Offline
            else -> Unknown(throwable.message)
        }
    }
}

/** Sugar for the many call sites that only need the classified error. */
fun Throwable.toAppError(): AppError = AppError.from(this)
