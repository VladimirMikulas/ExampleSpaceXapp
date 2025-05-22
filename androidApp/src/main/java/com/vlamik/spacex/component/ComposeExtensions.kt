package com.vlamik.spacex.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.vlamik.core.commons.AppText
import com.vlamik.spacex.common.utils.asString

/**
 * Compose extension property to resolve UiText to a String within a Composable function.
 * @return The resolved String.
 */
@Composable
fun AppText.asString(): String {
    val context = LocalContext.current
    return this.asString(context)
}