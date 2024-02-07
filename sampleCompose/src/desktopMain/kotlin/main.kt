import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.darkrockstudios.symspellkt.sample.App

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "SymSpellKtSample") {
        App()
    }
}

@Preview
@Composable
fun AppDesktopPreview() {
    App()
}