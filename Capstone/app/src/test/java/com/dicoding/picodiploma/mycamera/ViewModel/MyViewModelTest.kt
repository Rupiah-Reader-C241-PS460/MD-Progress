@file:Suppress("DEPRECATION")

package com.dicoding.picodiploma.mycamera.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.dicoding.picodiploma.mycamera.ViewModel.MyViewModel
import com.dicoding.picodiploma.mycamera.data.response.Data
import com.dicoding.picodiploma.mycamera.data.response.DetectorResponse
import com.dicoding.picodiploma.mycamera.data.retrofit.ApiConfig
import com.dicoding.picodiploma.mycamera.data.retrofit.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.MultipartBody
import org.junit.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.File

@ExperimentalCoroutinesApi
class MyViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var apiService: ApiService

    @Mock
    private lateinit var observer: Observer<DetectorResponse?>

    @Mock
    private lateinit var errorObserver: Observer<String?>

    private lateinit var viewModel: MyViewModel

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        Dispatchers.setMain(Dispatchers.Unconfined)
        viewModel = MyViewModel()
        viewModel.predictResult.observeForever(observer)
        viewModel.error.observeForever(errorObserver)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `predictImage should post value to predictResult on success`() = runBlocking {
        val dummyFile = File("uang2.jpg")
        val dummyResponse = DetectorResponse(
            data = Data(
                result = "50000",
                createdAt = "2022-01-01T00:00:00Z",
                suggestion = "Dummy suggestion",
                id = "dummy_id"
            ),
            message = "Success",
            status = "200"
        )

        `when`(apiService.predictImage(any(MultipartBody.Part::class.java))).thenReturn(dummyResponse)

        viewModel.predictImage(dummyFile)

        verify(observer).onChanged(dummyResponse)
        verify(errorObserver, never()).onChanged(anyString())
    }

    @Test
    fun `predictImage should post value to error on failure`() = runBlocking {
        val dummyFile = File("logo_no_bg.png")
        val errorMessage = "Network error"

        `when`(apiService.predictImage(any(MultipartBody.Part::class.java))).thenThrow(RuntimeException(errorMessage))

        viewModel.predictImage(dummyFile)

        verify(errorObserver).onChanged(errorMessage)
        verify(observer, never()).onChanged(any())
    }
}
