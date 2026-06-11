package com.adam.habituator.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.adam.habituator.HabituatorApplication
import com.adam.habituator.data.AppContainer

@Composable
fun rememberAppContainer(): AppContainer =
    (LocalContext.current.applicationContext as HabituatorApplication).container
