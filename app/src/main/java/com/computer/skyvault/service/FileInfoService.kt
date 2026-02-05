package com.computer.skyvault.service

import com.computer.skyvault.common.dto.LoadFileListRequest
import com.computer.skyvault.common.recycleitem.FileItem
import com.computer.skyvault.common.vo.FileInfoVo
import com.computer.skyvault.common.vo.PageData
import com.computer.skyvault.common.vo.PageResponseResult
import com.computer.skyvault.common.vo.R
import com.computer.skyvault.utils.ApiClient
import com.computer.skyvault.utils.DataUtil
import okhttp3.Request

object FileInfoService {

    private const val PREFIX = "file"

    fun loadFileList(req: LoadFileListRequest,
                     addHeaders: (Request.Builder) -> Unit = {},
                     onSuccess: (PageResponseResult<FileItem>) -> Unit,
                     onFailure: (String) -> Unit) {
        ApiClient.sendOkHttpRequest<PageResponseResult<FileItem>>(
            path = "$PREFIX/loadFileList",
            method = ApiClient.POST,
            body = DataUtil.toJsonRequestBody(req),
            addHeaders = addHeaders,
            onFailure = { error ->
                onFailure(error)
            }
        ) { response ->
            onSuccess(response)
        }
    }
}