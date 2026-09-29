package com.trigger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.trigger.app.navigation.TriggerNavGraph
import com.trigger.app.service.GameServiceBinder
import com.trigger.core.ui.TriggerTheme

class MainActivity : ComponentActivity() {
    private lateinit var services:GameServiceBinder
    override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState); services=GameServiceBinder(this); services.prepareActivation()
        setContent { TriggerTheme { TriggerNavGraph(services) } }
    }
}
