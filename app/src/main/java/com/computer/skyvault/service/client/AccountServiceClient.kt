package com.computer.skyvault.service.client

import com.computer.skyvault.common.dto.LoginRequest
import com.computer.skyvault.common.vo.LoginInfoVo
import com.computer.skyvault.common.vo.R
import com.computer.skyvault.utils.ApiClient
import com.computer.skyvault.utils.DataUtils

object AccountServiceClient {

    private const val PREFIX = "account"

    fun login(
        req: LoginRequest,
        onSuccess: (R<LoginInfoVo>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        ApiClient.sendOkHttpRequest<R<LoginInfoVo>>(
            path = "$PREFIX/login",
            method = ApiClient.POST,
            body = DataUtils.toJsonRequestBody(req),
            onFailure = { error ->
                onFailure(error)
            },
            onResponse = { response ->
                onSuccess(response)
            }
        )
    }
}