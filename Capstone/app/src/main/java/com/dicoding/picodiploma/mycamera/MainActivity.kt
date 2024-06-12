package com.dicoding.picodiploma.mycamera

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.dicoding.picodiploma.mycamera.ViewModel.MyViewModel
import com.dicoding.picodiploma.mycamera.databinding.ActivityMainBinding
import java.io.File
import java.io.InputStream
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var gestureDetector: GestureDetector
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var audioManager: AudioManager
    private lateinit var viewModel: MyViewModel
    private lateinit var resultTextView: TextView
    private var currentImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(MyViewModel::class.java)
        resultTextView = binding.resultTextView
        textToSpeech = TextToSpeech(this, this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                startCamera()
                return true
            }
        })
        val buttonPredict = binding.buttonPredict
        buttonPredict.setOnClickListener {
            currentImageUri?.let {
                val file = uriToFile(it)
                viewModel.predictImage(file)
            } ?: run {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
            viewModel.predictResult.observe(this, Observer { response ->
                response?.let {
                    resultTextView.text = "Result: ${it.data.result}\nSuggestion: ${it.data.suggestion}"
                } ?: run {
                    resultTextView.text = "No result from API"
                }
            })
            viewModel.error.observe(this, Observer { errorMessage ->
                errorMessage?.let {
                    Log.e("API_ERROR", it)
                    resultTextView.text = "Error: $it"
                }
            })
        }
    }
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("id", "ID"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Bahasa tidak didukung", Toast.LENGTH_SHORT).show()
            } else {
                speakOut("Double tap untuk membuka kamera")
            }
        } else {
            Toast.makeText(this, "Inisialisasi TextToSpeech gagal", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }

    private fun speakOut(text: String) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event!!) || super.onTouchEvent(event)
    }

    private fun startCamera() {
        currentImageUri = getImageUri(this)
        launcherIntentCamera.launch(currentImageUri!!)
    }

    private val launcherIntentCamera = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            showImage()
        }
    }

    private fun showImage() {
        currentImageUri?.let {
            Log.d("Image URI", "showImage: $it")
            binding.previewImageView.setImageURI(it)
        }
    }
    private fun uriToFile(uri: Uri): File {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("image", ".jpg", cacheDir)
        tempFile.outputStream().use { outputStream ->
            inputStream?.copyTo(outputStream)
        }
        return tempFile
    }
}
