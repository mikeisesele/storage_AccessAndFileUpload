package com.decagon.storage_accessandfileupload

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ImageUploadService {
    @Multipart
    @POST("upload")
    fun uploadImage(@Part image: MultipartBody.Part): Call <PhotoFormat>

    object RetrofitService {
        // initiate retrofit instance
        val retrofit: ImageUploadService by lazy {
            Retrofit.Builder()
                .baseUrl("https://darot-image-upload-service.herokuapp.com/api/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ImageUploadService::class.java)
        }
    }
}
