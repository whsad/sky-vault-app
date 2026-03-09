package com.computer.skyvault

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.computer.skyvault.common.recycleitem.DividerItem
import com.computer.skyvault.common.recycleitem.HeaderItem
import com.computer.skyvault.common.recycleitem.MenuItem
import com.computer.skyvault.common.recycleitem.NavItem
import com.computer.skyvault.databinding.MainActivityBinding
import com.computer.skyvault.manager.LoginManager
import com.computer.skyvault.ui.customview.CustomNavigationView
import com.computer.skyvault.ui.login.LoginActivity
import com.computer.skyvault.ui.myfiles.MyFilesFragment
import com.computer.skyvault.ui.transfer.TransferDialogFragment
import com.computer.skyvault.utils.setViewClickListener
import com.google.android.material.snackbar.Snackbar

/**
 * 主页面 Activity
 * 负责侧边栏导航、顶部选择栏控制、返回键逻辑处理
 */
class MainActivity : AppCompatActivity() {
    // 日志 TAG
    private val TAG = MainActivity::class.simpleName

    // 布局绑定
    private lateinit var binding: MainActivityBinding

    // 导航相关
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var customNavView: CustomNavigationView
    private lateinit var navController: NavController

    // 返回键相关
    private var backPressedTime: Long = 0
    private val exitInterval = 2000L // 双击退出间隔
    private val homeDestinationId = R.id.nav_my_files // 首页ID
    private var isAtHomeDestination = true

    // 登录管理
    private lateinit var loginManager: LoginManager

    // 顶部导航栏
    private lateinit var topSelectionBar: View
    private lateinit var tvSelectionCount: TextView
    private lateinit var tvCancel: TextView
    private lateinit var tvCheckAll: TextView
    private var isCheckAll = false

    // 菜单ID常量（便于维护）
    private val MENU_ID_TRANSFER = R.id.menu_transfer

    // 导航菜单项
    private val navigationMenuItems by lazy {
        listOf<NavItem>(
            MenuItem(R.id.nav_my_files, "My files", R.drawable.ic_menu_my_files),
            MenuItem(R.id.nav_recent, "Recent", R.drawable.ic_menu_recent),
            MenuItem(R.id.nav_shared, "Shared", R.drawable.ic_menu_shared),
            MenuItem(R.id.nav_starred, "Starred", R.drawable.ic_menu_starred),
            MenuItem(R.id.nav_categories, "Categories", R.drawable.ic_menu_categories),
            DividerItem(),
            MenuItem(R.id.nav_trash, "Trash", R.drawable.ic_menu_trash)
        )
    }

    // AppBar 配置的目标ID集合
    private val appBarTargetIds by lazy {
        setOf(
            R.id.nav_my_files, R.id.nav_recent, R.id.nav_shared,
            R.id.nav_starred, R.id.nav_categories, R.id.nav_trash
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化登录管理器并检查登录状态
        loginManager = LoginManager.getInstance(application)

        // 检查登录状态
        if (!loginManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 设置状态栏样式
        window.statusBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        // 初始化布局
        binding = MainActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化顶部选择栏
        initTopSelectionBar()

        // 初始化工具栏和导航
        initToolbarAndNavigation()

        // 初始化自定义导航视图
        initCustomNavigationView()

        // 监听导航目的地变化
        setupDestinationChangedListener()

        // 处理返回键
        setupOnBackPressed()
    }

    /**
     * 初始化顶部选择栏（绑定视图 + 设置点击事件）
     */
    private fun initTopSelectionBar() {
        // 绑定顶部操作栏
        topSelectionBar = binding.appBarMain.topSelectionBar.root
        tvSelectionCount = binding.appBarMain.topSelectionBar.tvSelectionCount
        tvCancel = binding.appBarMain.topSelectionBar.tvCancel
        tvCheckAll = binding.appBarMain.topSelectionBar.tvCheckAll

        // 设置点击事件
        setViewClickListener(tvCancel) { exitSelectionMode() }
        setViewClickListener(tvCheckAll) { toggleSelectAll() }
    }

    /**
     * 初始化工具栏和导航控制器
     */
    private fun initToolbarAndNavigation() {
        // 设置工具栏
        setSupportActionBar(binding.appBarMain.toolbar)

        // 绑定导航相关视图
        drawerLayout = binding.drawerLayout
        customNavView = binding.customNavView
        navController = findNavController(R.id.nav_host_fragment_content_main)

        // 配置 AppBar
        appBarConfiguration = AppBarConfiguration(appBarTargetIds, drawerLayout)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    /**
     * 初始化自定义侧边栏导航视图
     */
    private fun initCustomNavigationView() {
        // 设置用户信息头部
        loginManager.getLoginInfo()?.let { loginInfo ->
            val header = HeaderItem (
                nickname = loginInfo.user.nick_name,
                email = loginInfo.user.email
            )
            customNavView.setHeader(header)
        }

        customNavView.setMenuItems(navigationMenuItems)

        // 设置默认选中项
        val defaultSelectedId = navController.currentDestination?.id ?: homeDestinationId
        customNavView.setSelectedItem(defaultSelectedId)

        // 设置点击监听
        customNavView.setOnNavigationItemSelectedListener { itemId ->
            handleNavigationItemClick(itemId)
        }

        // 设置上传按钮点击
        customNavView.setOnUploadClickListener {
            // 处理上传文件逻辑
            Snackbar.make(binding.root, "Upload files clicked", Snackbar.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理侧边栏菜单项点击
     */
    private fun handleNavigationItemClick(itemId: Int) {
        // 如果点击的是当前已选中的项，只关闭抽屉
        if (navController.currentDestination?.id == itemId) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return
        }

        // 导航到目标页面
        navController.navigate(itemId)

        // 关闭抽屉
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    /**
     * 监听导航目的地变化
     */
    private fun setupDestinationChangedListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // 更新侧边栏选中状态
            customNavView.setSelectedItem(destination.id)

            // 检查是否在首页
            isAtHomeDestination = destination.id == homeDestinationId

            // 更新 ActionBar
            supportActionBar?.title = getDestinationTitle(destination.id)
        }
    }

    /**
     * 根据导航ID获取页面标题
     */
    private fun getDestinationTitle(destinationId: Int): String {
        return when (destinationId) {
            R.id.nav_my_files -> "My Files"
            R.id.nav_recent -> "Recent"
            R.id.nav_shared -> "Shared"
            R.id.nav_starred -> "Starred"
            R.id.nav_categories -> "Categories"
            R.id.nav_trash -> "Trash"
            else -> getString(R.string.app_name)
        }
    }

    /**
     * 配置返回键回调
     */
    private fun setupOnBackPressed() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    /**
     * 处理返回键逻辑
     */
    private fun handleBackPress() {
        // 1. 选择模式下，退出选择模式
        if (topSelectionBar.isVisible) {
            exitSelectionMode()
            return
        }

        // 2. 抽屉打开时，关闭抽屉
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return
        }

        // 3. 非首页时，返回首页
        if (!isAtHomeDestination) {
            navController.navigate(homeDestinationId)
            return
        }

        // 4. 首页时，双击退出
        handleDoubleBackExit()
    }

    /**
     * 处理双击返回退出逻辑
     */
    private fun handleDoubleBackExit() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime < exitInterval) {
            finish()
        } else {
            Snackbar.make(binding.root, "Press back again to exit", Snackbar.LENGTH_SHORT).show()
            backPressedTime = currentTime
        }
    }

    /**
     * 显示顶部选择栏并更新选中数量
     */
    fun showTopSelectionBar(selectedCount: Int) {
        if (topSelectionBar.isGone) {
            topSelectionBar.visibility = View.VISIBLE
            AnimationUtils.loadAnimation(this, R.anim.slide_in_from_top)
                .apply { topSelectionBar.startAnimation(this) }
        }
        tvSelectionCount.text = "已选中 $selectedCount 个文件"
    }

    /**
     * 隐藏顶部选择栏
     */
    fun hideTopSelectionBar() {
        if (topSelectionBar.isVisible) {
            AnimationUtils.loadAnimation(this, R.anim.slide_out_to_top)
                .apply {
                    setAnimationListener(object : Animation.AnimationListener {
                        override fun onAnimationEnd(animation: Animation?) {
                            topSelectionBar.visibility = View.GONE
                        }
                        override fun onAnimationStart(animation: Animation?) {}
                        override fun onAnimationRepeat(animation: Animation?) {}
                    })
                    topSelectionBar.startAnimation(this)
                }
        }
    }

    /**
     * 退出选择模式
     */
    private fun exitSelectionMode() {
        getMyFilesFragment()?.exitSelectionMode()
        resetSelectionBarState()
        Log.d(TAG, "exitSelectionMode: 取消选择模式")
    }

    /**
     * 切换全选/取消全选状态
     */
    private fun toggleSelectAll() {
        getMyFilesFragment()?.checkAll(isCheckAll)
        isCheckAll = !isCheckAll
        tvCheckAll.text = if (isCheckAll) "取消全选" else "全选"
        Log.d(TAG, "toggleSelectAll: 全选状态切换为 $isCheckAll")
    }

    /**
     * 重置选择栏状态
     */
    private fun resetSelectionBarState() {
        isCheckAll = false
        tvCheckAll.text = "全选"
        tvSelectionCount.text = "已选中 0 个文件"
    }

    /**
     * 获取 MyFilesFragment 实例
     */
    private fun getMyFilesFragment(): MyFilesFragment? {
        return supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
            ?.let { it as? androidx.navigation.fragment.NavHostFragment }
            ?.childFragmentManager
            ?.fragments
            ?.firstOrNull() as? MyFilesFragment
    }

    /**
     * 加载AppBar菜单（添加传输按钮）
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // 加载菜单资源文件
        menuInflater.inflate(R.menu.menu_main_toolbar, menu)
        return true
    }

    /**
     * 处理AppBar菜单点击事件
     */
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when(item.itemId) {
            MENU_ID_TRANSFER -> {
                // 点击传输按钮，打开文件传输页面
                openTransferPage()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * 打开文件传输页面
     */
    private fun openTransferPage() {
        // TODO: 替换为文件传输页面跳转逻辑
        // 示例1：使用Navigation导航
        // navController.navigate(R.id.action_to_transfer_fragment)
        // 示例2：使用Intent跳转Activity
        // startActivity(Intent(this, TransferActivity::class.java))

        val dialog = TransferDialogFragment.newInstance()
        dialog.show(supportFragmentManager, TransferDialogFragment.TAG)
//        // 临时提示（测试用）
//        Snackbar.make(binding.root, "打开文件传输页面", Snackbar.LENGTH_SHORT).show()
    }


    /**
     * 物理返回键（兼容旧版本）
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBackPress()
            true
        } else {
            super.onKeyDown(keyCode, event)
        }
    }

    /**
     * 处理ActionBar返回按钮
     */
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
