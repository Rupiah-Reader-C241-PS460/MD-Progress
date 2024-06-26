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
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.tts.UtteranceProgressListener
import android.view.View
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.dicoding.picodiploma.mycamera.CameraActivity.Companion.CAMERAX_RESULT

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var gestureDetector: GestureDetector
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var audioManager: AudioManager
    lateinit var viewModel: MyViewModel
    private lateinit var resultTextView: TextView
    private var currentImageUri: Uri? = null
    private lateinit var progressBar: ProgressBar

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permission request granted", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Permission request denied", Toast.LENGTH_LONG).show()
            }
        }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(
            this,
            REQUIRED_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!allPermissionsGranted()) {
            requestPermissionLauncher.launch(REQUIRED_PERMISSION)
        }

        progressBar = findViewById(R.id.progressBar)
        viewModel = ViewModelProvider(this).get(MyViewModel::class.java)
        resultTextView = binding.resultTextView
        textToSpeech = TextToSpeech(this, this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                startCameraX()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                super.onLongPress(e)
                currentImageUri?.let {
                    val file = uriToFile(it)
                    viewModel.predictImage(file)
                } ?: run {
                    Toast.makeText(this@MainActivity, "No image selected", Toast.LENGTH_SHORT).show()
                }
            }
        })

        viewModel.predictResult.observe(this, Observer { response ->
            progressBar.visibility = android.view.View.GONE
            response?.let {
                val resultText = "Hasil Uang adalah : ${it.data.result} Rupiah"
                binding.resultTextView.text = resultText
                speakOut(resultText + "...........Ketuk layar dua kali jika ingin mengambil gambar lagi")
            } ?: run {
                resultTextView.text = "No result from API"
            }
        })
        viewModel.error.observe(this, Observer { errorMessage ->
            progressBar.visibility = android.view.View.GONE
            errorMessage?.let {
                Log.e("API_ERROR", it)
                resultTextView.text = "Error: $it"
            }
        })
        viewModel.isLoading.observe(this, Observer { isLoading ->
            if (isLoading) {
                progressBar.visibility = View.VISIBLE // Show ProgressBar
                speakOut("Diproses")
            } else {
                progressBar.visibility = View.GONE // Hide ProgressBar
            }
        })
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale("id", "ID"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Bahasa tidak didukung", Toast.LENGTH_SHORT).show()
            } else {
                speakOut("Ketuk layar dua kali untuk membuka kamera")
            }

            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    // Do nothing
                }

                override fun onDone(utteranceId: String?) {
                    // Teks selesai dibacakan, hilangkan teks dari TextView
                    runOnUiThread {
                        binding.resultTextView.text = ""
                    }
                }

                override fun onError(utteranceId: String?) {
                    // Handle error here
                }
            })
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

    fun speakOut(text: String) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0)

        val params = HashMap<String, String>()
        params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "utteranceId"

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, params)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event!!) || super.onTouchEvent(event)
    }

    private fun startCameraX() {
        val intent = Intent(this, CameraActivity::class.java)
        launcherIntentCameraX.launch(intent)
    }

    private val launcherIntentCameraX = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == CAMERAX_RESULT) {
            currentImageUri = it.data?.getStringExtra(CameraActivity.EXTRA_CAMERAX_IMAGE)?.toUri()
            showImage()
        }
    }

    private fun showImage() {
        currentImageUri?.let {
            Log.d("Image URI", "showImage: $it")
            binding.previewImageView.setImageURI(it)
            // Tambahkan audio setelah gambar ditampilkan
            speakOut("Tahan layar selama 2 detik untuk mengetahui nominal uang")
        }
    }

    fun uriToFile(uri: Uri): File {
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        val tempFile = File.createTempFile("image", ".jpg", cacheDir)
        tempFile.outputStream().use { outputStream ->
            inputStream?.copyTo(outputStream)
        }
        return tempFile
    }

    companion object {
        private const val REQUIRED_PERMISSION = Manifest.permission.CAMERA
    }
}
