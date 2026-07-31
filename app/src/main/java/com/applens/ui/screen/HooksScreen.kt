package com.applens.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.applens.data.HookRule
import com.applens.ui.theme.*

@Composable
fun HooksScreen(
    rules: List<HookRule>,
    controllerMode: String,
    onApply: (HookRule) -> Unit,
    onRevert: (HookRule) -> Unit,
    onRemove: (HookRule) -> Unit,
    onBatchApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // 顶部：大标题 + 批量执行按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hook 管理",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            if (rules.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentBlue)
                        .clickable(onClick = onBatchApply)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "批量执行",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        // 控制器模式
        Text(
            text = "控制模式: $controllerMode",
            modifier = Modifier.padding(start = 20.dp, end = 16.dp, bottom = 12.dp),
            fontSize = 14.sp,
            color = TextSecondary
        )

        if (rules.isEmpty()) {
            // 空状态提示
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 56.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "暂无 Hook 规则",
                        fontSize = 16.sp,
                        color = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "在 Activity 详情中添加规则后，将在此处管理",
                        fontSize = 13.sp,
                        color = TextTertiary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
            ) {
                items(
                    items = rules,
                    key = { "${it.packageName}/${it.activityFullName}" }
                ) { rule ->
                    HookRuleCard(
                        rule = rule,
                        onApply = onApply,
                        onRevert = onRevert,
                        onRemove = onRemove
                    )
                }
            }
        }
    }
}

@Composable
private fun HookRuleCard(
    rule: HookRule,
    onApply: (HookRule) -> Unit,
    onRevert: (HookRule) -> Unit,
    onRemove: (HookRule) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ActionLabel(rule.action)

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = rule.activityFullName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = rule.packageName,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionButton(
                    text = "执行",
                    containerColor = AccentBlueLight,
                    contentColor = AccentBlue,
                    onClick = { onApply(rule) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(
                    text = "撤销",
                    containerColor = StatusYellowBg,
                    contentColor = StatusYellow,
                    onClick = { onRevert(rule) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                ActionButton(
                    text = "移除",
                    containerColor = StatusRedBg,
                    contentColor = StatusRed,
                    onClick = { onRemove(rule) }
                )
            }
        }
    }
}

@Composable
private fun ActionLabel(action: HookRule.Action) {
    val (text, bgColor, fgColor) = when (action) {
        HookRule.Action.DISABLE -> Triple("禁用", StatusRedBg, StatusRed)
        HookRule.Action.ENABLE -> Triple("启用", StatusGreenBg, StatusGreen)
        HookRule.Action.BLOCK -> Triple("阻止", StatusYellowBg, StatusYellow)
        HookRule.Action.HOOK_TAG -> Triple("标记", AccentBlueLight, AccentBlue)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = fgColor
        )
    }
}

@Composable
private fun ActionButton(
    text: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}
