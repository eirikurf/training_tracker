package com.ericf.treinociclico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ericf.treinociclico.ui.TrainingTrackerApp
import com.ericf.treinociclico.ui.theme.TrainingTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrainingTrackerTheme {
                TrainingTrackerApp()
            }
        }
    }
}
