package com.example.ankiclone.data.api

import retrofit2.http.*

interface AnkiApiService {
    @POST("/api/auth/register")
    suspend fun register(@Body request: AuthRequest): AuthResponse

    @POST("/api/auth/login")
    suspend fun login(@Body request: AuthRequest): AuthResponse

    @GET("/api/decks")
    suspend fun getDecks(): List<Deck>

    @POST("/api/decks")
    suspend fun createDeck(@Body deck: DeckRequest): CreateDeckResponse

    @DELETE("/api/decks/{id}")
    suspend fun deleteDeck(@Path("id") deckId: Int): retrofit2.Response<Unit>

    @POST("/api/translate/words")
    suspend fun translateWords(@Body request: TranslateWordsRequest): TranslateWordsResponse

    @POST("/api/decks/{id}/cards")
    suspend fun addCardsToDeck(@Path("id") deckId: Int, @Body request: AddCardsRequest): retrofit2.Response<Unit>

    @Multipart
    @POST("/api/decks")
    suspend fun importDeck(@Part file: okhttp3.MultipartBody.Part): retrofit2.Response<okhttp3.ResponseBody>

    @GET("/api/decks/{id}/cards")
    suspend fun getCardsForDeck(@Path("id") deckId: Int): List<Card>

    @GET("/api/decks/{id}/cards/all")
    suspend fun getAllCardsForDeck(@Path("id") deckId: Int): List<Card>

    @GET("/api/review/due")
    suspend fun getDueCards(): List<Card>

    @PUT("/api/cards/{id}")
    suspend fun updateCard(@Path("id") cardId: Int, @Body request: UpdateCardRequest): retrofit2.Response<Unit>

    @DELETE("/api/cards/{id}")
    suspend fun deleteCard(@Path("id") cardId: Int): retrofit2.Response<Unit>

    @POST("/api/cards/{id}/review")
    suspend fun reviewCard(@Path("id") cardId: Int, @Body result: ReviewResult): ReviewResponse

    @GET("/api/admin/users")
    suspend fun getUsers(): List<User>

    @GET("/api/stats")
    suspend fun getStats(): StatsResponse

    @GET("/api/admin/users/{id}/decks")
    suspend fun getUserDecks(@Path("id") userId: Int): List<Deck>

    @GET("/api/admin/users/{id}/stats")
    suspend fun getUserStats(@Path("id") userId: Int): StatsResponse
}
