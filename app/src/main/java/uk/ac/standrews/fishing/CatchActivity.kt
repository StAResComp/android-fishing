package uk.ac.standrews.fishing

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import uk.ac.standrews.fishing.fishing.FishingDao
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.floor

/**
 * Catch Activity. Where users toggle tracking and view/enter details of day's catch
 */
class CatchActivity : ComponentActivity() {

    private var trackingLocation by mutableStateOf(false)
    private lateinit var fishingDao: FishingDao

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            this@CatchActivity.toggleTracking()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.fishingDao = AppDatabase.getAppDataBase(this).fishingDao()
        val app = this.application as FishingApplication
        this.trackingLocation = app.trackingLocation
        setContent {
            MaterialTheme {
                CatchForm()
            }
        }
    }

    private fun toggleTracking() {
        val app = (this.application as FishingApplication)
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            this@CatchActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (fineLocationPermission == PackageManager.PERMISSION_GRANTED) {
            app.toggleLocationTracking()
        }
        else {
            requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        this.trackingLocation = app.trackingLocation
    }

    private fun goToMap() {}
}
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun CatchForm() {

    val CATCH_TYPES = arrayOf("Nephrops", "Lobster/Crab")
    val NEPHROPS = CATCH_TYPES[0]
    val LOBSTER_CRAB = CATCH_TYPES[1]

    var catchType by remember { mutableStateOf("Select catch type") }
    var stringId by remember { mutableStateOf("") }
    var latDeg by remember { mutableStateOf("0") }
    var latMin by remember { mutableStateOf("0") }
    var latSec by remember { mutableStateOf("0.0") }
    var lonDeg by remember { mutableStateOf("0") }
    var lonMin by remember { mutableStateOf("0") }
    var lonSec by remember { mutableStateOf("0.0") }
    val formatter = SimpleDateFormat("HH:mm")
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
            onValueChange = {
                if (it.toIntOrNull() != null || it.trim() == "") {
                    stringId = it
                }
            },
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
                            if (it.toIntOrNull() != null || it.trim() == "") {
                                latDeg = it
                            }
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
                            if (it.toIntOrNull() != null || it.trim() == "") {
                                latMin = it
                            }
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
                            if (it.toDoubleOrNull() != null || it.trim() == "") {
                                latSec = it
                            }
                        },
                        label = { Text("Sec")},
                        modifier = Modifier
                            .width(72.dp)
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
                            if (it.toIntOrNull() != null || it.trim() == "") {
                                lonDeg = it
                            }
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
                            if (it.toIntOrNull() != null || it.trim() == "") {
                                lonMin = it
                            }
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
                            if (it.toDoubleOrNull() != null || it.trim() == "") {
                                lonSec = it
                            }
                        },
                        label = { Text("Sec")},
                        modifier = Modifier
                            .width(72.dp)
                            .padding(horizontal = 2.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }
            Column {
                Button(
                    onClick = { /*TODO*/ },
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
                LocalContext.current,
                {_, hr: Int, mn: Int ->
                    cal.set(Calendar.HOUR_OF_DAY, hr)
                    cal.set(Calendar.MINUTE, mn)
                    tmString = formatter.format(cal.time)
                }, cal[Calendar.HOUR_OF_DAY], cal[Calendar.MINUTE], true
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
            OutlinedTextField(
                value = numSmall,
                singleLine = true,
                onValueChange = {
                    if (it.toDoubleOrNull() != null || it.trim() == "") {
                        numSmall = it
                    }
                },
                label = { Text("Small cases") }
            )
            OutlinedTextField(
                value = numMedium,
                singleLine = true,
                onValueChange = {
                    if (it.toDoubleOrNull() != null || it.trim() == "") {
                        numMedium = it
                    }
                },
                label = { Text("Medium cases") }
            )
            OutlinedTextField(
                value = numLarge,
                singleLine = true,
                onValueChange = {
                    if (it.toDoubleOrNull() != null || it.trim() == "") {
                        numLarge = it
                    }
                },
                label = { Text("Large cases") }
            )
            OutlinedTextField(
                value = wtReturned,
                singleLine = true,
                onValueChange = {
                    if (it.toDoubleOrNull() != null || it.trim() == "") {
                        wtReturned = it
                    }
                },
                label = { Text("Weight returned") },
                suffix = { Text("kg") }
            )
        }
        if (catchType == LOBSTER_CRAB) {
            OutlinedTextField(
                value = numLobsterRetained,
                singleLine = true,
                onValueChange = {
                    if (it.toIntOrNull() != null || it.trim() == "") {
                        numLobsterRetained = it
                    }
                },
                label = { Text("Lobsters retained") }
            )
            OutlinedTextField(
                value = numLobsterReturned,
                singleLine = true,
                onValueChange = {
                    if (it.toIntOrNull() != null || it.trim() == "") {
                        numLobsterReturned = it
                    }
                },
                label = { Text("Lobsters returned") }
            )
            OutlinedTextField(
                value = numBrownRetained,
                singleLine = true,
                onValueChange = {
                    if (it.toIntOrNull() != null || it.trim() == "") {
                        numBrownRetained = it
                    }
                },
                label = { Text("Brown crabs retained") }
            )
            OutlinedTextField(
                value = numBrownReturned,
                singleLine = true,
                onValueChange = {
                    if (it.toIntOrNull() != null || it.trim() == "") {
                        numBrownReturned = it
                    }
                },
                label = { Text("Brown crabs returned") }
            )
            OutlinedTextField(
                value = numVelvetRetained,
                singleLine = true,
                onValueChange = {
                    if (it.toIntOrNull() != null || it.trim() == "") {
                        numVelvetRetained = it
                    }
                },
                label = { Text("Velvet crabs retained") }
            )
            OutlinedTextField(
                value = numVelvetReturned,
                singleLine = true,
                onValueChange = {
                    if (it.toIntOrNull() != null || it.trim() == "") {
                        numVelvetReturned = it
                    }
                },
                label = { Text("Velvet crabs returned") }
            )
        }
        Button(
            onClick = {},
        ) {
            Text("Submit")
        }
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

@Composable
fun TrackSwitch(tracking: Boolean, toggleFun: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = stringResource(R.string.tracking))
        Spacer(modifier = Modifier.padding(start = 8.dp))
        Switch(
            checked = tracking,
            onCheckedChange = { toggleFun() }
        )
    }
}

@Composable
fun MapButton(mapFun: () -> Unit) {
    Button(
        onClick = mapFun
    ) {
        Text(text = stringResource(R.string.view_map))
    }
}

@Composable
fun TrackControls(
    tracking: Boolean,
    toggleFun: () -> Unit,
    mapFun: () -> Unit
) {
    Column (
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Row (){
            Text(
                text = "Location",
                style = MaterialTheme.typography.headlineMedium)
        }
        Row {
            TrackSwitch(tracking, toggleFun)
            Spacer(modifier = Modifier.padding(start = 16.dp))
            MapButton(mapFun)
        }
    }
}




