package uk.ac.standrews.fishing

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
import java.util.Date

class CatchViewModel (private val repository: CatchRepository): ViewModel() {

    val allFullCatches: LiveData<List<FullCatch>> = repository.allFullCatches.asLiveData()

    fun insertCatch(aCatch: Catch) = viewModelScope.launch {
        repository.insertCatch(aCatch)
    }

    fun insertNephropsCatch(nephropsCatch: NephropsCatch) = viewModelScope.launch {
        repository.insertNephropsCatch(nephropsCatch)
    }

    fun insertLobsterCrabCatch(lobsterCrabCatch: LobsterCrabCatch) = viewModelScope.launch {
        repository.insertLobsterCrabCatch(lobsterCrabCatch)
    }

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
        numVelvetReturned: Int
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
