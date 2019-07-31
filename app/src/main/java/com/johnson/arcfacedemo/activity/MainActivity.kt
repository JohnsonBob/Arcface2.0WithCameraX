package com.johnson.arcfacedemo.activity

import android.Manifest
import android.content.pm.PackageManager
import com.johnson.arcfacedemo.R
import com.ruiting.exhibition.activity.base.BaseActivity

import android.graphics.*
import android.util.Size
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Rational
import android.view.Surface
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.camera.core.*
import com.arcsoft.face.*
import com.johnson.arcfacedemo.arcface.faceserver.CompareResult
import com.johnson.arcfacedemo.arcface.faceserver.FaceServer
import com.johnson.arcfacedemo.arcface.model.DrawInfo
import com.johnson.arcfacedemo.arcface.utils.ConfigUtil
import com.johnson.arcfacedemo.arcface.utils.DrawHelper
import com.johnson.arcfacedemo.arcface.utils.face.FaceHelper
import com.johnson.arcfacedemo.arcface.utils.face.FaceListener
import com.johnson.arcfacedemo.arcface.utils.face.RequestFeatureStatus
import com.johnson.arcfacedemo.common.Constants
import kotlinx.android.synthetic.main.activity_main.*
import com.johnson.arcfacedemo.utils.ImageUtil
import java.io.ByteArrayOutputStream
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread


class MainActivity : BaseActivity(), ViewTreeObserver.OnGlobalLayoutListener {

    override fun initView(): Int = R.layout.activity_main
    private var lensFacing = CameraX.LensFacing.BACK
    /**
     * 所需的所有权限信息
     */
    private val NEEDED_PERMISSIONS =
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_STATE
        )
    private val ACTION_REQUEST_PERMISSIONS = 0x001
    private var faceEngine: FaceEngine? = null
    private var afCode = -1
    private val MAX_DETECT_NUM = 5
    private val TAG = "MainActivity"
    private var faceHelper: FaceHelper? = null
    private var previeWidth = -1
    private var previeHeight = -1
    private val requestFeatureStatusMap = ConcurrentHashMap<Int, Int>()
    private val SIMILAR_THRESHOLD = 0.8f
    private var compareResultList: MutableList<CompareResult> = arrayListOf()
    private var drawHelper: DrawHelper? = null
    private var isGetFaceId = false
    /**
     * 活体检测的开关
     */
    private val livenessDetect = false


    private var tempWidth = -1
    private var tempHeight = -1
    private var tempdata: ByteArray? = null
    private var temprect: Rect? = null
    private var firstOne = true
    private var tempbitmap: Bitmap? = null


    override fun setupViews() {
        val faceEngine = FaceEngine()
        val activeCode = faceEngine.active(this, Constants.APP_ID, Constants.SDK_KEY)

        //本地人脸库初始化
        FaceServer.getInstance().init(this)
        //在布局结束后才做初始化操作
        tv_viewFinder.getViewTreeObserver().addOnGlobalLayoutListener(this)

    }

    /**
     * 初始化相机
     */
    fun startCamera() {
        try {
            val previewConfig = PreviewConfig.Builder().apply {
                setTargetAspectRatio(Rational(1, 1))
                setTargetResolution(Size(tv_viewFinder.width, tv_viewFinder.height))
            }.setLensFacing(lensFacing).build()

            // Build the viewfinder use case
            val preview = Preview(previewConfig)

            // Every time the viewfinder is updated, recompute layout
            preview.setOnPreviewOutputUpdateListener {

                // To update the SurfaceTexture, we have to remove it and re-add it
                val parent = tv_viewFinder.parent as ViewGroup
                parent.removeView(tv_viewFinder)
                parent.addView(tv_viewFinder, 0)

                tv_viewFinder.surfaceTexture = it.surfaceTexture
                updateTransform()

//                previeWidth = it.textureSize.width
//                previeHeight = it.textureSize.height
//                initFaceHelp()
            }


            CameraX.bindToLifecycle(this, buildImageAnalysisUseCase(), preview)
        } catch (e: Exception) {
            Toast.makeText(this, "打开相机失败!", Toast.LENGTH_LONG).show()
        }

    }


    fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = tv_viewFinder.width / 2f
        val centerY = tv_viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when (tv_viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        tv_viewFinder.setTransform(matrix)
    }

    fun buildImageAnalysisUseCase(): ImageAnalysis {
        // 分析器配置 Config 的建造者
        val analyzerThread = HandlerThread("LuminosityAnalysis").apply { start() }
        val analysisConfig = ImageAnalysisConfig.Builder()
            // 分辨率
//            .setTargetResolution(Size(tv_viewFinder.width, tv_viewFinder.height))
            // 宽高比例
//            .setTargetAspectRatio(Rational(tv_viewFinder.width, tv_viewFinder.height))
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
            temprect = image.cropRect
//            val type = image.format
//            previeWidth = image.width
//            previeHeight = image.height
            tempWidth = image.width
            tempHeight = image.height
            if (firstOne) {
                firstOne = false
                previeWidth = tempWidth
                previeHeight = tempHeight
                initFaceHelp()
            }

//            val buffer = image.planes[0].buffer
//            val bytes = ByteArray(buffer.remaining())
//            buffer.get(bytes)
            tempdata = ImageUtil.getBytesFromImageAsType(image.image, ImageUtil.NV21)
            doGetFaceCode(tempdata, tempWidth, tempHeight, temprect)


        }
        return analysis
    }

    private fun doGetFaceCode(data: ByteArray?, width: Int, height: Int, rect: Rect?) {
        if (isGetFaceId || data == null || rect == null) return
        thread {
            isGetFaceId = true


//            val yuv = YuvImage(data, ImageFormat.NV21, width, height, null)
//            val ops = ByteArrayOutputStream()
//            yuv.compressToJpeg(rect, 60, ops)
//            val data1 = ops.toByteArray()
//            tempbitmap = BitmapFactory.decodeByteArray(data1, 0, data.size)
//
//            val a = 10


            face_rect_view.clearFaceInfo()

            val facePreviewInfoList = faceHelper?.onPreviewFrame(data)
            if (facePreviewInfoList != null && drawHelper != null) {
                val drawInfoList = ArrayList<DrawInfo>()
                for (i in facePreviewInfoList.indices) {
                    val name = faceHelper?.getName(facePreviewInfoList[i].trackId)
                    drawInfoList.add(
                        DrawInfo(
                            facePreviewInfoList[i].faceInfo.rect,
                            GenderInfo.UNKNOWN,
                            AgeInfo.UNKNOWN_AGE,
                            LivenessInfo.UNKNOWN,
                            name ?: facePreviewInfoList[i].trackId.toString()
                        )
                    )
                }
                drawHelper?.draw(face_rect_view, drawInfoList)
            }

            if(facePreviewInfoList != null){
                for (i in facePreviewInfoList.indices) {
                    faceHelper?.requestFaceFeature(
                        data,
                        facePreviewInfoList.get(i).faceInfo,
                        width,
                        height,
                        FaceEngine.CP_PAF_NV21,
                        facePreviewInfoList.get(i).trackId
                    )
                }
            }

            isGetFaceId = false
        }
    }

    override fun onGlobalLayout() {
        tv_viewFinder.getViewTreeObserver().removeOnGlobalLayoutListener(this)
        if (hasPermission(*NEEDED_PERMISSIONS)) {
            requestPermission(ACTION_REQUEST_PERMISSIONS, *NEEDED_PERMISSIONS)
        } else {
            initEngine()
            initCamera()
        }
    }

    override fun doRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode == ACTION_REQUEST_PERMISSIONS) {
            var isAllGranted = true
            for (grantResult in grantResults) {
                isAllGranted = isAllGranted and (grantResult == PackageManager.PERMISSION_GRANTED)
            }
            if (isAllGranted) {
                initEngine()
                initCamera()
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initCamera() {
        tv_viewFinder.post { startCamera() }

        tv_viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }

    }

    /**
     * 在相机初始化后初始化人脸识别工具
     */
    private fun initFaceHelp() {
        //如果相机未初始化
        if (previeWidth == -1 || previeHeight == -1) return
        val faceListener = object : FaceListener {
            override fun onFail(e: Exception) {
                Log.e(TAG, "onFail: " + e.message)
            }

            override fun onFaceFeatureInfoGet(faceFeature: FaceFeature?, requestId: Int?) {
                //FR成功
                if (faceFeature != null) {
                    Log.e(
                        TAG,
                        "onPreview: fr end = " + System.currentTimeMillis() + " trackId = " + requestId
                    )

                    //不做活体检测的情况，直接搜索
                    if (!livenessDetect) {
                        searchFace(faceFeature, requestId)
                    }

                } else {
                    requestFeatureStatusMap[requestId!!] = RequestFeatureStatus.FAILED
                }//FR 失败
            }


        }
//        val matrix = Matrix()
//        tv_viewFinder.getTransform(matrix)
//
//        var a: FloatArray = FloatArray(10)
//        matrix.getValues(a)

        drawHelper = DrawHelper(
            previeWidth,
            previeHeight,
            face_rect_view.width,
            face_rect_view.height,
            tv_viewFinder.display.rotation,
            0,
            false
        )

        faceHelper = FaceHelper.Builder()
            .faceEngine(faceEngine)
            .frThreadNum(MAX_DETECT_NUM)
            .previewSize(previeWidth, previeHeight)
            .faceListener(faceListener)
            .currentTrackId(ConfigUtil.getTrackId(this@MainActivity.getApplicationContext()))
            .build()
    }

    /**
     * 初始化引擎
     */
    private fun initEngine() {
        faceEngine = FaceEngine()
        afCode = faceEngine?.init(
            this,
            FaceEngine.ASF_DETECT_MODE_VIDEO,
            ConfigUtil.getFtOrient(this),
            16,
            MAX_DETECT_NUM,
            FaceEngine.ASF_FACE_RECOGNITION or FaceEngine.ASF_FACE_DETECT or FaceEngine.ASF_LIVENESS
        ) ?: -1
        val versionInfo = VersionInfo()
        faceEngine?.getVersion(versionInfo)
        Log.i(TAG, "initEngine:  init: $afCode  version:$versionInfo")

        if (afCode != ErrorInfo.MOK) {
            Toast.makeText(this, getString(R.string.init_failed, afCode), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 销毁引擎
     */
    private fun unInitEngine() {
        faceEngine ?: return
        if (afCode == ErrorInfo.MOK) {
            afCode = faceEngine!!.unInit()
            Log.i(TAG, "unInitEngine: $afCode")
        }
    }

    override fun onDestroy() {
        //faceHelper中可能会有FR耗时操作仍在执行，加锁防止crash
        if (faceHelper != null) {
            synchronized(faceHelper!!) {
                unInitEngine()
            }
            ConfigUtil.setTrackId(this, faceHelper?.getCurrentTrackId())
            faceHelper?.release()
        } else {
            unInitEngine()
        }

        FaceServer.getInstance().unInit()

        super.onDestroy()
    }

    private fun searchFace(frFace: FaceFeature, requestId: Int?) {
        /*Observable
            .create(ObservableOnSubscribe<Any> { emitter ->
                //                        Log.i(TAG, "subscribe: fr search start = " + System.currentTimeMillis() + " trackId = " + requestId);
                val compareResult = FaceServer.getInstance().getTopOfFaceLib(frFace)
                //                        Log.i(TAG, "subscribe: fr search end = " + System.currentTimeMillis() + " trackId = " + requestId);
                if (compareResult == null) {
//                    emitter.onError(null)
                } else {
                    emitter.onNext(compareResult)
                }
            })
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : Observer<CompareResult> {


                override fun onSubscribe(d: Disposable) {

                }

                override fun onNext(compareResult: CompareResult) {
                    if (compareResult == null || compareResult!!.userName == null) {
                        requestFeatureStatusMap.put(requestId!!, RequestFeatureStatus.FAILED)
                        faceHelper?.addName(requestId, "VISITOR $requestId")
                        return
                    }

                    //                        Log.i(TAG, "onNext: fr search get result  = " + System.currentTimeMillis() + " trackId = " + requestId + "  similar = " + compareResult.getSimilar());
                    if (compareResult!!.similar > SIMILAR_THRESHOLD) {
                        var isAdded = false
                        if (compareResultList == null) {
                            requestFeatureStatusMap.put(requestId!!, RequestFeatureStatus.FAILED)
                            faceHelper?.addName(requestId, "VISITOR $requestId")
                            return
                        }
                        for (compareResult1 in compareResultList) {
                            if (compareResult1.trackId === requestId) {
                                isAdded = true
                                break
                            }
                        }
                        if (!isAdded) {
                            //对于多人脸搜索，假如最大显示数量为 MAX_DETECT_NUM 且有新的人脸进入，则以队列的形式移除
                            if (compareResultList.size >= MAX_DETECT_NUM) {
                                compareResultList.removeAt(0)

                            }
                            //添加显示人员时，保存其trackId
                            compareResult?.trackId = requestId
                            compareResultList.add(compareResult)

                        }
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.SUCCEED)
                        faceHelper?.addName(requestId, compareResult!!.userName)

                    } else {
                        requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED)
                        faceHelper?.addName(requestId, "VISITOR $requestId")
                    }
                }

                override fun onError(e: Throwable) {
                    requestFeatureStatusMap.put(requestId, RequestFeatureStatus.FAILED)
                }

                override fun onComplete() {

                }
            })*/
        val a = 10
    }

}
