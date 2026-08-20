package com.bushy.health

import android.content.Context
import android.content.Intent

object ShareUtils {
    fun shareProgress(context: Context, stats: UserStats) {
        val shareText = """
            🚀 Level Up! 
            I'm now Level ${stats.level} in Bushy Health!
            🔥 Steps today: ${stats.steps}
            💪 Pushups: ${stats.pushups}
            Check it out! #BushyHealth #FitnessGame
        """.trimIndent()

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Share your progress")
        context.startActivity(shareIntent)
    }
}
