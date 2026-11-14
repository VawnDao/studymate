package com.studymate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.CompositionLocalProvider
import com.studymate.app.ui.StudyMateApp
import com.studymate.app.ui.adaptive.LocalWidthClass
import com.studymate.app.ui.adaptive.WidthClass
import com.studymate.app.ui.theme.StudyMateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // Tính lại khi xoay màn hình, đổi kích thước cửa sổ hay mở máy gập.
            val widthClass = WidthClass.from(calculateWindowSizeClass(this).widthSizeClass)
            CompositionLocalProvider(LocalWidthClass provides widthClass) {
                StudyMateTheme {
                    StudyMateApp()
                }
            }
        }
    }
}
