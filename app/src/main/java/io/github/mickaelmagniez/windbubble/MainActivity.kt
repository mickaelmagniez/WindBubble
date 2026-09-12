package io.github.mickaelmagniez.windbubble

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.mickaelmagniez.windbubble.ui.navigation.WindBubbleNavHost
import io.github.mickaelmagniez.windbubble.ui.theme.WindBubbleTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            WindBubbleTheme {
                Surface(Modifier.fillMaxSize()) {
                    WindBubbleNavHost()
                }
            }
        }
    }
}
