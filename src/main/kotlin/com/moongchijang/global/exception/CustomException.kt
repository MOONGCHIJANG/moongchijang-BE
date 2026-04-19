package com.moongchijang.global.exception

class CustomException(
    val errorCode: ErrorCode,
    val detail: String? = null
) : RuntimeException(errorCode.message)
