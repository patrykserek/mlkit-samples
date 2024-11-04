package com.google.mlkit.md.classification


import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.*
import kotlin.coroutines.cancellation.CancellationException

val mainDispatcher = Dispatchers.Main
val defaultDispatcher = Dispatchers.Default
val unconfinedDispatcher = Dispatchers.Unconfined
val ioDispatcher = Dispatchers.IO

sealed class Either<out L, out R> {
    data class Left<out L>(val value: L) : Either<L, Nothing>()

    data class Right<out R>(val value: R) : Either<Nothing, R>()

    companion object {
        fun <R> right(value: R): Either<Nothing, R> = Right(value)

        fun <L> left(value: L): Either<L, Nothing> = Left(value)
    }
}

fun <T, R> Either<T, R>.fold(
    left: (T) -> Any,
    right: (R) -> Any,
): Any =
    when (this) {
        is Either.Left -> left(value)
        is Either.Right -> right(value)
    }

suspend fun <R> tryCatch(block: () -> R): Either<Throwable, R> {
    return try {
        Either.right(block())
    } catch (e: Throwable) {
        if (e is CancellationException) throw e
        Either.left(e)
    }
}


object AssetUtil {

    private const val TAG = "AssetUtil"

    suspend fun copyAssetsFolder(context: Context, sourceAsset: String, targetFolder: File) {

        withContext(ioDispatcher) {
            tryCatch {
                launch {
                    val assetManager = context.assets
                    val assets = assetManager.list(sourceAsset)
                    if (!assets.isNullOrEmpty()) {
                        if (!targetFolder.exists()) {
                            targetFolder.mkdirs()
                        }
                        for (itemInFolder in assets) {
                            val currentAssetPath = "$sourceAsset/$itemInFolder"
                            val isFile = assetManager.list(currentAssetPath)!!.isEmpty()

                            val target = File(targetFolder, itemInFolder)
                            if (isFile) {
                                // The file to copy into
                                copyAssetFile(context, currentAssetPath, target)
                            } else {
                                // The folder to create
                                if (!target.exists()) {
                                    target.mkdirs()
                                }
                                // Seek to see if the folder contains any file
                                copyAssetsFolder(context, currentAssetPath, target)
                            }
                        }
                    }
                }
            }.fold(
                left = { Log.e(TAG, "copyAssetsFolder: ", it) },
                right = { Log.d(TAG, "copyAssetsFolder: Success") },
            )
        }

    }

    @Throws(IOException::class)
    suspend fun copyAssetFile(context: Context, sourceAsset: String, target: File) {
        if (target.exists() && target.length() > 0) {
            return
        }

        withContext(Dispatchers.IO) {
            val inputStream: InputStream = context.assets.open(sourceAsset)
            val outputStream: OutputStream = FileOutputStream(target)

            inputStream.use { inputs ->
                outputStream.use { os ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (inputs.read(buffer).also { read = it } != -1) {
                        os.write(buffer, 0, read)
                    }
                    os.flush()
                }
            }
        }
    }

    @Throws(IOException::class)
    fun assetFilePath(context: Context, assetName: String): String {
        val inputLength: Long
        try {
            val assetStream = context.assets.open(assetName)
            inputLength = assetStream.available().toLong()
            assetStream.close()
        } catch (e: IOException) {
            Log.e(TAG, "assetFilePath: ", e)
            return ""
        }
        val file = File(context.filesDir, assetName)
        val outputLength = file.length()
        if (file.exists() && outputLength > 0) {
            if (outputLength == inputLength) {
                return file.absolutePath
            } else {
                file.writeText("")
            }
        }

        return try {
            runBlocking { copyAssetFile(context, assetName, file) }
            file.absolutePath
        } catch (_: Exception) {
            ""
        }
    }

    @Throws(IOException::class)
    fun assetFile(context: Context, assetName: String): File? {
        val inputLength: Long
        try {
            val assetStream = context.assets.open(assetName)
            inputLength = assetStream.available().toLong()
            assetStream.close()
        } catch (e: IOException) {
            Log.e(TAG, "assetFilePath: ", e)
            return null
        }
        val file = File(context.filesDir, assetName)
        val outputLength = file.length()
        if (file.exists() && outputLength > 0) {
            if (outputLength == inputLength) {
                return file
            } else {
                file.writeText("")
            }
        }

        return try {
            runBlocking { copyAssetFile(context, assetName, file) }
            file
        } catch (_: Exception) {
            null
        }
    }
}
