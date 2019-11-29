package com.sunzn.stream.library

import android.app.DownloadManager
import android.content.*
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.Handler
import com.sunzn.stream.library.bean.Data
import com.sunzn.stream.library.bean.Stream
import com.sunzn.stream.library.vars.Value
import java.io.File

object StreamManager {

    private var id: Long = 0

    private lateinit var context: Context

    private var url: String = Value.EMPTY

    private var title: String = Value.EMPTY

    private var filename: String = Value.EMPTY

    private var description: String = Value.EMPTY

    private var listener: StreamListener? = null

    private var observer: UpdateObserver? = null

    private val handler: Handler by lazy { Handler() }

    private val receiver: UpdateReceiver by lazy { UpdateReceiver() }

    private val manager: DownloadManager by lazy { context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }

    fun setUrl(url: String): StreamManager {
        this.url = url
        return this
    }

    fun setTitle(title: String): StreamManager {
        this.title = title
        return this
    }

    fun setFileName(filename: String): StreamManager {
        this.filename = filename
        return this
    }

    fun setDescription(description: String): StreamManager {
        this.description = description
        return this
    }

    fun setListener(listener: StreamListener): StreamManager {
        this.listener = listener
        return this
    }

    fun exec(context: Context) {
        this.context = context

        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), filename)

        val request: DownloadManager.Request = DownloadManager.Request(Uri.parse(url))
        request.setAllowedOverRoaming(true)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        request.setTitle(title)
        request.setDescription(description)
        request.setDestinationUri(Uri.fromFile(file))

        id = manager.enqueue(request)
        val uri: Uri? = ContentUris.withAppendedId(Uri.parse("content://downloads/all_downloads"), id)

        if (observer != null) {
            context.contentResolver.unregisterContentObserver(observer!!)
            observer = null
        }
        observer = UpdateObserver(handler)

        if (uri != null) {
            context.contentResolver.registerContentObserver(uri, true, observer!!)
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }

    }

    class UpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val did: Long = intent!!.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (did == id) {
                val query: DownloadManager.Query = DownloadManager.Query().setFilterById(id)
                val cursor: Cursor = manager.query(query)
                if (cursor.moveToFirst()) {
                    val status: Int = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    if (DownloadManager.STATUS_SUCCESSFUL == status) {
                        val title: String = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE))
                        val description: String = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_DESCRIPTION))
                        val url: String = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_URI))
                        val mediaType: String = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE))
                        val localUri: String = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                        val totalSize: Int = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val stream = Stream(title, description, url, filename, localUri, mediaType, totalSize)

                        listener?.onSuccess(stream)
                    } else {
                        listener?.onFailure()
                    }
                } else {
                    listener?.onFailure()
                }
            }
        }
    }

    class UpdateObserver(handler: Handler) : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            if (id != 0L) {
                val values: IntArray = getBytesAndStatus(id)
                val curSize: Int = values[0] // 当前大小
                val totSize: Int = values[1] // 总大小
                val status: Int = values[2]  // 下载状态
                listener?.onProgress(Data(curSize, totSize, status))
            }
        }
    }

    private fun getBytesAndStatus(id: Long): IntArray {
        val bytesAndStatus = intArrayOf(-1, -1, 0)
        val query: DownloadManager.Query = DownloadManager.Query().setFilterById(id)

        try {
            val cursor: Cursor = manager.query(query)
            if (cursor.moveToFirst()) {
                bytesAndStatus[0] = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                bytesAndStatus[1] = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                bytesAndStatus[2] = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return bytesAndStatus
    }

}