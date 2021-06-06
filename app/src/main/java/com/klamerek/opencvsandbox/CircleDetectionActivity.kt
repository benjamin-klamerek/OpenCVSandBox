package com.klamerek.opencvsandbox

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.klamerek.opencvsandbox.databinding.ActivityCircleDetectionBinding
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import kotlin.math.roundToInt

class CircleDetectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCircleDetectionBinding
    private lateinit var imageBuffer: Bitmap

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> {
                    ImageActivityChannel.image.let {
                        imageBuffer = it!!
                        binding.circleDetectionImageView.setImageBitmap(it)
                        binding.circleDetectionImageView.rotation =
                            result.data?.getFloatExtra("imageRotation", 0.0F) ?: 0.0F
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCircleDetectionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        OpenCVLoader.initDebug()

        binding.applyCircleDetectionButton.setOnClickListener {
            performCircleDetection()
        }

        binding.takePictureButton.setOnClickListener {
            startForResult.launch(Intent(this, CameraActivity::class.java))
        }

        imageBuffer = BitmapFactory.decodeResource(resources, R.drawable.card_test_1);
        binding.circleDetectionImageView.setImageBitmap(imageBuffer)
    }

    private fun performCircleDetection() {
        val bitmap = imageBuffer.copy(imageBuffer.config, true);

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGB2GRAY)

        Utils.matToBitmap(mat, bitmap)

        Imgproc.medianBlur(mat, mat, 5)

        Utils.matToBitmap(mat, bitmap)

        val rows = mat.rows()
        val circles = Mat()
        Imgproc.HoughCircles(
            mat, circles, Imgproc.HOUGH_GRADIENT, 1.0, rows / 8.0,
            binding.thresholdCannyEdgeSlider.values[0].toDouble(),
            binding.thresholdAccumulatorSlider.values[0].toDouble(),
            binding.circlesSizeSlider.values[0].toInt(),
            binding.circlesSizeSlider.values[1].toInt()
        )

        for (x in 0 until circles.cols()) {
            val c = circles[0, x]
            val center = Point(c[0], c[1])
            // circle center
            Imgproc.circle(mat, center, 1, Scalar(0.0, 100.0, 100.0), 3, 8, 0)
            // circle outline
            val radius = c[2].roundToInt()
            Imgproc.circle(mat, center, radius, Scalar(255.0, 0.0, 255.0), 3, 8, 0)
        }

        Utils.matToBitmap(mat, bitmap)

        binding.circleDetectionImageView.setImageBitmap(bitmap)
    }
}