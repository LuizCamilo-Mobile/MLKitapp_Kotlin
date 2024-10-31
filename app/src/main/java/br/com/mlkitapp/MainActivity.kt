package br.com.mlkitapp

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import br.com.mlkitapp.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.jvm.Throws

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var imageCapture: Uri? = null
    private lateinit var imageLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeImageLauncher()
        initializeCameraLaucher()
        setCaptureImageButtonListener()
    }

    private fun initializeImageLauncher() {
        imageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                hanImageResult(uri)
            }
    }

    private fun hanImageResult(uri: Uri?) {
        uri ?: return showToast("Nenhuma imagem selecionada")
        imageCapture = uri
        binding.imageCapture.setImageURI(uri)
        recognizeTextFromImagem(uri)
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun recognizeTextFromImagem(uri: Uri) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromFilePath(this, uri)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
               displayRecognizedText(visionText.text)
            }
            .addOnFailureListener{ e ->
                handleTextRecognitionFailure(e)
            }
    }

    private fun displayRecognizedText(text: String) {
        if (text.isBlank()) {
            showToast("Nenhum texto encontrado")
        } else {
            binding.textResult.text = text
        }
    }

    private fun handleTextRecognitionFailure(e: Exception) {
        showToast("Erro ao reconhecer o texto: ${e.localizedMessage}")
        e.printStackTrace()
    }

    private fun initializeCameraLaucher() {
        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    imageCapture?.let { uri ->
                        binding.imageCapture.setImageURI(uri)
                        recognizeTextFromImagem(uri)
                    }
                } else {
                    showToast("Erro ao capturar a imagem")
                }
            }
    }

    private fun setCaptureImageButtonListener() {
        binding.takePicture.setOnClickListener {
            checkCameraPermissionAndOpenCamera()
        }
    }

    private fun checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION
                )
            } else {
                openCamera()
        }
    }

    private fun openCamera() {
        val imageUri = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.resolveActivity(packageManager)?.also {
            val photoFile: File? = try {
                createImageFile()
            } catch (e: IOException) {
                null
            }
            photoFile?.also {
                val photoUri: Uri = FileProvider.getUriForFile(
                    this,
                    "${applicationContext.packageName}.provider",it
                )
                imageCapture = photoUri
                cameraLauncher.launch(photoUri)
            }
        }
    }
    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 101
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyHHdd", Locale.getDefault()).format(Date())
        val storeDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storeDir
        ).apply {
            imageCapture = Uri.fromFile(this)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                showToast("Permissão de câmera negada")
            }
        }
    }
}





