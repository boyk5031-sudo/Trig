package com.trigger.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import com.trigger.app.launcher.GameLauncherScreen
import com.trigger.app.service.GameServiceBinder

class MainActivity : ComponentActivity() {
    private lateinit var services: GameServiceBinder
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); services=GameServiceBinder(this)
        setContent { MaterialTheme { GameLauncherScreen(context=this, onLaunch={ app, turbo -> services.launch(app.packageName,app.loadLabel(packageManager).toString(), turbo); packageManager.getLaunchIntentForPackage(app.packageName)?.let(::startActivity) }) } }
    }
}
