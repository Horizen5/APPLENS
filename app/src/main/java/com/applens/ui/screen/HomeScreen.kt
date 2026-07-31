package com.applens.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// HyperOS / MIUI X 设计风格色板
private val BgColor = Color(0xFFF5F5F5)
private val CardBg = Color(0xFFFFFFFF)
private val TitleColor = Color(0xFF1A1A1A)
private val SubTitleColor = Color(0xFF666666)
private val AccentColor = Color(0xFF5B8CFF)
private val DividerColor = Color(0xFFEEEEEE)
private val StatusGreenBg = Color(0xFFE3F6EA)
private val StatusGreenFg = Color(0xFF1B873A)
private val StatusYellowBg = Color(0xFFFFF1C8)
private val StatusYellowFg = Color(0xFFB26A00)
private val OkGreen = Color(0xFF34A853)
private val ErrRed = Color(0xFFEA4335)

@Composable
fun HomeScreen(
    hasRoot: Boolean,
    hasUsage: Boolean,
    hasOverlay: Boolean,
    onGrantRoot: () -> Unit,
    onGrantUsage: () -> Unit,
    onGrantOverlay: () -> Unit,
    onStartScan: () -> Unit
) {
    val allGranted = hasRoot && hasUsage && hasOverlay
    val grantedCount = listOf(hasRoot, hasUsage, hasOverlay).count { it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 28.dp, bottom = 36.dp)
    ) {
        // 1. 大标题
        Text(
            text = "APPLENS",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TitleColor
        )

        Spacer(Modifier.height(20.dp))

        // 2. 状态卡片
        StatusCard(allGranted = allGranted, grantedCount = grantedCount, total = 3)

        Spacer(Modifier.height(16.dp))

        // 3. 权限列表卡片
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                PermissionRow(
                    title = "Root 权限",
                    statusText = if (hasRoot) "已授权" else "未授权",
                    granted = hasRoot,
                    onGrant = onGrantRoot,
                    showDivider = true
                )
                PermissionRow(
                    title = "使用情况访问",
                    statusText = if (hasUsage) "已授权" else "未授权",
                    granted = hasUsage,
                    onGrant = onGrantUsage,
                    showDivider = true
                )
                PermissionRow(
                    title = "悬浮窗权限",
                    statusText = if (hasOverlay) "已授权" else "未授权",
                    granted = hasOverlay,
                    onGrant = onGrantOverlay,
                    showDivider = false
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 4. 开始扫描入口
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(AccentColor)
                .clickable { onStartScan() }
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Radar,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "开始扫描",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        // 5. 底部声明
        Text(
            text = "本应用仅用于学习与安全研究目的，请勿用于任何违反法律法规的场景。\n使用本工具所产生的一切后果由使用者自行承担。",
            fontSize = 12.sp,
            color = SubTitleColor,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StatusCard(allGranted: Boolean, grantedCount: Int, total: Int) {
    val bg = if (allGranted) StatusGreenBg else StatusYellowBg
    val fg = if (allGranted) StatusGreenFg else StatusYellowFg
    val icon = if (allGranted) Icons.Filled.VerifiedUser else Icons.Filled.Warning
    val title = if (allGranted) "全部权限已就绪" else "部分权限缺失"
    val subtitle = if (allGranted) {
        "环境检查通过，可以开始扫描应用行为"
    } else {
        "请先授予缺失的权限（$grantedCount/$total）"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(bg)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(36.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = fg
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = fg.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    statusText: String,
    granted: Boolean,
    onGrant: () -> Unit,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TitleColor
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (granted) Icons.Filled.Check else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (granted) OkGreen else ErrRed,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = statusText,
                        fontSize = 13.sp,
                        color = if (granted) OkGreen else ErrRed
                    )
                }
            }

            if (granted) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(AccentColor.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "已授权",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentColor
                    )
                }
            } else {
                Button(
                    onClick = onGrant,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                ) {
                    Text(
                        text = "去授权",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(DividerColor)
            )
        }
    }
}
