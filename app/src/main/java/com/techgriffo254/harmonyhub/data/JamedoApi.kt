package com.techgriffo254.harmonyhub.data
import com.techgriffo254.harmonyhub.domain.model.Artist
import com.techgriffo254.harmonyhub.domain.model.Track
import retrofit2.http.GET
import retrofit2.http.Query

interface JamendoApiService {
    @GET("tracks/")
    suspend fun getTracks(
        @Query("client_id") clientId: String ,
        @Query("access_token") accessToken: String,
        @Query("format") format: String = "json",
    ): TracksResponse

    @GET("artist")
    suspend fun getArtist(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json"
    ): Artist


    @GET("tracks/")
    suspend fun searchTracks(
        @Query("access_token") accessToken: String,
        @Query("search") query: String,
        @Query("format") format: String = "json"
    ): TracksResponse

}

data class TracksResponse(
    val results: List<Track>
)
