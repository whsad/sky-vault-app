package com.computer.skyvault.service

import com.computer.skyvault.common.dto.LoginRequest
import com.computer.skyvault.common.vo.LoginInfoVo
import com.computer.skyvault.common.vo.R
import com.computer.skyvault.utils.ApiClient
import com.computer.skyvault.utils.DataUtil

object AccountService {

    private const val PREFIX = "account"

    fun login(
        req: LoginRequest,
        onSuccess: (R<LoginInfoVo>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        ApiClient.sendOkHttpRequest<R<LoginInfoVo>>(
            path = "$PREFIX/login",
            method = ApiClient.POST,
            body = DataUtil.toJsonRequestBody(req),
            onFailure = { error ->
                onFailure(error)
            },
            onResponse = { response ->
                onSuccess(response)
            }
        )
    }
}