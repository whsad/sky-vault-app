package com.computer.skyvault.common.vo

data class BaseResponse(
    val code: Int,
    val message: String,
    val timestamp: String
)

data class PageData<T>(
    val list: List<T>,
    val total: Int,
    val page_no: Int,
    val page_size: Int,
    val has_more: Boolean
)

data class PageResponseResult<T>(
    val code: Int,
    val message: String,
    val timestamp: String,
    val data: PageData<T>?
)

data class ResponseResult<T>(
    val code: Int,
    val message: String,
    val timestamp: String,
    val data: T?
)

typealias R<T> = ResponseResult<T>