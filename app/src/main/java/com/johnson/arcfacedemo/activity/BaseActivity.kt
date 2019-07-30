package com.ruiting.exhibition.activity.base

import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.blankj.utilcode.util.AdaptScreenUtils


/**
 * BaseActivity是所有Activity的基类，把一些公共的方法放到里面，如基础样式设置，权限封装，网络状态监听等
 * 2019年6月18日16:48:22
 */
abstract class BaseActivity : AppCompatActivity(), LifecycleOwner {

    lateinit var mactivity: Activity


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 隐藏标题栏
        supportActionBar?.hide()
//        hideBottomUIMenu()

        // 沉浸效果
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // 透明状态栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // 透明导航栏
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }*/

        // 添加到Activity工具类
//        ActivityUtil.getInstance().addActivity(this, javaClass)
        //保持亮屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mactivity = this

        // 执行初始化方法
        setContentView(initView())
        setupViews()
        loadData()

    }

    override fun onResume() {
        super.onResume()
        val resources = this.resources
        val configuration = resources.configuration
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }

    /**
     * 隐藏虚拟按键
     */
    private fun hideBottomUIMenu() {
        //隐藏虚拟按键，并且全屏
        if (Build.VERSION.SDK_INT > 11 && Build.VERSION.SDK_INT < 19) { // lower api
            window.decorView.systemUiVisibility = View.GONE
        } else if (Build.VERSION.SDK_INT >= 19) {
            //for new api versions.
            val params = window.attributes
            params.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
            window.attributes = params

        }
    }


    /**
     * 权限检查方法，false代表没有该权限，ture代表有该权限
     */
    fun hasPermission(vararg permissions: String): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }


    /**
     * 权限请求方法
     */
    fun requestPermission(code: Int, vararg permissions: String) {
        ActivityCompat.requestPermissions(this, permissions, code)
    }

    /**
     * 处理请求权限结果事件
     *
     * @param requestCode  请求码
     * @param permissions  权限组
     * @param grantResults 结果集
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        doRequestPermissionsResult(requestCode, grantResults)
    }

    /**
     * 处理请求权限结果事件
     *
     * @param requestCode  请求码
     * @param grantResults 结果集
     */
    open fun doRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {}

    /**
     * 使用AndroidUtilCode框架做屏幕适配
     * @return
     */
    override fun getResources(): Resources {
        return AdaptScreenUtils.adaptWidth(super.getResources(), 1920)
    }

    /**
     * 设置view
     */
    open fun setupViews() {}

    /**
     * 初始化
     * 设置布局文件
     *
     * @return
     */
    abstract fun initView(): Int

    /**
     * 懒加载数据
     */
    open fun loadData() {}
}
