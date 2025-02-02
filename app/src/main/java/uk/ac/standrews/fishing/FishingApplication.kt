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
                    val landedsToUpload = fishingDao.getUnuploadedLandedsForPeriod(day.first, day.second)
                    val positionsToUpload = trackDao.getUnuploadedPositions()

                    if (landedsToUpload.isNotEmpty() || positionsToUpload.isNotEmpty()) {
                        val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        //df.timeZone = TimeZone.getTimeZone("UTC")

                        val landedsJson = JSONArray()
                        landedsToUpload.forEach { lws ->
                            val landedJson = JSONObject()
                            landedJson.put("id", lws.aCatch.id)
                            landedJson.put("species", lws.species.first().name)
                            landedJson.put("weight", lws.aCatch.weight)
                            landedJson.put("timestamp", df.format(lws.aCatch.timestamp))
                            landedsJson.put(landedJson)
                        }

                        val positionsJson = JSONArray()
                        positionsToUpload.forEach { pos ->
                            val positionJson = JSONObject()
                            positionJson.put("id", pos.id)
                            positionJson.put("latitude", pos.latitude)
                            positionJson.put("longitude", pos.longitude)
                            positionJson.put("accuracy", pos.accuracy)
                            positionJson.put("timestamp", df.format(pos.timestamp))
                            positionsJson.put(positionJson)
                        }

                        val json = JSONObject()
                        json.put("device", Settings.Secure.getString(this@FishingApplication.contentResolver, Settings.Secure.ANDROID_ID))
                        json.put("captures", landedsJson)
                        json.put("positions", positionsJson)

                        val url = getString(R.string.fishing_data_url)

                        val callback = object : VolleyCallback {
                            override fun onSuccess(result: JSONObject) {
                                val towsJson = result.getJSONArray("tows")
                                val towIds = arrayListOf<Int>()
                                if (towsJson.length() > 0) {
                                    for (i in 0 until towsJson.length()) {
                                        towIds.add((towsJson[i] as JSONObject).getInt("id"))
                                    }
                                }
                                val landedsJson = result.getJSONArray("captures")
                                val landedIds = arrayListOf<Int>()
                                for (i in 0 until landedsJson.length()) {
                                    landedIds.add((landedsJson[i] as JSONObject).getInt("id"))
                                }
                                val positionsJson = result.getJSONArray("positions")
                                val positionIds = arrayListOf<Int>()
                                for (i in 0 until positionsJson.length()) {
                                    positionIds.add((positionsJson[i] as JSONObject).getInt("id"))
                                }
                                Executors.newSingleThreadExecutor().execute {
                                    if (landedIds.isNotEmpty()) {
                                        if (landedIds.size > 999) {
                                            val chunkedLandedIds = landedIds.chunked(999)
                                            chunkedLandedIds.forEach { list ->
                                                fishingDao.markLandedsUploaded(list, Date())
                                            }
                                        }
                                        else {
                                            fishingDao.markLandedsUploaded(landedIds, Date())
                                        }
                                    }
                                    if (positionIds.isNotEmpty()) {
                                        if (positionIds.size > 999) {
                                            val chunkedPositionIds = positionIds.chunked(999)
                                            chunkedPositionIds.forEach { list ->
                                                trackDao.markPositionsUploaded(list, Date())
                                            }
                                        }
                                        else {
                                            trackDao.markPositionsUploaded(positionIds, Date())
                                        }
                                    }
                                }
                                Toast.makeText(baseContext, R.string.data_submission_success, Toast.LENGTH_LONG).show()
                            }

                            override fun onError(result: String?) {
                                Log.e("POST", "Failure!")
                                //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                            }
                        }

                        val request = object : JsonObjectRequest(
                            Request.Method.POST, url, json,
                            Response.Listener {
                                callback.onSuccess(json)
                            },
                            Response.ErrorListener { error ->
                                Toast.makeText(baseContext, R.string.data_submission_failure, Toast.LENGTH_LONG).show()
                                if (error.networkResponse != null && error.networkResponse.data != null) {
                                    callback.onError(VolleyError(String(error.networkResponse.data)).message)
                                }
                            }
                        ) {
                            @Throws(AuthFailureError::class)
                            override fun getHeaders(): Map<String, String> {
                                val headers: Map<String, String> = hashMapOf(
                                    "Authorization" to "Bearer $accessToken",
                                    "Content-Type" to "text/plain"
                                )
                                return headers
                            }
                        }
                        RequestQueueSingleton.getInstance(this@FishingApplication).addToRequestQueue(request)
                    }
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
