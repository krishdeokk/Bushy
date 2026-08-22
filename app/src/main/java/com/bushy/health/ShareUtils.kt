package com.bushy.health

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareUtils {
    fun shareProgress(context: Context, stats: UserStats) {
        val shareText = """
            🔥 Today on Bushy:
            👟 Steps: ${stats.steps}
            ⚡ Calories Burned: ${stats.calories}
            💪 Pushups tracked: ${stats.pushups}
            Check out my progress! #BushyHealth #FitnessGame
        """.trimIndent()

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, "Share your progress")
        context.startActivity(shareIntent)
    }

    fun shareBitmap(context: Context, bitmap: Bitmap, title: String) {
        try {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            val stream = FileOutputStream("$cachePath/image.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val imagePath = File(context.cacheDir, "images")
            val newFile = File(imagePath, "image.png")
            val contentUri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", newFile)

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            context.startActivity(Intent.createChooser(shareIntent, title))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
