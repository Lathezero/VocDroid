package com.example.ankiclone.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ankiclone.data.api.AddCardsRequest
import com.example.ankiclone.data.api.CardRequest
import com.example.ankiclone.data.api.Deck
import com.example.ankiclone.data.api.DeckRequest
import com.example.ankiclone.data.api.RetrofitClient
import com.example.ankiclone.data.api.TranslateWordsRequest
import com.example.ankiclone.ui.components.AppScreen
import com.example.ankiclone.ui.components.BackTextButton
import com.example.ankiclone.ui.components.EmptyStateCard
import com.example.ankiclone.ui.components.GlassCard
import com.example.ankiclone.ui.components.PrimaryButton
import com.example.ankiclone.ui.components.ScreenHeader
import com.example.ankiclone.ui.components.SecondaryButton
import com.example.ankiclone.ui.components.SectionCard
import com.example.ankiclone.ui.components.StatChip
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import retrofit2.HttpException
import java.net.URLEncoder

private const val OCR_TRANSLATION_BATCH_SIZE = 100
private val ocrWordRegex = Regex("""[A-Za-z]+(?:[’'`-][A-Za-z]+)*""")
private val ocrLinePrefixRegex = Regex("""^\s*(?:\d+[\.)、]|\(?[a-zA-Z]\)|[-*•·])\s*""")
private val ignoredOcrTokens = setOf(
    "a", "an", "n", "v", "adj", "adv", "prep", "conj", "pron", "num", "art", "int", "vi", "vt",
    "noun", "verb", "adjective", "adverb", "preposition", "conjunction", "pronoun",
    "am", "is", "are", "was", "were", "be", "been", "being", "do", "does", "did",
    "the", "this", "that", "these", "those", "and", "or", "but", "so", "if", "to",
    "of", "in", "on", "at", "by", "for", "with", "from", "as", "it", "its", "they",
    "their", "them", "he", "she", "his", "her", "we", "our", "you", "your",
    "word", "words", "meaning", "meanings", "translation", "translations", "page", "unit", "lesson"
)
private val ocrTranslateClient = OkHttpClient()

private fun normalizeOcrWord(rawWord: String): String {
    return rawWord
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("""^[\d\s\p{Punct}]+"""), "")
        .replace(Regex("[’‘`]"), "'")
        .replace(Regex("^[^a-z]+|[^a-z'-]+$"), "")
        .removeSuffix("'s")
        .let { word ->
            if (word.endsWith("s'")) word.dropLast(1) else word
        }
        .trim()
}

private fun isUsefulOcrWord(word: String): Boolean {
    return word.length > 1 &&
        word !in ignoredOcrTokens &&
        ocrWordRegex.matches(word) &&
        word.count { it == '-' || it == '\'' } <= 2
}

private fun addOcrWord(words: LinkedHashSet<String>, rawWord: String) {
    val word = normalizeOcrWord(rawWord)
    if (isUsefulOcrWord(word)) {
        words.add(word)
    }
}

private fun splitPotentialWordList(text: String): List<String> {
    return text
        .replace(Regex("""([a-z])([A-Z])"""), "$1 $2")
        .split(Regex("""[\s,，;；/／|、]+"""))
        .map { normalizeOcrWord(it) }
        .filter(::isUsefulOcrWord)
}

private fun containsChinese(text: String): Boolean {
    return text.any { it in '\u3400'..'\u9fff' }
}

private fun extractWordsFromOcrLine(line: String): List<String> {
    val strongSeparatorMatch = Regex("""\s*(?:[:：=]|\s+[-–—]\s+)\s*""").find(line)
    if (strongSeparatorMatch != null) {
        return splitPotentialWordList(line.substring(0, strongSeparatorMatch.range.first))
    }

    val listSeparatorParts = Regex("""\s*[,，;；/／|、]\s*""").split(line).filter { it.isNotBlank() }
    if (listSeparatorParts.size > 1) {
        val hasTranslationLikeTail = listSeparatorParts.drop(1).any(::containsChinese)
        val targetText = if (hasTranslationLikeTail) listSeparatorParts.first() else line
        return splitPotentialWordList(targetText)
    }

    return splitPotentialWordList(line)
}

private fun extractWordsFromOcrText(text: String): List<String> {
    val words = LinkedHashSet<String>()

    text
        .replace('\r', '\n')
        .lines()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .forEach { rawLine ->
            val line = rawLine.replace(ocrLinePrefixRegex, "").trim()
            if (line.isBlank()) return@forEach

            val segmentWords = extractWordsFromOcrLine(line)
            if (segmentWords.isNotEmpty()) {
                segmentWords.forEach { words.add(it) }
            } else {
                ocrWordRegex.findAll(line).forEach { match -> addOcrWord(words, match.value) }
            }
        }

    if (words.isEmpty()) {
        ocrWordRegex.findAll(text).forEach { match -> addOcrWord(words, match.value) }
    }

    return words.toList()
}

private fun parseTranslationPayload(payload: String): String {
    val trimmedPayload = payload.trim()
    if (trimmedPayload.startsWith("[")) {
        val googlePayload = JSONArray(trimmedPayload)
        val translatedParts = googlePayload.optJSONArray(0) ?: return ""
        return buildString {
            for (index in 0 until translatedParts.length()) {
                val part = translatedParts.optJSONArray(index)
                if (part != null) append(part.optString(0))
            }
        }.trim()
    }

    return JSONObject(trimmedPayload)
        .optJSONObject("responseData")
        ?.optString("translatedText")
        ?.trim()
        .orEmpty()
}

private fun requestDirectTranslation(url: String): String {
    val request = Request.Builder()
        .url(url)
        .header("Accept", "application/json")
        .header("User-Agent", "Mozilla/5.0")
        .build()

    ocrTranslateClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return ""
        return parseTranslationPayload(response.body?.string().orEmpty())
    }
}

private fun translateWordDirectly(word: String): String {
    val encodedWord = URLEncoder.encode(word, Charsets.UTF_8.name())
    val myMemoryUrl = "https://api.mymemory.translated.net/get?q=$encodedWord&langpair=en%7Czh-CN"
    val googleUrl = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=zh-CN&dt=t&q=$encodedWord"

    return requestDirectTranslation(myMemoryUrl)
        .takeIf { it.isNotBlank() && !it.equals(word, ignoreCase = true) }
        ?: requestDirectTranslation(googleUrl)
}

private suspend fun translateWordsDirectly(words: List<String>): Map<String, String> {
    return withContext(Dispatchers.IO) {
        words.mapNotNull { word ->
            val translation = runCatching { translateWordDirectly(word) }.getOrDefault("")
            translation
                .takeIf { it.isNotBlank() }
                ?.let { word to it }
        }.toMap()
    }
}

private suspend fun translateWordsWithBackend(words: List<String>): Map<String, String> {
    val translations = mutableMapOf<String, String>()

    words.chunked(OCR_TRANSLATION_BATCH_SIZE).forEach { batch ->
        val response = RetrofitClient.apiService.translateWords(TranslateWordsRequest(batch))
        response.translations.forEach { entry ->
            val word = normalizeOcrWord(entry.word)
            val translation = entry.translation.trim()
            if (word.isNotBlank() && translation.isNotBlank()) {
                translations[word] = translation
            }
        }
    }

    return translations
}

private fun buildTranslatedCards(words: List<String>, translations: Map<String, String>): List<CardRequest> {
    return words.map { word ->
        val translation = translations[word]?.takeIf { it.isNotBlank() } ?: "待补充释义"
        CardRequest(front = word, back = translation)
    }
}

@Composable
fun OcrImportScreen(onNavigateBack: () -> Unit) {
    var step by remember { mutableStateOf(1) }
    var rawText by remember { mutableStateOf("") }
    var parsedCards by remember { mutableStateOf<List<CardRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    DisposableEffect(recognizer) {
        onDispose {
            recognizer.close()
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isLoading = true
            try {
                val image = InputImage.fromFilePath(context, uri)
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        rawText = visionText.text
                        step = 2
                        isLoading = false
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "图片识别失败: ${e.message}", Toast.LENGTH_LONG).show()
                        isLoading = false
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "读取图片失败: ${e.message}", Toast.LENGTH_LONG).show()
                isLoading = false
            }
        }
    }

    fun parseText() {
        val words = extractWordsFromOcrText(rawText)
        if (words.isEmpty()) {
            Toast.makeText(context, "未能提取出有效英文单词，请检查识别结果", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        coroutineScope.launch {
            try {
                val translationMap = translateWordsWithBackend(words)
                val cards = buildTranslatedCards(words, translationMap)

                if (cards.isEmpty()) {
                    Toast.makeText(context, "提取到单词，但未获取到中文释义", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                parsedCards = cards
                Toast.makeText(context, "成功提取并翻译 ${cards.size} 个单词", Toast.LENGTH_SHORT).show()
                step = 3
            } catch (e: Exception) {
                val directTranslations = translateWordsDirectly(words)
                val cards = buildTranslatedCards(words, directTranslations)

                if (cards.isEmpty()) {
                    val message = if (e is HttpException && e.code() == 404) {
                        "翻译接口 404，请重启后端服务或确认服务器地址指向本项目后端"
                    } else {
                        "翻译失败: ${e.message}"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    return@launch
                }

                parsedCards = cards
                Toast.makeText(context, "后端翻译不可用，已直接翻译 ${cards.size} 个单词", Toast.LENGTH_LONG).show()
                step = 3
            } finally {
                isLoading = false
            }
        }
    }

    AppScreen(scrollable = step != 2) {
        when (step) {
            1 -> {
                ScreenHeader(
                    eyebrow = "OCR 识别导入",
                    title = "从图片或文本生成词条",
                    subtitle = "支持先识别、再确认、最后选择导入到现有牌组或新建牌组。"
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatChip(label = "当前步骤", value = "1 / 3", modifier = Modifier.weight(1f))
                    StatChip(label = "导入方式", value = "文字 / 图片", modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(18.dp))
                SectionCard(
                    title = "选择导入方式",
                    subtitle = "文字导入适合粘贴词表，图片导入适合截图 OCR。"
                ) {
                    SecondaryButton(
                        text = "导入文字",
                        onClick = { step = 2 },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PrimaryButton(
                        text = if (isLoading) "图片识别中..." else "导入图片并 OCR 识别",
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )
                    if (isLoading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                BackTextButton(onClick = onNavigateBack)
            }

            2 -> {
                ScreenHeader(
                    eyebrow = "编辑与确认",
                    title = "检查识别结果",
                    subtitle = "系统会从文本中提取英文单词，再自动补全中文释义后导入。"
                )
                Spacer(modifier = Modifier.height(16.dp))
                SectionCard(
                    title = "待解析文本",
                    subtitle = "可以先修正 OCR 结果，再执行单词提取和中文翻译。"
                ) {
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp),
                        label = { Text("识别结果 / 手动输入文本") }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SecondaryButton(
                            text = "上一步",
                            onClick = { step = 1 },
                            modifier = Modifier.weight(1f)
                        )
                        PrimaryButton(
                            text = if (isLoading) "提取并翻译中..." else "提取单词并翻译",
                            onClick = { parseText() },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        )
                    }
                    if (isLoading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            else -> {
                DeckSelectionView(
                    parsedCards = parsedCards,
                    onSuccess = {
                        Toast.makeText(context, "导入成功", Toast.LENGTH_SHORT).show()
                        onNavigateBack()
                    },
                    onCancel = { step = 2 }
                )
            }
        }
    }
}

@Composable
fun DeckSelectionView(
    parsedCards: List<CardRequest>,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    var decks by remember { mutableStateOf<List<Deck>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showNewDeckDialog by remember { mutableStateOf(false) }
    var newDeckName by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            decks = RetrofitClient.apiService.getDecks()
        } catch (_: Exception) {
            Toast.makeText(context, "获取牌组失败", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    fun parseErrorMessage(rawMessage: String?, fallback: String): String {
        return rawMessage
            ?.takeIf { it.isNotBlank() }
            ?.let { message ->
                Regex(""""error"\s*:\s*"([^"]+)"""").find(message)?.groupValues?.getOrNull(1)
                    ?: message
            }
            ?: fallback
    }

    fun addCardsToDeck(deckId: Int) {
        isLoading = true
        coroutineScope.launch {
            try {
                val response = RetrofitClient.apiService.addCardsToDeck(deckId, AddCardsRequest(parsedCards))
                if (!response.isSuccessful) {
                    val message = parseErrorMessage(
                        response.errorBody()?.string(),
                        "添加卡片失败"
                    )
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    isLoading = false
                    return@launch
                }
                onSuccess()
            } catch (e: Exception) {
                Toast.makeText(context, "添加卡片失败: ${e.message}", Toast.LENGTH_SHORT).show()
                isLoading = false
            }
        }
    }

    fun createAndAddCards() {
        if (newDeckName.isBlank()) {
            Toast.makeText(context, "牌组名称不能为空", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        coroutineScope.launch {
            try {
                val newDeck = RetrofitClient.apiService.createDeck(DeckRequest(name = newDeckName))
                val response = RetrofitClient.apiService.addCardsToDeck(
                    newDeck.deckId,
                    AddCardsRequest(parsedCards)
                )
                if (!response.isSuccessful) {
                    val message = parseErrorMessage(
                        response.errorBody()?.string(),
                        "导入到新牌组失败"
                    )
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    isLoading = false
                    return@launch
                }
                showNewDeckDialog = false
                onSuccess()
            } catch (e: Exception) {
                Toast.makeText(context, "新建牌组失败: ${e.message}", Toast.LENGTH_SHORT).show()
                isLoading = false
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            eyebrow = "导入目标",
            title = "选择牌组",
            subtitle = "你可以把结果追加到现有牌组，或者直接新建一个牌组。"
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatChip(label = "当前步骤", value = "3 / 3", modifier = Modifier.weight(1f))
            StatChip(label = "词条数量", value = parsedCards.size.toString(), modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(18.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
        } else {
            PrimaryButton(
                text = "新建牌组并导入",
                onClick = { showNewDeckDialog = true },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            if (decks.isEmpty()) {
                EmptyStateCard(
                    title = "暂无现有牌组",
                    description = "你可以直接新建一个牌组来保存识别结果。",
                    modifier = Modifier.weight(1f)
                )
            } else {
                SectionCard(
                    title = "添加到现有牌组",
                    subtitle = "点击任一牌组即可追加导入识别出的词条。",
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(decks) { deck ->
                            GlassCard(
                                modifier = Modifier.clickable { addCardsToDeck(deck.id) }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(text = deck.name, style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = deck.description ?: "点击追加导入到该牌组",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        BackTextButton(text = "返回上一步", onClick = onCancel)
    }

    if (showNewDeckDialog) {
        AlertDialog(
            onDismissRequest = { showNewDeckDialog = false },
            title = { Text("新建牌组") },
            text = {
                OutlinedTextField(
                    value = newDeckName,
                    onValueChange = { newDeckName = it },
                    label = { Text("牌组名称") },
                    singleLine = true
                )
            },
            confirmButton = {
                PrimaryButton(text = "确认创建", onClick = { createAndAddCards() })
            },
            dismissButton = {
                BackTextButton(text = "取消", onClick = { showNewDeckDialog = false })
            }
        )
    }
}
