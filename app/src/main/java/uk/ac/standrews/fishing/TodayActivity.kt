package uk.ac.standrews.fishing

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Switch
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.*

/**
 * Home Activity. Where users toggle tracking and view/enter details of day's catch
 */
class TodayActivity : ArchiveActivity() {

    private var trackingLocation by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //setDayAndTime()

        //Bind to layout
        bindView()

        //setUpTracker()
        val navigation = findViewById<BottomNavigationView>(R.id.navigation)
        navigation.menu.findItem(R.id.navigation_today).isChecked = true
    }

    override fun bindView() {
        val app = this.application as FishingApplication
        this.trackingLocation = app.trackingLocation
        setContentView(R.layout.activity_today).apply {
            val composeView = findViewById<ComposeView>(R.id.compose_view)
            composeView.setContent {
                MaterialTheme {
                    TrackSwitch(this@TodayActivity.trackingLocation) { this@TodayActivity.toggleTracking() }
                }
            }
        }
    }

    override fun setDayAndTime() {
        this.day = (this.application as FishingApplication).getPeriodBoundaries()
        val c = Calendar.getInstance()
        c.time = day.first
        c.add(Calendar.HOUR_OF_DAY, 12)
        timestamp = c.time
    }

    private fun toggleTracking() {
        if (ContextCompat.checkSelfPermission(this@TodayActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@TodayActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 568)
        }
        else{
            (this.application as FishingApplication).toggleLocationTracking()
        }
        this.trackingLocation = (this.application as FishingApplication).trackingLocation
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 568) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                (this.application as FishingApplication).toggleLocationTracking()
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}

@Composable
fun TrackSwitch(tracking: Boolean, toggleFun: () -> Unit) {
    Switch(
        checked = tracking,
        onCheckedChange = { toggleFun() }
    )
}





