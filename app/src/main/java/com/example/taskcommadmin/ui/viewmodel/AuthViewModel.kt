package com.example.taskcommadmin.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taskcommadmin.data.SupabaseClientProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class AuthViewModel : ViewModel() {
	private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
	val authState: StateFlow<AuthState> = _authState.asStateFlow()

	private val _isSubmitting = MutableStateFlow(false)
	val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

	private val _message = MutableStateFlow<String?>(null)
	val message: StateFlow<String?> = _message.asStateFlow()

	private fun postMessage(text: String?) { _message.value = text }

	private fun auth(client: SupabaseClient) = client.pluginManager.getPlugin(Auth)
	private fun postgrest(client: SupabaseClient) = client.pluginManager.getPlugin(Postgrest)

	fun init(context: Context) {
		val client = SupabaseClientProvider.getClient(context)
		viewModelScope.launch {
			auth(client).sessionStatus.collect { status ->
				when (status) {
					is SessionStatus.Authenticated -> {
						val uid = status.session.user?.id ?: return@collect
						if (isAdmin(client, uid)) {
							_authState.value = AuthState.Authenticated(uid)
						} else {
							_authState.value = AuthState.Error("Not authorized as admin")
						}
					}
					is SessionStatus.NotAuthenticated -> _authState.value = AuthState.Unauthenticated
					is SessionStatus.LoadingFromStorage -> _authState.value = AuthState.Loading
					is SessionStatus.NetworkError -> {
						_authState.value = AuthState.Error("Network error")
						postMessage("Network error")
					}
				}
			}
		}
	}

	fun signIn(context: Context, email: String, password: String) {
		val client = SupabaseClientProvider.getClient(context)
		viewModelScope.launch {
			try {
				_isSubmitting.value = true
				_authState.value = AuthState.Loading
				postMessage("Signing in...")
				auth(client).signInWith(Email) {
					this.email = email
					this.password = password
				}
				val uid = auth(client).currentSessionOrNull()?.user?.id
				if (uid != null && isAdmin(client, uid)) {
					_authState.value = AuthState.Authenticated(uid)
					postMessage("Signed in")
				} else {
					auth(client).signOut()
					_authState.value = AuthState.Error("Not authorized as admin")
				}
			} catch (e: Exception) {
				_authState.value = AuthState.Error(e.message ?: "Sign in failed")
				postMessage(e.message ?: "Sign in failed")
			} finally {
				_isSubmitting.value = false
			}
		}
	}

	@Serializable
	private data class ProfileRow(
		val id: String? = null,
		val email: String? = null,
		val role: String? = null
	)

	private suspend fun isAdmin(client: SupabaseClient, uid: String): Boolean {
		return try {
			val email = auth(client).currentSessionOrNull()?.user?.email
			// by id
			val resById = postgrest(client)["profiles"].select {
				filter { eq("id", uid) }
				limit(1)
			}.decodeList<ProfileRow>()
			var role = resById.firstOrNull()?.role?.lowercase()
			if (role == null && !email.isNullOrBlank()) {
				// fallback by email
				val resByEmail = postgrest(client)["profiles"].select {
					filter { eq("email", email) }
					limit(1)
				}.decodeList<ProfileRow>()
				role = resByEmail.firstOrNull()?.role?.lowercase()
			}
			if (role == null) postMessage("Admin check: no profile row found for uid=$uid email=$email")
			role == "admin"
		} catch (e: Exception) {
			postMessage("Admin check failed: ${e.message}")
			false
		}
	}

	fun clearMessage() { postMessage(null) }

	fun signOut(context: Context) {
		val client = SupabaseClientProvider.getClient(context)
		viewModelScope.launch {
			auth(client).signOut()
			_authState.value = AuthState.Unauthenticated
			postMessage("Signed out")
		}
	}
}

sealed class AuthState {
	object Loading : AuthState()
	object Unauthenticated : AuthState()
	data class Authenticated(val userId: String) : AuthState()
	data class Error(val message: String) : AuthState()
}
