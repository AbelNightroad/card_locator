package com.gitlab.abelnightroad.data

import com.gitlab.abelnightroad.db.DeckCardEntity
import kotlinx.serialization.json.Json

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val errors: List<String>) : ValidationResult
}

interface FormatRule {
    fun validate(
        format: String,
        cards: List<DeckCardEntity>,
        legalitiesMap: Map<String, String> = emptyMap()
    ): List<String>
}

class FormatValidator private constructor(val rules: List<FormatRule>) {
    fun validate(
        format: String,
        cards: List<DeckCardEntity>,
        legalitiesMap: Map<String, String> = emptyMap()
    ): ValidationResult {
        val errors = rules.flatMap { it.validate(format, cards, legalitiesMap) }
        return if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }

    companion object {
        fun forFormat(format: String): FormatValidator = registry[format] ?: withLegality

        val commanderValidator = FormatValidator(listOf(CommanderCountRule, ColorIdentityRule, LegalityRule))
        private val withLegality = FormatValidator(listOf(LegalityRule))

        private val registry = mapOf(
            "Commander" to commanderValidator,
            "Brawl" to commanderValidator
        )
    }

    override fun toString(): String = "FormatValidator(${rules.size} rules)"
}

private object CommanderCountRule : FormatRule {
    override fun validate(
        format: String,
        cards: List<DeckCardEntity>,
        legalitiesMap: Map<String, String>
    ): List<String> {
        val cmdCount = cards.count { it.slot == "commander" }
        return when {
            cmdCount == 0 -> listOf("Missing commander — $format decks require exactly 1 commander")
            cmdCount > 1 -> listOf("Too many commanders ($cmdCount) — $format decks require exactly 1 commander")
            else -> emptyList()
        }
    }
}

private object ColorIdentityRule : FormatRule {
    override fun validate(
        format: String,
        cards: List<DeckCardEntity>,
        legalitiesMap: Map<String, String>
    ): List<String> {
        val commander = cards.find { it.slot == "commander" } ?: return emptyList()
        val cmdColors = commander.colorIdentity
        if (cmdColors.isBlank()) return emptyList()
        return cards
            .filter { it.slot != "commander" && it.slot != "companion" }
            .filter { !DeckRepository.isColorIdentityValid(it.colorIdentity, cmdColors) }
            .map { "${it.cardName} (${it.colorIdentity}) exceeds commander color identity ($cmdColors)" }
    }
}

private object LegalityRule : FormatRule {
    private val json = Json { ignoreUnknownKeys = true }

    override fun validate(
        format: String,
        cards: List<DeckCardEntity>,
        legalitiesMap: Map<String, String>
    ): List<String> {
        val formatKey = format.lowercase()
        return cards
            .filter { it.scryfallId.isNotBlank() }
            .filter { card ->
                val raw = legalitiesMap[card.scryfallId]
                if (raw.isNullOrBlank()) return@filter false
                val status = parseLegality(raw, formatKey)
                status != "legal" && status != "restricted"
            }
            .map { "${it.cardName} is not legal in $format" }
    }

    private fun parseLegality(jsonStr: String, format: String): String? {
        return try {
            json.decodeFromString<Map<String, String>>(jsonStr)[format]
        } catch (_: Exception) { null }
    }
}
