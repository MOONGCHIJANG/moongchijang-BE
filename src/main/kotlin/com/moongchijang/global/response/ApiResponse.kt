package com.moongchijang.global.response

import com.moongchijang.global.exception.ErrorCode

data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ErrorResponse?
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> =
            ApiResponse(success = true, data = data, error = null)

        fun success(): ApiResponse<Nothing> =
            ApiResponse(success = true, data = null, error = null)

        fun fail(errorCode: ErrorCode, errorMessage: String? = null): ApiResponse<Nothing> =
            ApiResponse(
                success = false,
                data = null,
                error = ErrorResponse(
                    code = errorCode.code,
                    message = errorCode.message,
                    errorMessage = errorMessage
                )
            )
    }
}
