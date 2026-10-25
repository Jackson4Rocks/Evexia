package com.jackson4rocks.evexia.ui.components

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jackson4rocks.evexia.ui.AppScreen
import com.jackson4rocks.evexia.ui.theme.DF
import com.jackson4rocks.evexia.ui.theme.LocalReduceMotion
import top.yukonga.miuix.kmp.blur.Backdrop

private data class BottomBarTab(
    val screen: AppScreen,
    val icon: ImageVector,
    val label: String,
)

private val tabs = listOf(
    BottomBarTab(AppScreen.Home, Icons.Outlined.Home, "Home"),
    BottomBarTab(AppScreen.Subjects, Icons.Outlined.FolderOpen, "Subjects"),
    BottomBarTab(AppScreen.Timetable, Icons.Outlined.CalendarMonth, "Week"),
    BottomBarTab(AppScreen.Tasks, Icons.Outlined.CheckCircle, "Tasks"),
    BottomBarTab(AppScreen.FlowAi, Icons.Outlined.AutoAwesome, "Evexia AI"),
)

@Composable
fun EvexiaBottomBar(
    selectedRoot: AppScreen,
    onSelect: (AppScreen) -> Unit,
    backdrop: Any?,
    modifier: Modifier = Modifier,
) {
    val colors = DF.colors
    val type = DF.type
    val reduceMotion = LocalReduceMotion.current
    val selectedIndex = tabs.indexOfFirst { it.screen == selectedRoot }.coerceAtLeast(0)

    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        val glassBackdrop = backdrop as? Backdrop
        if (
            !reduceMotion &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            glassBackdrop != null
        ) {
            LiquidGlassBottomBar(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                selectedIndex = selectedIndex,
                onSelected = { index -> onSelect(tabs[index].screen) },
                tabsCount = tabs.size,
                backdrop = glassBackdrop,
                isBlurEnabled = true,
            ) { activateTab ->
                tabs.forEachIndexed { index, tab ->
                    LiquidGlassBottomBarItem(
                        selected = index == selectedIndex,
                        onClick = { activateTab(index) },
                    ) {
                        val contentColor = LocalBottomBarContentColor.current
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp),
                        )
                        BasicText(
                            text = tab.label,
                            style = type.caption.copy(
                                color = contentColor,
                                fontSize = 9.5.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }
            }
        } else {
            CalculatorVaultBottomBar(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                selectedIndex = selectedIndex,
                onSelected = { index -> onSelect(tabs[index].screen) },
                tabsCount = tabs.size,
                containerColor = colors.silhouette,
                accentColor = colors.accentInk,
                tabContentColor = colors.textSecondary,
            ) { activateTab ->
                tabs.forEachIndexed { index, tab ->
                    CalculatorVaultBottomBarItem(
                        selected = index == selectedIndex,
                        onClick = { activateTab(index) },
                    ) {
                        val contentColor = LocalBottomBarContentColor.current
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp),
                        )
                        BasicText(
                            text = tab.label,
                            style = type.caption.copy(
                                color = contentColor,
                                fontSize = 9.5.sp,
                                lineHeight = 11.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }
            }
        }
    }
}
