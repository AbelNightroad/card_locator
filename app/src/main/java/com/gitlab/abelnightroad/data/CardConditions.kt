package com.gitlab.abelnightroad.data

object CardConditions {
    const val M = "M"
    const val NM = "NM"
    const val SP = "SP"
    const val PL = "PL"
    const val HP = "HP"
    const val D = "D"

    val ALL = listOf(M, NM, SP, PL, HP, D)

    fun displayName(condition: String): String = when (condition) {
        M -> "Mint"
        NM -> "Near Mint"
        SP -> "Slightly Played"
        PL -> "Played"
        HP -> "Heavily Played"
        D -> "Damaged"
        else -> condition
    }
}
