package uk.ac.standrews.fishing

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import uk.ac.standrews.fishing.db.Catch
import uk.ac.standrews.fishing.db.CatchRepository
import uk.ac.standrews.fishing.db.FullCatch
import uk.ac.standrews.fishing.db.LobsterCrabCatch
import uk.ac.standrews.fishing.db.NephropsCatch
import uk.ac.standrews.fishing.db.WrasseCatch
import uk.ac.standrews.fishing.network.CatchesToPost
import uk.ac.standrews.fishing.network.FishingApi
import java.lang.Exception
import java.net.SocketTimeoutException
import java.util.Calendar
import java.util.Date

class CatchViewModel (private val repository: CatchRepository): ViewModel() {

    val allFullCatches: LiveData<List<FullCatch>> = repository.allFullCatches.asLiveData()
    val numUnsubmittedCatches: LiveData<Int> = repository.numUnsubmittedCatches.asLiveData()

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
        //this@CatchViewModel.postCatches()
    }
    fun postCatches(deviceId : String) = viewModelScope.launch {
        val catchesToPost = CatchesToPost(deviceId, this@CatchViewModel.repository.unsubmittedFullCatches())
        Log.d("API", "Data to send: ${catchesToPost.toString()}")
        Log.d("API", "Includes ${catchesToPost.catches.count()} catches")
        val ids = catchesToPost.getCatchIds()
        try {
            val submissionResult = FishingApi.retrofitService.postCatches(catchesToPost).enqueue(object:
                Callback<Response<Any>> {
                    override fun onResponse(call: Call<Response<Any>>, response: Response<Response<Any>>) {
                        Log.d("API", "Success! ${response.code()}")
                        if (response.code() == 200) { //Needs to check submissionResult
                            ids.forEach() {
                                viewModelScope.launch {
                                    val aCatch = this@CatchViewModel.repository.getCatch(it)
                                    val cal = Calendar.getInstance()
                                    aCatch.uploaded = cal.time
                                    this@CatchViewModel.repository.updateCatch(aCatch)

                                }
                            }
                        }
                    }
                    override fun onFailure(call: Call<Response<Any>>, t: Throwable) {
                        Log.d("API", "Failure")
                    }
                })
        }
        catch (e: SocketTimeoutException) {
            Log.e("API", "Connection timed out")
        }
        catch (e: Exception) {
            Log.e("API", "Something else happened")
            val msg = e.message
            if (msg != null) {
                Log.e("API", msg)
            }
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
