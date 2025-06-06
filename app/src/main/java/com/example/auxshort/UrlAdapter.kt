package com.example.auxshort

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class UrlAdapter(
    private var urls: MutableList<UrlItem>,
    private val onDeleteClick: (UrlItem, Int) -> Unit,
    private val onAnalyticsClick: (UrlItem) -> Unit,
) : RecyclerView.Adapter<UrlAdapter.UrlViewHolder>() {

    // Formatter for parsing the ISO 8601 date string from the server.
    private val inputFormatter = DateTimeFormatter.ISO_ZONED_DATE_TIME
    // Formatter for displaying the date in a user-friendly way.
    private val outputFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault())


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UrlViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_url, parent, false)
        return UrlViewHolder(view)
    }

    override fun onBindViewHolder(holder: UrlViewHolder, position: Int) {
        holder.bind(urls[position])
    }

    override fun getItemCount(): Int = urls.size

    // Updates the entire list of URLs.
    fun updateUrls(newUrls: List<UrlItem>) {
        urls = newUrls.toMutableList()
        notifyDataSetChanged()
    }

    // Removes an item from the list at a specific position.
    fun removeItem(position: Int) {
        if (position in 0 until urls.size) {
            urls.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    // Formats the date string or returns a default value.
    private fun formatDisplayDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "N/A"
        return try {
            val zonedDateTime = ZonedDateTime.parse(dateString, inputFormatter)
            "Created: ${zonedDateTime.format(outputFormatter)}"
        } catch (e: Exception) {
            "Invalid date" // Fallback for parsing errors
        }
    }

    inner class UrlViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val originalUrl: TextView = itemView.findViewById(R.id.textViewOriginalUrl)
        private val shortenedUrl: TextView = itemView.findViewById(R.id.textViewShortenedUrl)
        private val createdAt: TextView = itemView.findViewById(R.id.textViewCreatedAt)
        private val deleteButton: Button = itemView.findViewById(R.id.buttonDelete)
        private val analyticsButton: Button = itemView.findViewById(R.id.buttonAnalytics)

        fun bind(urlItem: UrlItem) {
            originalUrl.text = urlItem.original ?: "N/A"
            shortenedUrl.text = urlItem.shortened ?: "N/A"
            createdAt.text = formatDisplayDate(urlItem.createdAt)

            deleteButton.setOnClickListener {
                // Use adapterPosition to ensure the position is correct,
                // especially during animations.
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(urlItem, position)
                }
            }
            analyticsButton.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onAnalyticsClick(urlItem)
                }
            }
        }
    }
}
