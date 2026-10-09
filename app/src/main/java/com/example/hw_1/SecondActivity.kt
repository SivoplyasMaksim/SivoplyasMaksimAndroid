package com.example.hw_1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hw_1.ui.theme.HW_1Theme

class SecondActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val receivedString = intent.getStringExtra("EXTRA_DATA") ?: "Данные не переданы"

        setContent {
            HW_1Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SecondScreen(receivedData = receivedString)
                }
            }
        }
    }
}

@Composable
fun SecondScreen(receivedData: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = receivedData,
            style = MaterialTheme.typography.headlineSmall
        )
    }
}
