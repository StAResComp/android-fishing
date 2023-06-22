package uk.ac.standrews.fishing.network

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import uk.ac.standrews.fishing.db.FullCatch

private const val BASE_URL = "https://fishing.st-andrews.ac.uk/rifg/"
private val interceptor = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
private val client = OkHttpClient.Builder().addInterceptor(interceptor).build()
private val retrofit = Retrofit.Builder()
    .client(client)
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BASE_URL)
    .build()

class CatchesToPost(
    val deviceId: String,
    val catches: List<FullCatch>
) {
    fun getCatchIds(): List<Int> {
        return this.catches.map {it.id}
    }
}

interface FishingApiService {
    @POST("app")
    fun postCatches(@Body body: CatchesToPost): Call<Response<Any>>
}

object FishingApi {
    val retrofitService: FishingApiService by lazy {
        retrofit.create(FishingApiService::class.java)
    }
}
