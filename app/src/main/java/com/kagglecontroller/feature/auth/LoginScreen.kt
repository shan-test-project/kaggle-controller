package com.kagglecontroller.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.kagglecontroller.core.ui.components.OpenInKaggleButton
import com.kagglecontroller.core.ui.theme.CodeFont
import com.kagglecontroller.core.ui.theme.KC

@Composable
fun LoginScreen(busy: Boolean, error: String?, onSignIn: (String) -> Unit) {
    var token by rememberSaveable { mutableStateOf("") }
    var visible by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(KC.Ink, Color(0xFF0B1330))))
            .statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(48.dp))
        Column(Modifier.widthIn(max = 480.dp)) {
            Text("Kaggle Controller", style = MaterialTheme.typography.headlineMedium, color = KC.Text)
            Spacer(Modifier.height(4.dp))
            Text("Your Kaggle workspace, redesigned for Android.", color = KC.TextDim, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(32.dp))

            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(KC.Surface).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Connect your account", style = MaterialTheme.typography.titleMedium, color = KC.Text)
                Text(
                    "Create an API token on Kaggle (Settings, API, Generate New Token), copy it, and paste it here. " +
                        "It starts with KGAT_.",
                    color = KC.TextDim, style = MaterialTheme.typography.bodyMedium,
                )
                OpenInKaggleButton("https://www.kaggle.com/settings/api", label = "Open Kaggle API settings")
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it.trim() },
                    label = { Text("API token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = CodeFont),
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = error != null,
                    supportingText = {
                        when {
                            error != null -> Text(error)
                            token.isNotEmpty() && !token.startsWith("KGAT_") ->
                                Text("This doesn't look like a KGAT_ token. Legacy kaggle.json keys aren't supported yet.")
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (visible) "Hide token" else "Show token",
                            )
                        }
                    },
                )
                Button(
                    onClick = { onSignIn(token) },
                    enabled = token.isNotBlank() && !busy,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (busy) CircularProgressIndicator(Modifier.height(20.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Verify and connect")
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "The token is checked with Kaggle, then stored encrypted on this phone (Android Keystore). " +
                    "It's only ever sent to api.kaggle.com. There is no Kaggle Controller server.",
                color = KC.TextDim, style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
