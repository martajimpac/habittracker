package com.marta.habittracker.data.repository

import android.app.Application
import com.marta.habittracker.core.network.NetworkChecker
import com.marta.habittracker.data.mapper.SocialMapper
import com.marta.habittracker.data.remote.FakeProfileRemote
import com.marta.habittracker.domain.DataResult
import com.marta.habittracker.domain.model.AppError
import com.marta.habittracker.domain.usecase.FakeAuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], application = Application::class)

class ProfileRepositoryImplTest {

    private val mapper = SocialMapper()
    private val authRepository = FakeAuthRepository()

    @Test
    fun `uploadAvatar writes bytes and returns profile with avatarUrl set`() = runTest {
        val remote = FakeProfileRemote()
        val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x01, 0x02)
        val repository = createRepository(remote = remote)

        val result = repository.uploadAvatar(jpegBytes)

        assertTrue(result is DataResult.Success)
        val avatarUrl = checkNotNull((result as DataResult.Success).data.avatarUrl)
        assertTrue(avatarUrl.startsWith(FakeProfileRemote.PUBLIC_URL))
        assertTrue(avatarUrl.contains("v="))
        assertArrayEquals(jpegBytes, remote.lastUploadedBytes)
        assertEquals(1, remote.uploadAvatarCalls)
    }

    @Test
    fun `clearAvatar returns profile with avatarUrl null`() = runTest {
        val remote = FakeProfileRemote(
            initial = FakeProfileRemote.defaultDto.copy(avatarUrl = FakeProfileRemote.PUBLIC_URL),
        )
        val repository = createRepository(remote = remote)

        val result = repository.clearAvatar()

        assertTrue(result is DataResult.Success)
        assertEquals(null, (result as DataResult.Success).data.avatarUrl)
        assertEquals(1, remote.clearAvatarCalls)
    }

    @Test
    fun `uploadAvatar offline returns Network without calling remote`() = runTest {
        val remote = FakeProfileRemote()
        val repository = createRepository(online = false, remote = remote)

        val result = repository.uploadAvatar(byteArrayOf(1, 2, 3))

        assertEquals(DataResult.Error(AppError.Common.Network), result)
        assertEquals(0, remote.uploadAvatarCalls)
    }

    @Test
    fun `clearAvatar offline returns Network without calling remote`() = runTest {
        val remote = FakeProfileRemote()
        val repository = createRepository(online = false, remote = remote)

        val result = repository.clearAvatar()

        assertEquals(DataResult.Error(AppError.Common.Network), result)
        assertEquals(0, remote.clearAvatarCalls)
    }

    @Test
    fun `updateDisplayName does not send avatarUrl so existing avatar is kept`() = runTest {
        val remote = FakeProfileRemote(
            initial = FakeProfileRemote.defaultDto.copy(avatarUrl = FakeProfileRemote.PUBLIC_URL),
        )
        val repository = createRepository(remote = remote)

        val result = repository.updateDisplayName("Marta Gomez")

        assertTrue(result is DataResult.Success)
        assertEquals(FakeProfileRemote.PUBLIC_URL, (result as DataResult.Success).data.avatarUrl)
        assertEquals(null, remote.lastUpdateMyProfileAvatarUrl)
        assertEquals(1, remote.updateMyProfileCalls)
        assertEquals(0, remote.uploadAvatarCalls)
        assertEquals(0, remote.clearAvatarCalls)
    }

    @Test
    fun `deleteAccount offline returns Network without calling remote or signing out`() = runTest {
        val remote = FakeProfileRemote()
        val repository = createRepository(online = false, remote = remote)

        val result = repository.deleteAccount()

        assertEquals(DataResult.Error(AppError.Common.Network), result)
        assertEquals(0, remote.deleteAccountCalls)
        assertEquals(0, authRepository.signOutCalls)
    }

    @Test
    fun `deleteAccount success invokes remote then signs out`() = runTest {
        val remote = FakeProfileRemote()
        val repository = createRepository(remote = remote)

        val result = repository.deleteAccount()

        assertTrue(result is DataResult.Success)
        assertEquals(1, remote.deleteAccountCalls)
        assertEquals(1, authRepository.signOutCalls)
    }

    @Test
    fun `deleteAccount remote failure returns Unknown without signing out`() = runTest {
        val remote = FakeProfileRemote(
            deleteAccountException = RuntimeException("invoke failed"),
        )
        val repository = createRepository(remote = remote)

        val result = repository.deleteAccount()

        assertEquals(DataResult.Error(AppError.Common.Unknown), result)
        assertEquals(1, remote.deleteAccountCalls)
        assertEquals(0, authRepository.signOutCalls)
    }

    private fun createRepository(
        online: Boolean = true,
        remote: FakeProfileRemote = FakeProfileRemote(),
    ): ProfileRepositoryImpl = ProfileRepositoryImpl(
        networkChecker = NetworkChecker { online },
        profileRemote = remote,
        socialMapper = mapper,
        authRepository = authRepository,
    )
}
