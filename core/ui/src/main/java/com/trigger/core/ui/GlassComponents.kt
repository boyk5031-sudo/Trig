package com.trigger.core.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable fun GlassCard(modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xCC292B32),Color(0xAA191A1F)))).border(1.dp,Color.White.copy(.10f),RoundedCornerShape(24.dp)).padding(18.dp),content=content)
}
@Composable fun StatusIndicator(label:String,active:Boolean,detail:String?=null,modifier:Modifier=Modifier) {
    val opacity by animateFloatAsState(if(active) 1f else .65f, spring(stiffness=Spring.StiffnessMediumLow), label="status-opacity")
    Row(modifier,verticalAlignment=Alignment.CenterVertically) {
        Surface(Modifier.size(9.dp),shape=RoundedCornerShape(50),color=(if(active) Color(0xFF57D5A0) else Color(0xFFFF8278)).copy(alpha=opacity)) {}
        Spacer(Modifier.width(9.dp)); Column { Text(label,color=MaterialTheme.colorScheme.onSurface); if(detail!=null) Text(detail,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall) }
    }
}
