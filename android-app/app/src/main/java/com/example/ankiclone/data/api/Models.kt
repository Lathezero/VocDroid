package com.example.ankiclone.data.api

data class AuthRequest(
    val username: String,
    val password: String
)

data class AuthResponse(
    val token: String,
    val user: User
)

data class User(
    val id: Int,
    val username: String,
    val role: String
)

data class DeckRequest(
    val name: String,
    val description: String? = null
)

data class CreateDeckResponse(
    val message: String,
    val deckId: Int
)

data class TranslateWordsRequest(
    val words: List<String>
)

data class TranslationEntry(
    val word: String,
    val translation: String
)

data class TranslateWordsResponse(
    val translations: List<TranslationEntry>
)

data class CardRequest(
    val front: String,
    val back: String,
    val partOfSpeech: String? = null
)

data class AddCardsRequest(
    val cards: List<CardRequest>
)

data class Deck(
    val id: Int,
    val user_id: Int,
    val name: String,
    val description: String?
)

data class Card(
    val id: Int,
    val deck_id: Int,
    val front_content: String,
    val back_content: String,
    val part_of_speech: String?,
    val status: String,
    val next_review: String?,
    val interval: Int,
    val ease_factor: Int
)

data class ReviewResult(
    val performanceRating: Int // e.g., 0=Again, 1=Hard, 2=Good, 3=Easy
)

data class ReviewResponse(
    val success: Boolean,
    val next_review: String?
)

data class ProficiencyStat(
    val status: String?,
    val count: Int
)

data class FrequencyStat(
    val date: String?,
    val count: Int
)

data class StatsResponse(
    val proficiency: List<ProficiencyStat>,
    val frequency: List<FrequencyStat>
)
