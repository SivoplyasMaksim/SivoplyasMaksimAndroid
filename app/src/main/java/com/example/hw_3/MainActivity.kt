package com.example.hw_3

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.isVisible
import com.example.hw_3.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getPreferences(MODE_PRIVATE) }


    private val refreshActiveNotifications = object : Runnable {
        override fun run() {
            updateActiveNotificationsUi()
            handler.postDelayed(this, REFRESH_INTERVAL_MS)
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            updatePermissionUi()
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.createChannels(this)
        setupImportanceSpinner()
        setupClickListeners()


        if (savedInstanceState == null) {
            requestNotificationPermissionIfNeeded()

            handleNotificationIntent(intent, appWasRunning = false)
        }
        updatePermissionUi()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent, appWasRunning = true)
    }

    override fun onResume() {
        super.onResume()
        updatePermissionUi()
        handler.post(refreshActiveNotifications)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshActiveNotifications)
    }



    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun updatePermissionUi() {
        val allowed = NotificationHelper.notificationsEnabled(this)
        binding.contentGroup.isVisible = allowed
        binding.deniedGroup.isVisible = !allowed
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        startActivity(intent)
    }


    private fun handleNotificationIntent(intent: Intent?, appWasRunning: Boolean) {
        if (intent == null) return
        if (!intent.getBooleanExtra(NotificationHelper.EXTRA_FROM_NOTIFICATION, false)) return

        val title = intent.getStringExtra(NotificationHelper.EXTRA_TITLE).orEmpty()
        val text = intent.getStringExtra(NotificationHelper.EXTRA_TEXT).orEmpty()

        Toast.makeText(this, if (appWasRunning) text else title, Toast.LENGTH_LONG).show()

        intent.removeExtra(NotificationHelper.EXTRA_FROM_NOTIFICATION)
    }


    private fun setupImportanceSpinner() {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.importance_entries)
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.importanceSpinner.adapter = adapter
        binding.importanceSpinner.setSelection(1)
    }

    private fun selectedImportance(): Int =
        when (binding.importanceSpinner.selectedItemPosition) {
            0 -> NotificationManager.IMPORTANCE_MIN
            2 -> NotificationManager.IMPORTANCE_HIGH
            else -> NotificationManager.IMPORTANCE_DEFAULT
        }

    private fun setupClickListeners() {
        binding.openSettingsButton.setOnClickListener { openAppSettings() }
        binding.sendButton.setOnClickListener { sendNotification() }
        binding.editButton.setOnClickListener { editNotification() }
        binding.deleteButton.setOnClickListener { deleteById() }
        binding.deleteAllButton.setOnClickListener { deleteAll() }
        binding.secondScreenButton.setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }
    }


    private fun sendNotification() {
        if (!NotificationHelper.notificationsEnabled(this)) {
            updatePermissionUi()
            toast("Разрешение на уведомления не предоставлено")
            return
        }

        val title = binding.titleInput.text.toString().trim().ifEmpty { "Без заголовка" }
        val text = binding.textInput.text.toString().trim().ifEmpty { "Без текста" }
        val id = nextNotificationId()

        NotificationHelper.show(
            context = this,
            id = id,
            title = title,
            text = text,
            nonCancellable = binding.ongoingCheckbox.isChecked,
            importance = selectedImportance()
        )
        toast("Уведомление отправлено, id = $id")
        updateActiveNotificationsUi()
    }

    private fun editNotification() {
        val id = readId() ?: return
        val existing = NotificationHelper.activeNotifications(this).firstOrNull { it.id == id }
        if (existing == null) {
            toast("Уведомление с id = $id не найдено")
            updateActiveNotificationsUi()
            return
        }

        val title = binding.titleInput.text.toString().trim().ifEmpty { "Без заголовка" }
        val text = binding.textInput.text.toString().trim().ifEmpty { "Без текста" }

        val nonCancellable =
            (existing.notification.flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0
        val importance = NotificationHelper.importanceOf(existing.notification)

        NotificationHelper.show(this, id, title, text, nonCancellable, importance)
        toast("Уведомление id = $id обновлено")
        updateActiveNotificationsUi()
    }

    private fun deleteById() {
        val id = readId() ?: return
        NotificationHelper.cancel(this, id)
        toast("Уведомление id = $id удалено")
        updateActiveNotificationsUi()
    }

    private fun deleteAll() {
        NotificationHelper.cancelAll(this)
        toast("Все уведомления удалены")
        updateActiveNotificationsUi()
    }

    private fun updateActiveNotificationsUi() {
        val active = NotificationHelper.activeNotifications(this).sortedBy { it.id }
        val hasActive = active.isNotEmpty()

        binding.idInput.isEnabled = hasActive
        binding.editButton.isEnabled = hasActive
        binding.deleteButton.isEnabled = hasActive
        binding.deleteAllButton.isEnabled = hasActive

        binding.activeIdsText.text = if (hasActive) {
            "Активные id: " + active.joinToString(", ") { it.id.toString() }
        } else {
            "Активных уведомлений нет"
        }
    }


    private fun readId(): Int? {
        val id = binding.idInput.text.toString().trim().toIntOrNull()
        if (id == null) toast("Введите корректный числовой id")
        return id
    }

    private fun nextNotificationId(): Int {
        val next = prefs.getInt(KEY_NEXT_ID, 1)
        prefs.edit { putInt(KEY_NEXT_ID, next + 1) }
        return next
    }

    private fun toast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {
        private const val REFRESH_INTERVAL_MS = 1000L
        private const val KEY_NEXT_ID = "next_notification_id"
    }
}