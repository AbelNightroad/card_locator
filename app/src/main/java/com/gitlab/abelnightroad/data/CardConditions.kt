package com.gitlab.abelnightroad.data

object CardConditions {
    const val NM = "NM"
    const val LP = "LP"
    const val MP = "MP"
    const val HP = "HP"
    const val DM = "DM"

    val ALL = listOf(NM, LP, MP, HP, DM)

    fun displayName(condition: String): String = when (condition) {
        NM -> "Near Mint"
        LP -> "Lightly Played"
        MP -> "Moderately Played"
        HP -> "Heavily Played"
        DM -> "Damaged"
        else -> condition
    }
}
