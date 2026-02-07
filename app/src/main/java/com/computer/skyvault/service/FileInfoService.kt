package com.computer.skyvault.service

import com.computer.skyvault.common.dto.LoadFileListRequest
import com.computer.skyvault.common.dto.NewFolderRequest
import com.computer.skyvault.common.recycleitem.FileItem
import com.computer.skyvault.common.vo.PageResponseResult
import com.computer.skyvault.utils.ApiClient
import com.computer.skyvault.utils.DataUtil
import okhttp3.Request

private const val TAG = "FileInfoService"

object FileInfoService {

    private const val PREFIX = "file"

    fun loadFileList(
        req: LoadFileListRequest,
        addHeaders: (Request.Builder) -> Unit = {},
        onSuccess: (PageResponseResult<FileItem>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        ApiClient.sendOkHttpRequest<PageResponseResult<FileItem>>(
            path = "$PREFIX/loadFileList",
            method = ApiClient.POST,
            body = DataUtil.toJsonRequestBody(req),
            addHeaders = addHeaders,
            onFailure = { error ->
                onFailure(error)
            },
            onResponse = { response ->
                onSuccess(response)
            }
        )
    }

    fun uploadFile(){

    }

    fun getFileCoverPath(
        fileCoverPath: String
    ): String {
        val normalizedPath = fileCoverPath.replace("\\\\", "/").replace("\\", "/")
        return "${ApiClient.BASE_URL}$PREFIX/getFileCover/$normalizedPath"
    }

    fun newFolder(
        req: NewFolderRequest,
        addHeaders: (Request.Builder) -> Unit = {},
        onSuccess: (PageResponseResult<FileItem>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        ApiClient.sendOkHttpRequest<PageResponseResult<FileItem>>(
            path = "$PREFIX/newFolder",
            method = ApiClient.POST,
            body = DataUtil.toJsonRequestBody(req),
            addHeaders = addHeaders,
            onFailure = { error ->
                onFailure(error)
            },
            onResponse = { response ->
                onSuccess(response)
            }
        )
    }
}