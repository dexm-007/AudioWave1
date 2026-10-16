package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.MusicRepository
import com.example.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Audiowave Player", appName)
    }

    @Test
    fun `parse synchronized LRC lyrics correctly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = MusicRepository(context)
        val lrc = """
            [00:00.00]First line
            [00:02.50]Second line
            [00:10.00]Third line
        """.trimIndent()

        val parsed = repo.parseLrc(lrc)
        assertEquals(3, parsed.size)
        assertEquals(0L, parsed[0].timestampMs)
        assertEquals("First line", parsed[0].text)
        assertEquals(2500L, parsed[1].timestampMs)
        assertEquals("Second line", parsed[1].text)
        assertEquals(10000L, parsed[2].timestampMs)
        assertEquals("Third line", parsed[2].text)
    }
}
