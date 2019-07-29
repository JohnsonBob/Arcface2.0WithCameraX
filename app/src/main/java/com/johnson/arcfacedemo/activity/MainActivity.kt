package com.johnson.arcfacedemo.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import com.johnson.arcfacedemo.R
import com.ruiting.exhibition.activity.base.BaseActivity

import android.Manifest
import android.graphics.*
import android.util.Size
import android.os.Handler
import android.os.HandlerThread
import android.util.Rational
import android.view.Surface
import android.view.ViewGroup
import androidx.camera.core.*
import kotlinx.android.synthetic.main.activity_main.*
import com.johnson.arcfacedemo.utils.ImageUtil
import java.io.ByteArrayOutputStream



class MainActivity : BaseActivity() {
    override fun initView(): Int = R.layout.activity_main
    private var lensFacing = CameraX.LensFacing.BACK

    override fun setupViews() {

        viewFinder.post { startCamera() }

        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

    }

    fun startCamera() {
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetAspectRatio(Rational(1, 1))
            setTargetResolution(Size(viewFinder.width, viewFinder.height))
        }.setLensFacing(lensFacing).build()

        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        CameraX.bindToLifecycle(this, buildImageAnalysisUseCase(), preview)

    }


    fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when (viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)
    }

    fun buildImageAnalysisUseCase(): ImageAnalysis {
        // 分析器配置 Config 的建造者
        val analyzerThread = HandlerThread("LuminosityAnalysis").apply { start() }
        val analysisConfig = ImageAnalysisConfig.Builder()
            // 分辨率
            .setTargetResolution(Size(viewFinder.width, viewFinder.height))
            // 宽高比例
            .setTargetAspectRatio(Rational(viewFinder.width, viewFinder.height))
            // 图像渲染模式
            .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            // 设置回调的线程
            .setCallbackHandler(Handler(analyzerThread.looper))
            .build()

        // 创建分析器 ImageAnalysis 对象
        val analysis = ImageAnalysis(analysisConfig)

        // setAnalyzer 传入实现了 analyze 接口的类
        analysis.setAnalyzer { image, rotationDegrees ->
            // 可以得到的一些图像信息，参见 ImageProxy 类相关方法
            val rect = image.cropRect
            val width = image.width
            val height = image.height

//            val buffer = image.planes[0].buffer
//            val bytes = ByteArray(buffer.remaining())
//            buffer.get(bytes)

            val bytes1 = ImageUtil.getBytesFromImageAsType(image.image, ImageUtil.NV21)

            val yuv = YuvImage(bytes1, ImageFormat.NV21, width, height, null)
            val ops = ByteArrayOutputStream()
            yuv.compressToJpeg(rect, 60, ops)
            val data = ops.toByteArray()
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)

            val a = 10

        }
        return analysis
    }
}
