package com.copytrading.ui

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
        private val tvSymbol: TextView = itemView.findViewById(R.id.tvSymbol)
        private val tvType: TextView = itemView.findViewById(R.id.tvType)
        private val tvVolume: TextView = itemView.findViewById(R.id.tvVolume)
        private val tvOpenPrice: TextView = itemView.findViewById(R.id.tvOpenPrice)
        private val tvCurrentPrice: TextView = itemView.findViewById(R.id.tvCurrentPrice)
        private val tvProfit: TextView = itemView.findViewById(R.id.tvProfit)
        private val tvSlTp: TextView = itemView.findViewById(R.id.tvSlTp)
        private val tvComment: TextView = itemView.findViewById(R.id.tvComment)
        private val btnClose: View = itemView.findViewById(R.id.btnClose)

        fun bind(pos: Position) {
            tvSymbol.text = pos.symbol
            tvType.text = pos.type
            tvType.setTextColor(itemView.context.getColor(
                if (pos.type == "BUY") R.color.profit else R.color.loss
            ))
            tvVolume.text = String.format("%.2f", pos.volume)
            tvOpenPrice.text = String.format("%.2f", pos.open_price)
            tvCurrentPrice.text = String.format("%.2f", pos.current_price)

            val profitStr = String.format("%+.2f$", pos.profit)
            tvProfit.text = profitStr
            tvProfit.setTextColor(itemView.context.getColor(
                if (pos.profit >= 0) R.color.profit else R.color.loss
            ))

            tvSlTp.text = "SL: ${String.format("%.2f", pos.sl)} | TP: ${String.format("%.2f", pos.tp)}"
            tvComment.text = pos.comment

            btnClose.setOnClickListener {
                onCloseClick(pos.ticket)
            }
        }
    }
}
