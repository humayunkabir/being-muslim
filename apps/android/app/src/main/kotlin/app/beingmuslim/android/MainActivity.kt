package app.beingmuslim.android

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import app.beingmuslim.android.ui.AppRoot
import app.beingmuslim.android.ui.theme.BeingMuslimTheme

// AppCompatActivity rather than ComponentActivity: it is what applies the
// user's chosen app language on API 32 and below (issue #14).
class MainActivity : AppCompatActivity() {
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
