package com.example.hackathonproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.hackathonproject.ui.navigation.BillSplitterApp
import com.example.hackathonproject.ui.theme.HackathonProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HackathonProjectTheme {
                BillSplitterApp()
            }
        }
    }
}
