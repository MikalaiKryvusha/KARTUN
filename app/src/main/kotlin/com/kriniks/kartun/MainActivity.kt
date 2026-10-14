package com.kriniks.kartun

import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kriniks.kartun.ui.HomeScreen
import com.kriniks.kartun.ui.ManualScreen
import com.kriniks.kartun.ui.ResultScreen
import com.kriniks.kartun.ui.SettingsScreen
import com.kriniks.kartun.ui.SetupScreen
import com.kriniks.kartun.ui.WorkingScreen
import com.kriniks.kartun.ui.theme.KartunTheme

/**
 * The one activity: the main path of phase 2 (plans/03) - home → setup → processing → result, switched by
 * [UpscaleViewModel.screen]. Photos come from the system photo picker (no storage permission, plans/01 §2.3) or from
 * another app's «Поделиться» (ACTION_SEND). An adb launch with the `in` extra runs the phase 0 [EngineProbe]. [TESTED: 2026-10-14 · walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md]
 */
class MainActivity : ComponentActivity() {

    private val vm: UpscaleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The phase 0 adb probe exists only in debuggable builds (debug, stage); a release ignores the `in` extra.
        if (BuildConfig.DEBUG && EngineProbe.runIfRequested(this, intent)) return
        if (savedInstanceState == null) sharedImage(intent)?.let(vm::open)
        setContent { KartunTheme { KartunApp(vm) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        sharedImage(intent)?.let(vm::open)
    }

    /** The image another app sent with «Поделиться → KARTUN», if any. */
    private fun sharedImage(intent: Intent): Uri? =
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true)
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else null
}

@Composable
private fun KartunApp(vm: UpscaleViewModel) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(vm::open) }
    val pick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    when (vm.screen) {
        UpscaleViewModel.Screen.Home -> {
            val err = vm.error?.let { (res, arg) -> if (arg != null) stringResource(res, arg) else stringResource(res) }
            val clipboard = LocalContext.current.getSystemService(ClipboardManager::class.java)
            HomeScreen(
                onPick = pick,
                onSettings = vm::openSettings,
                onPasteLink = { vm.openLink(clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text) },
                downloading = vm.downloading, error = err, onDismissError = vm::dismissError,
            )
        }
        UpscaleViewModel.Screen.Setup -> SetupScreen(vm, onPickOther = pick)
        UpscaleViewModel.Screen.Working -> WorkingScreen(vm)
        UpscaleViewModel.Screen.Result -> ResultScreen(vm)
        UpscaleViewModel.Screen.Settings -> SettingsScreen(vm, onBack = vm::back)
        UpscaleViewModel.Screen.Manual -> ManualScreen(onBack = vm::back)
    }
}
