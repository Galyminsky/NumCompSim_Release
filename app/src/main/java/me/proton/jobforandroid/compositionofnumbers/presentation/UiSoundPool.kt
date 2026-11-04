package me.proton.jobforandroid.compositionofnumbers.presentation

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

internal class UiSoundPool(context: Context, vararg sounds: Int) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()
    private val ids: Map<Int, Int>
    private val loaded = mutableSetOf<Int>()
    private var pending: Int? = null
    private var released = false

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (!released && status == 0) {
                loaded += sampleId
                if (pending == sampleId) {
                    pending = null
                    pool.play(sampleId, 1f, 1f, 1, 0, 1f)
                }
            }
        }
        ids = sounds.associateWith { pool.load(context, it, 1) }
    }

    fun play(sound: Int) {
        if (released) return
        val id = ids[sound] ?: return
        if (id in loaded) pool.play(id, 1f, 1f, 1, 0, 1f) else pending = id
    }

    fun release() {
        released = true
        pending = null
        pool.setOnLoadCompleteListener(null)
        pool.release()
    }
}
