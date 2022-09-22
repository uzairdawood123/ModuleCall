package com.vdotok.streaming.models

data class CandidateModel(
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val candidate: String
)
