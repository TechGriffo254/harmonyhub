package com.techgriffo254.harmonyhub.domain.model

import retrofit2.http.Url

data class Artist(
    val artistId: String,
    val name: String,
    val website: String,
    val joinDate: String,
    val imageURl: String,
    val shortUrl: String,
    val shareUrl: String
)
