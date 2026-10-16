package com.example.ui.components

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.R
import com.example.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Collections

object TrackArtworkHelper {
    // Fast in-memory cache mapping track cacheKey -> File or Uri
    private val memoryCache = LruCache<String, Any>(200)
    // Negative cache so tracks without cover art are never scanned twice
    private val negativeCache = Collections.synchronizedSet(HashSet<String>())

    fun getCachedArtwork(context: Context, track: Track): Any? {
        val key = getCacheKey(track)
        if (negativeCache.contains(key)) return null
        memoryCache.get(key)?.let { return it }

        // Check if we already extracted and saved a thumbnail to local disk cache
        val diskFile = getDiskCacheFile(context, key)
        if (diskFile.exists() && diskFile.length() > 0L) {
            memoryCache.put(key, diskFile)
            return diskFile
        }
        return null
    }

    fun isKnownNoArtwork(track: Track): Boolean {
        return negativeCache.contains(getCacheKey(track))
    }

    private fun getCacheKey(track: Track): String {
        val raw = if (track.path.isNotBlank()) track.path else "${track.id}_${track.albumId}"
        return raw.hashCode().toUInt().toString(16)
    }

    private fun getDiskCacheFile(context: Context, key: String): File {
        val dir = File(context.cacheDir, "artwork_thumbs").apply {
            if (!exists()) mkdirs()
        }
        return File(dir, "art_$key.jpg")
    }

    suspend fun resolveArtwork(context: Context, track: Track): Any? {
        val key = getCacheKey(track)
        if (negativeCache.contains(key)) return null
        memoryCache.get(key)?.let { return it }

        return withContext(Dispatchers.IO) {
            if (negativeCache.contains(key)) return@withContext null
            memoryCache.get(key)?.let { return@withContext it }

            // 1. Check persistent local disk cache first (instant offline file read)
            val diskFile = getDiskCacheFile(context, key)
            if (diskFile.exists() && diskFile.length() > 0L) {
                memoryCache.put(key, diskFile)
                return@withContext diskFile
            }

            // 2. Try embedded ID3 / MP3 / FLAC picture via MediaMetadataRetriever
            var mmr: MediaMetadataRetriever? = null
            try {
                mmr = MediaMetadataRetriever()
                var dataSourceSet = false

                if (track.path.isNotBlank() && File(track.path).exists()) {
                    mmr.setDataSource(track.path)
                    dataSourceSet = true
                } else if (track.uri.isNotBlank()) {
                    mmr.setDataSource(context, Uri.parse(track.uri))
                    dataSourceSet = true
                }

                if (dataSourceSet) {
                    val rawBytes = mmr.embeddedPicture
                    if (rawBytes != null && rawBytes.isNotEmpty()) {
                        val decoded = decodeDownsampledBitmap(rawBytes, 300)
                        if (decoded != null) {
                            // Save compressed thumbnail to disk cache so future app launches are instant
                            try {
                                FileOutputStream(diskFile).use { out ->
                                    decoded.compress(Bitmap.CompressFormat.JPEG, 82, out)
                                }
                                decoded.recycle()
                                memoryCache.put(key, diskFile)
                                return@withContext diskFile
                            } catch (_: Exception) {
                                memoryCache.put(key, decoded)
                                return@withContext decoded
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                try {
                    mmr?.release()
                } catch (_: Exception) {
                }
            }

            // 3. Fallback: Verify if MediaStore actually has an album art stream (ignoring demo 500L)
            if (track.albumId > 0L && track.albumId != 500L) {
                try {
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        track.albumId
                    )
                    // Verify the stream actually opens before trusting the URI!
                    context.contentResolver.openInputStream(albumArtUri)?.use { stream ->
                        val rawBytes = stream.readBytes()
                        if (rawBytes.isNotEmpty()) {
                            val decoded = decodeDownsampledBitmap(rawBytes, 300)
                            if (decoded != null) {
                                FileOutputStream(diskFile).use { out ->
                                    decoded.compress(Bitmap.CompressFormat.JPEG, 82, out)
                                }
                                decoded.recycle()
                                memoryCache.put(key, diskFile)
                                return@withContext diskFile
                            }
                        }
                    }
                } catch (_: Exception) {
                    // MediaStore had no art for this albumId; proceed to negative cache
                }
            }

            // 4. Mark as negative so we never touch the file system for this track again
            negativeCache.add(key)
            null
        }
    }

    private fun decodeDownsampledBitmap(rawBytes: ByteArray, targetPx: Int): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, boundsOptions)
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

        var sampleSize = 1
        while (boundsOptions.outWidth / (sampleSize * 2) >= targetPx &&
            boundsOptions.outHeight / (sampleSize * 2) >= targetPx
        ) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
    }
}

@Composable
fun TrackArtworkView(
    track: Track?,
    modifier: Modifier = Modifier,
    placeholderIconSize: Dp = 28.dp,
    targetSizePx: Int = 144,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null
) {
    if (track == null) {
        PlaceholderArt(modifier, placeholderIconSize, contentDescription)
        return
    }

    val context = LocalContext.current
    var artwork by remember(track.id, track.path) {
        mutableStateOf(TrackArtworkHelper.getCachedArtwork(context, track))
    }

    LaunchedEffect(track.id, track.path) {
        if (artwork == null && !TrackArtworkHelper.isKnownNoArtwork(track)) {
            val resolved = TrackArtworkHelper.resolveArtwork(context, track)
            if (resolved != null) {
                artwork = resolved
            }
        }
    }

    if (artwork != null) {
        val imageRequest = remember(artwork, targetSizePx) {
            ImageRequest.Builder(context)
                .data(artwork)
                .size(targetSizePx, targetSizePx)
                .crossfade(false) // Disabled crossfade to prevent frame drops during fast list scrolling
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }

        AsyncImage(
            model = imageRequest,
            contentDescription = contentDescription ?: track.title,
            contentScale = contentScale,
            modifier = modifier,
            error = painterResource(id = R.drawable.ic_audiowave_logo),
            placeholder = painterResource(id = R.drawable.ic_audiowave_logo)
        )
    } else {
        PlaceholderArt(modifier, placeholderIconSize, contentDescription ?: track.title)
    }
}

@Composable
private fun PlaceholderArt(
    modifier: Modifier,
    size: Dp,
    contentDescription: String?
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(id = R.drawable.ic_audiowave_logo),
            contentDescription = contentDescription,
            tint = Color.Unspecified,
            modifier = Modifier.size(size)
        )
    }
}