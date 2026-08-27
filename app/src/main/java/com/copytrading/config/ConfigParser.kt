package com.copytrading.config

/**
 * Parseur .env → groupes de champs selon la spec CONFIG_SCREEN_SPEC §2
 */
object ConfigParser {

    data class Field(
        val key: String,
        var value: String,
        val help: String = "",
        val type: FieldType = FieldType.TEXT,
        val originalIndex: Int = 0, // position dans le fichier original
        val isMergedChannel: Boolean = false,
        val originalChannelKeys: List<String> = emptyList()
    )

    data class Group(
        val title: String,
        val fields: MutableList<Field> = mutableListOf(),
        val originalTitleLine: String = "" // ligne titre originale pour reconstruction
    )

    enum class FieldType {
        CHANNEL_LIST,    // §5.4 tags/chips
        SELECT_INLINE,   // §3.1 Spinner
        TEXT_INLINE,     // §3.2 EditText inline
        BOOL,            // §5.3 SwitchMaterial
        PASSWORD,        // §5.1 EditText password + œil
        NUMBER_INLINE,   // §5.2 EditText numérique inline 72dp
        FLOAT_INLINE,    // §5.2 EditText décimal inline 72dp
        NUMBER,          // §5.1 EditText numérique pleine largeur
        TEXT             // §5.1 EditText texte pleine largeur
    }

    // §3.1 Table de sélection
    private val SELECT_OPTIONS = mapOf(
        "NEWS_MIN_IMPACT" to listOf("low", "medium", "high"),
        "TV_FILTER_TIMEFRAME" to listOf("1m", "5m", "10m", "15m", "30m")
    )

    // §3.2 Clés forcées en text-inline
    private val TEXT_INLINE_KEYS = setOf(
        "MAGIC_NUMBER", "TV_FILTER_SYMBOL", "TV_FILTER_SCREENER", "TV_FILTER_EXCHANGE"
    )

    /**
     * Parse le texte .env en liste de groupes
     */
    fun parse(envText: String): List<Group> {
        val groups = mutableListOf<Group>()
        var currentGroup = Group("General")
        var hasFoundSection = false
        var fieldIndex = 0

        val lines = envText.lines()

        for (line in lines) {
            val trimmed = line.trim()

            // §2.1 Détection des sections (titres de groupe)
            if (trimmed.startsWith("#")) {
                val titleText = trimmed.removePrefix("#").trim()
                if (isSectionTitle(titleText)) {
                    // Nouveau groupe
                    if (currentGroup.fields.isNotEmpty() || hasFoundSection) {
                        groups.add(currentGroup)
                    }
                    val cleanTitle = cleanSectionTitle(titleText)
                    currentGroup = Group(cleanTitle, originalTitleLine = trimmed)
                    hasFoundSection = true
                    continue
                }
                // Sinon c'est une ligne de documentation → ignorée
                continue
            }

            // §2.2 Détection des paires clé/valeur
            val kvMatch = Regex("^([A-Z][A-Z0-9_]*)=(.*)$").matchEntire(trimmed)
            if (kvMatch != null) {
                val key = kvMatch.groupValues[1]
                var rawValue = kvMatch.groupValues[2].trim()
                var help = ""

                // Extraction du commentaire d'aide
                val hashIndex = rawValue.indexOf(" #")
                if (hashIndex >= 0) {
                    help = rawValue.substring(hashIndex + 2).trim()
                    rawValue = rawValue.substring(0, hashIndex).trim()
                }

                val fieldType = detectFieldType(key, rawValue)
                currentGroup.fields.add(
                    Field(
                        key = key,
                        value = rawValue,
                        help = help,
                        type = fieldType,
                        originalIndex = fieldIndex++
                    )
                )
            }
        }

        // Ajouter le dernier groupe
        if (currentGroup.fields.isNotEmpty()) {
            groups.add(currentGroup)
        }

        // §2.3 Fusion des canaux Telegram
        return groups.map { group -> mergeChannelFields(group) }
    }

    /**
     * §2.3 Fusion spéciale : canaux Telegram
     */
    private fun mergeChannelFields(group: Group): Group {
        val channelPattern = Regex("^TG_CHANNEL_(\\d+)$")
        val channelFields = group.fields.filter { channelPattern.matches(it.key) }
            .sortedBy { channelPattern.find(it.key)!!.groupValues[1].toInt() }

        if (channelFields.isEmpty()) return group

        val mergedValue = channelFields.joinToString(", ") { it.value }
        val originalKeys = channelFields.map { it.key }

        val mergedField = Field(
            key = "TG_CHANNEL_MERGED",
            value = mergedValue,
            help = "",
            type = FieldType.CHANNEL_LIST,
            originalIndex = channelFields.first().originalIndex,
            isMergedChannel = true,
            originalChannelKeys = originalKeys
        )

        val newFields = group.fields.toMutableList()
        // Retirer tous les TG_CHANNEL_N
        newFields.removeAll { channelPattern.matches(it.key) }
        // Insérer le champ fusionné à la position du premier
        val insertIndex = newFields.indexOfFirst { it.originalIndex > mergedField.originalIndex }
        if (insertIndex >= 0) {
            newFields.add(insertIndex, mergedField)
        } else {
            newFields.add(mergedField)
        }

        return group.copy(fields = newFields)
    }

    /**
     * §3 Détection du type de champ
     */
    private fun detectFieldType(key: String, value: String): FieldType {
        // 1. Channel list (marqué après fusion, mais on détecte aussi directement)
        if (key == "TG_CHANNEL_MERGED") return FieldType.CHANNEL_LIST

        // 2. §3.1 Table de sélection
        if (key in SELECT_OPTIONS) return FieldType.SELECT_INLINE

        // 3. §3.2 Texte inline forcé
        if (key in TEXT_INLINE_KEYS) return FieldType.TEXT_INLINE

        // 4. Bool
        if (value.lowercase() == "true" || value.lowercase() == "false") return FieldType.BOOL

        // 5. Password
        if (key.contains("PASSWORD") || key.contains("API_HASH")) return FieldType.PASSWORD

        // 6. Entier signé 1-4 chiffres
        if (Regex("^-?\\d{1,4}$").matches(value)) return FieldType.NUMBER_INLINE

        // 7. Décimal signé ≤ 6 caractères
        if (Regex("^-?\\d+\\.\\d+$").matches(value) && value.length <= 6) return FieldType.FLOAT_INLINE

        // 8. Entier ou décimal (cas général)
        if (Regex("^-?\\d+(\\.\\d+)?$").matches(value)) return FieldType.NUMBER

        // 9. Sinon texte
        return FieldType.TEXT
    }

    /**
     * §2.1 Détermine si une ligne commentaire est un titre de section
     */
    private fun isSectionTitle(text: String): Boolean {
        // Contient ── ou === (2+ répétitions)
        if (text.contains(Regex("[─=]{2,}"))) return true
        // Après nettoyage, pas de , ni . ni : ni : en fin, et ≤ 45 chars
        val cleaned = text.replace(Regex("[─=]+"), " ").trim()
        if (cleaned.length > 45) return false
        if (cleaned.contains(",") || cleaned.contains(".")) return false
        if (cleaned.contains(": ") || cleaned.endsWith(":")) return false
        return cleaned.isNotEmpty()
    }

    /**
     * Nettoie le texte du titre de section
     */
    private fun cleanSectionTitle(text: String): String {
        return text.replace(Regex("[─=]+"), " ").trim()
    }

    /**
     * §4 Génération du libellé
     */
    fun fieldLabel(key: String): String {
        if (key == "TG_CHANNEL_MERGED") return "Canaux Telegram"
        return key.split("_").joinToString(" ") { part ->
            part.lowercase().replaceFirstChar { it.uppercase() }
        }
    }

    /**
     * §3.1 Options pour les select-inline
     */
    fun getSelectOptions(key: String): List<String> {
        return SELECT_OPTIONS[key] ?: emptyList()
    }

    /**
     * §6 Sauvegarde — reconstruit le texte .env
     */
    fun rebuildEnvText(groups: List<Group>, fieldValues: Map<String, String>, channelLists: Map<String, List<String>>): String {
        val sb = StringBuilder()

        for (group in groups) {
            // Titre de section
            if (group.title != "General" && group.originalTitleLine.isNotEmpty()) {
                if (sb.isNotEmpty()) sb.appendLine()
                sb.appendLine(group.originalTitleLine)
            }

            for (field in group.fields) {
                if (field.isMergedChannel) {
                    // §6 — Développer en TG_CHANNEL_1=..., TG_CHANNEL_2=...
                    val channels = channelLists[field.key] ?: emptyList()
                    for ((i, ch) in channels.withIndex()) {
                        val origKey = "TG_CHANNEL_${i + 1}"
                        sb.appendLine("$origKey=$ch")
                    }
                } else {
                    val currentValue = fieldValues[field.key] ?: field.value
                    val line = if (field.help.isNotEmpty()) {
                        "${field.key}=$currentValue # ${field.help}"
                    } else {
                        "${field.key}=$currentValue"
                    }
                    sb.appendLine(line)
                }
            }
        }

        return sb.toString()
    }
}
