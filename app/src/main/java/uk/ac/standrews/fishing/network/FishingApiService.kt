package uk.ac.standrews.fishing.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import uk.ac.standrews.fishing.db.FullCatch

private const val BASE_URL = "https://fishing.st-andrews.ac.uk"
private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BASE_URL)
    .build()

class CatchesToPost(
    private val deviceId: String,
    private val catches: List<FullCatch>
) {
    fun getCatchIds(): List<Int> {
        return this.catches.map {it.id}
    }
}

interface FishingApiService {
    @POST("catches")
    suspend fun postCatches(@Body body: CatchesToPost): String
}

object FishingApi {
    val retrofitService: FishingApiService by lazy {
        retrofit.create(FishingApiService::class.java)
    }
}
