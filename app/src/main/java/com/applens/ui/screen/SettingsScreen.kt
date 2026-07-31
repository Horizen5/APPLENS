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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.applens.core.ControllerType
import com.applens.ui.theme.*

@Composable
fun SettingsScreen(
    followSystem: Boolean,
    onFollowSystemChange: (Boolean) -> Unit,
    controllerType: ControllerType,
    onControllerTypeChange: (ControllerType) -> Unit,
    animSpeed: Float,
    onAnimSpeedChange: (Float) -> Unit,
    versionName: String,
    onAboutClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 28.dp, bottom = 36.dp)
    ) {
        Text(
            text = "设置",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // 1. 通用
        SectionTitle("通用")
        SettingsCard {
            SwitchRow(
                title = "跟随系统",
                subtitle = "深色模式跟随系统设置",
                checked = followSystem,
                onCheckedChange = onFollowSystemChange
            )
        }

        // 2. 控制模式
        SectionTitle("控制模式")
        SettingsCard {
            RadioRow(
                title = ControllerType.PM.displayName,
                selected = controllerType == ControllerType.PM,
                onClick = { onControllerTypeChange(ControllerType.PM) }
            )
            ThinDivider()
            RadioRow(
                title = ControllerType.IFW.displayName,
                selected = controllerType == ControllerType.IFW,
                onClick = { onControllerTypeChange(ControllerType.IFW) }
            )
            ThinDivider()
            RadioRow(
                title = ControllerType.IFW_PLUS_PM.displayName,
                selected = controllerType == ControllerType.IFW_PLUS_PM,
                onClick = { onControllerTypeChange(ControllerType.IFW_PLUS_PM) },
                badge = "推荐"
            )
            Spacer(Modifier.height(12.dp))
            DescriptionBox(text = controllerType.description)
        }

        // 3. 动画
        SectionTitle("动画")
        SettingsCard {
            SliderRow(speed = animSpeed, onSpeedChange = onAnimSpeedChange)
        }

        // 4. 关于
        SectionTitle("关于")
        SettingsCard {
            InfoRow(title = "版本", value = versionName)
            ThinDivider()
            ArrowRow(title = "关于", onClick = onAboutClick)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = TextTertiary,
        modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
private fun ThinDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(DividerColor)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentBlue,
                checkedBorderColor = AccentBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE0E0E0),
                uncheckedBorderColor = Color(0xFFBDBDBD)
            )
        )
    }
}

@Composable
private fun RadioRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    badge: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = AccentBlue,
                unselectedColor = Color(0xFFBDBDBD)
            )
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        if (badge != null) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(AccentBlueLight)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccentBlue
                )
            }
        }
    }
}

@Composable
private fun DescriptionBox(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AccentBlueLight)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun SliderRow(
    speed: Float,
    onSpeedChange: (Float) -> Unit
) {
    val speedText = "%.2f".format(java.util.Locale.US, speed) + "x"
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "动画速度",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = speedText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentBlue
            )
        }
        Spacer(Modifier.height(12.dp))
        Slider(
            value = speed,
            onValueChange = onSpeedChange,
            valueRange = 0f..2f,
            colors = SliderDefaults.colors(
                thumbColor = AccentBlue,
                activeTrackColor = AccentBlue,
                inactiveTrackColor = Color(0xFFE0E0E0)
            )
        )
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = value,
            fontSize = 14.sp,
            color = TextTertiary
        )
    }
}

@Composable
private fun ArrowRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(22.dp)
        )
    }
}
