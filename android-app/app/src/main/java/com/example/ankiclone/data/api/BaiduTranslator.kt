package com.example.ankiclone.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest

/**
 * 百度通用翻译 API（英译中）。
 *
 * 接入步骤：在 https://fanyi-api.baidu.com/ 注册「通用文本翻译」，
 * 把控制台里的 APPID 和密钥填到下面两个常量即可。
 *
 * 注意：通用翻译接口只返回译文 dst，不返回结构化词性，
 * 因此本翻译器只产出中文释义。
 */
object BaiduTranslator {
    // TODO: 填入你在百度翻译开放平台申请到的 APPID 和密钥
    private const val APP_ID = "20260610002629501"
    private const val SECRET_KEY = "3kyBY1MpLj9lnQNzv3Bf"

    private const val ENDPOINT = "https://fanyi-api.baidu.com/api/trans/vip/translate"

    /** APPID/密钥是否已配置。未配置时调用方应回退到其它翻译源。 */
    val isConfigured: Boolean
        get() = APP_ID.isNotBlank() && SECRET_KEY.isNotBlank() &&
            APP_ID != "在此填入APPID" && SECRET_KEY != "在此填入密钥"

    private val client = OkHttpClient()

    /**
     * 批量翻译。百度支持用换行符拼接多条 query，一次请求返回对应的多条结果，
     * 既省请求次数也规避标准版 1 QPS 的限流。
     *
     * @return word -> 中文释义 的映射；翻译失败或未配置时返回空 Map。
     */
    suspend fun translate(words: List<String>): Map<String, String> {
        if (!isConfigured || words.isEmpty()) return emptyMap()

        return withContext(Dispatchers.IO) {
            runCatching { requestBatch(words) }.getOrDefault(emptyMap())
        }
    }

    private fun requestBatch(words: List<String>): Map<String, String> {
        val query = words.joinToString("\n")
        val salt = (System.currentTimeMillis() % 1_000_000).toString()
        val sign = md5(APP_ID + query + salt + SECRET_KEY)

        val body = FormBody.Builder()
            .add("q", query)
            .add("from", "en")
            .add("to", "zh")
            .add("appid", APP_ID)
            .add("salt", salt)
            .add("sign", sign)
            .build()

        val request = Request.Builder()
            .url(ENDPOINT)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return emptyMap()
            return parseResult(response.body?.string().orEmpty())
        }
    }

    private fun parseResult(payload: String): Map<String, String> {
        val json = JSONObject(payload)
        // 出错时百度返回 error_code/error_msg，没有 trans_result
        val results = json.optJSONArray("trans_result") ?: return emptyMap()

        val translations = mutableMapOf<String, String>()
        for (index in 0 until results.length()) {
            val item = results.optJSONObject(index) ?: continue
            val src = item.optString("src").trim()
            val dst = item.optString("dst").trim()
            if (src.isNotBlank() && dst.isNotBlank()) {
                translations[src.lowercase()] = dst
            }
        }
        return translations
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
