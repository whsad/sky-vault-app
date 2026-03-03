package com.computer.skyvault

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.computer.skyvault.common.recycleitem.DividerItem
import com.computer.skyvault.common.recycleitem.HeaderItem
import com.computer.skyvault.common.recycleitem.MenuItem
import com.computer.skyvault.common.recycleitem.NavItem
import com.computer.skyvault.databinding.ModuleActivityMainBinding
import com.computer.skyvault.manager.LoginManager
import com.computer.skyvault.ui.customview.CustomNavigationView
import com.computer.skyvault.ui.login.LoginActivity
import com.computer.skyvault.ui.myfiles.MyFilesFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ModuleActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var customNavView: CustomNavigationView
    private lateinit var navController: NavController
    private var backPressedTime: Long = 0
    private var isAtHomeDestination = true
    private val homeDestinationId = R.id.nav_my_files
    private lateinit var loginManager: LoginManager

    // 顶部导航栏
    private lateinit var topSelectionBar: View
    private lateinit var tvSelectionCount: TextView
    private lateinit var tvCancel: TextView
    private lateinit var tvCheckAll: TextView
    private var isCheckAll = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化 LoginManager
        loginManager = LoginManager.getInstance(application)

        // 检查登录状态
        if (!loginManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        window.statusBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        binding = ModuleActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 绑定顶部操作栏
        topSelectionBar = binding.appBarMain.topSelectionBar.root
        tvSelectionCount = binding.appBarMain.topSelectionBar.tvSelectionCount
        tvCancel = binding.appBarMain.topSelectionBar.tvCancel
        tvCheckAll = binding.appBarMain.topSelectionBar.tvCheckAll

        // 设置按钮点击
        tvCancel.setOnClickListener { exitSelectionMode() }
        tvCheckAll.setOnClickListener { selectAllInCurrentFragment() }

        val toolbar = binding.appBarMain.toolbar
        setSupportActionBar(toolbar)

        drawerLayout = binding.drawerLayout
        customNavView = binding.customNavView

        navController = findNavController(R.id.nav_host_fragment_content_main)

        // 初始化自定义导航视图
        initCustomNavigationView()

        // 监听导航目的地变化
        setupDestinationChangedListener()

        // 配置 AppBarConfiguration
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_my_files, R.id.nav_recent, R.id.nav_shared,
                R.id.nav_starred, R.id.nav_categories, R.id.nav_trash
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        // 处理返回键
        setupOnBackPressed()
    }

    // 由 Fragment 触发（推荐通过接口或 Event）
    fun showTopSelectionBar(selectedCount: Int) {
        if (topSelectionBar.isGone) {
            topSelectionBar.visibility = View.VISIBLE
            val anim = AnimationUtils.loadAnimation(this, R.anim.slide_in_from_top)
            topSelectionBar.startAnimation(anim)
        }
        tvSelectionCount.text = "已选中 $selectedCount 个文件"
    }

    fun hideTopSelectionBar() {
        if (topSelectionBar.isVisible) {
            val anim = AnimationUtils.loadAnimation(this, R.anim.slide_out_to_top)
            anim.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationEnd(animation: Animation?) {
                    topSelectionBar.visibility = View.GONE
                }

                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
            })
            topSelectionBar.startAnimation(anim)
        }
    }

    private fun getCurrentMyFilesFragment(): MyFilesFragment? {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as? androidx.navigation.fragment.NavHostFragment
        return navHost?.childFragmentManager?.fragments?.firstOrNull() as? MyFilesFragment
    }

    private fun exitSelectionMode() {
        getCurrentMyFilesFragment()?.exitSelectionMode()
        isCheckAll = false
        tvCheckAll.text = "全选"
        tvSelectionCount.text = "已选中 0 个文件"
        Log.d(TAG, "exitSelectionMode: 取消已被点击")
    }

    private fun selectAllInCurrentFragment() {
        getCurrentMyFilesFragment()?.checkAll(isCheckAll)
        isCheckAll = !isCheckAll
        tvCheckAll.text = if (isCheckAll) "取消全选" else "全选"
        Log.d(TAG, "selectAllInCurrentFragment: 全选已被点击")
    }

    private fun initCustomNavigationView() {

        // 从 LoginManager 获取登录信息
        val loginInfo = loginManager.getLoginInfo()
        if (loginInfo != null) {
            // 设置头部信息
            val header = HeaderItem(
                nickname = loginInfo.user.nick_name,
                email = loginInfo.user.email // 或者用其他字段
            )
            customNavView.setHeader(header)
        }

        // 设置菜单项
        val menuItems = listOf<NavItem>(
            MenuItem(R.id.nav_my_files, "My files", R.drawable.ic_menu_my_files),
            MenuItem(R.id.nav_recent, "Recent", R.drawable.ic_menu_recent),
            MenuItem(R.id.nav_shared, "Shared", R.drawable.ic_menu_shared),
            MenuItem(R.id.nav_starred, "Starred", R.drawable.ic_menu_starred),
            MenuItem(R.id.nav_categories, "Categories", R.drawable.ic_menu_categories),
            DividerItem(),
            MenuItem(R.id.nav_trash, "Trash", R.drawable.ic_menu_trash),
        )
        customNavView.setMenuItems(menuItems)

        // 设置当前选中项
        val currentDestination = navController.currentDestination?.id
        if (currentDestination != null) {
            customNavView.setSelectedItem(currentDestination)
        } else {
            customNavView.setSelectedItem(R.id.nav_my_files)
        }

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

    private fun handleNavigationItemClick(itemId: Int) {
        // 如果点击的是当前已选中的项，只关闭抽屉
        if (navController.currentDestination?.id == itemId) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return
        }

        // 导航到目标 Fragment
        when (itemId) {
            R.id.nav_my_files -> {
                // 导航到 My Files，并清除返回栈
                navController.navigate(itemId)
            }

            R.id.nav_recent -> {
                navController.navigate(itemId)
            }

            R.id.nav_shared -> {
                navController.navigate(itemId)
            }

            R.id.nav_starred -> {
                navController.navigate(itemId)
            }

            R.id.nav_categories -> {
                navController.navigate(itemId)
            }

            R.id.nav_trash -> {
                navController.navigate(itemId)
            }
        }

        // 关闭抽屉
        drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun setupDestinationChangedListener() {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // 更新侧边栏选中状态
            customNavView.setSelectedItem(destination.id)

            // 检查是否在首页
            isAtHomeDestination = destination.id == homeDestinationId

            // 更新 ActionBar
            supportActionBar?.let {
                it.title = when (destination.id) {
                    R.id.nav_my_files -> "My Files"
                    R.id.nav_recent -> "Recent"
                    R.id.nav_shared -> "Shared"
                    R.id.nav_starred -> "Starred"
                    R.id.nav_categories -> "Categories"
                    R.id.nav_trash -> "Trash"
                    else -> getString(R.string.app_name)
                }
            }
        }
    }

    private fun setupOnBackPressed() {
        // 添加返回键回调
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })
    }

    private fun handleBackPress() {
        // 如果当前是 MyFilesFragment 且处于选择模式 → 退出选择模式
        if (topSelectionBar.isVisible) {
            exitSelectionMode()
            return
        }

        // 否则按原逻辑处理
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return
        }

        // 如果当前不在首页，返回首页
        if (!isAtHomeDestination) {
            navController.navigate(R.id.nav_my_files)
            return
        }

        // 首页双击退出
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            finish()
        } else {
            Snackbar.make(binding.root, "Press back again to exit", Snackbar.LENGTH_SHORT).show()
            backPressedTime = System.currentTimeMillis()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleBackPress()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
