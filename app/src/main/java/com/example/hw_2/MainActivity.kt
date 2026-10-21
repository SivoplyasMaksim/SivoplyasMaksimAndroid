package com.example.hw_2

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil.compose.AsyncImage
import com.example.hw_2.ui.theme.HW_2Theme
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val generatedPassword = PasswordGenerator.generatePassword()

        setContent {
            HW_2Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(generatedPassword)
                }
            }
        }
    }
}

@Composable
fun AppNavigation(generatedPassword: String) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Keys.ROUTE_LOGIN) {

        composable(Keys.ROUTE_LOGIN) {
            LoginScreen(
                generatedPassword = generatedPassword,
                onNavigateToResult = { email, password ->
                    val encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8.toString())
                    val encodedPassword = URLEncoder.encode(password, StandardCharsets.UTF_8.toString())
                    navController.navigate("${Keys.ROUTE_RESULT.replace("{email}", encodedEmail).replace("{password}", encodedPassword)}")
                }
            )
        }

        composable(
            route = Keys.ROUTE_RESULT,
            arguments = listOf(
                navArgument(Keys.ARG_EMAIL) { type = NavType.StringType },
                navArgument(Keys.ARG_PASSWORD) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString(Keys.ARG_EMAIL) ?: ""
            val password = backStackEntry.arguments?.getString(Keys.ARG_PASSWORD) ?: ""

            ResultScreen(
                email = email,
                password = password,
                generatedPassword = generatedPassword,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun LoginScreen(
    generatedPassword: String,
    onNavigateToResult: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val titleText = stringResource(id = R.string.title_authorization)
    val emailLabel = stringResource(id = R.string.label_email)
    val passwordLabel = stringResource(id = R.string.label_password)
    val loginButtonText = stringResource(id = R.string.button_login)
    val errorEmailEmpty = stringResource(id = R.string.error_email_empty)
    val errorEmailInvalid = stringResource(id = R.string.error_email_invalid)
    val errorPasswordEmpty = stringResource(id = R.string.error_password_empty)
    val errorPasswordInvalid = stringResource(id = R.string.error_password_invalid)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = titleText,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                emailError = null
            },
            label = { Text(emailLabel) },
            isError = emailError != null,
            supportingText = {
                emailError?.let { Text(it) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = null
            },
            label = { Text(passwordLabel) },
            isError = passwordError != null,
            supportingText = {
                passwordError?.let { Text(it) }
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    validateAndNavigate(
                        email = email,
                        password = password,
                        generatedPassword = generatedPassword,
                        onNavigateToResult = onNavigateToResult,
                        setEmailError = { emailError = it },
                        setPasswordError = { passwordError = it },
                        context = context,
                        errorEmailEmpty = errorEmailEmpty,
                        errorEmailInvalid = errorEmailInvalid,
                        errorPasswordEmpty = errorPasswordEmpty,
                        errorPasswordInvalid = errorPasswordInvalid
                    )
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                validateAndNavigate(
                    email = email,
                    password = password,
                    generatedPassword = generatedPassword,
                    onNavigateToResult = onNavigateToResult,
                    setEmailError = { emailError = it },
                    setPasswordError = { passwordError = it },
                    context = context,
                    errorEmailEmpty = errorEmailEmpty,
                    errorEmailInvalid = errorEmailInvalid,
                    errorPasswordEmpty = errorPasswordEmpty,
                    errorPasswordInvalid = errorPasswordInvalid
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(loginButtonText)
        }
    }
}

fun validateAndNavigate(
    email: String,
    password: String,
    generatedPassword: String,
    onNavigateToResult: (String, String) -> Unit,
    setEmailError: (String?) -> Unit,
    setPasswordError: (String?) -> Unit,
    context: Context,
    errorEmailEmpty: String,
    errorEmailInvalid: String,
    errorPasswordEmpty: String,
    errorPasswordInvalid: String
) {
    var isValid = true

    if (email.isBlank()) {
        setEmailError(errorEmailEmpty)
        isValid = false
    } else {
        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (!emailRegex.matches(email)) {
            setEmailError(errorEmailInvalid)
            isValid = false
        } else {
            setEmailError(null)
        }
    }

    if (password.isBlank()) {
        setPasswordError(errorPasswordEmpty)
        isValid = false
    } else {
        val passwordRegex = Regex("^[A-Za-z0-9]{8,}$")
        if (!passwordRegex.matches(password)) {
            setPasswordError(errorPasswordInvalid)
            isValid = false
        } else {
            setPasswordError(null)
        }
    }

    if (isValid) {
        onNavigateToResult(email, password)
    }
}

@Composable
fun ResultScreen(
    email: String,
    password: String,
    generatedPassword: String,
    onNavigateBack: () -> Unit
) {
    var isPasswordVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val isPasswordCorrect = password == generatedPassword

    val imageUrl = if (isPasswordCorrect) {
        "https://external-content.duckduckgo.com/iu/?u=https%3A%2F%2Fimg.freepik.com%2Fpremium-vector%2Fcorrect-green-grunge-stamp-isolated-white-background_565470-3420.jpg%3Fw%3D2000&f=1&nofb=1&ipt=4f5df29f13f622ecf8ff9e50236bb23bc95440692b0ee71907713d2f312472b1&ipo=images"
    } else {
        "https://external-content.duckduckgo.com/iu/?u=https%3A%2F%2Ftse1.mm.bing.net%2Fth%2Fid%2FOIP.Rc29Xcw0GJSnaNm8PwDe7AAAAA%3Fr%3D0%26pid%3DApi&f=1&ipt=ee712b5e6ab0994c3f50b106c2cd1af3f0f9c0a11835049ba43e413b76bf14bb&ipo=images"
    }

    val generatedPasswordLabel = stringResource(id = R.string.label_generated_password)
    val copyPasswordText = stringResource(id = R.string.button_copy_password)
    val backButtonText = stringResource(id = R.string.button_back)
    val toastCopied = stringResource(id = R.string.toast_password_copied)
    val contentDescSuccess = stringResource(id = R.string.content_desc_success)
    val contentDescError = stringResource(id = R.string.content_desc_error)
    val clipboardLabel = Keys.CLIPBOARD_LABEL

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = if (isPasswordCorrect) contentDescSuccess else contentDescError,
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 32.dp)
        )

        Text(
            text = "Email: $email",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = generatedPassword,
                onValueChange = {},
                label = { Text(generatedPasswordLabel) },
                readOnly = true,
                visualTransformation = if (isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                Text(
                    text = if (isPasswordVisible) "hide" else "show",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText(clipboardLabel, generatedPassword)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, toastCopied, Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(copyPasswordText)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(backButtonText)
        }
    }
}