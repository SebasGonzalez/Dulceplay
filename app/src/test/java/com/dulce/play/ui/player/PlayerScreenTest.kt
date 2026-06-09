package com.dulce.play.ui.player

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.dulce.play.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlayerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testPlayerScreenRendersAndInteracts() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = PlayerViewModel(application)

        composeTestRule.setContent {
            MyApplicationTheme {
                PlayerScreen(
                    viewModel = viewModel,
                    onBack = {}
                )
            }
        }

        // Wait for Compose to render
        composeTestRule.waitForIdle()

        // 1. Verify screen renders the tab bar and starts on "REPRODUCTOR" tab
        composeTestRule.onNodeWithText("REPRODUCTOR").assertExists()
        composeTestRule.onNodeWithText("BUSCAR & IMPORTAR").assertExists()

        // 2. Verify current track info is correct and exists (initially Midnight Odyssey)
        composeTestRule.onNodeWithText("Midnight Odyssey").assertExists()
        composeTestRule.onNodeWithText("Retro Synth Lord").assertExists()

        // 3. Test interacting with playback controls (previous, play/pause, next)
        composeTestRule.onNodeWithContentDescription("Pista Anterior").assertExists().performClick()
        composeTestRule.onNodeWithContentDescription("Reproducir").assertExists().performClick()
        composeTestRule.onNodeWithContentDescription("Siguiente Pista").assertExists().performClick()

        // 4. Test interacting with Equalizer Panel Toggle
        composeTestRule.onNodeWithContentDescription("Equalizer").assertExists().performClick()
        composeTestRule.waitForIdle()

        // 5. Test tab switching to local/search biblioteca
        composeTestRule.onNodeWithText("BUSCAR & IMPORTAR").performClick()
        composeTestRule.waitForIdle()

        // Verify elements on "Buscar & Importar" tab render correctly
        composeTestRule.onNodeWithText("SINTONIZAR ALMACENAMIENTO").assertExists()
        composeTestRule.onNodeWithText("TU BIBLIOTECA LOCAL (0 ARCHIVOS)").assertExists()

        // Test typing into search query field
        composeTestRule.onNode(hasSetTextAction()).assertExists().performTextInput("Daft Punk")
        composeTestRule.waitForIdle()
    }
}
