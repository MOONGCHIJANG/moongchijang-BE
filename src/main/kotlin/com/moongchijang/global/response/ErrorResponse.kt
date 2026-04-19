package com.moongchijang.global.response

data class ErrorResponse(
    val code: String,
    val message: String,
    val detail: String?
)
