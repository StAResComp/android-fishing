package uk.ac.standrews.fishing

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import uk.ac.standrews.fishing.fishing.Catch
import uk.ac.standrews.fishing.fishing.FullCatch
import uk.ac.standrews.fishing.fishing.LobsterCrabCatch
import uk.ac.standrews.fishing.fishing.NephropsCatch
import uk.ac.standrews.fishing.fishing.WrasseCatch
import uk.ac.standrews.fishing.network.CatchesToPost
import uk.ac.standrews.fishing.network.FishingApi
import java.net.SocketTimeoutException
import java.util.Calendar
import java.util.Date

class CatchViewModel (private val repository: CatchRepository): ViewModel() {

    val allFullCatches: LiveData<List<FullCatch>> = repository.allFullCatches.asLiveData()

    fun insertFullCatch(
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
        numWrasseReturned: Int,
    ) = viewModelScope.launch {
        val aCatch = Catch(
            stringId = stringId, lat = lat, lon = lon, timestamp = timestamp
        )
        val catchId = repository.insertCatch(aCatch)
        if (catchType == NEPHROPS) {
            val nephropsCatch = NephropsCatch(
                catchId = catchId.toInt(),
                numSmallCases = numSmall,
                numMediumCases = numMedium,
                numLargeCases = numLarge,
                wtReturned = wtReturned
            )
            repository.insertNephropsCatch(nephropsCatch)
        }
        else if (catchType == LOBSTER_CRAB) {
            val lobsterCrabCatch = LobsterCrabCatch(
                catchId = catchId.toInt(),
                numLobsterRetained = numLobsterRetained,
                numLobsterReturned = numLobsterReturned,
                numBrownRetained = numBrownRetained,
                numBrownReturned = numBrownReturned,
                numVelvetRetained = numVelvetRetained,
                numVelvetReturned = numVelvetReturned
            )
            repository.insertLobsterCrabCatch(lobsterCrabCatch)
        }
        else if (catchType == WRASSE) {
            val wrasseCatch = WrasseCatch(
                catchId = catchId.toInt(),
                numWrasseRetained = numWrasseRetained,
                numWrasseReturned = numWrasseReturned
            )
            repository.insertWrasseCatch(wrasseCatch)
        }
        this@CatchViewModel.postCatches()
    }

    private suspend fun postCatches() {

        val catchesToPost = CatchesToPost("DEVICE_1", this@CatchViewModel.repository.unsubmittedFullCatches())
        val ids = catchesToPost.getCatchIds()
        try {
            val submissionResult = FishingApi.retrofitService.postCatches(catchesToPost)
            Log.d("API",submissionResult)
            if (false) { //Needs to check submissionResult
               ids.forEach() {
                   val aCatch = this@CatchViewModel.repository.getCatch(it)
                   val cal = Calendar.getInstance()
                   aCatch.uploaded = cal.time
                   this@CatchViewModel.repository.updateCatch(aCatch)
               }
            }
        }
        catch (e: SocketTimeoutException) {
            Log.e("API", "Connection timed out")
        }
    }
}

class CatchViewModelFactory(private val repository: CatchRepository): ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CatchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CatchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
