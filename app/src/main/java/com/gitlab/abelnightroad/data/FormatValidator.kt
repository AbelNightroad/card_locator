package com.gitlab.abelnightroad.data

import com.gitlab.abelnightroad.db.DeckCardEntity

sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val errors: List<String>) : ValidationResult
}

fun interface FormatRule {
    fun validate(
        format: String,
        cards: List<DeckCardEntity>
    ): List<String>
}

class FormatValidator private constructor(val rules: List<FormatRule>) {
    fun validate(
        format: String,
        cards: List<DeckCardEntity>
    ): ValidationResult {
        val errors = rules.flatMap { it.validate(format, cards) }
        return if (errors.isEmpty()) ValidationResult.Valid
        else ValidationResult.Invalid(errors)
    }

    companion object {
        fun forFormat(format: String): FormatValidator = registry[format] ?: defaultValidator

        val defaultValidator = FormatValidator(emptyList())
        val commanderValidator = FormatValidator(listOf(CommanderCountRule, ColorIdentityRule))

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
        cards: List<DeckCardEntity>
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
        cards: List<DeckCardEntity>
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
