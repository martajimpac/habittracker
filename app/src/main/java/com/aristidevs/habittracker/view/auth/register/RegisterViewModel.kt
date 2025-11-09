package com.aristidevs.habittracker.view.auth.register

import android.util.Patterns
import androidx.lifecycle.ViewModel
import com.aristidevs.habittracker.domain.usecase.Login
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(val login: Login) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState

    fun onRegisterChanged(phoneNumber: String) {
        _uiState.update { state ->
            state.copy(
                inputValue = phoneNumber,
                isRegisterEnabled =  when(_uiState.value.mode){
                    MY_MODE.EMAIL -> {
                        isPhoneNumberValid(phoneNumber)
                    }
                    MY_MODE.PHONE -> {
                        isEmailAddressValid(phoneNumber)
                    }
                }

            )
        }

    }

    fun onChangeMode() {
        registerState(true)
        _uiState.update {
            it.copy(
                mode = if(it.mode == MY_MODE.EMAIL) MY_MODE.PHONE else MY_MODE.EMAIL,
                inputValue = ""
            )
        }
    }


    /**** MÉTODOS PRIVADOS ****/

    private fun registerState(isLoading: Boolean){
        _uiState.update { it.copy(isLoading = isLoading) }
    }

    private fun isPhoneNumberValid(phoneNumber: String): Boolean = Patterns.PHONE.matcher(phoneNumber).matches()

    private fun isEmailAddressValid(email: String): Boolean = Patterns.EMAIL_ADDRESS.matcher(email).matches()

}

data class RegisterUiState(
    val inputValue: String = "",
    val isLoading: Boolean = false,
    val isRegisterEnabled: Boolean = true,
    val mode: MY_MODE = MY_MODE.EMAIL,
)

enum class MY_MODE {
    PHONE, EMAIL
}
