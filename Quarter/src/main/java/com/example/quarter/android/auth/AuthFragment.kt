package com.example.quarter.android.auth

import DataModel
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.quarter.android.R
import com.example.quarter.android.data.FirestoreSync
import com.google.android.gms.auth.api.signin.GoogleSignIn
import android.widget.EditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AuthFragment : Fragment() {

    private val dataModel: DataModel by activityViewModels()
    private var isRegisterMode = false

    private val webClientId = "1070209063071-tnf69e8q40o9v1ca42g30m55v9u6pleo.apps.googleusercontent.com"

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).result
            account?.idToken?.let { idToken ->
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val user = AuthManager.signInWithGoogle(idToken)
                        if (user != null) {
                            onAuthSuccess(user.uid, user.email ?: "")
                        }
                    } catch (e: Exception) {
                        showError("Ошибка Google входа: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_auth, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = view.findViewById<TextView>(R.id.auth_title)
        val emailInput = view.findViewById<EditText>(R.id.email_input)
        val passwordInput = view.findViewById<EditText>(R.id.password_input)
        val confirmPasswordLayout = view.findViewById<LinearLayout>(R.id.confirm_password_layout)
        val confirmPasswordInput = view.findViewById<EditText>(R.id.confirm_password_input)
        val errorText = view.findViewById<TextView>(R.id.error_text)
        val authButton = view.findViewById<Button>(R.id.auth_button)
        val googleButton = view.findViewById<Button>(R.id.google_sign_in_button)
        val toggleMode = view.findViewById<TextView>(R.id.toggle_mode)
        val loggedInSection = view.findViewById<LinearLayout>(R.id.logged_in_section)
        val userEmail = view.findViewById<TextView>(R.id.user_email)
        val signOutButton = view.findViewById<Button>(R.id.sign_out_button)

        // Закрытие по тапу на фон (всегда доступно)
        view.findViewById<View>(R.id.clickable_background).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Firebase не настроен — показываем заглушку
        if (!AuthManager.isAvailable) {
            title.text = "Аккаунт"
            emailInput.visibility = View.GONE
            passwordInput.visibility = View.GONE
            confirmPasswordLayout.visibility = View.GONE
            authButton.visibility = View.GONE
            googleButton.visibility = View.GONE
            toggleMode.visibility = View.GONE
            errorText.text = "Firebase не настроен. Добавьте google-services.json"
            errorText.visibility = View.VISIBLE
            return
        }

        // Если уже залогинен — показываем профиль
        if (AuthManager.isLoggedIn) {
            showLoggedInState(
                title, emailInput, passwordInput, confirmPasswordLayout,
                authButton, googleButton, toggleMode, loggedInSection,
                userEmail, signOutButton, errorText
            )
            return
        }

        // Переключение вход/регистрация
        toggleMode.setOnClickListener {
            isRegisterMode = !isRegisterMode
            if (isRegisterMode) {
                title.text = "Регистрация"
                authButton.text = "Зарегистрироваться"
                confirmPasswordLayout.visibility = View.VISIBLE
                toggleMode.text = "Уже есть аккаунт? Войти"
            } else {
                title.text = "Вход"
                authButton.text = "Войти"
                confirmPasswordLayout.visibility = View.GONE
                toggleMode.text = "Нет аккаунта? Зарегистрироваться"
            }
            errorText.visibility = View.GONE
        }

        // Email auth
        authButton.setOnClickListener {
            val email = emailInput.text?.toString()?.trim() ?: ""
            val password = passwordInput.text?.toString() ?: ""

            if (email.isEmpty() || password.isEmpty()) {
                showError("Заполните все поля")
                return@setOnClickListener
            }

            if (isRegisterMode) {
                val confirmPassword = confirmPasswordInput.text?.toString() ?: ""
                if (password != confirmPassword) {
                    showError("Пароли не совпадают")
                    return@setOnClickListener
                }
                if (password.length < 6) {
                    showError("Пароль должен быть не менее 6 символов")
                    return@setOnClickListener
                }
            }

            authButton.isEnabled = false
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val user = if (isRegisterMode) {
                        AuthManager.registerWithEmail(email, password)
                    } else {
                        AuthManager.signInWithEmail(email, password)
                    }
                    if (user != null) {
                        onAuthSuccess(user.uid, user.email ?: email)
                    } else {
                        showError("Не удалось выполнить вход")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("AuthFragment", "Auth error", e)
                    showError(e.localizedMessage ?: e.message ?: "Неизвестная ошибка")
                } finally {
                    authButton.isEnabled = true
                }
            }
        }

        // Google Sign-In
        googleButton.setOnClickListener {
            val client = AuthManager.getGoogleSignInClient(requireContext(), webClientId)
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    private fun onAuthSuccess(uid: String, email: String) {
        dataModel.isLoggedIn.value = true
        dataModel.userName.value = email

        val prefs = requireContext().getSharedPreferences("MyAppPrefs", android.content.Context.MODE_PRIVATE)

        // Проверяем, есть ли данные в Firestore
        FirestoreSync.checkUserExists(uid) { exists ->
            activity?.runOnUiThread {
                if (!exists) {
                    // Первый вход — загружаем локальные данные в Firestore
                    FirestoreSync.createProfile(uid, email)
                    FirestoreSync.syncLocalToFirestore(requireContext(), uid)
                    prefs.edit().putBoolean("SYNCED_TO_FIRESTORE", true).apply()
                } else {
                    // Данные есть в облаке — проверяем, есть ли конфликт с локальными
                    val localHowMany = try {
                        (prefs.getString("HOW_MANY", "0") ?: "0").toDouble()
                    } catch (_: Exception) { 0.0 }
                    val alreadySynced = prefs.getBoolean("SYNCED_TO_FIRESTORE", false)

                    if (localHowMany > 0 && !alreadySynced) {
                        // Есть и локальные данные и облачные — показываем диалог выбора
                        parentFragmentManager.popBackStack()
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.place_holder, com.example.quarter.android.data.DataConflictFragment.newInstance())
                            .addToBackStack(null)
                            .commit()
                        return@runOnUiThread
                    }
                    prefs.edit().putBoolean("SYNCED_TO_FIRESTORE", true).apply()
                }

                // Проверяем статус подписки
                FirestoreSync.getSubscriptionStatus(uid) { sub ->
                    activity?.runOnUiThread {
                        dataModel.isPremium.value = sub?.premium ?: false
                    }
                }
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun showLoggedInState(
        title: TextView,
        emailInput: EditText,
        passwordInput: EditText,
        confirmPasswordLayout: LinearLayout,
        authButton: Button,
        googleButton: View,
        toggleMode: TextView,
        loggedInSection: LinearLayout,
        userEmail: TextView,
        signOutButton: Button,
        errorText: TextView
    ) {
        // Скрываем форму входа
        emailInput.visibility = View.GONE
        passwordInput.visibility = View.GONE
        confirmPasswordLayout.visibility = View.GONE
        authButton.visibility = View.GONE
        googleButton.visibility = View.GONE
        toggleMode.visibility = View.GONE
        errorText.visibility = View.GONE

        // Показываем профиль
        title.text = "Аккаунт"
        loggedInSection.visibility = View.VISIBLE
        userEmail.text = AuthManager.currentUser?.email ?: ""

        signOutButton.setOnClickListener {
            AuthManager.signOut(requireContext(), webClientId)
            dataModel.isLoggedIn.value = false
            dataModel.isPremium.value = false
            dataModel.userName.value = null
            parentFragmentManager.popBackStack()
        }

        view?.findViewById<View>(R.id.clickable_background)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun showError(message: String) {
        view?.findViewById<TextView>(R.id.error_text)?.let {
            it.text = message
            it.visibility = View.VISIBLE
        }
    }

    private fun getFirebaseErrorMessage(e: Exception): String {
        val msg = e.localizedMessage ?: e.message ?: "Неизвестная ошибка"
        return when {
            msg.contains("no user record", ignoreCase = true) -> "Пользователь не найден"
            msg.contains("password is invalid", ignoreCase = true) -> "Неверный пароль"
            msg.contains("email address is badly formatted", ignoreCase = true) -> "Некорректный email"
            msg.contains("already in use", ignoreCase = true) -> "Email уже зарегистрирован"
            msg.contains("network", ignoreCase = true) -> "Ошибка сети. Проверьте подключение"
            else -> msg
        }
    }

    companion object {
        fun newInstance() = AuthFragment()
    }
}
