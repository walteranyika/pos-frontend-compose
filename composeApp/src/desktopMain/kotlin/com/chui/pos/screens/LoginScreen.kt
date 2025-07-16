package com.chui.pos.screens


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.chui.pos.viewmodels.LoginEvent
import com.chui.pos.viewmodels.LoginUiState
import com.chui.pos.viewmodels.LoginViewModel
import org.koin.compose.koinInject


class LoginScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = koinInject<LoginViewModel>()
        val loginState = viewModel.loginState
        val pin = viewModel.pin


        // Navigate on successful login
        LaunchedEffect(Unit) {
            viewModel.events.collect { event ->
                when (event) {
                    is LoginEvent.NavigateToHome -> {
                        // This will clear the back stack and make HomeScreen the new root
                        navigator.replaceAll(PosScreen)
                    }
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.width(500.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Enter Your PIN", style = MaterialTheme.typography.headlineMedium)

                // PIN Display
                Text(
                    text = "●".repeat(pin.length).padEnd(4, '○'),
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Numeric Keypad
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val buttons = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        // Use a 4x3 grid for a more standard keypad layout
                        listOf("Clear", "0", "Login")
                    )
                    buttons.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { label ->
                                PinButton(
                                    label = label,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        when (label) {
                                            "Login" -> viewModel.onLoginClicked()
                                            "Clear" -> viewModel.onClear()
                                            else -> viewModel.onPinChange(pin + label)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (loginState is LoginUiState.Loading) {
                    CircularProgressIndicator()
                }

                if (loginState is LoginUiState.Error) {
                    Text(
                        text = loginState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PinButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isAction = label == "Login" || label == "Clear"

    // Use different colors for action buttons vs. number buttons
    val colors = when (label) {
        "Login" -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiary
        )
        "Clear" -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        else -> ButtonDefaults.buttonColors()
    }

    Button(
        onClick = onClick,
        modifier = modifier.height(140.dp).width(140.dp),
        shape = RoundedCornerShape(3.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
        colors = colors
    ) {
        // Use a smaller font size for the action buttons
        Text(label, fontSize = if (isAction) 18.sp else 24.sp)
    }
}