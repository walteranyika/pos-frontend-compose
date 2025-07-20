package com.chui.pos.services

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

private val logger = KotlinLogging.logger {}

/**
 * A service responsible for playing sound effects.
 * It uses a dedicated I/O coroutine scope to avoid blocking the UI thread.
 */
class SoundService {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun playAddToCartSound() {
        scope.launch {
            try {
                // Load the sound file from the application's resources.
                val resourceStream = this::class.java.getResourceAsStream("/sounds/beep.wav")
                if (resourceStream == null) {
                    logger.error { "Sound file not found: /sounds/beep.wav. Make sure it's in the resources folder." }
                    return@launch
                }

                // The AudioSystem requires a stream that supports mark/reset, so we use a BufferedInputStream.
                BufferedInputStream(resourceStream).use { bufferedStream ->
                    AudioSystem.getAudioInputStream(bufferedStream).use { audioInputStream ->
                        val clip: Clip = AudioSystem.getClip()
                        clip.open(audioInputStream)
                        clip.start() // Play the sound.
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "An error occurred while trying to play the sound." }
            }
        }
    }
}