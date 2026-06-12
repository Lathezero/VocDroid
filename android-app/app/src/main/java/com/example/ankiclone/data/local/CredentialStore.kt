package com.example.ankiclone.data.local

import android.content.Context

/**
 * 使用 SharedPreferences 持久化登录凭据，用于「记住账号密码」功能。
 *
 * 注意：密码以明文形式保存在应用私有目录下，仅适合本地学习场景。
 * 若需更高安全性，可改用 androidx.security:security-crypto 的 EncryptedSharedPreferences。
 */
class CredentialStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var rememberEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMEMBER, false)
        private set(value) {
            prefs.edit().putBoolean(KEY_REMEMBER, value).apply()
        }

    val savedUsername: String
        get() = prefs.getString(KEY_USERNAME, "").orEmpty()

    val savedPassword: String
        get() = prefs.getString(KEY_PASSWORD, "").orEmpty()

    /** 勾选「记住」并登录成功后调用，保存凭据。 */
    fun save(username: String, password: String) {
        prefs.edit()
            .putBoolean(KEY_REMEMBER, true)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .apply()
    }

    /** 未勾选「记住」时调用，清除已保存的凭据。 */
    fun clear() {
        prefs.edit()
            .putBoolean(KEY_REMEMBER, false)
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "auth_credentials"
        private const val KEY_REMEMBER = "remember_enabled"
        private const val KEY_USERNAME = "saved_username"
        private const val KEY_PASSWORD = "saved_password"
    }
}
