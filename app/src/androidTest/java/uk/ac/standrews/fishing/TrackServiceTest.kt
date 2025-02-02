package uk.ac.standrews.fishing

import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import androidx.test.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import uk.ac.standrews.fishing.track.TrackService
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class TrackServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private lateinit var locationManager: LocationManager
    private val locationProvider = LocationManager.GPS_PROVIDER
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        locationManager = InstrumentationRegistry.getTargetContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.addTestProvider(locationProvider, false, false, false,
            false, true, true, true, 0,
            5)
        locationManager.setTestProviderEnabled(locationProvider, true)
        db = AppDatabase.getAppDataBase(InstrumentationRegistry.getTargetContext())
    }

    @Test
    fun testLocationLogging() {
        (InstrumentationRegistry.getTargetContext().applicationContext as FishingApplication).startTrackingLocation()
        val l = Location(locationProvider)
        val lat = (Math.random() - 0.5) * 2 * 90
        val lon = (Math.random() - 0.5) * 2 * 180
        val time = System.currentTimeMillis()
        val acc = (Math.random()  * 1000).toFloat()
        l.latitude = lat
        l.longitude = lon
        l.time = time
        l.accuracy = acc
        l.elapsedRealtimeNanos = System.nanoTime()
        locationManager.setTestProviderLocation(locationProvider, l)
        TimeUnit.SECONDS.sleep(10)
        Executors.newSingleThreadExecutor().execute {
            val pos = db.trackDao().getLastPosition()
            assert(lat == pos.latitude)
            assert(lon == pos.longitude)
            assert(time == pos.timestamp.time)
            assert(acc == pos.accuracy)
        }
    }

    @Test
    fun test00rejection() {
        (InstrumentationRegistry.getTargetContext().applicationContext as FishingApplication).startTrackingLocation()
        var numRowsPre = 0
        Executors.newSingleThreadExecutor().execute {
            numRowsPre = db.trackDao().countPositions()
        }
        val l = Location(locationProvider)
        val lat = 0.0
        val lon = 0.0
        val time = System.currentTimeMillis()
        val acc = 0.0f
        l.latitude = lat
        l.longitude = lon
        l.time = time
        l.accuracy = acc
        l.elapsedRealtimeNanos = System.nanoTime()
        locationManager.setTestProviderLocation(locationProvider, l)
        TimeUnit.SECONDS.sleep(10)
        Executors.newSingleThreadExecutor().execute {
            val numRowsPost = db.trackDao().countPositions()
            if (numRowsPost > 0) {
                val pos = db.trackDao().getLastPosition()
                assert(lat != pos.latitude)
                assert(lon != pos.longitude)
                assert(time != pos.timestamp.time)
                assert(acc != pos.accuracy)
            }
            assert(numRowsPre == numRowsPost)
        }
    }

}
