package com.techgriffo254.harmonyhub.data
import com.techgriffo254.harmonyhub.domain.model.Track
import retrofit2.http.GET
import retrofit2.http.Query

interface JamendoApiService {
    @GET("tracks/")
    suspend fun getTracks(
        @Query("client_id") clientId: String ,
        @Query("access_token") accessToken: String,
        @Query("format") format: String = "json",
        @Query("audioformat") audioFormat: String = "mp3"
    ): TracksResponse

}

data class TracksResponse(
    val results: List<Track>
)
