package com.trigger.core.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkScheme=darkColorScheme(primary=Color(0xFF9DE9FF),secondary=Color(0xFFB9C7FF),tertiary=Color(0xFF87E3C1),background=Color(0xFF0D0E11),surface=Color(0xFF17181C),surfaceVariant=Color(0xFF25272D))
private val LightScheme=lightColorScheme(primary=Color(0xFF00677D),secondary=Color(0xFF485D92),tertiary=Color(0xFF176B51))
@Composable fun TriggerTheme(darkTheme:Boolean=isSystemInDarkTheme(),content:@Composable ()->Unit) {
    val context=LocalContext.current
    val scheme=when { Build.VERSION.SDK_INT>=31 && darkTheme -> dynamicDarkColorScheme(context); Build.VERSION.SDK_INT>=31 -> dynamicLightColorScheme(context); darkTheme -> DarkScheme; else -> LightScheme }
    MaterialTheme(colorScheme=scheme,typography=Typography(),content=content)
}
