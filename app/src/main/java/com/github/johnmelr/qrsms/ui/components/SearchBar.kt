package com.github.johnmelr.qrsms.ui.components

import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun SearchBar(
    onValueChange: (String) -> Unit =  {},
    modifier: Modifier = Modifier
) {
    var searchInput by remember { mutableStateOf("") }

    TextField(value = searchInput, onValueChange = onValueChange)
}