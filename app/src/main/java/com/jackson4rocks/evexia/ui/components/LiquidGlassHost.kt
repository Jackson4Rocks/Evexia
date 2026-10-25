package com.jackson4rocks.evexia.ui.components

import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop

/**
 * Captures the screen content so the liquid-glass bar can sample what is
 * actually behind it. Only loaded on API 33+ where miuix-blur is supported;
 * older devices use the lightweight fallback bar and never touch this class.
 */
@RequiresApi(33)
@Composable
fun LiquidGlassHost(
    enabled: Boolean,
    content: @Composable () -> Unit,
    overlay: @Composable (Backdrop) -> Unit,
) {
    val backdrop = rememberLayerBackdrop()
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .then(if (enabled) Modifier.layerBackdrop(backdrop) else Modifier),
        ) {
            content()
        }
        Box(Modifier.fillMaxSize()) {
            overlay(backdrop)
        }
    }
}
