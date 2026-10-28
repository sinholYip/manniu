package com.ranke.health.ui.i18n

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** 界面语言。SYSTEM 表示跟随系统，另外两项为应用内强制切换。 */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    CHINESE("zh"),
    ENGLISH("en");

    companion object {
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}

/**
 * 语言偏好存放在独立的 SharedPreferences 文件里：
 * 不进入备份 JSON，换语言不会改动任何业务数据，也不会影响与小程序版的互操作。
 */
internal object LocaleSettings {
    private const val PREFS = "ranke_locale"
    private const val KEY_LANGUAGE = "app_language"

    fun language(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return AppLanguage.fromTag(prefs.getString(KEY_LANGUAGE, null))
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, language.tag).apply()
    }

    /** 在 attachBaseContext 阶段套用语言，重建 Activity 后全应用资源即为对应语言。 */
    fun wrap(base: Context): Context {
        val locale = localeFor(language(base)) ?: return base
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return base.createConfigurationContext(configuration)
    }

    /** 当前生效的语言，用于读取已套用的配置而非系统默认值。 */
    fun currentLocale(context: Context): Locale =
        context.resources.configuration.locales[0] ?: Locale.getDefault()

    private fun localeFor(language: AppLanguage): Locale? = when (language) {
        AppLanguage.SYSTEM -> null
        AppLanguage.CHINESE -> Locale.SIMPLIFIED_CHINESE
        AppLanguage.ENGLISH -> Locale.ENGLISH
    }
}
