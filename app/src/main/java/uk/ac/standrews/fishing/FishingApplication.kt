package uk.ac.standrews.fishing

import android.app.Application
import android.content.Context
import com.android.volley.*
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.util.*
import java.util.concurrent.Executors

/**
 * Extends [android.app.Application] to handle tracking independently of any activity
 *
 * @constructor creates an instance with tracking location off
 */
class FishingApplication : Application() {

    fun postData(day: Pair<Date, Date>): Boolean {
        var success = true
        Executors.newSingleThreadExecutor().execute {
            val db = AppDatabase.getAppDataBase(this@FishingApplication)
            val fishingDao = db.fishingDao()
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
