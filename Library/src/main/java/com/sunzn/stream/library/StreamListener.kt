package com.sunzn.stream.library

import com.sunzn.stream.library.bean.Data
import com.sunzn.stream.library.bean.Stream

interface StreamListener {

    fun onProgress(value: Data)

    fun onSuccess(bean: Stream)

    fun onFailure()

}