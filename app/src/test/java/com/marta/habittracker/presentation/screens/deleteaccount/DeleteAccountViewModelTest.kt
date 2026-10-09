package com.marta.habittracker.presentation.screens.deleteaccount

import android.app.Application
import com.marta.habittracker.R
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.usecase.FakeProfileRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], application = Application::class)
class DeleteAccountViewModelTest {

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
    fun `isDeleteEnabled is false until confirmation trim equals DELETE`() {
        val viewModel = createViewModel()

        assertFalse(viewModel.uiState.value.isDeleteEnabled)

        viewModel.onConfirmationChanged("delete")
        assertFalse(viewModel.uiState.value.isDeleteEnabled)

        viewModel.onConfirmationChanged(" DELETE ")
        assertTrue(viewModel.uiState.value.isDeleteEnabled)
    }

    @Test
    fun `onDeleteConfirmed is no-op when token does not match`() = runTest {
        val repository = FakeProfileRepository(deleteAccountResult = DataResult.Success(Unit))
        val viewModel = createViewModel(repository)

        viewModel.onConfirmationChanged("delete")
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertEquals(0, repository.deleteAccountCalls)
        assertFalse(viewModel.uiState.value.isDeleteEnabled)
        assertFalse(viewModel.uiState.value.isDeleting)
    }

    @Test
    fun `confirm success emits AccountDeleted and calls repository once`() = runTest {
        val repository = FakeProfileRepository(deleteAccountResult = DataResult.Success(Unit))
        val viewModel = createViewModel(repository)
        val event = async { viewModel.events.first() }

        viewModel.onConfirmationChanged("DELETE")
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertEquals(DeleteAccountEvent.AccountDeleted, event.await())
        assertEquals(1, repository.deleteAccountCalls)
        assertFalse(viewModel.uiState.value.isDeleting)
    }

    @Test
    fun `confirm error emits ShowError without AccountDeleted`() = runTest {
        val repository = FakeProfileRepository(
            deleteAccountResult = DataResult.Error(AppError.Common.Unknown),
        )
        val viewModel = createViewModel(repository)
        val event = async { viewModel.events.first() }

        viewModel.onConfirmationChanged("DELETE")
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertEquals(
            DeleteAccountEvent.ShowError(R.string.error_common_unknown),
            event.await(),
        )
        assertEquals(1, repository.deleteAccountCalls)
        assertFalse(viewModel.uiState.value.isDeleting)
    }

    @Test
    fun `confirm offline emits Network ShowError`() = runTest {
        val repository = FakeProfileRepository(
            deleteAccountResult = DataResult.Error(AppError.Common.Network),
        )
        val viewModel = createViewModel(repository)
        val event = async { viewModel.events.first() }

        viewModel.onConfirmationChanged("DELETE")
        viewModel.onDeleteConfirmed()
        advanceUntilIdle()

        assertEquals(
            DeleteAccountEvent.ShowError(R.string.error_no_internet),
            event.await(),
        )
        assertEquals(1, repository.deleteAccountCalls)
        assertFalse(viewModel.uiState.value.isDeleting)
    }

    private fun createViewModel(
        repository: FakeProfileRepository = FakeProfileRepository(),
    ): DeleteAccountViewModel = DeleteAccountViewModel(
        profileRepository = repository,
        appContext = RuntimeEnvironment.getApplication(),
    )
}
