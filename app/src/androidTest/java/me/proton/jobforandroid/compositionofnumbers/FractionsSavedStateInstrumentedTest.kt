package me.proton.jobforandroid.compositionofnumbers

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import me.proton.jobforandroid.compositionofnumbers.presentation.FractionsGameViewModel
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

/** Fresh ViewModels and fresh String-only handles exercise transport, not OS process death. */
@RunWith(AndroidJUnit4::class)
class FractionsSavedStateInstrumentedTest {
    private fun main(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    private fun copy(handle: SavedStateHandle) = SavedStateHandle(mapOf(
        "level" to "TEST", "fractionsSession" to requireNotNull(handle.get<String>("fractionsSession"))))

    @Test fun questionAndFeedbackRoundTripPreserveOrderAndRejectOldObjects() = main {
        val handle = SavedStateHandle(mapOf("level" to "TEST"))
        val first = FractionsGameViewModel(handle)
        val question = requireNotNull(first.refresh()?.question)
        val saved = requireNotNull(handle.get<String>("fractionsSession"))
        val restoredHandle = copy(handle)
        val restored = FractionsGameViewModel(restoredHandle)
        assertNull(restored.errorMessage)
        assertEquals(saved, restoredHandle.get<String>("fractionsSession"))
        val current = requireNotNull(restored.refresh()?.question)
        assertEquals(question.exercise, current.exercise)
        assertEquals(question.options, current.options)
        restored.answer(question, 0) // Old rendered object belongs to a different owner.
        assertEquals(0, restored.refresh()?.answered)
        restored.answer(current, 0)
        val feedback = requireNotNull(restored.refresh())
        assertEquals(1, feedback.answered)
        val feedbackHandle = copy(restoredHandle)
        val feedbackRecord = feedbackHandle.get<String>("fractionsSession")
        val feedbackOwner = FractionsGameViewModel(feedbackHandle)
        assertEquals(feedbackRecord, feedbackHandle.get<String>("fractionsSession"))
        assertEquals(feedback.phase, feedbackOwner.refresh()?.phase)
        assertEquals(feedback.selectedIndex, feedbackOwner.refresh()?.selectedIndex)
        assertEquals(1, feedbackOwner.refresh()?.answered)
    }

    @Test fun expiredSnapshotFinishesAndNavigationMarkerSurvivesAnotherOwner() = main {
        val handle = SavedStateHandle(mapOf("level" to "TEST"))
        FractionsGameViewModel(handle)
        val envelope = JSONObject(requireNotNull(handle.get<String>("fractionsSession")))
        val payload = JSONObject(envelope.getString("payload"))
            .put("deadline", SystemClock.elapsedRealtime()).toString()
        val checksum = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        handle["fractionsSession"] = envelope.put("payload", payload).put("checksum", checksum).toString()
        val expired = FractionsGameViewModel(copy(handle))
        assertNull(expired.errorMessage)
        assertEquals(0, expired.refresh()?.result?.answeredCount)
        val finishedHandle = copy(handle)
        val finished = FractionsGameViewModel(finishedHandle)
        finished.markResultNavigated()
        finished.markResultNavigated()
        val again = FractionsGameViewModel(copy(finishedHandle))
        assertTrue(requireNotNull(again.refresh()).resultNavigated)
        assertEquals(0, again.refresh()?.answered)
    }

    @Test fun malformedAndChecksumDamagedRecordsNeverGenerateReplacement() = main {
        val valid = SavedStateHandle(mapOf("level" to "TEST"))
        FractionsGameViewModel(valid)
        val damaged = JSONObject(requireNotNull(valid.get<String>("fractionsSession")))
            .put("checksum", "damaged").toString()
        for (record in listOf("invalid", damaged)) {
            val handle = SavedStateHandle(mapOf("level" to "TEST", "fractionsSession" to record))
            repeat(2) {
                val owner = FractionsGameViewModel(handle)
                assertEquals(R.string.fractions_restore_error, owner.errorMessage)
                assertNull(owner.refresh())
                assertEquals("invalid", handle.get<String>("fractionsSession"))
            }
        }
    }
}
