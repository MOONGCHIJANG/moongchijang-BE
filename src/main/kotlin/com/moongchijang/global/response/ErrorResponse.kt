package com.moongchijang.global.response

data class ErrorResponse(
    val code: Int,
    val message: String,
    val errorMessage: String?
)
