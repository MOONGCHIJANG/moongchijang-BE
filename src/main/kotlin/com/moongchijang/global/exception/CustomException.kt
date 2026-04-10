package com.moongchijang.global.exception

class CustomException(
    val errorCode: ErrorCode,
    val errorMessage: String? = null
) : RuntimeException(errorCode.message)
