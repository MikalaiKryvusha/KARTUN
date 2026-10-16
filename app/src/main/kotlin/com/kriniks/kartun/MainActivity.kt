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
import com.kriniks.kartun.ui.AboutScreen
import com.kriniks.kartun.ui.HomeScreen
import com.kriniks.kartun.ui.LinkDialog
import com.kriniks.kartun.ui.ManualScreen
import com.kriniks.kartun.ui.ModelsScreen
import com.kriniks.kartun.ui.ResultScreen
import com.kriniks.kartun.ui.SettingsScreen
import com.kriniks.kartun.ui.SetupScreen
import com.kriniks.kartun.ui.WorkingScreen
import com.kriniks.kartun.ui.theme.KartunTheme

/**
 * The one activity: the main path of phase 2 (plans/03) - home → setup → processing → result, switched by
 * [UpscaleViewModel.screen]. Photos come from the system photo picker (no storage permission, plans/01 §2.3) or from
 * another app's «Поделиться» (ACTION_SEND). An adb launch with the `in` extra opens the debug build's engine probe screen
 * ([DebugTools]). [TESTED: 2026-10-14 · walked by touch on the Titan - testcases/reports/2026-10-14_F2_screens_slice1.md]
 */
class MainActivity : ComponentActivity() {

    private val vm: UpscaleViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // The adb engine probe (tools/titan-probe.mjs, the `in` extra) opens the debug build's probe screen; stage and
        // release ignore it (src/nodebug/DebugTools).
        if (savedInstanceState == null) DebugTools.onIntent(this, intent)
        if (savedInstanceState == null) sharedImage(intent)?.let(vm::open)
        setContent { KartunTheme { if (!DebugTools.Overlay()) KartunApp(vm) } }
    }

    override fun onStart() { super.onStart(); ProcessingState.appVisible = true }

    override fun onStop() { ProcessingState.appVisible = false; super.onStop() }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        sharedImage(intent)?.let(vm::open)
    }

    /** The image another app sent with «Поделиться → KARTUN» (ACTION_SEND) or opened with «Открыть с помощью → KARTUN»
     *  (ACTION_VIEW); the manifest's filters decide the types - images and application/octet-stream (a TGA has no
     *  type of its own on Android); a file that is not a picture ends in the decoder's honest error. */
    private fun sharedImage(intent: Intent): Uri? = when (intent.action) {
        Intent.ACTION_SEND -> intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        Intent.ACTION_VIEW -> intent.data
        else -> null
    }
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
            if (vm.linkDialog) LinkDialog(onDownload = vm::downloadLink, onClose = vm::closeLinkDialog)
        }
        UpscaleViewModel.Screen.Setup -> SetupScreen(vm, onPickOther = pick)
        UpscaleViewModel.Screen.Working -> WorkingScreen(vm)
        UpscaleViewModel.Screen.Result -> ResultScreen(vm)
        UpscaleViewModel.Screen.Settings -> SettingsScreen(vm, onBack = vm::back)
        UpscaleViewModel.Screen.Manual -> ManualScreen(onBack = vm::back)
        UpscaleViewModel.Screen.About -> AboutScreen(onBack = vm::back)
        UpscaleViewModel.Screen.Models -> ModelsScreen(vm)
        UpscaleViewModel.Screen.Terms -> ManualScreen(onBack = vm::back, raw = R.raw.terms, title = R.string.terms_title)
    }
}
