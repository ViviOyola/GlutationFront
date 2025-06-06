package com.example.glutationapp.ui

import android.util.Log
import com.example.glutationapp.data.ProfileRepository
import com.example.glutationapp.model.UserData
import com.example.glutationapp.model.UserSession
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit4.MockKRule
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.lang.Exception

@ExperimentalCoroutinesApi
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

@ExperimentalCoroutinesApi
class ProfileViewModelTest {

    @get:Rule
    val mockkRule = MockKRule(this)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @RelaxedMockK
    private lateinit var mockRepository: ProfileRepository

    private lateinit var viewModel: ProfileViewModel

    private val testUser = UserData(1, "Test User", "test@example.com", "123 Test St", "1234567890")

    @Before
    fun setup() {
        // Mock all static Android framework methods to prevent test crashes
        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0

        UserSession.login(testUser)
        coEvery { mockRepository.fetchOrderHistory(testUser.id) } returns Result.success(emptyList())
        viewModel = ProfileViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        UserSession.logout()
        unmockkStatic(Log::class) // Clean up the mocks
    }

    @Test
    fun `when viewmodel is initialized, it loads user data and fetches order history`() = runTest {
        val orders = listOf(OrderHistoryItem(1, "2024-01-01", "100"))
        coEvery { mockRepository.fetchOrderHistory(testUser.id) } returns Result.success(orders)

        viewModel = ProfileViewModel(mockRepository)
        advanceUntilIdle()

        assertEquals(testUser.name, viewModel.uiState.value.editableName)
        assertEquals(testUser.email, viewModel.uiState.value.editableEmail)
        assertEquals(orders, viewModel.uiState.value.orderHistory)
        assertFalse(viewModel.uiState.value.isLoadingOrders)
    }

    @Test
    fun `fetchOrderHistory fails and updates state with error`() = runTest {
        val errorMessage = "Database error"
        coEvery { mockRepository.fetchOrderHistory(testUser.id) } returns Result.failure(Exception(errorMessage))

        viewModel.fetchOrderHistory()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.orderHistoryError!!.contains(errorMessage))
        assertTrue(viewModel.uiState.value.orderHistory.isEmpty())
        assertFalse(viewModel.uiState.value.isLoadingOrders)
    }

    @Test
    fun `onEditModeChange updates state correctly`() {
        viewModel.onEditModeChange(true)
        assertTrue(viewModel.uiState.value.isEditMode)

        viewModel.onEditModeChange(false)
        assertFalse(viewModel.uiState.value.isEditMode)
    }

    @Test
    fun `input changes update state`() {
        viewModel.onNameChange("New Name")
        viewModel.onEmailChange("new@email.com")

        assertEquals("New Name", viewModel.uiState.value.editableName)
        assertEquals("new@email.com", viewModel.uiState.value.editableEmail)
    }

    @Test
    fun `saveChanges with invalid input shows errors and does not call repository`() = runTest {
        viewModel.onNameChange("")
        viewModel.onEmailChange("invalid-email")

        viewModel.saveChanges()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.nameError)
        assertNotNull(viewModel.uiState.value.emailError)
        coVerify(exactly = 0) { mockRepository.updateUserData(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `saveChanges with valid input updates user data successfully`() = runTest {
        val newName = "Updated Name"
        val newEmail = "updated@example.com"
        val updatedUserData = UserData(testUser.id, newName, newEmail, testUser.address, testUser.telefonoContacto)
        coEvery { mockRepository.updateUserData(testUser.id, newName, newEmail, testUser.address, testUser.telefonoContacto!!) } returns Result.success(updatedUserData)

        viewModel.onNameChange(newName)
        viewModel.onEmailChange(newEmail)

        viewModel.saveChanges()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isEditMode)
        assertEquals(newName, UserSession.userName)
        assertNull(viewModel.uiState.value.generalError)
    }

    @Test
    fun `saveChanges fails on repository error`() = runTest {
        val errorMessage = "Update failed"
        coEvery { mockRepository.updateUserData(any(), any(), any(), any(), any()) } returns Result.failure(Exception(errorMessage))

        // Set edit mode to true to simulate user starting an edit
        viewModel.onEditModeChange(true)

        viewModel.onNameChange("Valid Name")
        viewModel.onEmailChange("valid@email.com")
        viewModel.onAddressChange("Valid Address")
        viewModel.onTelefonoChange("1234567890")

        viewModel.saveChanges()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.generalError)
        assertTrue(viewModel.uiState.value.generalError!!.contains(errorMessage))
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isEditMode)
    }

    @Test
    fun `delete order successfully refreshes order history`() = runTest {
        val orderId = 123
        coEvery { mockRepository.deleteOrder(orderId) } returns Result.success(true)
        coEvery { mockRepository.fetchOrderHistory(testUser.id) } returns Result.success(emptyList())

        viewModel.onShowConfirmDeleteDialog(orderId)
        viewModel.confirmDeleteOrder()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showConfirmDeleteDialog)
        assertTrue(viewModel.uiState.value.orderHistory.isEmpty())
        coVerify { mockRepository.deleteOrder(orderId) }
        coVerify { mockRepository.fetchOrderHistory(testUser.id) }
    }

    @Test
    fun `delete order fails on repository error`() = runTest {
        val orderId = 123
        val errorMessage = "Deletion failed"
        coEvery { mockRepository.deleteOrder(orderId) } returns Result.failure(Exception(errorMessage))

        viewModel.onShowConfirmDeleteDialog(orderId)
        viewModel.confirmDeleteOrder()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isDeletingOrder)
        assertNotNull(viewModel.uiState.value.generalError)
        assertTrue(viewModel.uiState.value.generalError!!.contains(errorMessage))
    }

    @Test
    fun `resetAndDiscardChanges resets state to original user data`() {
        viewModel.onEditModeChange(true)
        viewModel.onNameChange("Something temporary")
        viewModel.onEmailChange("temp@email.com")

        viewModel.resetAndDiscardChanges()

        assertFalse(viewModel.uiState.value.isEditMode)
        assertEquals(testUser.name, viewModel.uiState.value.editableName)
        assertEquals(testUser.email, viewModel.uiState.value.editableEmail)
        assertNull(viewModel.uiState.value.nameError)
        assertNull(viewModel.uiState.value.generalError)
    }

    @Test
    fun `dialog dismissal updates state correctly`() {
        viewModel.onShowConfirmDeleteDialog(123)
        assertTrue(viewModel.uiState.value.showConfirmDeleteDialog)

        viewModel.onDismissConfirmDeleteDialog()
        assertFalse(viewModel.uiState.value.showConfirmDeleteDialog)
        assertNull(viewModel.uiState.value.orderToDeleteId)
    }
} 