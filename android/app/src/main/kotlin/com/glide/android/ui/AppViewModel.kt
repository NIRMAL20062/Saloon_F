package com.glide.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.glide.android.data.auth.PhoneLogin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Which part of the app to show: login when signed out, the app when signed in. Logout anywhere lands on login. */
@HiltViewModel
class AppViewModel
    @Inject
    constructor(
        login: PhoneLogin,
    ) : ViewModel() {
        val signedIn: StateFlow<Boolean> =
            login.session
                .map { it != null }
                .stateIn(viewModelScope, SharingStarted.Eagerly, login.session.value != null)
    }
