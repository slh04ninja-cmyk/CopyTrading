package com.copytrading.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.copytrading.R
import com.copytrading.model.Position

class PositionAdapter : RecyclerView.Adapter<PositionAdapter.ViewHolder>() {

    private var positions: List<Position> = emptyList()

    fun setPositions(newPositions: List<Position>) {
        positions = newPositions
        notifyDataSetChanged()
    }

    fun getCurrentPositions(): List<Position> = positions

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_position, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pos = positions[position]
        holder.bind(pos)
    }

    override fun getItemCount() = positions.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val posDot: View = itemView.findViewById(R.id.posDot)
        private val tvSymbol: TextView = itemView.findViewById(R.id.tvSymbol)
        private val tvProfit: TextView = itemView.findViewById(R.id.tvProfit)
        private val tvVolume: TextView = itemView.findViewById(R.id.tvVolume)
        private val tvOpenPrice: TextView = itemView.findViewById(R.id.tvOpenPrice)
        private val tvTp: TextView = itemView.findViewById(R.id.tvTp)
        private val tvSl: TextView = itemView.findViewById(R.id.tvSl)
        private val badgeChannel: TextView = itemView.findViewById(R.id.badgeChannel)
        private val badgeSignal: TextView = itemView.findViewById(R.id.badgeSignal)
        private val badgeOrder: TextView = itemView.findViewById(R.id.badgeOrder)
        private val badgeReason: TextView = itemView.findViewById(R.id.badgeReason)
        private val tvOpenTime: TextView = itemView.findViewById(R.id.tvOpenTime)


        fun bind(pos: Position) {
            tvSymbol.text = pos.symbol

            // Dot color
            posDot.background.setTint(itemView.context.getColor(
                if (pos.type == "BUY") R.color.profit else R.color.loss
            ))

            // Profit
            val profitStr = String.format("%+.2f$", pos.profit)
            tvProfit.text = profitStr
            tvProfit.setTextColor(itemView.context.getColor(
                if (pos.profit >= 0) R.color.profit else R.color.loss
            ))

            // Row 2: Volume | PE | TP | SL
            tvVolume.text = String.format("%.2f", pos.volume)
            tvOpenPrice.text = String.format("%.2f", pos.open_price)
            tvTp.text = String.format("%.2f", pos.tp)
            tvSl.text = String.format("%.2f", pos.sl)

            // Row 3: Parse comment CH{canal}-{signal}-{order}
            val parts = pos.comment.split("-")
            if (parts.size >= 3 && parts[0].startsWith("CH")) {
                badgeChannel.text = parts[0]
                badgeSignal.text = parts[1]
                badgeOrder.text = parts[2]
                badgeChannel.visibility = View.VISIBLE
                badgeSignal.visibility = View.VISIBLE
                badgeOrder.visibility = View.VISIBLE
            } else {
                badgeChannel.text = "MANUEL"
                badgeChannel.setTextColor(itemView.context.getColor(R.color.text_muted))
                badgeChannel.setBackgroundColor(Color.parseColor("#1A555577"))
                badgeChannel.visibility = View.VISIBLE
                badgeSignal.visibility = View.GONE
                badgeOrder.visibility = View.GONE
            }

            // BLOC 3 (11/10/2026) : BADGE JAUNE = raison de fermeture ecrite par le bot
            // (NWS news, CF conflit, DB doublon, NS nouveau signal, DD perte journaliere,
            //  DTG objectif atteint, DEL signal supprime, MAN manuel, EOD fin de journee)
            val codesRaison = listOf("NWS", "CF", "DB", "NS", "DD", "DTG", "DEL", "MAN", "EOD")
            val commentUp = pos.comment.uppercase()
            val raison = codesRaison.firstOrNull { c ->
                commentUp == c || commentUp.endsWith("-$c") || commentUp.endsWith(":$c") ||
                    commentUp.contains("-$c-") || commentUp.startsWith("$c-")
            }
            if (raison != null) {
                badgeReason.text = raison
                badgeReason.visibility = View.VISIBLE
            } else {
                badgeReason.visibility = View.GONE
            }

            // BLOC 4 (11/10/2026) : DUREE PROPRE A CHAQUE POSITION (+ sa lettre MK/L1/L2/L3/L4)
            // But : comparer MK vs L1 vs L2 (ou L3 vs L4) en regardant leurs durees respectives.
            // Remplace l'ancienne "duree du signal" affichee a droite des badges.
            val lettrePos = if (parts.size >= 3 && parts[0].startsWith("CH")) parts[2] else ""
            val dureePos = try {
                val t0 = java.time.OffsetDateTime.parse(pos.time)
                val mins = java.time.Duration.between(
                    t0, java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC)
                ).toMinutes()
                if (mins < 60) "${mins}m" else String.format("%dh%02d", mins / 60, mins % 60)
            } catch (e: Exception) {
                ""
            }
            // Temps d'ouverture (UTC) : pos.time = ISO "2026-09-07T12:36:45+00:00"
            val openTime = if (pos.time.length >= 16) pos.time.substring(11, 16) else ""
            if (lettrePos.isNotEmpty() && dureePos.isNotEmpty()) {
                tvOpenTime.text = dureePos   // la lettre MK/L1/L2 est deja dans le badge Order a gauche
                tvOpenTime.visibility = View.VISIBLE
            } else if (openTime.isNotEmpty()) {
                tvOpenTime.text = openTime
                tvOpenTime.visibility = View.VISIBLE
            } else {
                tvOpenTime.visibility = View.GONE
            }


        }
    }
}
