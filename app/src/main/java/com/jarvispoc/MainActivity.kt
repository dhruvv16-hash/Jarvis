package com.jarvispoc

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.jarvispoc.ai.CaptionEngine
import com.jarvispoc.ai.CaptionEngines
import com.jarvispoc.ai.ModelLocator
import com.jarvispoc.core.AgentLog
import com.jarvispoc.core.FlowResult
import com.jarvispoc.core.LogEntry
import com.jarvispoc.core.LogLevel
import com.jarvispoc.core.Photos
import com.jarvispoc.flows.AmazonOrderFlow
import com.jarvispoc.flows.Flow
import com.jarvispoc.flows.InstagramPostFlow
import com.jarvispoc.service.JarvisAccessibilityService
import com.jarvispoc.service.Notifier
import com.jarvispoc.voice.VoiceCommand
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Process-scoped, not Activity-scoped, and never closed here: the model
        // costs several GB and tens of seconds to load, so tying its lifetime
        // to this Activity would rebuild it on every configuration change.
        val captionEngine = CaptionEngines.shared(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    ControlPanel(captionEngine)
                }
            }
        }
    }
}

@Composable
private fun ControlPanel(captionEngine: CaptionEngine) {
    val context = LocalContext.current
    val uiScope = rememberCoroutineScope()

    var serviceBound by remember { mutableStateOf(false) }
    var serviceEnabledInSettings by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf("") }
    var runningFlow by remember { mutableStateOf<Job?>(null) }

    // Two separate gates, deliberately. Buying something and posting something
    // are different decisions with different blast radii.
    var autoPlaceOrder by remember { mutableStateOf(false) }
    var autoSharePost by remember { mutableStateOf(true) }

    var amazonQuery by remember { mutableStateOf("usb c cable") }

    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var shareUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }
    var captionStyle by remember { mutableStateOf("warm, understated, a little witty") }
    var modelStatus by remember { mutableStateOf("checking…") }
    var heardText by remember { mutableStateOf("") }
    // Off by default: forcing offline makes the recogniser refuse outright on a
    // device with no offline language pack, rather than falling back.
    var preferOfflineSpeech by remember { mutableStateOf(false) }

    val logs by AgentLog.entries.collectAsState()

    val requestNotifications = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* notifications are best-effort; the toast fallback always works */ }

    LaunchedEffect(Unit) {
        // Both of these touch the filesystem / a ContentProvider. LaunchedEffect
        // runs on Main, and the poll below repeats once a second for the life of
        // the screen — that is a lot of main-thread IPC to do for a status line.
        modelStatus = withContext(Dispatchers.IO) { ModelLocator.describe(context) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        while (true) {
            val bound = JarvisAccessibilityService.instance != null
            val enabled = withContext(Dispatchers.IO) {
                JarvisAccessibilityService.isEnabled(context)
            }
            serviceBound = bound
            serviceEnabledInSettings = enabled
            delay(1_000)
        }
    }

    // ---- actions (declared before the launchers that call them) -------------

    /**
     * @return true if the flow was actually started. Callers that hand ownership
     *   of [busy] over to the flow MUST check this — a silent false here with an
     *   assumed hand-off leaves `busy` stuck true and every button disabled.
     */
    fun startFlow(flow: Flow, autoConfirm: Boolean): Boolean {
        val service = JarvisAccessibilityService.instance
        if (service == null) {
            AgentLog.error("accessibility service is not running — enable it first")
            return false
        }
        busy = true
        lastResult = ""
        AgentLog.info("--- ${flow.name} starting (autoConfirm=$autoConfirm) ---")
        // Runs on the *service* scope, not the Activity's: the flow must keep
        // going after we drop to the background and the target app takes over.
        runningFlow = service.scope.launch {
            try {
                val result = runCatching { flow.run(service.executor, autoConfirm) }
                    .getOrElse {
                        if (it is CancellationException) throw it
                        FlowResult.Failed("unexpected", "${it.javaClass.simpleName}: ${it.message}")
                    }

                when (result) {
                    is FlowResult.Success -> AgentLog.success(result.summary)
                    is FlowResult.AwaitingUser -> AgentLog.halt(result.summary)
                    is FlowResult.Failed -> {
                        AgentLog.error(result.summary)
                        // Capture the screen we died on — this is the dump
                        // that tells us which selector to fix.
                        service.executor.dumpScreen("failure-${flow.name.replace(' ', '-')}")
                    }
                }

                // The target app owns the screen right now, so say it out loud
                // rather than only writing to a trace nobody can see.
                Notifier.announce(service, flow.name, result.summary)
                withContext(NonCancellable + Dispatchers.Main) { lastResult = result.summary }
            } finally {
                // Must survive cancellation, or Stop leaves the UI wedged.
                withContext(NonCancellable + Dispatchers.Main) { busy = false }
            }
        }
        return true
    }

    fun stopFlow() {
        runningFlow?.cancel()
        runningFlow = null
        AgentLog.warn("flow cancelled by user")
    }

    fun dumpAfterDelay(tag: String) {
        val service = JarvisAccessibilityService.instance
        if (service == null) {
            AgentLog.error("accessibility service is not running — enable it first")
            return
        }
        service.scope.launch {
            AgentLog.info("dumping in ${DUMP_DELAY_MS / 1000}s — switch to the screen you want captured")
            delay(DUMP_DELAY_MS)
            service.executor.dumpScreen(tag)
            Notifier.toast(service, "Screen dumped")
        }
    }

    /**
     * Photo -> caption -> post, driven by what was spoken.
     *
     * Each stage bails with a specific message rather than a generic failure:
     * with three chained stages, "it didn't work" is useless, and the whole
     * point of the trace is to say *which* stage stopped and why.
     */
    fun runVoiceCommand(cmd: VoiceCommand) {
        AgentLog.info("heard: \"${cmd.raw}\"")
        AgentLog.info("parsed: ${cmd.summary}")

        when (cmd.target) {
            VoiceCommand.Target.AMAZON -> {
                val product = cmd.searchQuery
                if (product.isNullOrBlank()) {
                    AgentLog.error(
                        "heard an Amazon request but no product — say what to buy, " +
                            "e.g. \"order a USB C cable on Amazon\". Guessing is not an " +
                            "option when money is involved."
                    )
                    return
                }
                // Mirror it into the card so the parsed product is visible, not
                // just spoken — a mis-transcription should be obvious on screen.
                amazonQuery = product
                if (autoPlaceOrder) {
                    AgentLog.warn(
                        "VOICE ORDER with auto-confirm ON — will place a real COD order " +
                            "for \"$product\" if Pay on Delivery can be confirmed"
                    )
                }
                startFlow(AmazonOrderFlow(product), autoPlaceOrder)
                return
            }
            VoiceCommand.Target.UNKNOWN -> {
                AgentLog.error("could not tell which app you meant — say \"Instagram\" explicitly")
                return
            }
            VoiceCommand.Target.INSTAGRAM -> Unit
        }

        busy = true
        uiScope.launch {
            var handedOff = false
            try {
                // 1 — photo
                if (cmd.useMostRecentPhoto) {
                    if (!hasMediaPermission(context)) {
                        AgentLog.error("gallery access denied — cannot pick the most recent photo")
                        return@launch
                    }
                    val uri = Photos.mostRecent(context)
                    if (uri == null) {
                        AgentLog.error("no photos found in the gallery")
                        return@launch
                    }
                    val bitmap = Photos.load(context, uri)
                    if (bitmap == null) {
                        AgentLog.error("could not decode the most recent photo")
                        return@launch
                    }
                    photo = bitmap
                    shareUri = Photos.stageForShare(context, bitmap)
                    caption = ""
                    AgentLog.success("using most recent photo (${bitmap.width}x${bitmap.height})")
                }

                val bitmap = photo
                if (bitmap == null) {
                    AgentLog.error("no photo selected — say \"most recent photo\", or pick one by hand")
                    return@launch
                }

                // 2 — caption
                if (cmd.autoCaption) {
                    captionStyle = cmd.tone
                    AgentLog.info("generating caption on device (tone: ${cmd.tone})…")
                    val generated = captionEngine.caption(bitmap, cmd.tone).getOrElse { "" }
                    if (generated.isBlank()) {
                        AgentLog.error(
                            "caption generation failed — check the model line in Status above"
                        )
                        return@launch
                    }
                    caption = generated
                    AgentLog.success("caption ready")
                }
                if (caption.isBlank()) {
                    AgentLog.error("no caption to post")
                    return@launch
                }

                // 3 — post
                val uri = shareUri
                if (uri == null) {
                    AgentLog.error("photo was never staged for sharing")
                    return@launch
                }
                handedOff = startFlow(InstagramPostFlow(uri, caption.trim()), autoSharePost)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                // Without this the whole process dies and the user sees a crash
                // instead of a diagnosis. Anything unexpected in the chain
                // becomes a trace line plus a full stack in logcat.
                AgentLog.error("voice command failed unexpectedly", t)
            } finally {
                // startFlow owns `busy` from here on; clearing it would unlock
                // the UI while the flow is still running.
                if (!handedOff) busy = false
            }
        }
    }

    // ---- launchers ----------------------------------------------------------

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Decode + JPEG re-encode of a full-resolution photo is far too heavy
        // to run inline on the callback's main-thread invocation.
        uiScope.launch {
            busy = true
            try {
                val bitmap = Photos.load(context, uri)
                if (bitmap == null) {
                    AgentLog.error("photo could not be decoded")
                } else {
                    photo = bitmap
                    shareUri = Photos.stageForShare(context, bitmap)
                    caption = ""
                    AgentLog.success("photo loaded: ${bitmap.width}x${bitmap.height}")
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (t: Throwable) {
                AgentLog.error("loading the photo failed unexpectedly", t)
            } finally {
                busy = false
            }
        }
    }

    val speak = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // Claimed by onSpeakPressed to block a double launch; release it here on
        // every path. runVoiceCommand re-claims it if it actually starts work.
        busy = false
        if (result.resultCode != Activity.RESULT_OK) {
            AgentLog.warn("voice input cancelled")
            return@rememberLauncherForActivityResult
        }
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            .orEmpty()
        if (spoken.isBlank()) {
            AgentLog.warn("nothing was recognised")
            return@rememberLauncherForActivityResult
        }
        heardText = spoken
        // Parsing and dispatch run on the main thread inside this callback, so
        // an exception here would take the process down rather than surface.
        runCatching { runVoiceCommand(VoiceCommand.parse(spoken)) }
            .onFailure {
                busy = false
                AgentLog.error("could not handle the spoken command", it)
            }
    }

    val requestMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            AgentLog.warn("gallery access denied — \"most recent photo\" will not work")
        }
        // Proceed either way: the command may not need the gallery at all.
        if (!launchSpeech(context, preferOfflineSpeech, speak::launch)) busy = false
    }

    fun onSpeakPressed() {
        // `enabled = !busy` on the button is not enough: two taps can land
        // before recomposition, launching the recogniser twice and racing two
        // command chains. Claim the flag synchronously instead.
        if (busy) return
        busy = true
        val launched = if (hasMediaPermission(context)) {
            launchSpeech(context, preferOfflineSpeech, speak::launch)
        } else {
            runCatching { requestMedia.launch(mediaPermissionName()) }.isSuccess
        }
        if (!launched) busy = false
    }

    // ---- UI -----------------------------------------------------------------

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("JARVIS POC", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        // ------------------------------------------------------------ status
        item {
            SectionCard("Status") {
                StatusLine(
                    "Accessibility service",
                    when {
                        serviceBound -> "running"
                        serviceEnabledInSettings -> "enabled, waiting to bind…"
                        else -> "OFF"
                    },
                    ok = serviceBound,
                )
                StatusLine("Caption model", modelStatus, ok = !modelStatus.contains("MISSING"))
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                ) { Text("Open Accessibility settings") }

                if (busy) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = { stopFlow() }) { Text("Stop running flow") }
                }
                if (lastResult.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(lastResult, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // ------------------------------------------------------------- voice
        item {
            SectionCard("Voice command") {
                Text(
                    "Try: \"choose the most recent photo and post it on Instagram " +
                        "with a relevant caption\"",
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onSpeakPressed() },
                    enabled = !busy,
                ) { Text("Speak a command") }

                if (heardText.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("Heard: \"$heardText\"", fontSize = 13.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Say a tone to steer the caption: funny, witty, professional, " +
                        "casual, poetic, short.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = preferOfflineSpeech,
                        onCheckedChange = { preferOfflineSpeech = it },
                    )
                    Text(
                        if (preferOfflineSpeech) {
                            "  On-device speech — fails outright without an offline language pack"
                        } else {
                            "  Speech may use Google's servers. Turn on to force on-device."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // ------------------------------------------------------------ amazon
        item {
            SectionCard("1 · Amazon — search, match & add to cart") {
                OutlinedTextField(
                    value = amazonQuery,
                    onValueChange = { amazonQuery = it },
                    label = { Text("Product to search") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Matches product title, verifies on product page, adds to cart, and verifies cart.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { startFlow(AmazonOrderFlow(amazonQuery.trim()), true) },
                    enabled = (serviceBound || serviceEnabledInSettings) && !busy && amazonQuery.isNotBlank(),
                ) { Text("Search & Add to Cart") }
            }
        }

        // --------------------------------------------------------- instagram
        item {
            SectionCard("2 · Instagram — caption and post") {
                Button(
                    onClick = {
                        pickPhoto.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !busy,
                ) { Text(if (photo == null) "Pick a photo" else "Pick a different photo") }

                photo?.let { bitmap ->
                    Spacer(Modifier.height(8.dp))
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "selected photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = captionStyle,
                    onValueChange = { captionStyle = it },
                    label = { Text("Caption tone") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val bitmap = photo ?: return@Button
                        busy = true
                        uiScope.launch {
                            try {
                                AgentLog.info("generating caption on device…")
                                val result = captionEngine.caption(bitmap, captionStyle)
                                caption = result.getOrElse { "" }
                                if (result.isSuccess) AgentLog.success("caption ready")
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (t: Throwable) {
                                AgentLog.error("caption generation failed unexpectedly", t)
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = photo != null && !busy,
                ) { Text("Generate caption (on-device)") }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Caption — edit before posting") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))
                DangerToggle(
                    checked = autoSharePost,
                    onCheckedChange = { autoSharePost = it },
                    onText = "Will tap Share — the post goes live",
                    offText = "Fills the caption in, stops before Share.",
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        uiScope.launch {
                            val uri = shareUri ?: photo?.let { Photos.stageForShare(context, it) }
                            if (uri == null) {
                                AgentLog.error("no photo available to share")
                                return@launch
                            }
                            shareUri = uri
                            startFlow(InstagramPostFlow(uri, caption.trim()), autoSharePost)
                        }
                    },
                    enabled = (serviceBound || serviceEnabledInSettings) && !busy && photo != null && caption.isNotBlank(),
                ) { Text("Send to Instagram") }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Use a throwaway account. Instagram bans accounts it detects as automated.",
                    fontSize = 12.sp,
                    color = Color(0xFFFF8A65),
                )
            }
        }

        // ------------------------------------------------------------- debug
        item {
            SectionCard("Debug — screen dumper") {
                Text(
                    "Press a button, then switch to the screen you want captured. " +
                        "Dumps land in Android/data/com.jarvispoc/files/dumps/. " +
                        "A failed flow dumps automatically.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { dumpAfterDelay("amazon") },
                        enabled = serviceBound,
                    ) { Text("Dump (amazon)") }
                    OutlinedButton(
                        onClick = { dumpAfterDelay("instagram") },
                        enabled = serviceBound,
                    ) { Text("Dump (instagram)") }
                }
            }
        }

        // --------------------------------------------------------------- log
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Trace", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = { AgentLog.clear() }) { Text("Clear") }
            }
        }

        items(logs.asReversed()) { entry -> LogRow(entry) }
    }
}

// ---- helpers ---------------------------------------------------------------

private fun mediaPermissionName(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        @Suppress("DEPRECATION")
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

private fun hasMediaPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, mediaPermissionName()) ==
        PackageManager.PERMISSION_GRANTED

/**
 * @param preferOffline keeps recognition on-device, which is what the project's
 *   no-cloud-AI rule wants — but it is NOT a soft preference. With no offline
 *   language pack installed for the current locale the recogniser refuses
 *   outright ("Voice search isn't available") rather than falling back to the
 *   network, so it defaults to off and is opt-in from the UI.
 */
private fun launchSpeech(
    context: Context,
    preferOffline: Boolean,
    launch: (Intent) -> Unit,
): Boolean {
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
        AgentLog.error(
            "no speech recogniser is installed or enabled on this device — check that " +
                "the Google app is enabled, or type the caption by hand"
        )
        return false
    }
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
        )
        putExtra(RecognizerIntent.EXTRA_PROMPT, "What should JARVIS do?")
        // Some recognisers refuse a request with no explicit language.
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
        if (preferOffline) {
            AgentLog.info("requesting on-device recognition (needs an offline language pack)")
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
    }
    return runCatching { launch(intent) }.onFailure {
        AgentLog.error(
            "could not start the recogniser (${it.javaClass.simpleName}: ${it.message})"
        )
    }.isSuccess
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun DangerToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onText: String,
    offText: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            "  ${if (checked) onText else offText}",
            fontSize = 12.sp,
            color = if (checked) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatusLine(label: String, value: String, ok: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("$label: ", fontSize = 13.sp)
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (ok) Color(0xFF7FE3B0) else Color(0xFFFF8A65),
        )
    }
}

@Composable
private fun LogRow(entry: LogEntry) {
    val color = when (entry.level) {
        LogLevel.ERROR -> Color(0xFFFF6B6B)
        LogLevel.WARN -> Color(0xFFFFC14D)
        LogLevel.HALT -> Color(0xFFFFA726)
        LogLevel.SUCCESS -> Color(0xFF7FE3B0)
        LogLevel.STEP -> Color(0xFF9FC7FF)
        LogLevel.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        "${timeFormat.format(Date(entry.at))}  ${entry.message}",
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        color = color,
    )
}

private const val DUMP_DELAY_MS = 5_000L

private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
