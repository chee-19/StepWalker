package com.cheewei.stepwalker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cheewei.stepwalker.ui.theme.StepWalkerTheme

/** Privacy explanation opened from Health Connect's permission screen. */
class PermissionsRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StepWalkerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("StepWalker step data", style = MaterialTheme.typography.headlineMedium)
                        Text("StepWalker reads today's step total from Health Connect to display your walking progress. It requests only permission to read steps and does not write health data.")
                        Text("This version reads data while the app is open. It does not save your step data or send it to a server. You can revoke access at any time in Health Connect settings.")
                        Button(onClick = { finish() }) { Text("Close") }
                    }
                }
            }
        }
    }
}
