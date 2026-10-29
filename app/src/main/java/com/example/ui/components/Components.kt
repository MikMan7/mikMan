package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlertSeverity
import com.example.data.model.UserStatus
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanDark
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.NavyCard
import com.example.ui.theme.NavyCardLight
import com.example.ui.theme.SuccessGreen

@Composable
fun StatMetricCard(
    title: String,
    value: String,
    unit: String = "",
    icon: ImageVector,
    accentColor: Color = CyanDark,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NavyCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (unit.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuotaProgressBar(
    percentage: Double,
    modifier: Modifier = Modifier
) {
    val progress = (percentage / 100.0).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600),
        label = "quota_progress"
    )

    val color = when {
        percentage >= 100.0 -> AlertRed
        percentage >= 85.0 -> AlertAmber
        percentage >= 70.0 -> CyanAccent
        else -> SuccessGreen
    }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(NavyCardLight)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(color.copy(alpha = 0.7f), color)
                        )
                    )
            )
        }
    }
}

@Composable
fun UserStatusChip(
    status: UserStatus,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status) {
        UserStatus.ACTIVE -> Triple(
            SuccessGreen.copy(alpha = 0.15f),
            SuccessGreen,
            if (isArabic) UserStatus.ACTIVE.labelAr else UserStatus.ACTIVE.labelEn
        )
        UserStatus.WARNING -> Triple(
            AlertAmber.copy(alpha = 0.15f),
            AlertAmber,
            if (isArabic) UserStatus.WARNING.labelAr else UserStatus.WARNING.labelEn
        )
        UserStatus.THROTTLED -> Triple(
            InfoBlue.copy(alpha = 0.15f),
            InfoBlue,
            if (isArabic) UserStatus.THROTTLED.labelAr else UserStatus.THROTTLED.labelEn
        )
        UserStatus.EXCEEDED -> Triple(
            AlertRed.copy(alpha = 0.2f),
            AlertRed,
            if (isArabic) UserStatus.EXCEEDED.labelAr else UserStatus.EXCEEDED.labelEn
        )
        UserStatus.SUSPENDED -> Triple(
            Color.Gray.copy(alpha = 0.2f),
            Color.LightGray,
            if (isArabic) UserStatus.SUSPENDED.labelAr else UserStatus.SUSPENDED.labelEn
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(0.8.dp, textColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SeverityBadge(
    severity: AlertSeverity,
    isArabic: Boolean,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (severity) {
        AlertSeverity.CRITICAL -> Pair(
            AlertRed,
            if (isArabic) AlertSeverity.CRITICAL.labelAr else AlertSeverity.CRITICAL.labelEn
        )
        AlertSeverity.WARNING -> Pair(
            AlertAmber,
            if (isArabic) AlertSeverity.WARNING.labelAr else AlertSeverity.WARNING.labelEn
        )
        AlertSeverity.INFO -> Pair(
            InfoBlue,
            if (isArabic) AlertSeverity.INFO.labelAr else AlertSeverity.INFO.labelEn
        )
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.6.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
