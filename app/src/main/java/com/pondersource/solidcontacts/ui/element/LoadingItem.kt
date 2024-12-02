package com.pondersource.solidcontacts.ui.element

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoadingItem(
    message: String,
) {
    Column {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(message)
    }
}
