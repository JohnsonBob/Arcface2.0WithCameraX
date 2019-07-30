package com.johnson.arcfacedemo.arcface.utils.face


import com.arcsoft.face.FaceFeature

interface FaceListener {
    /**
     * 当出现异常时执行
     *
     * @param e 异常信息
     */
    fun onFail(e: Exception)


    /**
     * 请求人脸特征后的回调
     *
     * @param faceFeature    人脸特征数据
     * @param requestId 请求码
     */
    fun onFaceFeatureInfoGet(faceFeature: FaceFeature?, requestId: Int?)
}
