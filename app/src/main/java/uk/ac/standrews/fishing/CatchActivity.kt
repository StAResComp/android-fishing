package uk.ac.standrews.fishing

import android.Manifest
import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.PermissionChecker
import androidx.lifecycle.LiveData
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uk.ac.standrews.fishing.fishing.FullCatch
import java.text.DateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.reflect.KSuspendFunction0

val CATCH_TYPES = arrayOf("Nephrops", "Lobster/Crab", "Wrasse")
val NEPHROPS = CATCH_TYPES[0]
val LOBSTER_CRAB = CATCH_TYPES[1]
val WRASSE = CATCH_TYPES[2]

/**
 * Catch Activity. Where users toggle tracking and view/enter details of day's catch
 */
class CatchActivity : ComponentActivity() {

    private val catchViewModel: CatchViewModel by viewModels {
        CatchViewModelFactory((application as FishingApplication).repository)
    }

    @SuppressLint("MissingPermission")
    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {isGranted ->
        if (isGranted) { }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            this.FishingLayout()
        }
    }

    @Composable
    fun FishingLayout() {
        requestPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        if (
            PermissionChecker.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PermissionChecker.PERMISSION_GRANTED
        ) {
            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    SendCatches(
                        this@CatchActivity.catchViewModel.numUnsubmittedCatches,
                        this@CatchActivity.catchViewModel::postCatches
                    )
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    CatchForm(this@CatchActivity.catchViewModel::insertFullCatch)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    CatchList(this@CatchActivity.catchViewModel.allFullCatches)
                }
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
        numVelvetReturned: Int,
        numWrasseRetained: Int,
        numWrasseReturned: Int
    ) -> Unit) {


    val formatter = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.UK)

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    var catchType by remember { mutableStateOf("Select catch type") }

    var stringId by remember { mutableStateOf(TextFieldValue("")) }
    val stringIdError = stringId.text.trim().isEmpty()

    var lat by remember { mutableStateOf(0.0) }
    var lon by remember { mutableStateOf(0.0) }
    var latDeg by remember { mutableStateOf(TextFieldValue("0")) }
    var latMin by remember { mutableStateOf(TextFieldValue("0")) }
    var latSec by remember { mutableStateOf(TextFieldValue("0.0")) }
    var latDir by remember { mutableStateOf("N") }
    var lonDeg by remember { mutableStateOf(TextFieldValue("0")) }
    var lonMin by remember { mutableStateOf(TextFieldValue("0")) }
    var lonSec by remember { mutableStateOf(TextFieldValue("0.0")) }
    var lonDir by remember { mutableStateOf("W") }
    var cal by remember { mutableStateOf(Calendar.getInstance()) }
    var now = cal.time
    var tmString by remember { mutableStateOf(formatter.format(now)) }
    var numSmall by remember { mutableStateOf(TextFieldValue("0.0")) }
    var numMedium by remember { mutableStateOf(TextFieldValue("0.0")) }
    var numLarge by remember { mutableStateOf(TextFieldValue("0.0")) }
    var wtReturned by remember { mutableStateOf(TextFieldValue("0.0")) }
    var numLobsterRetained by remember { mutableStateOf(TextFieldValue("0")) }
    var numLobsterReturned by remember { mutableStateOf(TextFieldValue("0")) }
    var numBrownRetained by remember { mutableStateOf(TextFieldValue("0")) }
    var numBrownReturned by remember { mutableStateOf(TextFieldValue("0")) }
    var numVelvetRetained by remember { mutableStateOf(TextFieldValue("0")) }
    var numVelvetReturned by remember { mutableStateOf(TextFieldValue("0")) }
    var numWrasseRetained by remember { mutableStateOf(TextFieldValue("0")) }
    var numWrasseReturned by remember { mutableStateOf(TextFieldValue("0")) }

    var getCurrentLocation by remember { mutableStateOf(false) }

    DisposableEffect(getCurrentLocation) {
        scope.launch(Dispatchers.IO) {
            val result = locationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()
            result?.let { fetchedLocation ->
                lat = fetchedLocation.latitude
                lon = fetchedLocation.longitude
                val latDegMinSec = coordsDecimalToDegrees(lat)
                latDeg = TextFieldValue(latDegMinSec[0].toString())
                latMin = TextFieldValue(latDegMinSec[1].toString())
                latSec = TextFieldValue(String.format("%.2f", latDegMinSec[2]))
                val lonDegMinSec = coordsDecimalToDegrees(lon)
                lonDeg = TextFieldValue(lonDegMinSec[0].toString())
                lonMin = TextFieldValue(lonDegMinSec[1].toString())
                lonSec = TextFieldValue(String.format("%.2f", lonDegMinSec[2]))
            }
        }
        onDispose {
            getCurrentLocation = false
        }
    }

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
            label = { Text("String ID") },
            isError = stringIdError,
            supportingText = {
                if (stringIdError) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "String ID must be entered",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            trailingIcon = {
                if (stringIdError)
                    Icon(Icons.Filled.Warning,"error", tint = MaterialTheme.colorScheme.error)
            },
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
                            if (chkNum(it.text)) { latDeg = it }
                        },
                        label = { Text("Deg")},
                        modifier = Modifier
                            .width(72.dp)
                            .padding(horizontal = 2.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = coordError(latDeg, 90)
                    )
                    OutlinedTextField(
                        value = latMin,
                        onValueChange = {
                            if (chkNum(it.text)) { latMin = it }
                        },
                        label = { Text("Min")},
                        modifier = Modifier
                            .width(72.dp)
                            .padding(horizontal = 2.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = coordError(latMin, 60)
                    )
                    OutlinedTextField(
                        value = latSec,
                        onValueChange = {
                            if (chkNum(it.text, false)) { latSec = it }
                        },
                        label = { Text("Sec")},
                        modifier = Modifier
                            .width(80.dp)
                            .padding(horizontal = 2.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = coordError(latSec, 60, false)
                    )
                    var latDirExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = latDirExpanded,
                        onExpandedChange = { latDirExpanded = !latDirExpanded })
                    {
                        OutlinedTextField(
                            readOnly = true,
                            value = latDir,
                            singleLine = true,
                            onValueChange = {},
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = expanded
                                )
                            },
                           modifier = Modifier
                               .menuAnchor()
                               .width(80.dp),
                            label = { Text("Dir") }
                        )
                        ExposedDropdownMenu(
                            expanded = latDirExpanded,
                            onDismissRequest = { latDirExpanded = false }
                        ) {
                            arrayOf("N", "S").forEach {
                                DropdownMenuItem(
                                    text = { Text(it) },
                                    onClick = {
                                        latDirExpanded = false
                                        latDir = it
                                    }
                                )

                            }
                        }
                    }
                }
                Row (
                    verticalAlignment = Alignment.CenterVertically
                ){
                    Text("Lon", modifier = Modifier.padding(horizontal = 2.dp))
                    OutlinedTextField(
                        value = lonDeg,
                        onValueChange = {
                            if (chkNum(it.text)) { lonDeg = it }
                        },
                        label = { Text("Deg")},
                        modifier = Modifier
                            .width(72.dp)
                            .padding(horizontal = 2.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = coordError(lonDeg, 180)
                    )
                    OutlinedTextField(
                        value = lonMin,
                        onValueChange = {
                            if (chkNum(it.text)) { lonMin = it }
                        },
                        label = { Text("Min")},
                        modifier = Modifier
                            .width(72.dp)
                            .padding(horizontal = 2.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = coordError(lonMin, 60)
                    )
                    OutlinedTextField(
                        value = lonSec,
                        onValueChange = {
                            if (chkNum(it.text, false)) { lonSec = it }
                        },
                        label = { Text("Sec")},
                        modifier = Modifier
                            .width(80.dp)
                            .padding(horizontal = 2.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = coordError(lonSec, 60, false)
                    )
                    var lonDirExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = lonDirExpanded,
                        onExpandedChange = { lonDirExpanded = !lonDirExpanded })
                    {
                        OutlinedTextField(
                            readOnly = true,
                            value = lonDir,
                            singleLine = true,
                            onValueChange = {},
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = expanded
                                )
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .width(80.dp),
                            label = { Text("Dir") }
                        )
                        ExposedDropdownMenu(
                            expanded = lonDirExpanded,
                            onDismissRequest = { lonDirExpanded = false }
                        ) {
                            arrayOf("E", "W").forEach {
                                DropdownMenuItem(
                                    text = { Text(it) },
                                    onClick = {
                                        lonDirExpanded = false
                                        lonDir = it
                                    }
                                )

                            }
                        }
                    }
                }
            }
            Column {
                Button(
                    onClick = {
                        getCurrentLocation = true
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
                enabled = true,
                onValueChange = {},
                label = { Text("Time")},
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .onFocusChanged {
                        if (it.isFocused) timePickerDialog.show()
                    }
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
                        if (chkNum(it.text, false)) { numSmall = it }
                    },
                    modifier = Modifier
                        .width(144.dp)
                        .onFocusChanged {
                            if (!it.hasFocus && numSmall.text
                                    .trim()
                                    .isEmpty()
                            ) {
                                numSmall = numSmall.copy("0.0")
                            }
                        },
                    label = { Text("Small cases") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = numMedium,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it.text, false)) { numMedium = it }
                    },
                    modifier = Modifier
                        .width(144.dp)
                        .onFocusChanged {
                            if (!it.hasFocus && numMedium.text
                                    .trim()
                                    .isEmpty()
                            ) {
                                numMedium = numMedium.copy("0.0")
                            }
                        },
                    label = { Text("Medium cases") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = numLarge,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it.text, false)) { numLarge = it }
                    },
                    modifier = Modifier
                        .width(144.dp)
                        .onFocusChanged {
                            if (!it.hasFocus && numLarge.text
                                    .trim()
                                    .isEmpty()
                            ) {
                                numLarge = numLarge.copy("0.0")
                            }
                        },
                    label = { Text("Large cases") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            OutlinedTextField(
                value = wtReturned,
                singleLine = true,
                onValueChange = {
                    if (chkNum(it.text, false)) { wtReturned = it }
                },
                modifier = Modifier.onFocusChanged {
                    if(!it.hasFocus && wtReturned.text.trim().isEmpty()) {
                        wtReturned = wtReturned.copy("0.0")
                    }
                },
                label = { Text("Weight returned") },
                suffix = { Text("kg") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
        else if (catchType == LOBSTER_CRAB) {
            Row {
                OutlinedTextField(
                    value = numLobsterRetained,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it.text)) { numLobsterRetained = it }
                    },
                    modifier = Modifier.onFocusChanged {
                        if(!it.hasFocus && numLobsterRetained.text.trim().isEmpty()) {
                            numLobsterRetained = numLobsterRetained.copy("0.0")
                        }
                    },
                    label = { Text("Lobsters retained") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = numLobsterReturned,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it.text)) { numLobsterReturned = it }
                    },
                    modifier = Modifier.onFocusChanged {
                        if(!it.hasFocus && numLobsterReturned.text.trim().isEmpty()) {
                            numLobsterReturned = numLobsterReturned.copy("0.0")
                        }
                    },
                    label = { Text("Lobsters returned") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            Row {
                OutlinedTextField(
                    value = numBrownRetained,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it.text)) { numBrownRetained = it }
                    },
                    modifier = Modifier.onFocusChanged {
                        if(!it.hasFocus && numBrownRetained.text.trim().isEmpty()) {
                            numBrownRetained = numBrownRetained.copy("0.0")
                        }
                    },
                    label = { Text("Brown crabs retained") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = numBrownReturned,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it.text)) { numBrownReturned = it }
                    },
                    label = { Text("Brown crabs returned") },
                    modifier = Modifier.onFocusChanged {
                        if(!it.hasFocus && numBrownReturned.text.trim().isEmpty()) {
                            numBrownReturned = numBrownReturned.copy("0.0")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            Row {
                OutlinedTextField(
                    value = numVelvetRetained,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it.text)) { numVelvetRetained = it }
                    },
                    label = { Text("Velvet crabs retained") },
                    modifier = Modifier.onFocusChanged {
                        if(!it.hasFocus && numVelvetRetained.text.trim().isEmpty()) {
                            numVelvetRetained = numVelvetRetained.copy("0.0")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = numVelvetReturned,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it.text)) { numVelvetReturned = it }
                    },
                    label = { Text("Velvet crabs returned") },
                    modifier = Modifier.onFocusChanged {
                        if(!it.hasFocus && numVelvetReturned.text.trim().isEmpty()) {
                            numVelvetReturned = numVelvetReturned.copy("0.0")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

            }
        }
        else if (catchType == WRASSE) {
            Row {
                OutlinedTextField(
                    value = numWrasseRetained,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it.text)) {
                            numWrasseRetained = it
                        }
                    },
                    modifier = Modifier.onFocusChanged {
                        if (!it.hasFocus && numWrasseRetained.text.trim()
                                .isEmpty()
                        ) {
                            numWrasseRetained = numWrasseRetained.copy("0.0")
                        }
                    },
                    label = { Text("Wrasse retained") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = numWrasseReturned,
                    singleLine = true,
                    onValueChange = {
                        if (chkNum(it.text)) {
                            numWrasseReturned = it
                        }
                    },
                    modifier = Modifier.onFocusChanged {
                        if (!it.hasFocus && numWrasseReturned.text.trim()
                                .isEmpty()
                        ) {
                            numWrasseReturned = numWrasseReturned.copy("0.0")
                        }
                    },
                    label = { Text("Wrasse returned") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        }
        if (catchType == LOBSTER_CRAB || catchType == NEPHROPS || catchType == WRASSE) {
            Button(
                onClick = {
                    lat = coordsDegreesToDecimal(
                        latDeg.text.toInt(),
                        latMin.text.toInt(),
                        latSec.text.toDouble(),
                        latDir == "N"
                    )
                    lon = coordsDegreesToDecimal(
                        lonDeg.text.toInt(),
                        lonMin.text.toInt(),
                        lonSec.text.toDouble(),
                        lonDir == "E"
                    )
                    onSubmit(
                        catchType,
                        stringId.text,
                        lat,
                        lon,
                        cal.time,
                        numSmall.text.toDouble(),
                        numMedium.text.toDouble(),
                        numLarge.text.toDouble(),
                        wtReturned.text.toDouble(),
                        numLobsterRetained.text.toInt(),
                        numLobsterReturned.text.toInt(),
                        numBrownRetained.text.toInt(),
                        numBrownReturned.text.toInt(),
                        numVelvetRetained.text.toInt(),
                        numVelvetReturned.text.toInt(),
                        numWrasseRetained.text.toInt(),
                        numWrasseReturned.text.toInt()
                    )
                    catchType = "Select catch type"
                    stringId = TextFieldValue("")
                    getCurrentLocation = true
                    cal = Calendar.getInstance()
                    now = cal.time
                    tmString = formatter.format(now)
                    numSmall = TextFieldValue("0.0")
                    numMedium = TextFieldValue("0.0")
                    numLarge = TextFieldValue("0.0")
                    wtReturned = TextFieldValue("0.0")
                    numLobsterRetained = TextFieldValue("0")
                    numLobsterReturned = TextFieldValue("0")
                    numBrownRetained = TextFieldValue("0")
                    numBrownReturned = TextFieldValue("0")
                    numVelvetRetained = TextFieldValue("0")
                    numVelvetReturned = TextFieldValue("0")
                    numWrasseRetained = TextFieldValue("0")
                    numWrasseReturned = TextFieldValue("0")
                },
                enabled = !stringIdError
            ) {
                Text("Submit")
            }
        }
    }
}

fun chkNum(numString: String, isInt: Boolean = true): Boolean {
    if (isInt) {
        return (numString.toIntOrNull() != null && numString.toInt() >= 0) || numString.trim().isEmpty()
    } else {
        return (numString.toDoubleOrNull() != null && numString.toDouble() >= 0.0) || numString.trim().isEmpty()
    }
}

fun coordError(textValue: TextFieldValue, max: Int, isInt: Boolean = true): Boolean {
    if (textValue.text.trim().isEmpty()) {
       return true
    }
    else if (isInt) {
        return (
            !chkNum(textValue.text) ||
                textValue.text.toInt() < 0 ||
                textValue.text.toInt() >= max
            )
    }
    else {
        return (
            !chkNum(textValue.text, false) ||
                textValue.text.toDouble() < 0 ||
                textValue.text.toDouble() >= max
            )
    }
}

@Composable
fun SendCatches(numCatches: LiveData<Int>, postCatches: () -> Unit) {
    Column (
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically
        ) {
            val nc = numCatches.observeAsState()
            nc.let {
                if (it.value != null && (it.value as Int) > 0) {
                    Text(text = "${it.value} catches to be uploaded")
                    Spacer(modifier = Modifier.width(4.dp))
                    Button(
                        onClick = {
                            postCatches()
                        }
                    ) {
                        Text("Upload now")
                    }
                }
            }
        }

    }
}

@Composable
fun CatchList(catches: LiveData<List<FullCatch>>) {
    Column (
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text("Previous Catches", style = MaterialTheme.typography.headlineMedium)
        val catchesState = catches.observeAsState()
        catchesState.value?.let {
            LazyColumn {
                items(it.reversed()) {
                    Card(
                        shape = RectangleShape,
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 4.dp
                        ),
                        modifier = Modifier
                            .width(480.dp)
                            .padding(4.dp)
                    ) {
                       Column {
                           Row (
                               verticalAlignment = Alignment.CenterVertically,
                               horizontalArrangement = Arrangement.SpaceBetween,
                               modifier = Modifier
                                   .fillMaxWidth()
                                   .padding(4.dp)
                           ){
                               val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.UK)
                               Text(
                                   text = formatter.format(it.timestamp),
                                   style = MaterialTheme.typography.headlineSmall
                               )
                               val lat = coordsDecimalToDegrees(it.lat)
                               val ns = if (lat[3] == 1) "N" else "S"
                               val lon = coordsDecimalToDegrees(it.lon)
                               val ew = if (lat[3] == 1) "E" else "W"
                               Column (
                                   horizontalAlignment = Alignment.End
                               ){
                                   Text("${lat[0]}º ${lat[1]}' ${String.format("%.2f",lat[2])}\" $ns")
                                   Text("${lon[0]}º ${lon[1]}' ${String.format("%.2f",lon[2])}\" $ew")
                               }
                           }
                           Divider()
                           if (it.nephropsId != null) {
                               Row (
                                   verticalAlignment = Alignment.CenterVertically,
                                   horizontalArrangement = Arrangement.SpaceBetween,
                                   modifier = Modifier
                                       .fillMaxWidth()
                                       .padding(4.dp)
                               ) {
                                   Text("Nehprops")
                                   Text("String: ${it.stringNum}")
                               }
                               Row (
                                   verticalAlignment = Alignment.CenterVertically,
                                   horizontalArrangement = Arrangement.SpaceBetween,
                                   modifier = Modifier
                                       .fillMaxWidth()
                                       .padding(4.dp)
                               ) {
                                   Column {
                                       Text("Small cases: ${it.numSmallCases}")
                                       Text("Medium cases: ${it.numMediumCases}")
                                       Text("Large cases: ${it.numLargeCases}")
                                   }
                                   Column {
                                       Text("Weight returned: ${it.wtReturned}kg")
                                   }
                               }
                           }
                           else if (it.lobsterCrabId != null) {
                               Row (
                                   verticalAlignment = Alignment.CenterVertically,
                                   horizontalArrangement = Arrangement.SpaceBetween,
                                   modifier = Modifier
                                       .fillMaxWidth()
                                       .padding(4.dp)
                               ) {
                                   Text("Lobster/Crab")
                                   Text("String: ${it.stringNum}")
                               }
                               Row (
                                   verticalAlignment = Alignment.CenterVertically,
                                   horizontalArrangement = Arrangement.SpaceBetween,
                                   modifier = Modifier
                                       .fillMaxWidth()
                                       .padding(4.dp)
                               ) {
                                   Column {
                                       Text("Lobsters retained: ${it.numlobsterRetained}")
                                       Text("Brown crabs retained: ${it.numBrownRetained}")
                                       Text("Velvet crabs retained: ${it.numVelvetRetained}")
                                   }
                                   Column {
                                       Text("Lobsters returned: ${it.numlobsterReturned}")
                                       Text("Brown crabs returned: ${it.numBrownReturned}")
                                       Text("Velvet crabs returned: ${it.numVelvetReturned}")
                                   }
                               }
                           }
                           else if (it.wrasseId != null) {
                               Row (
                                   verticalAlignment = Alignment.CenterVertically,
                                   horizontalArrangement = Arrangement.SpaceBetween,
                                   modifier = Modifier
                                       .fillMaxWidth()
                                       .padding(4.dp)
                               ) {
                                   Text("Wrasse")
                                   Text("String: ${it.stringNum}")
                               }
                               Row (
                                   verticalAlignment = Alignment.CenterVertically,
                                   horizontalArrangement = Arrangement.SpaceBetween,
                                   modifier = Modifier
                                       .fillMaxWidth()
                                       .padding(4.dp)
                               ) {
                                   Column {
                                       Text("Wrasse retained: ${it.numWrasseRetained}")
                                   }
                                   Column {
                                       Text("Wrasse returned: ${it.numWrasseReturned}")
                                   }
                               }
                           }
                           else {
                               Row (
                                   verticalAlignment = Alignment.CenterVertically,
                                   horizontalArrangement = Arrangement.SpaceBetween,
                                   modifier = Modifier
                                       .fillMaxWidth()
                                       .padding(4.dp)
                               ) {
                                   Text("Unknown")
                                   Text("String: ${it.stringNum}")
                               }
                           }
                       }
                    }
                }
            }
        }
    }
}

fun coordsDecimalToDegrees(decimalCoord: Double): Array<Number> {
    var northOrEast = 0
    if (decimalCoord >= 0.0) {
        northOrEast = 1
    }
    val degrees = floor(abs(decimalCoord)).toInt()
    val minutes = floor((abs(decimalCoord) - degrees) * 60.0).toInt()
    val seconds = (abs(decimalCoord) - degrees - minutes / 60.0) * 3600.0
    return arrayOf(degrees, minutes, seconds, northOrEast)
}

fun coordsDegreesToDecimal(
    degrees: Int,
    minutes: Int,
    seconds: Double = 0.0,
    eastNorth: Boolean
): Double {
    val absVal = degrees + minutes / 60.0 + seconds / 3600.0
    return if (eastNorth) absVal else absVal * -1
}





