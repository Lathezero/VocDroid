package com.example.ankiclone.data.local

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Compose 友好的英文发音 helper，基于安卓框架内置的 TextToSpeech（零依赖、无需额外权限）。
 *
 * 用法：
 * ```
 * val speak = rememberTts()
 * IconButton(onClick = { speak(word) }) { ... }
 * ```
 *
 * 引擎会在进入组合时创建、离开组合时自动释放；初始化完成前调用会被安全忽略。
 */
@Composable
fun rememberTts(): (String) -> Unit {
    val context = LocalContext.current
    // 用可变状态持有引擎实例与就绪标记，初始化是异步的。
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
            }
        }
        engine.language = Locale.US
        tts = engine

        onDispose {
            engine.stop()
            engine.shutdown()
            tts = null
            ready = false
        }
    }

    return remember {
        speak@{ text: String ->
            val engine = tts
            if (!ready || engine == null || text.isBlank()) return@speak
            engine.language = Locale.US
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts-${text.hashCode()}")
        }
    }
}
