package com.copytrading.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.copytrading.R
import com.copytrading.model.Position

class PositionAdapter(
    private val onCloseClick: (Long) -> Unit
) : RecyclerView.Adapter<PositionAdapter.ViewHolder>() {

    private var positions: List<Position> = emptyList()

    fun setPositions(newPositions: List<Position>) {
        positions = newPositions
        notifyDataSetChanged()
    }

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
        private val btnClose: View = itemView.findViewById(R.id.btnClose)

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

            btnClose.setOnClickListener {
                onCloseClick(pos.ticket)
            }
        }
    }
}
