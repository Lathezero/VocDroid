package com.example.ankiclone.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ankiclone.data.api.RetrofitClient
import com.example.ankiclone.ui.components.AppScreen
import com.example.ankiclone.ui.components.BackTextButton
import com.example.ankiclone.ui.components.InfoPill
import com.example.ankiclone.ui.components.PrimaryButton
import com.example.ankiclone.ui.components.ScreenHeader
import com.example.ankiclone.ui.components.SectionCard
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@Composable
fun ImportScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isUploading = true
            coroutineScope.launch {
                try {
                    val file = getFileFromUri(context, uri)
                    if (file != null) {
                        val requestFile = file.asRequestBody(mediaTypeForFile(file.name).toMediaTypeOrNull())
                        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                        
                        val response = RetrofitClient.apiService.importDeck(body)
                        if (response.isSuccessful) {
                            Toast.makeText(context, "导入成功！", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        } else {
                            Toast.makeText(
                                context,
                                "导入失败: ${parseImportError(response.errorBody()?.string())}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(context, "无法读取文件", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "错误: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isUploading = false
                }
            }
        }
    }

    AppScreen {
        Column(
            verticalArrangement = Arrangement.Center
        ) {
            ScreenHeader(
                eyebrow = "文件导入",
                title = "导入 JSON 或 Anki 牌组",
                subtitle = "支持 `.json` 和 `.apkg` 文件，导入后会同步到当前用户的后端数据。"
            )
            Spacer(modifier = Modifier.height(18.dp))
            SectionCard(
                title = "选择文件",
                subtitle = "建议直接从文件管理器选择标准牌组文件。"
            ) {
                InfoPill(text = "支持 JSON / APKG")
                Spacer(modifier = Modifier.height(14.dp))
                PrimaryButton(
                    text = if (isUploading) "正在上传解析..." else "选择文件并导入",
                    onClick = { filePickerLauncher.launch("*/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUploading
                )
                if (isUploading) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            BackTextButton(text = "取消", onClick = onNavigateBack)
        }
    }
}

private fun mediaTypeForFile(fileName: String): String {
    return when (fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
        "json" -> "application/json"
        "apkg" -> "application/octet-stream"
        else -> "application/octet-stream"
    }
}

private fun parseImportError(rawMessage: String?): String {
    val message = rawMessage.orEmpty().trim()
    if (message.isBlank()) return "服务器未返回错误详情"

    return runCatching {
        JSONObject(message).optString("error").takeIf { it.isNotBlank() }
    }.getOrNull() ?: message
}

fun getFileFromUri(context: Context, uri: Uri): File? {
    var fileName = "temp_file"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                fileName = cursor.getString(nameIndex)
            }
        }
    }
    fileName = fileName.substringAfterLast('/').substringAfterLast('\\')
    
    val tempFile = File(context.cacheDir, fileName)
    try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
