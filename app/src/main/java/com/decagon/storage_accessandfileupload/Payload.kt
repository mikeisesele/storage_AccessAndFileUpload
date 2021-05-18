package com.decagon.storage_accessandfileupload

data class Payload (
    val downloadUri: String,
    val fileId: String,
    val filename: String,
    val fileType: String,
    val uploadStatus: Boolean
)