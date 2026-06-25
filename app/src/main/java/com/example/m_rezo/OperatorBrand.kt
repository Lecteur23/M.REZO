package com.example.m_rezo

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color

enum class OperatorBrand(
    val label: String,
    val accent: Color,
    val onAccent: Color,
    @DrawableRes val logoRes: Int,
    val simKeywords: List<String>
) {
    MTN("MTN", Color(0xFFFFCC00), Color(0xFF08111F), R.drawable.logo_mtn, listOf("mtn")),
    ORANGE("Orange", Color(0xFFFF6600), Color.White, R.drawable.logo_orange, listOf("orange")),
    MOOV("Moov", Color(0xFF0055A4), Color.White, R.drawable.logo_moov, listOf("moov"))
}
