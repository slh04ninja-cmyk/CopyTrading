package com.copytrading.config

/**
 * Codes de fermeture reconnus (badge jaune : Positions + popup Historique).
 *
 * La liste vient du SERVEUR : reglage MOTIF_CODES du .env du bot.
 * -> Ajouter un nouveau motif ne demande PLUS de reconstruire l'APK :
 *    on ajoute son code dans MOTIF_CODES (ecran Reglages), et c'est tout.
 * Repli sur la liste par defaut si le serveur ne fournit pas la cle (serveur plus ancien).
 */
object MotifCodes {

    val DEFAUT: List<String> = listOf(
        "NWS", "CF", "DB", "NS", "DD", "DTG", "TMO", "DEL", "MAN", "EOD"
    )

    @Volatile
    var codes: List<String> = DEFAUT

    /** Appele a chaque lecture de la config (/api/config). */
    fun setFromConfig(cfg: Map<String, String>?) {
        val brut = (cfg?.get("MOTIF_CODES") ?: "").trim()
        val liste = brut.split(",").map { it.trim().uppercase() }.filter { it.isNotEmpty() }
        codes = if (liste.isEmpty()) DEFAUT else liste
    }
}
