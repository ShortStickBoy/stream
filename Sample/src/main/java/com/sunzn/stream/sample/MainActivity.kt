package com.sunzn.stream.sample

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.sunzn.stream.library.StreamListener
import com.sunzn.stream.library.StreamManager
import com.sunzn.stream.library.bean.Data
import com.sunzn.stream.library.bean.Stream
import com.sunzn.stream.library.help.InstallHelper
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        download()
    }

    private fun download() {
        val url = "https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk"
        StreamManager
            .setTitle("QQ")
            .setDescription("正在下载...")
            .setUrl(url)
            .setFileName("QQ.apk")
            .setListener(object : StreamListener {
                override fun onProgress(value: Data) {
                    Log.e("StreamManager", "下载进度：${value.curSize} | ${value.totSize} | ${value.status}")
                    tv.text = String.format("下载进度：%d%%", value.curSize * 100L / value.totSize)
                }

                override fun onSuccess(bean: Stream) {
                    Log.e("StreamManager", "下载成功：$bean")
                    tv.text = "下载成功"

                    // 7.0 系统不能在intent中包含file :///协议
                    val file = File(bean.localUri.replace("file://", ""))
                    val uri: Uri = FileProvider.getUriForFile(this@MainActivity, applicationContext.packageName + ".provider", file)
                    InstallHelper.install(this@MainActivity, uri)
                }

                override fun onFailure() {
                    Log.e("StreamManager", "下载失败")
                }

            }).exec(this)
    }

}
