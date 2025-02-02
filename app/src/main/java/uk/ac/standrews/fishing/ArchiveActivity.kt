package uk.ac.standrews.fishing

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import net.openid.appauth.AuthState
import uk.ac.standrews.fishing.fishing.FishingDao
import uk.ac.standrews.fishing.fishing.Catch
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import org.json.JSONException
import android.text.TextUtils
import android.widget.Switch


/**
 * Archive Activity. Where users view/enter details of previous days' catch
 */
open class ArchiveActivity : AppCompatActivity() {

    //Need to be bound to widget in onCreate
    private lateinit var mapButton: Button
    private lateinit var submitButton: Button
    private lateinit var landeds: Array<Pair<TextView, EditText>>
    private lateinit var fishingDao: FishingDao
    private var authState: AuthState? = null
    lateinit var day: Pair<Date, Date>
    lateinit var timestamp: Date

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fishingDao = AppDatabase.getAppDataBase(this).fishingDao()

        setDayAndTime()

        val c = Calendar.getInstance()
        c.time = day.first
        c.add(Calendar.HOUR_OF_DAY, 12)
        timestamp = c.time

        //Bind to layout
        bindView()

        var tripInfo: TextView = findViewById(R.id.trip_info)
        val df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()) as SimpleDateFormat
        tripInfo.setText("${df.format(day.first)} - ${df.format(day.second)}")

        mapButton = findViewById(R.id.map_button)

        landeds = arrayOf(
            Pair(findViewById(R.id.species_1_label), findViewById(R.id.species_1)),
            Pair(findViewById(R.id.species_2_label), findViewById(R.id.species_2)),
            Pair(findViewById(R.id.species_3_label), findViewById(R.id.species_3)),
            Pair(findViewById(R.id.species_4_label), findViewById(R.id.species_4)),
            Pair(findViewById(R.id.species_5_label), findViewById(R.id.species_5)),
            Pair(findViewById(R.id.species_6_label), findViewById(R.id.species_6))
        )

        submitButton = findViewById(R.id.submit_button)

        doLandedFields()

        mapButton.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("started_at", day.first.time)
            intent.putExtra("finished_at", day.second.time)
            startActivity(intent)
        }

        submitButton.setOnClickListener {
            val builder = AlertDialog.Builder(this@ArchiveActivity)
            builder.setTitle(R.string.confirm_submit_title)
            builder.setMessage(R.string.confirm_submit_message)
            builder.setIcon(R.drawable.ic_warning_black_24dp)
            builder.setPositiveButton(R.string.yes, DialogInterface.OnClickListener { dialog, _ ->
                submitData()
                dialog.dismiss()
            })
            builder.setNegativeButton(R.string.no, DialogInterface.OnClickListener { dialog, _ ->
                dialog.dismiss()
            })
            val alert = builder.create()
            alert.show()
        }

        //Navigation
        val navigation = findViewById<BottomNavigationView>(R.id.navigation)
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        navigation.menu.findItem(R.id.navigation_archive).isChecked = true

        authState = restoreAuthState()
    }

    private fun restoreAuthState(): AuthState? {
        val jsonString = getSharedPreferences("AuthStatePreference", Context.MODE_PRIVATE)
            .getString("AUTH_STATE", null)
        if (jsonString != null && !TextUtils.isEmpty(jsonString)) {
            try {
                return AuthState.jsonDeserialize(jsonString)
            } catch (jsonException: JSONException) {
                // should never happen
            }

        }
        return null
    }

    open fun bindView() {
        setContentView(R.layout.activity_archive)
    }

    open fun setDayAndTime() {
        day = (this.application as FishingApplication).getPeriodBoundaries(Date(intent.getLongExtra("midnight",0)))
        val c = Calendar.getInstance()
        c.time = day.first
        c.add(Calendar.HOUR_OF_DAY, 12)
        timestamp = c.time
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == 568) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                (this@ArchiveActivity.application as FishingApplication).startTrackingLocation()
            }
            else {
                //findViewById<Switch>(R.id.tracker).toggle()
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //Handle navigation
    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_today -> {
                val intent = Intent(this, TodayActivity::class.java)
                startActivity(intent)
            }
            R.id.navigation_archive -> {
                val cal = Calendar.getInstance()
                val dpd = DatePickerDialog(this@ArchiveActivity, DatePickerDialog.OnDateSetListener { _, year, month, day ->
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
            R.id.navigation_link -> {
                val intent = Intent(this, AuthActivity::class.java)
                startActivity(intent)
            }
        }
        false
    }

    private fun doLandedFields() {

        //Get existing values
        var speciesCallable = Callable {
            fishingDao.getSpecies()
        }
        val speciesList = Executors.newSingleThreadExecutor().submit(speciesCallable).get()
        val landedsCallable = Callable {
            fishingDao.getLandedsForPeriod(day.first, day.second)
        }
        val landedsList = Executors.newSingleThreadExecutor().submit(landedsCallable).get()
        var uploaded = false
        speciesList.forEachIndexed { index, species ->
            landeds[index].first.text = species.name
            var empty = true
            landedsList.forEach { lws ->
                if (empty && lws.species.first().id == species.id) {
                    landeds[index].second.setTag(R.id.landed_id_key, lws.aCatch.id)
                    landeds[index].second.setText(lws.aCatch.weight.toString())
                    empty = false
                }
                if (lws.aCatch.uploaded != null) {
                    uploaded = true
                }
            }
            if (empty) {
                landeds[index].second.setTag(R.id.landed_id_key, false)
            }
            landeds[index].second.setTag(R.id.species_id_key, species.id)
        }
        if (uploaded) {
            landeds.forEach { landed ->
                disableField(landed.second)
            }
            submitButton.visibility = View.GONE
        }

        //Set listeners
        landeds.forEach { pair ->
            val textField = pair.second
            textField.setOnFocusChangeListener { view, hasFocus ->
                if (!hasFocus) {
                    val field = view as EditText
                    submitLanded(field)
                }
            }
        }
    }

    private fun submitLanded(field: EditText) {
        if (field.text.matches("\\d+(\\.\\d+)?".toRegex())) {
            if (field.getTag(R.id.landed_id_key) is Number) {
                Executors.newSingleThreadExecutor().execute {
                    fishingDao.updateLanded(
                        (field.getTag(R.id.landed_id_key) as Int), field.text.toString().toDouble(), timestamp
                    )
                }
            }
            else {
                val c = Callable {
                    fishingDao.insertLanded(
                        Catch(
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

    private fun submitData() {
        landeds.forEach { pair ->
            val landed = pair.second
            submitLanded(landed)
        }
        if ((this.application as FishingApplication).postData(day, authState)) {
            landeds.forEach { pair ->
                val landed = pair.second
                disableField(landed)
            }
            submitButton.visibility = View.GONE
        }
    }

    private fun disableField(field: EditText) {
        field.setFocusable(false)
        field.setEnabled(false)
        field.setCursorVisible(false)
        field.keyListener = null
        field.inputType = InputType.TYPE_NULL
        field.setBackgroundColor(Color.TRANSPARENT)
    }
}
