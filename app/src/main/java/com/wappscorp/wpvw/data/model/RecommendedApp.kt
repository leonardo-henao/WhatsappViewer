package com.wappscorp.wpvw.data.model

import java.util.Locale

data class RecommendedApp(
    val name_es: String = "",
    val name_en: String = "",
    val description_es: String = "",
    val description_en: String = "",
    val url: String = "",
    val logo: String = "",
    val market: String = ""
)

fun RecommendedApp.displayName(): String {
    val lang = Locale.getDefault().language
    return when (lang) {
        "es" -> name_es.ifEmpty { name_en }
        else -> name_en.ifEmpty { name_es }
    }
}

fun RecommendedApp.displayDescription(): String {
    val lang = Locale.getDefault().language
    return when (lang) {
        "es" -> description_es.ifEmpty { description_en }
        else -> description_en.ifEmpty { description_es }
    }
}
