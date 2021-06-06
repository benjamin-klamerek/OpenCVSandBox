package com.klamerek.opencvsandbox

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.klamerek.opencvsandbox.databinding.ActivityCameraBinding
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var binding: ActivityCameraBinding

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        manageCameraPermission()

        cameraExecutor = Executors.newSingleThreadExecutor()
        binding.cameraCaptureButton.setOnClickListener {
            binding.cameraPreview.visibility = View.GONE
            takePicture()
        }
    }

    private fun manageCameraPermission() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    fun sendImageAsResult(bitmap: Bitmap, rotationDegrees: Int) {
        binding.cameraPreview.visibility = View.VISIBLE
        val closingIntent = Intent()
        ImageActivityChannel.image = bitmap
        closingIntent.putExtra("imageRotation", rotationDegrees.toFloat())
        setResult(RESULT_OK, closingIntent)
        finish()
    }

    class MyImageCapturedCallback(val cameraActivity: CameraActivity) :
        ImageCapture.OnImageCapturedCallback() {

        @ExperimentalGetImage
        override fun onCaptureSuccess(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image

            if (mediaImage != null) {
                val buffer: ByteBuffer = mediaImage.planes[0].buffer
                val bytes = ByteArray(buffer.capacity())
                buffer.get(bytes)
                val bitmapImage: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
                imageProxy.close()
                cameraActivity.sendImageAsResult(bitmapImage, imageProxy.imageInfo.rotationDegrees)
            }
        }

        override fun onError(exception: ImageCaptureException) {
            Log.e(TAG, "Use case binding failed", exception)
        }
    }

    private fun takePicture() {
        val imageCapture = imageCapture ?: return
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            MyImageCapturedCallback(this)
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
        }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

}