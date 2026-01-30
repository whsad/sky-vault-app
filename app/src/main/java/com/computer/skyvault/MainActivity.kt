package com.computer.skyvault

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.computer.skyvault.databinding.ActivityMainBinding
import com.computer.skyvault.model.DividerItem
import com.computer.skyvault.model.HeaderItem
import com.computer.skyvault.model.MenuItem
import com.computer.skyvault.model.NavItem
import com.computer.skyvault.ui.customview.CustomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var customNavView: CustomNavigationView
    private lateinit var navController: NavController

    /*    override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            drawerLayout = binding.drawerLayout
            customNavView = binding.customNavView


            val toolbar = binding.appBarMain.toolbar
            setSupportActionBar(toolbar)

            toolbar.setNavigationOnClickListener {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
            }

            initCustomNavigationView()


            // 设置点
    //        window.statusBarColor = Color.WHITE
    //        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    //
    //        setSupportActionBar(binding.appBarMain.toolbar)
    //
    //        binding.appBarMain.fab.setOnClickListener { view ->
    //            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
    //                .setAction("Action", null)
    //                .setAnchorView(R.id.fab).show()
    //        }
    //        val drawerLayout: DrawerLayout = binding.drawerLayout
    //        val navView: NavigationView = binding.navView
    //        val navController = findNavController(R.id.nav_host_fragment_content_main)
    //        // Passing each menu ID as a set of Ids because each
    //        // menu should be considered as top level destinations.
    //        appBarConfiguration = AppBarConfiguration(
    //            setOf(
    //                R.id.nav_my_files, R.id.nav_recent, R.id.nav_shared, R.id.nav_starred, R.id.nav_categories, R.id.nav_trash
    //            ), drawerLayout
    //        )
    //        setupActionBarWithNavController(navController, appBarConfiguration)
    //        customNavView.setupWithNavController(navController)
        }*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = binding.appBarMain.toolbar  // 使用 MaterialToolbar 的 ID
        setSupportActionBar(toolbar)

        drawerLayout = binding.drawerLayout
        customNavView = binding.customNavView

        navController = findNavController(R.id.nav_host_fragment_content_main)

        // 设置 ActionBarDrawerToggle 图标
        setupDrawerToggle(toolbar)
        // 初始化自定义导航视图
        initCustomNavigationView()

        // 配置 AppBarConfiguration
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_my_files, R.id.nav_recent, R.id.nav_shared,
                R.id.nav_starred, R.id.nav_categories, R.id.nav_trash
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    private fun initCustomNavigationView() {
        // 设置头部
        val header = HeaderItem(
            nickname = "John Doe",
            email = "john.doe@example.com"
        )
        customNavView.setHeader(header)

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

        // 设置选中项
        customNavView.setSelectedItem(R.id.nav_my_files)

        // 设置点击监听
        customNavView.setOnNavigationItemSelectedListener { itemId ->
            // 使用 Navigation Framework 导航
            if (navController.currentDestination?.id != itemId) {
                navController.navigate(itemId)
                // 更新选中状态
                navController.addOnDestinationChangedListener { _, destination, _ ->
                    customNavView.setSelectedItem(destination.id)
                }
            }
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun setupDrawerToggle(toolbar: androidx.appcompat.widget.Toolbar) {
        // 设置导航图标为汉堡菜单
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_hamburger_button)

        // 设置点击事件
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

//    @Deprecated("Deprecated in Java")
//    override fun onBackPressed() {
//        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
//            drawerLayout.closeDrawer(GravityCompat.START)
//        } else {
//            super.onBackPressed()
//        }
//    }

    fun openDrawer() {
        if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    fun closeDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }
}