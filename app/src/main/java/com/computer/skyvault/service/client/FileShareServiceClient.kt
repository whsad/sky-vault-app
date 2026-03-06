package com.computer.skyvault.service.client

import com.computer.skyvault.common.dto.ShareFileRequest
import com.computer.skyvault.common.vo.R
import com.computer.skyvault.model.FileShare
import com.computer.skyvault.utils.ApiClient
import com.computer.skyvault.utils.DataUtils
import okhttp3.Request

object FileShareServiceClient {

    private const val PREFIX = "share"

    fun shareFile(
        req: ShareFileRequest,
        addHeaders: (Request.Builder) -> Unit = {},
        onSuccess: (R<FileShare>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        ApiClient.sendOkHttpRequest<R<FileShare>>(
            path = "$PREFIX/shareFile",
            method = ApiClient.POST,
            body = DataUtils.toJsonRequestBody(req),
            addHeaders = addHeaders,
            onFailure = onFailure,
            onResponse = onSuccess
        )
    }
}