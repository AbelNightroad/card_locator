package com.gitlab.abelnightroad.data

internal val SUPERTYPES = setOf("Legendary", "Snow", "World", "Basic")

internal fun primaryType(typeLine: String): String {
    val t = typeLine.trim()
    val emdash = t.indexOf("\u2014")
    val typePart = if (emdash > 0) t.substring(0, emdash).trim() else t
    val types = typePart.split(" ").map { it.trim() }.filter { it.isNotBlank() && it !in SUPERTYPES }.toSet()
    return when {
        "Land" in types -> "Land"
        "Creature" in types -> "Creature"
        "Planeswalker" in types -> "Planeswalker"
        "Artifact" in types -> "Artifact"
        "Battle" in types -> "Battle"
        "Kindred" in types || "Tribal" in types ->
            types.firstOrNull { it != "Kindred" && it != "Tribal" } ?: "Other"
        types.firstOrNull() != null -> types.first()
        else -> "Other"
    }
}
