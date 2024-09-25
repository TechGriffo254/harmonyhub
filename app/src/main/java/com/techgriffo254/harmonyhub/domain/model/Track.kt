package com.techgriffo254.harmonyhub.domain.model

data class Track(
    val id: String,
    val name: String,
    val artistName: String,
    val albumName: String,
    val audio: String,
    val image: String
)