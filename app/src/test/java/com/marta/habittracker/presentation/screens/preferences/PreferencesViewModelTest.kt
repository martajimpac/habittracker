package com.marta.habittracker.presentation.screens.preferences

import android.app.Application
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.coroutines.DispatchersProvider
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.usecase.FakeProfileRepository
import com.marta.habittracker.domain.usecase.UpdateDisplayName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.reflect.full.memberProperties

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], application = Application::class)
class PreferencesViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load prefills display name from profile repository`() = runTest {
        val profile = FakeProfileRepository.defaultProfile.copy(displayName = "Alex Rivera")
        val repository = FakeProfileRepository(myProfileResult = DataResult.Success(profile))
        val viewModel = createViewModel(repository)

        advanceUntilIdle()

        assertEquals("Alex Rivera", viewModel.uiState.value.displayName)
        assertEquals(FakeProfileRepository.defaultProfile.avatarColor, viewModel.uiState.value.avatarColor)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `save blank name emits invalid display name error without calling repository`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val event = async { viewModel.events.first() }
        viewModel.onDisplayNameChanged("   ")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(
            PreferencesEvent.ShowError(R.string.error_preferences_invalid_display_name),
            event.await(),
        )
        assertEquals(0, repository.updateDisplayNameCalls)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `save success emits NavigateBack and has no sticky navigation on UiState`() = runTest {
        val repository = FakeProfileRepository()
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val event = async { viewModel.events.first() }
        viewModel.onDisplayNameChanged("Marta Gomez")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(PreferencesEvent.NavigateBack, event.await())
        assertEquals(1, repository.updateDisplayNameCalls)
        assertFalse(viewModel.uiState.value.isSaving)
        assertFalse(
            PreferencesUiState::class.memberProperties.any { property ->
                property.name.contains("navigate", ignoreCase = true)
            },
        )
    }

    @Test
    fun `upload avatar success updates avatarUrl`() = runTest {
        val uploaded = FakeProfileRepository.defaultProfile.copy(
            avatarUrl = "https://cdn.example/avatar.jpg",
        )
        val repository = FakeProfileRepository(uploadAvatarResult = DataResult.Success(uploaded))
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        viewModel.onAvatarBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte()))
        advanceUntilIdle()

        assertEquals("https://cdn.example/avatar.jpg", viewModel.uiState.value.avatarUrl)
        assertEquals(1, repository.uploadAvatarCalls)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `upload avatar error emits ShowError`() = runTest {
        val repository = FakeProfileRepository(
            uploadAvatarResult = DataResult.Error(AppError.Common.Network),
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val event = async { viewModel.events.first() }
        viewModel.onAvatarBytes(byteArrayOf(1, 2, 3))
        advanceUntilIdle()

        assertEquals(
            PreferencesEvent.ShowError(R.string.error_no_internet),
            event.await(),
        )
        assertEquals(null, viewModel.uiState.value.avatarUrl)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `remove photo success nulls avatarUrl`() = runTest {
        val withAvatar = FakeProfileRepository.defaultProfile.copy(
            avatarUrl = "https://cdn.example/avatar.jpg",
        )
        val repository = FakeProfileRepository(
            myProfileResult = DataResult.Success(withAvatar),
            clearAvatarResult = DataResult.Success(withAvatar.copy(avatarUrl = null)),
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()
        assertEquals("https://cdn.example/avatar.jpg", viewModel.uiState.value.avatarUrl)

        viewModel.onRemovePhoto()
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.avatarUrl)
        assertEquals(1, repository.clearAvatarCalls)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun `save network error emits ShowError and does not navigate back`() = runTest {
        val repository = FakeProfileRepository(
            updateDisplayNameResult = DataResult.Error(AppError.Common.Network),
        )
        val viewModel = createViewModel(repository)
        advanceUntilIdle()

        val event = async { viewModel.events.first() }
        viewModel.onDisplayNameChanged("Marta")
        viewModel.save()
        advanceUntilIdle()

        assertEquals(
            PreferencesEvent.ShowError(R.string.error_no_internet),
            event.await(),
        )
        assertFalse(event.await() is PreferencesEvent.NavigateBack)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    private fun createViewModel(
        repository: FakeProfileRepository,
    ): PreferencesViewModel = PreferencesViewModel(
        updateDisplayName = UpdateDisplayName(repository),
        profileRepository = repository,
        dispatchers = ImmediateDispatchers(dispatcher),
        appContext = RuntimeEnvironment.getApplication(),
    )

    private class ImmediateDispatchers(
        dispatcher: CoroutineDispatcher,
    ) : DispatchersProvider {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
    }
}
