package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Picture Collage Pro", appName)
  }

  @Test
  fun `verify ViewModel startNewProject initializes slots`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = com.example.viewmodel.MainViewModel(application)
    
    viewModel.startNewProject(9)
    assertEquals(9, viewModel.gridLayoutSize)
    assertEquals(9, viewModel.activeImages.size)
    assertEquals("New Collage", viewModel.activeProjectName)
  }
}
