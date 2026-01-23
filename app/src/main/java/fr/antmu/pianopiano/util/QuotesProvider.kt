package fr.antmu.pianopiano.util

import android.content.Context
import fr.antmu.pianopiano.R

object QuotesProvider {

    private var quotes: List<String>? = null

    fun getRandomQuote(context: Context): String {
        if (quotes == null) {
            quotes = context.resources.getStringArray(R.array.relaxing_quotes).toList()
        }
        return quotes?.randomOrNull() ?: context.getString(R.string.default_quote)
    }
}
