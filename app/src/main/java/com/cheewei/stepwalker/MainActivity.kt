package com.cheewei.stepwalker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cheewei.stepwalker.data.HealthConnectStepsReader
import com.cheewei.stepwalker.ui.StepsScreen
import com.cheewei.stepwalker.ui.theme.StepWalkerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val stepsReader = HealthConnectStepsReader(applicationContext)
        setContent {
            StepWalkerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    StepsScreen(stepsReader)
                }
            }
        }
    }
}
