package com.dicoding.picodiploma.mycamera.data.retrofit

import com.dicoding.picodiploma.mycamera.data.response.DetectorResponse
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("predict")
    suspend fun predictImage(
        @Part image: MultipartBody.Part
    ): DetectorResponse
}