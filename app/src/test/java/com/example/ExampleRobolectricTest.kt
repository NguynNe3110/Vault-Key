package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [36])
class ExampleRobolectricTest {
    @Test fun applicationName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("VaultKey", context.getString(R.string.app_name))
    }
}
