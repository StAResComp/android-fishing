package uk.ac.standrews.fishing

import android.app.Application
import android.net.NetworkCapabilities
import android.net.NetworkRequest

/**
 * Extends [android.app.Application] to handle tracking independently of any activity
 *
 * @constructor creates an instance with tracking location off
 */
class FishingApplication : Application() {

    private val database by lazy { AppDatabase.getAppDataBase(this) }
    val repository by lazy { CatchRepository(database.fishingDao()) }
    val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .build()
}
