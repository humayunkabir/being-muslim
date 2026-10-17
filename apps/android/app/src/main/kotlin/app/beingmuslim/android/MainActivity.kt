package app.beingmuslim.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.beingmuslim.android.ui.AppRoot
import app.beingmuslim.android.ui.theme.BeingMuslimTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeingMuslimTheme {
                AppRoot()
            }
        }
    }
}
