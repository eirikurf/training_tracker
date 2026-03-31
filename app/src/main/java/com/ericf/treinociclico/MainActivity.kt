package com.ericf.treinociclico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ericf.treinociclico.ui.TrainingTrackerApp
import com.ericf.treinociclico.ui.TrainingViewModel
import com.ericf.treinociclico.ui.theme.TrainingTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val trainingViewModel: TrainingViewModel = viewModel(
                factory = TrainingViewModel.factory(applicationContext),
            )
            TrainingTrackerTheme {
                TrainingTrackerApp(trainingViewModel)
            }
        }
    }
}
