package com.dicoding.picodiploma.mycamera.ViewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dicoding.picodiploma.mycamera.data.response.DetectorResponse
import com.dicoding.picodiploma.mycamera.data.retrofit.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class MyViewModel : ViewModel() {

    private val _predictResult = MutableLiveData<DetectorResponse?>()
    val predictResult: LiveData<DetectorResponse?> = _predictResult

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun predictImage(file: File) {
        val requestBody = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
        val multipartBody = MultipartBody.Part.createFormData("image", file.name, requestBody)

        _isLoading.postValue(true) // Show ProgressBar
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = ApiConfig.instance.predictImage(multipartBody)
                _predictResult.postValue(response)
            } catch (e: Exception) {
                _error.postValue(e.message)
            } finally {
                _isLoading.postValue(false) // Hide ProgressBar
            }
        }
    }
}