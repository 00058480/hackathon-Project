package com.example.hackathonproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.hackathonproject.ui.theme.HackathonProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HackathonProjectTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BillSplitterScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
