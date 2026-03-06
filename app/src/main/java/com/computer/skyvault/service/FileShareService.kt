package com.computer.skyvault.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.lifecycle.LifecycleOwner
import com.computer.skyvault.R
import com.computer.skyvault.common.dto.ShareFileRequest
import com.computer.skyvault.common.enums.ShareValidTypeEnum
import com.computer.skyvault.common.recycleitem.FileItem
import com.computer.skyvault.manager.LoginManager
import com.computer.skyvault.model.FileShare
import com.computer.skyvault.service.client.FileShareServiceClient
import com.computer.skyvault.utils.ApiClient
import com.computer.skyvault.utils.ApiClient.onMain
import com.computer.skyvault.utils.showToast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class FileShareService(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val loginManager: LoginManager,
    private val onRefreshFileList: () -> Unit
) {
    companion object {
        // 提取码相关常量
        private const val EXTRACT_CODE_LENGTH = 4
        private const val EXTRACT_CODE_CHAR_POOL = "0123456789abcdefghijklmnopqrstuvwxyz"

        // 日志TAG
        private const val TAG = "FileShareService"
    }

    /** 当前选中的有效期类型（默认永久有效） */
    private var currentValidType: Int = ShareValidTypeEnum.FOREVER.typeValue

    /** 当前提取码 */
    private var currentExtractCode: String? = null

    /** 是否是自定义提取码 */
    private var isCustomExtractCode: Boolean = false

    /** 是否记住有效期选择 */
    private var isRememberValidType: Boolean = false

    /**
     * 显示分享对话框
     */
    fun showFileShareDialog(fileItem: FileItem) {

        // 非记住状态时，重置有效期为永久
        if (!isRememberValidType) {
            currentValidType = ShareValidTypeEnum.FOREVER.typeValue
        }
//        val dialog = BottomSheetDialog(context, R.style.BottomSheetDialog)
//        val dialogView = LayoutInflater.from(context).inflate(R.layout.module_dialog_share_file, null)
//        dialog.setContentView(dialogView)
//        dialog.behavior.apply {
//            isHideable = true
//            state = BottomSheetBehavior.STATE_EXPANDED
//        }
//        // 防止弹窗外部点击消失（可选，根据业务调整）
//        dialog.setCanceledOnTouchOutside(false)

        val (dialog, dialogView) = createBottomSheetDialog(R.layout.module_dialog_share_file)

        // 绑定控件
        val tvValidType = dialogView.findViewById<TextView>(R.id.tvValidType)
        val layoutValidType = dialogView.findViewById<RelativeLayout>(R.id.layoutValidType)
        val layoutExtractCode = dialogView.findViewById<RelativeLayout>(R.id.layoutExtractCode)
        val tvExtractCode = dialogView.findViewById<TextView>(R.id.tvExtractCode)
        val btnShare = dialogView.findViewById<Button>(R.id.btnShare)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)
        val btnHelp = dialogView.findViewById<ImageView>(R.id.btnHelp)
        // todo 实现自动填充验证码进入分享文件页

//        // 初始化显示
//        tvValidType.text = getValidTypeText(currentValidType)
//        val randomCode = generateRandomCode()
//        currentCode = randomCode
//        tvExtractCode.text = currentCode

        // 初始化UI
        tvValidType.text = getValidTypeText(currentValidType)
        currentExtractCode = generateRandomExtractCode()
        tvExtractCode.text = currentExtractCode

        // 设置点击事件
        setViewClickListener(btnClose) { dialog.dismiss() }
        setViewClickListener(btnCancel) { dialog.dismiss() }

        // 帮助按钮点击事件
        setSingleClickListener(btnHelp) {
            showHelpTooltip(btnHelp)
        }

        // 有效期选择
        setViewClickListener(layoutValidType) {
            showValidTypeSelectorDialog { validType, code ->
                currentValidType = validType
                tvValidType.text = getValidTypeText(validType)
                if (code != null) {
                    currentExtractCode = code
                    tvExtractCode.text = code
                }
            }
        }

        // 提取码设置
        setViewClickListener(layoutExtractCode) {
            showExtractCodeSettingDialog(currentExtractCode, isCustomExtractCode) { code, custom ->
                currentExtractCode = code
                isCustomExtractCode = custom
                tvExtractCode.text = code ?: context.getString(R.string.share_code_unset)
            }
        }

        // 分享按钮
        setSingleClickListener(btnShare) {
            if (currentExtractCode.isNullOrEmpty()) {
                context.getString(R.string.share_code_empty_tip).showToast(context)
                return@setSingleClickListener
            }
            shareFile(fileItem, currentValidType, currentExtractCode!!) {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    /**
     * 显示提取码设置对话框
     * @param currentCode 当前提取码
     * @param isCustom 是否是自定义提取码
     * @param onConfirm 确认回调（提取码，是否自定义）
     */
    private fun showExtractCodeSettingDialog(
        currentCode: String?,
        isCustom: Boolean,
        onConfirm: (String?, Boolean) -> Unit
    ) {
//        val dialogView =
//            LayoutInflater.from(context).inflate(R.layout.module_dialog_extract_code_setting, null)
//        val dialog = BottomSheetDialog(context, R.style.BottomSheetDialog)
//        dialog.setContentView(dialogView)
        val (dialog, dialogView) = createBottomSheetDialog(R.layout.module_dialog_extract_code_setting)

        val btnComplete = dialogView.findViewById<TextView>(R.id.btnComplete)
        val layoutSystemRandom = dialogView.findViewById<RelativeLayout>(R.id.layoutSystemRandom)
        val layoutCustomCode = dialogView.findViewById<RelativeLayout>(R.id.layoutCustomCode)
        val layoutInputArea = dialogView.findViewById<LinearLayout>(R.id.layoutInputArea)
        val etCustomCode = dialogView.findViewById<EditText>(R.id.etCustomCode)
        val checkSystemRandom = dialogView.findViewById<ImageView>(R.id.checkSystemRandom)
        val checkCustomCode = dialogView.findViewById<ImageView>(R.id.checkCustomCode)

        var isSystemRandom = !isCustom
        updateExtractCodeCheckState(
            isSystemRandom,
            checkSystemRandom,
            checkCustomCode,
            layoutInputArea
        )
        if (!isSystemRandom) {
            etCustomCode.setText(currentCode ?: "")
        }

        // 系统随机选择
        setViewClickListener(layoutSystemRandom) {
            isSystemRandom = true
            updateExtractCodeCheckState(
                isSystemRandom,
                checkSystemRandom,
                checkCustomCode,
                layoutInputArea
            )
        }

        // 自定义提取码选择
        setViewClickListener(layoutCustomCode) {
            isSystemRandom = false
            updateExtractCodeCheckState(
                isSystemRandom,
                checkSystemRandom,
                checkCustomCode,
                layoutInputArea
            )
            etCustomCode.requestFocus()
        }


//        if (isSystemRandom) {
//            checkSystemRandom.visibility = View.VISIBLE
//            checkCustomCode.visibility = View.GONE
//            layoutInputArea.visibility = View.GONE
//        } else {
//            checkSystemRandom.visibility = View.GONE
//            checkCustomCode.visibility = View.VISIBLE
//            layoutInputArea.visibility = View.VISIBLE
//            etCustomCode.setText(currentCode ?: "")
//        }

//        layoutSystemRandom.setOnClickListener {
//            isSystemRandom = true
//            checkSystemRandom.visibility = View.VISIBLE
//            checkCustomCode.visibility = View.GONE
//            layoutInputArea.visibility = View.GONE
//        }
//
//        layoutCustomCode.setOnClickListener {
//            isSystemRandom = false
//            checkSystemRandom.visibility = View.GONE
//            checkCustomCode.visibility = View.VISIBLE
//            layoutInputArea.visibility = View.VISIBLE
//            etCustomCode.requestFocus()
//        }

        setSingleClickListener(btnComplete) {
            val code = if (isSystemRandom) {
                generateRandomExtractCode()
            } else {
                etCustomCode.text.toString().trim()
            }

            if (!isSystemRandom && code.isEmpty()) {
                context.getString(R.string.share_code_custom_empty_tip).showToast(context)
                return@setSingleClickListener
            }

            onConfirm(code, !isSystemRandom)
            dialog.dismiss()
        }

        dialog.show()
    }


    /**
     * 显示有效期选择对话框
     */
    fun showValidTypeSelectorDialog(onConfirm: (Int, String?) -> Unit) {
//        val dialogView =
//            LayoutInflater.from(context).inflate(R.layout.module_dialog_valid_type_selector, null)
//        val dialog = BottomSheetDialog(context, R.style.BottomSheetDialog)
//        dialog.setContentView(dialogView)

        val (dialog, dialogView) = createBottomSheetDialog(R.layout.module_dialog_valid_type_selector)

        // 绑定控件
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        val swRemember = dialogView.findViewById<SwitchCompat>(R.id.swRemember)
        val layoutRemember = dialogView.findViewById<RelativeLayout>(R.id.layoutRemember)

        // 有效期选项布局和勾选控件
        val validTypeLayouts = mapOf(
            ShareValidTypeEnum.SEVEN_DAYS.typeValue to dialogView.findViewById<RelativeLayout>(R.id.layout7Days),
            ShareValidTypeEnum.FOURTEEN_DAYS.typeValue to dialogView.findViewById<RelativeLayout>(R.id.layout14Days),
            ShareValidTypeEnum.THIRTY_DAYS.typeValue to dialogView.findViewById<RelativeLayout>(R.id.layout30Days),
            ShareValidTypeEnum.ONE_YEAR.typeValue to dialogView.findViewById<RelativeLayout>(R.id.layout365Days),
            ShareValidTypeEnum.FOREVER.typeValue to dialogView.findViewById<RelativeLayout>(R.id.layoutForever)
        )
        val validTypeChecks = mapOf(
            ShareValidTypeEnum.SEVEN_DAYS.typeValue to dialogView.findViewById<ImageView>(R.id.check7Days),
            ShareValidTypeEnum.FOURTEEN_DAYS.typeValue to dialogView.findViewById<ImageView>(R.id.check14Days),
            ShareValidTypeEnum.THIRTY_DAYS.typeValue to dialogView.findViewById<ImageView>(R.id.check30Days),
            ShareValidTypeEnum.ONE_YEAR.typeValue to dialogView.findViewById<ImageView>(R.id.check365Days),
            ShareValidTypeEnum.FOREVER.typeValue to dialogView.findViewById<ImageView>(R.id.checkForever)
        )


        /*
                val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)
                val layout7Days = dialogView.findViewById<RelativeLayout>(R.id.layout7Days)
                val layout14Days = dialogView.findViewById<RelativeLayout>(R.id.layout14Days)
                val layout30Days = dialogView.findViewById<RelativeLayout>(R.id.layout30Days)
                val layout365Days = dialogView.findViewById<RelativeLayout>(R.id.layout365Days)
                val layoutForever = dialogView.findViewById<RelativeLayout>(R.id.layoutForever)
                val layoutRemember = dialogView.findViewById<RelativeLayout>(R.id.layoutRemember)

                val check7Days = dialogView.findViewById<ImageView>(R.id.check7Days)
                val check14Days = dialogView.findViewById<ImageView>(R.id.check14Days)
                val check30Days = dialogView.findViewById<ImageView>(R.id.check30Days)
                val check365Days = dialogView.findViewById<ImageView>(R.id.check365Days)
                val checkForever = dialogView.findViewById<ImageView>(R.id.checkForever)

                val swRemember = dialogView.findViewById<SwitchCompat>(R.id.swRemember)
                val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        */

        if (isRememberValidType) swRemember.isChecked = isRememberValidType
        var selectedType = currentValidType
        updateValidTypeCheckState(selectedType, validTypeChecks)
        btnClose.setOnClickListener { dialog.dismiss() }

        // 绑定有效期选择点击事件
        validTypeLayouts.forEach { (type, layout) ->
            setViewClickListener(layout) {
                selectedType = type
                updateValidTypeCheckState(selectedType, validTypeChecks)
            }
        }

        // 记住选择开关点击
        setViewClickListener(layoutRemember) {
            swRemember.isChecked = !swRemember.isChecked
        }

        // 确认按钮点击
        setSingleClickListener(btnConfirm) {
            isRememberValidType = swRemember.isChecked
            // 记住状态下更新默认有效期
            if (isRememberValidType) {
                currentValidType = selectedType
            }
            onConfirm(selectedType, currentExtractCode)
            dialog.dismiss()
        }

        dialog.show()

//        // 根据当前选中的位置显示对应的勾选标记
//        when (selectedPosition) {
//            0 -> check7Days.visibility = View.VISIBLE
//            1 -> check14Days.visibility = View.VISIBLE
//            2 -> check30Days.visibility = View.VISIBLE
//            3 -> check365Days.visibility = View.VISIBLE
//            4 -> checkForever.visibility = View.VISIBLE
//        }
//
//        fun hideAllChecks() {
//            check7Days.visibility = View.GONE
//            check14Days.visibility = View.GONE
//            check30Days.visibility = View.GONE
//            check365Days.visibility = View.GONE
//            checkForever.visibility = View.GONE
//        }
//
//        layout7Days.setOnClickListener {
//            hideAllChecks()
//            check7Days.visibility = View.VISIBLE
//            selectedPosition = 0
//        }
//
//        layout14Days.setOnClickListener {
//            hideAllChecks()
//            check14Days.visibility = View.VISIBLE
//            selectedPosition = 1
//        }
//
//        layout30Days.setOnClickListener {
//            hideAllChecks()
//            check30Days.visibility = View.VISIBLE
//            selectedPosition = 2
//        }
//
//        layout365Days.setOnClickListener {
//            hideAllChecks()
//            check365Days.visibility = View.VISIBLE
//            selectedPosition = 3
//        }
//
//        layoutForever.setOnClickListener {
//            hideAllChecks()
//            checkForever.visibility = View.VISIBLE
//            selectedPosition = 4
//        }
//
//        layoutRemember.setOnClickListener {
//            swRemember.isChecked = !swRemember.isChecked
//        }
//
//        btnConfirm.setOnClickListener {
//            if (swRemember.isChecked) {
//                currentValidType = selectedPosition
//                isRememberValidType = true
//            } else {
//                isRememberValidType = false
//            }
//
//            onConfirm(selectedPosition, currentExtractCode)
//            dialog.dismiss()
//        }
//
//        dialog.show()
    }



    /**
     * 分享文件
     */
    private fun shareFile(
        fileItem: FileItem,
        validType: Int,
        code: String,
        onSuccess: () -> Unit
    ) {
        val token = loginManager.getLoginInfo()?.access_token ?:  run {
            context.getString(R.string.login_expired_tip).showToast(context)
            return
        }

        val req = ShareFileRequest(
            fileId = fileItem.fileId,
            validType = validType,
            code = code
        )

        FileShareServiceClient.shareFile(
            req = req,
            addHeaders = { it.addHeader("Authorization", "Bearer $token") },
            onSuccess = { result ->
                onMain {
                    if (result.code == 200) {
                        context.getString(R.string.share_success_tip).showToast(context)
                        result.data?.let { showShareResultDialog(it) }
                        onSuccess()
                    } else {
                        result.message.showToast(context)
                    }
                }
            },
            onFailure = { error ->
                onMain {
                    Log.e(TAG, "文件分享失败：$error")
                    context.getString(R.string.share_failure_tip, error).showToast(context)
                }
            }
        )
    }

    /**
     * 显示分享结果对话框
     */
    private fun showShareResultDialog(result: FileShare) {
//        val dialog = BottomSheetDialog(context, R.style.BottomSheetDialog)
//        val dialogView = LayoutInflater.from(context).inflate(R.layout.module_dialog_share_result, null)
//        dialog.setContentView(dialogView)
//        dialog.behavior.isHideable = true
//        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        val (dialog, dialogView) = createBottomSheetDialog(R.layout.module_dialog_share_result)


        val tvShareLink = dialogView.findViewById<TextView>(R.id.tvShareLink)
        val tvExtractCode = dialogView.findViewById<TextView>(R.id.tvExtractCode)
        val btnCopyLink = dialogView.findViewById<Button>(R.id.btnCopyLink)
        val btnClose = dialogView.findViewById<ImageButton>(R.id.btnClose)

        val shareUrl = "${ApiClient.BASE_URL}share/${result.shareId}"
        tvShareLink.text = shareUrl

        val extractCode = result.code.ifEmpty {
            Log.e(TAG, "分享结果中提取码为空！")
            context.getString(R.string.share_code_unset)
        }

        tvExtractCode.text = extractCode

        setSingleClickListener(btnCopyLink) {
            copyTextToClipboard(context, shareUrl, context.getString(R.string.share_link_copied_tip))
//            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
//            val clip = ClipData.newPlainText("分享链接", shareUrl)
//            clipboard.setPrimaryClip(clip)
//            "分享链接已复制".showToast(context)
        }

        setViewClickListener(btnClose) { dialog.dismiss() }
        dialog.show()
    }

    /**
     * 创建BottomSheetDialog（统一配置样式和行为）
     * @param layoutId 弹窗布局ID
     * @return 配置好的BottomSheetDialog
     */
    private fun createBottomSheetDialog(layoutId: Int): Pair<BottomSheetDialog, View> {
        val dialog = BottomSheetDialog(context, R.style.BottomSheetDialog)
        val dialogView = LayoutInflater.from(context).inflate(layoutId, null)
        dialog.setContentView(dialogView)
        // 配置弹窗行为
        dialog.behavior.apply {
            isHideable = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
//        // 防止弹窗外部点击消失（可选，根据业务调整）
//        dialog.setCanceledOnTouchOutside(false)
        return Pair(dialog, dialogView)
    }

    /**
     * 设置View点击事件（简化重复代码）
     */
    private fun setViewClickListener(view: View, onClick: () -> Unit) {
        view.setOnClickListener { onClick.invoke() }
    }

    /**
     * 设置防重复点击的View点击事件
     * @param interval 防重复点击间隔（默认500ms）
     */
    private fun setSingleClickListener(view: View, interval: Long = 500, onClick: () -> Unit) {
        view.setOnClickListener(object : View.OnClickListener {
            private var lastClickTime = 0L
            override fun onClick(v: View?) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime >= interval) {
                    lastClickTime = currentTime
                    onClick.invoke()
                }
            }
        })
    }

    /**
     * 更新提取码选择的勾选状态
     */
    private fun updateExtractCodeCheckState(
        isSystemRandom: Boolean,
        checkSystem: ImageView,
        checkCustom: ImageView,
        inputArea: LinearLayout
    ) {
        checkSystem.visibility = if (isSystemRandom) View.VISIBLE else View.GONE
        checkCustom.visibility = if (isSystemRandom) View.GONE else View.VISIBLE
        inputArea.visibility = if (isSystemRandom) View.GONE else View.VISIBLE
    }

    /**
     * 更新有效期选择的勾选状态
     * @param selectedType 选中的有效期类型
     * @param checkViews 有效期类型与勾选控件的映射
     */
    private fun updateValidTypeCheckState(selectedType: Int, checkViews: Map<Int, ImageView>) {
        // 隐藏所有勾选
        checkViews.values.forEach { it.visibility = View.GONE }
        // 显示选中的勾选
        checkViews[selectedType]?.visibility = View.VISIBLE
    }

    private fun showHelpTooltip(btnHelp: ImageView) {
        val tooltipText = "开启后分享链接会带上验证码，用户点击链接即可自动填充提取码"

        // 创建 PopupWindow
        val popupWindow = android.widget.PopupWindow(context).apply {
            isOutsideTouchable = true
            isFocusable = false

            // 创建内容视图
            val contentView = TextView(context).apply {
                text = tooltipText
                textSize = 14f
                setTextColor(android.graphics.Color.WHITE)
                setBackgroundColor(android.graphics.Color.parseColor("#333333"))
                setPadding(16, 12, 16, 12)
                maxWidth = 600
                setLineSpacing(0f, 1.2f)
                gravity = android.view.Gravity.CENTER
            }

            setContentView(contentView)
            width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            elevation = 8f
        }

        // 计算显示位置（在图标上方显示）
        val location = IntArray(2)
        btnHelp.getLocationInWindow(location)

        // 测量 contentView 以获取实际宽度
        val contentView = popupWindow.contentView as TextView
        contentView.measure(
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.AT_MOST),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )

        // 计算居中偏移量
        val xOffset = (btnHelp.width / 2) - (contentView.measuredWidth / 2)
        val yOffset = -btnHelp.height - 16

        popupWindow.showAsDropDown(btnHelp, xOffset, yOffset)

        // 延迟 5 秒后自动关闭
        btnHelp.postDelayed({
            popupWindow.dismiss()
        }, 5000)
    }

    /**
     * 生成随机提取码（4 位字母数字组合）
     */
    private fun generateRandomExtractCode(): String {
        return (1..EXTRACT_CODE_LENGTH)
            .map { EXTRACT_CODE_CHAR_POOL.random() }
            .joinToString("")
    }

    /**
     * 根据有效期类型获取对应的文本
     * @param type 有效期类型
     * @return 有效期文本
     */
    private fun getValidTypeText(type: Int): String {
        return ShareValidTypeEnum.getByType(type).description
    }

    /**
     * 复制文本到剪贴板
     * @param context 上下文
     * @param text 要复制的文本
     * @param successTip 复制成功提示语
     */
    private fun copyTextToClipboard(context: Context, text: String, successTip: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("share_link", text)
            clipboard.setPrimaryClip(clipData)
            successTip.showToast(context)
        } catch (e: Exception) {
            Log.e(TAG, "复制文本到剪贴板失败：${e.message}", e)
            context.getString(R.string.share_link_copy_failure_tip).showToast(context)
        }
    }
}