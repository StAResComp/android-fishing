package uk.ac.standrews.fishing

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.android.volley.*
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationService
import org.json.JSONArray
import org.json.JSONObject
import uk.ac.standrews.fishing.track.TrackService
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

/**
 * Extends [android.app.Application] to handle tracking independently of any activity
 *
 * @constructor creates an instance with tracking location off
 */
class FishingApplication : Application() {

    var trackingLocation: Boolean = false

    /**
     * Toggles location tracking
     */
    fun toggleLocationTracking() {
        Log.d("TRACK","Toggling tracking in application class")
        if (this.trackingLocation) {
            this.stopTrackingLocation()
        }
        else {
            this.startTrackingLocation()
        }
        Log.d("TRACK","Tracking: ${this.trackingLocation}")
    }

    /**
     * Starts location tracking
     */
    fun startTrackingLocation() {
        startService(Intent(this,TrackService::class.java))
        Toast.makeText(baseContext, R.string.started_tracking_location, Toast.LENGTH_LONG).show()
        trackingLocation = true
    }

    /**
     * Stops location tracking, with user notification via Toast
     */
    fun stopTrackingLocation() {
        stopService(Intent(this,TrackService::class.java))
        Toast.makeText(baseContext, R.string.stopped_tracking_location, Toast.LENGTH_LONG).show()
        trackingLocation = false
    }

    fun getPeriodBoundaries(timestamp: Date? = null): Pair<Date, Date> {
        val c = Calendar.getInstance()
        if (timestamp != null) {
            c.time = timestamp
        }
        if (c.get(Calendar.AM_PM) == Calendar.AM) {
            c.add(Calendar.DATE, -1)
        }
        c.set(Calendar.HOUR_OF_DAY, 12)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        val startTime = c.time
        c.add(Calendar.DATE, 1)
        val finishTime = c.time
        return Pair(startTime, finishTime)
    }

    fun postData(day: Pair<Date, Date>, authState: AuthState?): Boolean {
        var success = true
        if (authState != null) {
            val authService = AuthorizationService(this)
            authState.performActionWithFreshTokens(authService, AuthState.AuthStateAction { accessToken, _, _ ->
                Executors.newSingleThreadExecutor().execute {
                    val db = AppDatabase.getAppDataBase(this@FishingApplication)
                    val fishingDao = db.fishingDao()
                    val trackDao = db.trackDao()
                    val positionsToUpload = trackDao.getUnuploadedPositions()

                }
            })
        }
        return success
    }
}

class RequestQueueSingleton constructor(context: Context) {
    companion object {
        @Volatile
        private var INSTANCE: RequestQueueSingleton? = null
        fun getInstance(context: Context) =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: RequestQueueSingleton(context).also {
                    INSTANCE = it
                }
            }
    }
    val requestQueue: RequestQueue by lazy {
        // applicationContext is key, it keeps you from leaking the
        // Activity or BroadcastReceiver if someone passes one in.
        Volley.newRequestQueue(context.applicationContext)
    }
    fun <T> addToRequestQueue(req: Request<T>) {
        requestQueue.add(req)
    }
}

interface VolleyCallback {
    fun onSuccess(result: JSONObject)
    fun onError(result: String?)
}
