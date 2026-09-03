package com.fitlog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fitlog.app.ui.nav.AppNav
import com.fitlog.app.ui.theme.FitLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as FitLogApp
        setContent {
            val dark by app.settings.darkMode.collectAsStateWithLifecycle(initialValue = false)
            FitLogTheme(dark = dark) {
                AppNav()
            }
        }
    }
}
