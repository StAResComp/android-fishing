package uk.ac.standrews.fishing

import android.Manifest
import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uk.ac.standrews.fishing.fishing.Catch
import uk.ac.standrews.fishing.fishing.FishingDao
import uk.ac.standrews.fishing.fishing.LobsterCrabCatch
import uk.ac.standrews.fishing.fishing.NephropsCatch
import java.text.DateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.floor

val CATCH_TYPES = arrayOf("Nephrops", "Lobster/Crab")
val NEPHROPS = CATCH_TYPES[0]
val LOBSTER_CRAB = CATCH_TYPES[1]

/**
 * Catch Activity. Where users toggle tracking and view/enter details of day's catch
 */
class CatchActivity : ComponentActivity() {

    private lateinit var fishingDao: FishingDao

    @SuppressLint("MissingPermission")
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {isGranted ->
        if (isGranted) {
            setContent {
                MaterialTheme {
                    CatchForm(this::insertCatch)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.fishingDao = AppDatabase.getAppDataBase(this).fishingDao()
        requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        if (
            PermissionChecker.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PermissionChecker.PERMISSION_GRANTED
        ) {
            setContent {
                MaterialTheme {
                    CatchForm(this::insertCatch)
                }
            }
        }
    }

    fun insertCatch (
        catchType: String,
        stringId: String,
        lat: Double,
        lon: Double,
        timestamp: Date,
        numSmall: Double,
        numMedium: Double,
        numLarge: Double,
        wtReturned: Double,
        numLobsterRetained: Int,
        numLobsterReturned: Int,
        numBrownRetained: Int,
        numBrownReturned: Int,
        numVelvetRetained: Int,
        numVelvetReturned: Int
    ) {
        CoroutineScope(IO).launch {
            val aCatch = Catch(
                stringId = stringId, lat = lat, lon = lon, timestamp = timestamp
            )
            val catchId = this@CatchActivity.fishingDao.insertCatch(aCatch)
            if (catchType == NEPHROPS) {
                val nephropsCatch = NephropsCatch(
                    catchId = catchId.toInt(),
                    numSmallCases = numSmall,
                    numMediumCases = numMedium,
                    numLargeCases = numLarge,
                    wtReturned = wtReturned
                )
                this@CatchActivity.fishingDao.insertNephropsCatch(nephropsCatch)
            }
            else if (catchType == LOBSTER_CRAB) {
                val lobsterCrabCatch = LobsterCrabCatch(
                    catchId = catchId.toInt(),
                    numLobstersRetained = numLobsterRetained,
                    numLobstersReturned = numLobsterReturned,
                    numBrownRetained = numBrownRetained,
                    numBrownReturned = numBrownReturned,
                    numVelvetRetained = numVelvetRetained,
                    numVelvetReturned = numVelvetReturned
                )
                this@CatchActivity.fishingDao.insertLobsterCrabCatch(lobsterCrabCatch)
            }

        }
    }
}

@RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatchForm(onSubmit: (
        catchType: String,
        stringId: String,
        lat: Double,
        lon: Double,
        timestamp: Date,
        numSmall: Double,
        numMedium: Double,
        numLarge: Double,
        wtReturned: Double,
        numLobsterRetained: Int,
        numLobsterReturned: Int,
        numBrownRetained: Int,
        numBrownReturned: Int,
        numVelvetRetained: Int,
        numVelvetReturned: Int
    ) -> Unit) {


    val formatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.UK)

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    var locationInfo by remember { mutableStateOf("") }

    var catchType by remember { mutableStateOf("Select catch type") }
    var stringId by remember { mutableStateOf("") }
    var lat by remember { mutableStateOf(0.0) }
    var lon by remember { mutableStateOf(0.0) }
    var latDeg by remember { mutableStateOf("0") }
    var latMin by remember { mutableStateOf("0") }
    var latSec by remember { mutableStateOf("0.0") }
    var lonDeg by remember { mutableStateOf("0") }
    var lonMin by remember { mutableStateOf("0") }
    var lonSec by remember { mutableStateOf("0.0") }
    val cal by remember { mutableStateOf(Calendar.getInstance()) }
    val now = cal.time
    var tmString by remember { mutableStateOf(formatter.format(now)) }
    var numSmall by remember { mutableStateOf("0.0") }
    var numMedium by remember { mutableStateOf("0.0") }
    var numLarge by remember { mutableStateOf("0.0") }
    var wtReturned by remember { mutableStateOf("0.0") }
    var numLobsterRetained by remember { mutableStateOf("0") }
    var numLobsterReturned by remember { mutableStateOf("0") }
    var numBrownRetained by remember { mutableStateOf("0") }
    var numBrownReturned by remember { mutableStateOf("0") }
    var numVelvetRetained by remember { mutableStateOf("0") }
    var numVelvetReturned by remember { mutableStateOf("0") }

    Column (
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Record Catch", style = MaterialTheme.typography.headlineMedium)
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded })
        {
            OutlinedTextField(
                readOnly = true,
                value = catchType,
                singleLine = true,
                onValueChange = {},
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expanded
                    )
                },
                modifier = Modifier.menuAnchor(),
                label = { Text("Catch Type") }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                CATCH_TYPES.forEach { species ->
                    DropdownMenuItem(
                        text = { Text(species) },
                        onClick = {
                            expanded = false
                            catchType = species
                        }
                    )
                }
            }
        }
        OutlinedTextField(
            value = stringId,
            singleLine = true,
            onValueChange = { stringId = it },
            label = { Text("String ID") }
        )
        Row (
            verticalAlignment = Alignment.CenterVertically
        ){
            Column (
                horizontalAlignment = Alignment.End
            ){
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ){
                    Text("Lat", modifier = Modifier.padding(horizontal = 2.dp))
                    OutlinedTextField(
                        value = latDeg,
                        onValueChange = {
                            if (chkNum(it)) { latDeg = it }
                        },
                        label = { Text("Deg")},
                        modifier = Modifier
                            .width(72.dp)
                            .padding(horizontal = 2.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = latMin,
                        onValueChange = {
                            if (chkNum(it)) { latMin = it }
                        },
                        label = { Text("Min")},
                        modifier = Modifier
                            .width(72.dp)
                            .padding(horizontal = 2.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = latSec,
                        onValueChange = {
                            if (chkNum(it, false)) { latSec = it }
                        },
                        label = { Text("Sec")},
                        modifier = Modifier
                            .width(80.dp)
                            .padding(horizontal = 2.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
                Row (
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text("Lon", modifier = Modifier.padding(horizontal = 2.dp))
                    OutlinedTextField(
                        value = lonDeg,
                        onValueChange = {
                            if (chkNum(it)) { lonDeg = it }
                        },
                        label = { Text("Deg")},
                        modifier = Modifier
                            .width(72.dp)
                            .padding(horizontal = 2.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = lonMin,
                        onValueChange = {
                            if (chkNum(it)) { lonMin = it }
                        },
                        label = { Text("Min")},
                        modifier = Modifier
                            .width(72.dp)
                            .padding(horizontal = 2.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = lonSec,
                        onValueChange = {
                            if (chkNum(it, false)) { lonSec = it }
                        },
                        label = { Text("Sec")},
                        modifier = Modifier
                            .width(80.dp)
                            .padding(horizontal = 2.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
            Column {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val result = locationClient.getCurrentLocation(
                                Priority.PRIORITY_HIGH_ACCURACY,
                                CancellationTokenSource().token
                            ).await()
                            result?.let { fetchedLocation ->
                                lat = fetchedLocation.latitude
                                lon = fetchedLocation.longitude
                                val latDegMinSec = coordsDecimaltoDegrees(lat)
                                latDeg = latDegMinSec[0].toString()
                                latMin = latDegMinSec[1].toString()
                                latSec = String.format("%.2f", latDegMinSec[2])
                                val lonDegMinSec = coordsDecimaltoDegrees(lon)
                                lonDeg = lonDegMinSec[0].toString()
                                lonMin = lonDegMinSec[1].toString()
                                lonSec = String.format("%.2f", lonDegMinSec[2])
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.near_me), contentDescription = "")
                }
            }
        }
        Row (
            verticalAlignment = Alignment.CenterVertically,
        ){
            val timePickerDialog = TimePickerDialog(
                context,
                {_, hr: Int, mn: Int ->
                    cal.set(Calendar.HOUR_OF_DAY, hr)
                    cal.set(Calendar.MINUTE, mn)
                    tmString = formatter.format(cal.time)
                }, cal[Calendar.HOUR_OF_DAY], cal[Calendar.MINUTE], false
            )
            OutlinedTextField(
                value = tmString,
                singleLine = true,
                readOnly = true,
                onValueChange = {},
                label = { Text("Time")},
                modifier = Modifier
                    .width(200.dp)
                    .padding(horizontal = 2.dp)
            )
            Button(
                onClick = { timePickerDialog.show() },
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.history), contentDescription = "")
            }
        }
        if (catchType == NEPHROPS) {
            Row {
                OutlinedTextField(
                    value = numSmall,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it, false)) { numSmall = it }
                    },
                    modifier = Modifier.width(144.dp),
                    label = { Text("Small cases") }
                )
                OutlinedTextField(
                    value = numMedium,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it, false)) { numMedium = it }
                    },
                    modifier = Modifier.width(144.dp),
                    label = { Text("Medium cases") }
                )
                OutlinedTextField(
                    value = numLarge,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it, false)) { numLarge = it }
                    },
                    modifier = Modifier.width(144.dp),
                    label = { Text("Large cases") }
                )
            }
            OutlinedTextField(
                value = wtReturned,
                singleLine = true,
                onValueChange = {
                    if (chkNum(it, false)) { wtReturned = it }
                },
                label = { Text("Weight returned") },
                suffix = { Text("kg") }
            )
        }
        if (catchType == LOBSTER_CRAB) {
            Row {
                OutlinedTextField(
                    value = numLobsterRetained,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it)) { numLobsterRetained = it }
                    },
                    label = { Text("Lobsters retained") }
                )
                OutlinedTextField(
                    value = numLobsterReturned,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it)) { numLobsterReturned = it }
                    },
                    label = { Text("Lobsters returned") }
                )
            }
            Row {
                OutlinedTextField(
                    value = numBrownRetained,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it)) { numBrownRetained = it }
                    },
                    label = { Text("Brown crabs retained") }
                )
                OutlinedTextField(
                    value = numBrownReturned,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it)) { numBrownReturned = it }
                    },
                    label = { Text("Brown crabs returned") }
                )
            }
            Row {
                OutlinedTextField(
                    value = numVelvetRetained,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it)) { numVelvetRetained = it }
                    },
                    label = { Text("Velvet crabs retained") }
                )
                OutlinedTextField(
                    value = numVelvetReturned,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it)) { numVelvetReturned = it }
                    },
                    label = { Text("Velvet crabs returned") }
                )

            }
        }
        if (catchType == LOBSTER_CRAB || catchType == NEPHROPS) {
            Button(
                onClick = {
                    lat = coordsDegreestoDecimal(
                        latDeg.toInt(),
                        latMin.toInt(),
                        latSec.toDouble()
                    )
                    lon = coordsDegreestoDecimal(
                        lonDeg.toInt(),
                        lonMin.toInt(),
                        lonSec.toDouble()
                    )
                    onSubmit(
                        catchType,
                        stringId,
                        lat,
                        lon,
                        cal.time,
                        numSmall.toDouble(),
                        numMedium.toDouble(),
                        numLarge.toDouble(),
                        wtReturned.toDouble(),
                        numLobsterRetained.toInt(),
                        numLobsterReturned.toInt(),
                        numBrownRetained.toInt(),
                        numBrownReturned.toInt(),
                        numVelvetRetained.toInt(),
                        numVelvetReturned.toInt()
                    )
                },
            ) {
                Text("Submit")
            }
        }
    }
}

fun chkNum(numString: String, isInt: Boolean = true): Boolean {
    if(isInt) {
        return numString.toIntOrNull() != null || numString.trim() == ""
    }
    else {
        return numString.toDoubleOrNull() != null || numString.trim() == ""
    }
}

fun coordsDecimaltoDegrees(decimalCoord: Double): Array<Number> {
    val degrees = floor(decimalCoord).toInt()
    val minutes = floor((decimalCoord - degrees) * 60.0).toInt()
    val seconds = (decimalCoord - degrees - minutes / 60.0) * 3600.0
    return arrayOf(degrees, minutes, seconds)
}

fun coordsDegreestoDecimal(
    degrees: Int,
    minutes: Int,
    seconds: Double = 0.0
): Double {
    return degrees + minutes / 60.0 + seconds / 3600.0
}





