package com.example.chattaskai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chattaskai.data.profile.ProfileStore
import com.example.chattaskai.ui.components.LiquidBackground
import com.example.chattaskai.ui.theme.LocalLiquidColors
import com.example.chattaskai.ui.theme.FontLoader

@Composable
fun RegistrationScreen(
    onFinished: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val colors = LocalLiquidColors.current
    val store = remember { ProfileStore(context) }

    var phoneNumber by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        LiquidBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Taskline",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = FontLoader.lobster(),
                    color = Color.White,
                    fontSize = 38.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Register with your mobile number to get started",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = colors.cyan,
                        modifier = Modifier
                            .height(48.dp)
                            .fillMaxWidth(0.3f)
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = {
                            phoneNumber = it
                            error = ""
                        },
                        label = { Text("Mobile Number", color = Color.White.copy(alpha = 0.6f)) },
                        placeholder = { Text("+1 (555) 000-0000", color = Color.White.copy(alpha = 0.3f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.White.copy(alpha = 0.02f),
                            focusedContainerColor = Color.White.copy(alpha = 0.05f),
                            unfocusedTextColor = Color.White,
                            focusedTextColor = Color.White,
                            cursorColor = colors.cyan,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                            focusedBorderColor = colors.cyan
                        ),
                        singleLine = true
                    )

                    if (error.isNotEmpty()) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = {
                            when {
                                phoneNumber.isBlank() -> error = "Please enter a mobile number"
                                phoneNumber.replace(Regex("[^\\d+]"), "").length < 10 -> {
                                    error = "Please enter a valid mobile number"
                                }
                                else -> {
                                    store.setRegistrationComplete(phoneNumber)
                                    onFinished(phoneNumber)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.cyan)
                    ) {
                        Text("Continue", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
