package uk.ac.standrews.pescar

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_today.*
import android.widget.Switch
import android.widget.TextView
import uk.ac.standrews.pescar.fishing.FishingDao
import uk.ac.standrews.pescar.fishing.Landed
import uk.ac.standrews.pescar.fishing.Tow
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors

/**
 * Home Activity. Where users toggle tracking and view/enter details of today's catch
 */
class TodayActivity : AppCompatActivity() {

    //Need to be bound to widget in onCreate
    private lateinit var tracker: Switch
    private lateinit var mapButton: Button
    private lateinit var tows: Array<EditText>
    private lateinit var landeds: Array<Pair<TextView, EditText>>
    private lateinit var fishingDao: FishingDao
    private lateinit var today: Pair<Date, Date>
    private lateinit var timestamp: Date

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fishingDao = AppDatabase.getAppDataBase(this).fishingDao()

        today = (this.application as PescarApplication).getPeriodBoundaries()

        val c = Calendar.getInstance()
        c.time = today.first
        c.add(Calendar.HOUR_OF_DAY, 12)
        timestamp = c.time

        //Bind to layout
        setContentView(R.layout.activity_today)

        var tripInfo: TextView = findViewById(R.id.trip_info)
        val df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()) as SimpleDateFormat
        tripInfo.setText("${df.format(today.first)} - ${df.format(today.second)}")

        mapButton = findViewById(R.id.map_button)

        doTowFields()

        doLandedFields()

        mapButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("started_at", today.first.time)
            intent.putExtra("finished_at", today.second.time)
            startActivity(intent)
        }

        setUpTracker()

        //Navigation
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    private fun setUpTracker() {
        //Bind tracker switch to widget and set listener
        tracker = findViewById(R.id.tracker)
        if ((this.application as PescarApplication).trackingLocation) {
            tracker.toggle()
        }
        tracker.setOnCheckedChangeListener { _, isChecked ->
            var app = this@TodayActivity.application as PescarApplication
            if (!isChecked) {
                app.stopTrackingLocation()
            }
            else if (isChecked && ContextCompat.checkSelfPermission(
                    this@TodayActivity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this@TodayActivity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 568)
            }
            else if (isChecked && ContextCompat.checkSelfPermission(this@TodayActivity,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                app.startTrackingLocation()
            }
            else {
                tracker.toggle()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 568) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                (this@TodayActivity.application as PescarApplication).startTrackingLocation()
            }
            else {
                tracker.toggle()
            }
            return
        }
    }

    //Handle navigation
    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_today -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_archive -> {
                val cal = Calendar.getInstance()
                val dpd = DatePickerDialog(this@TodayActivity, DatePickerDialog.OnDateSetListener { view, year, month, day ->
                    val intent = Intent(this, ArchiveActivity::class.java)
                    val picked = Calendar.getInstance()
                    picked.set(Calendar.YEAR, year)
                    picked.set(Calendar.MONTH, month)
                    picked.set(Calendar.DAY_OF_MONTH, day)
                    picked.set(Calendar.HOUR_OF_DAY, 0)
                    picked.set(Calendar.MINUTE, 0)
                    picked.set(Calendar.SECOND, 0)
                    picked.set(Calendar.MILLISECOND, 0)
                    intent.putExtra("midnight", picked.timeInMillis)
                    startActivity(intent)
                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                dpd.show()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    private fun doTowFields() {

        //Get views
        tows = arrayOf(
            findViewById(R.id.tow_1),
            findViewById(R.id.tow_2),
            findViewById(R.id.tow_3),
            findViewById(R.id.tow_4),
            findViewById(R.id.tow_5),
            findViewById(R.id.tow_6)
        )

        //Get existing values
        val towsCallable = Callable {
            fishingDao.getTowsForPeriod(today.first, today.second)
        }
        val towList = Executors.newSingleThreadExecutor().submit(towsCallable).get()
        towList.forEachIndexed { index, tow ->
            tows[index].tag = tow.id
            tows[index].setText(tow.weight.toString())
        }

        //Set listeners
        tows.forEach { textField ->
            textField.setOnFocusChangeListener { view, hasFocus ->
                if (!hasFocus) {
                    val field = view as EditText
                    if (field.text.matches("\\d+(\\.\\d+)?".toRegex())) {
                        if (field.tag is Number) {
                            Executors.newSingleThreadExecutor().execute {
                                fishingDao.updateTow((field.tag as Int), field.text.toString().toDouble(), Date())
                            }
                        } else {
                            val c = Callable {
                                fishingDao.insertTow(
                                    Tow(weight = field.text.toString().toDouble(), timestamp = timestamp)
                                ).toInt()
                            }
                            field.tag = Executors.newSingleThreadExecutor().submit(c).get()
                        }
                    }
                }
            }
        }
    }

    private fun doLandedFields() {

        //Get views
        landeds = arrayOf(
            Pair(findViewById(R.id.species_1_label), findViewById(R.id.species_1)),
            Pair(findViewById(R.id.species_2_label), findViewById(R.id.species_2)),
            Pair(findViewById(R.id.species_3_label), findViewById(R.id.species_3)),
            Pair(findViewById(R.id.species_4_label), findViewById(R.id.species_4)),
            Pair(findViewById(R.id.species_5_label), findViewById(R.id.species_5)),
            Pair(findViewById(R.id.species_6_label), findViewById(R.id.species_6))
        )

        //Get existing values
        var speciesCallable = Callable {
            fishingDao.getSpecies()
        }
        val speciesList = Executors.newSingleThreadExecutor().submit(speciesCallable).get()
        val landedsCallable = Callable {
            fishingDao.getLandedsForPeriod(today.first, today.second)
        }
        val landedsList = Executors.newSingleThreadExecutor().submit(landedsCallable).get()
        speciesList.forEachIndexed { index, species ->
            landeds[index].first.text = species.name
            var empty = true
            landedsList.forEach { lws ->
                if (empty && lws.species.first().id == species.id) {
                    landeds[index].second.setTag(R.id.landed_id_key, lws.landed.id)
                    landeds[index].second.setText(lws.landed.weight.toString())
                    empty = false
                }
            }
            if (empty) {
                landeds[index].second.setTag(R.id.landed_id_key, false)
            }
            landeds[index].second.setTag(R.id.species_id_key, species.id)
        }

        //Set listeners
        landeds.forEach { pair ->
            val textField = pair.second
            textField.setOnFocusChangeListener { view, hasFocus ->
                if (!hasFocus) {
                    val field = view as EditText
                    if (field.text.matches("\\d+(\\.\\d+)?".toRegex())) {
                        if (field.getTag(R.id.landed_id_key) is Number) {
                            Executors.newSingleThreadExecutor().execute {
                                fishingDao.updateLanded(
                                    (field.getTag(R.id.landed_id_key) as Int), field.text.toString().toDouble(), Date()
                                )
                            }
                        }
                        else {
                            val c = Callable {
                                fishingDao.insertLanded(
                                    Landed(
                                        weight = field.text.toString().toDouble(),
                                        timestamp = timestamp,
                                        speciesId = (field.getTag(R.id.species_id_key) as Int)
                                    )
                                ).toInt()
                            }
                            field.setTag(R.id.landed_id_key, Executors.newSingleThreadExecutor().submit(c).get())
                        }
                    }
                }
            }
        }
    }
}
