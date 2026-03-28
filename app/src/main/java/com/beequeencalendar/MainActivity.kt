package com.beequeencalendar

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.beequeencalendar.data.AppDatabase
import com.beequeencalendar.databinding.ActivityMainBinding
import com.beequeencalendar.notification.AlarmScheduler
import com.beequeencalendar.notification.NotificationHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val prefs by lazy { getSharedPreferences("permissions", MODE_PRIVATE) }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no-op, user decides */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        setupActionBarWithNavController(navHost.navController)

        NotificationHelper.createChannel(this)
        requestNotificationPermission()

        // ✅ Показываем запрос ТОЛЬКО один раз
        requestIgnoreBatteryOptimization()

        val db = AppDatabase.getInstance(applicationContext)
        val version = db.openHelper.writableDatabase.version
        Log.d("MIGRATION_CHECK", "Database version: $version")
    }

    private fun requestIgnoreBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager

            // ✅ Проверяем: если уже разрешили ИЛИ уже спрашивали — не показываем диалог
            if (!pm.isIgnoringBatteryOptimizations(packageName) &&
                !prefs.getBoolean("battery_optimization_requested", false)) {

                MaterialAlertDialogBuilder(this)
                    .setTitle("Важно для работы будильников")
                    .setMessage("Чтобы уведомления приходили вовремя, отключите оптимизацию батареи для этого приложения.")
                    .setPositiveButton("Разрешить") { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    }
                    .setNegativeButton("Позже", null)
                    .setOnDismissListener {
                        // ✅ Запоминаем что запрос был показан (независимо от выбора)
                        prefs.edit().putBoolean("battery_optimization_requested", true).apply()
                    }
                    .show()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHost.navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        when (item.itemId) {
            R.id.action_archive -> {
                navHost.navController.navigate(R.id.archiveFragment)
                return true
            }
            R.id.action_settings -> {
                navHost.navController.navigate(R.id.settingsFragment)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}