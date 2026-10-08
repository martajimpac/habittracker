package com.marta.habittracker.presentation.widgets

import android.app.Application
import android.view.LayoutInflater
import com.marta.habittracker.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], application = Application::class)
class WidgetPreviewLayoutTest {

    @Test
    fun `habit challenge and weekly providers declare previewLayout`() {
        val context = RuntimeEnvironment.getApplication()

        assertEquals(
            R.layout.habit_widget_preview,
            previewLayoutResId(context, R.xml.habit_widget_info),
        )
        assertEquals(
            R.layout.challenge_widget_preview,
            previewLayoutResId(context, R.xml.challenge_widget_info),
        )
        assertEquals(
            R.layout.weekly_widget_preview,
            previewLayoutResId(context, R.xml.weekly_widget_info),
        )
    }

    @Test
    fun `preview layouts inflate`() {
        val inflater = LayoutInflater.from(RuntimeEnvironment.getApplication())

        assertNotNull(inflater.inflate(R.layout.habit_widget_preview, null))
        assertNotNull(inflater.inflate(R.layout.challenge_widget_preview, null))
        assertNotNull(inflater.inflate(R.layout.weekly_widget_preview, null))
    }

    private fun previewLayoutResId(context: android.content.Context, xmlRes: Int): Int {
        val parser = context.resources.getXml(xmlRes)
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG &&
                parser.name == "appwidget-provider"
            ) {
                return parser.getAttributeResourceValue(
                    "http://schemas.android.com/apk/res/android",
                    "previewLayout",
                    0,
                )
            }
        }
        return 0
    }
}
