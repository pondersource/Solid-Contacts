package com.pondersource.solidcontacts.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the device can currently reach the network.
 *
 * The app works either way; this only drives the "offline, changes will sync later" banner and
 * tells the sync scheduler when it is worth asking for a drain.
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val manager: ConnectivityManager? =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    val isOnline: Flow<Boolean> = callbackFlow {
        val cm = manager
        if (cm == null) {
            trySend(true)
            awaitClose { }
            return@callbackFlow
        }

        val online = mutableSetOf<Network>()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                online += network
                trySend(true)
            }

            override fun onLost(network: Network) {
                online -= network
                trySend(online.isNotEmpty())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities,
            ) {
                val usable = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                if (usable) online += network else online -= network
                trySend(online.isNotEmpty())
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
        trySend(currentlyOnline())

        awaitClose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }.conflate().distinctUntilChanged()

    /** A one-shot read, for code that cannot collect a flow. */
    fun currentlyOnline(): Boolean {
        val cm = manager ?: return true
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
