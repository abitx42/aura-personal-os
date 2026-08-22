package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// SEMANTIC COLORS & ACCENTS (Inspired by Fintech Reference)
// ==========================================
val SemanticGreen = Color(0xFF00D084)          // Available, Incoming, +₹, Completed
val SemanticGreenContainer = Color(0x2600D084) // 15% alpha green container
val SemanticGreenDark = Color(0xFF005A36)

val SemanticRed = Color(0xFFFF4D4D)            // Used, Outgoing, -₹, Debt Owed, Deletions
val SemanticRedContainer = Color(0x26FF4D4D)   // 15% alpha red container
val SemanticRedDark = Color(0xFF5C1B1B)

val SemanticGold = Color(0xFFFFB800)           // ✨ PRO badge, Default star, Highlights
val SemanticGoldContainer = Color(0x33FFB800)  // 20% alpha gold container

val RadiantOrange = Color(0xFFFF5B32)          // Signature Brand Accent
val RadiantOrangeMuted = Color(0xFF8A2E14)
val RadiantOrangeGlow = Color(0x33FF5B32)

// ==========================================
// BASE & SURFACE COLORS
// ==========================================
val DarkBackground = Color(0xFF101216)         // Rich matte dark background
val DarkSurface = Color(0xFF16181F)            // Dock / top bar surface
val DarkCard = Color(0xFF1D2028)               // Layered content cards
val DarkCardBorder = Color(0xFF282B36)         // Thin subtle card outline

val AmoledBackground = Color(0xFF000000)       // Pure pitch black
val AmoledSurface = Color(0xFF0C0D11)
val AmoledCard = Color(0xFF13151C)
val AmoledCardBorder = Color(0xFF20232E)

val LightBackground = Color(0xFFF7F8FA)        // Bone white background
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFF0F2F6)
val LightCardBorder = Color(0xFFE2E6EE)

// Text Colors
val TextWhitePrimary = Color(0xFFFFFFFF)
val TextBoneSecondary = Color(0xFFA0A5B5)
val TextMutedSteel = Color(0xFF6B7280)

val TextDarkPrimary = Color(0xFF121417)
val TextDarkSecondary = Color(0xFF555B6D)
val TextDarkMuted = Color(0xFF8E95A5)

// ==========================================
// MUTABLE THEME VARS (For compatibility)
// ==========================================
var AuraObsidian = DarkBackground
var AuraCharcoalBase = DarkSurface
var AuraSlateCard = DarkCard
var AuraSlateLight = DarkCardBorder

var AuraCyanNeon = RadiantOrange
var AuraCyanMuted = RadiantOrangeMuted
var AuraPurpleAccent = Color(0xFF7C4DFF)
var AuraCopperWarm = SemanticGold

var AuraWhiteMedium = TextBoneSecondary
var AuraWhiteMuted = TextMutedSteel

// Mood category tags
val MoodHappy = Color(0xFF00E676)
val MoodCalm = Color(0xFF29B6F6)
val MoodContent = Color(0xFFBA68C8)
val MoodNeutral = Color(0xFFECEFF1)
val MoodCreative = Color(0xFFFFD54F)
val MoodTired = Color(0xFF8D6E63)
val MoodSad = Color(0xFFEF5350)
