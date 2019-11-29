package com.sunzn.stream.library.bean

data class Stream(
    var title: String,
    var description: String,
    var url: String,
    var filename: String,
    var localUri: String,
    var mediaType: String,
    var totalSize: Int
)