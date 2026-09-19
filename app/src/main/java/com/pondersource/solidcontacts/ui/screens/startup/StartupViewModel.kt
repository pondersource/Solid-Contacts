package com.pondersource.solidcontacts.ui.screens.startup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pondersource.solidcontacts.data.repository.AccountRepository
import com.pondersource.solidcontacts.data.repository.AccountState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StartupDestination { Deciding, Home, Login }

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _destination = MutableStateFlow(StartupDestination.Deciding)
    val destination: StateFlow<StartupDestination> = _destination.asStateFlow()

    init {
        viewModelScope.launch {
            _destination.value = when (accountRepository.currentState()) {
                is AccountState.SignedIn -> StartupDestination.Home
                AccountState.SignedOut -> StartupDestination.Login
                AccountState.Checking -> StartupDestination.Deciding
            }
        }
    }
}
