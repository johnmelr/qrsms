package com.github.johnmelr.qrsms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    onQueryChange: (String) -> Unit =  {},
    onClick: () -> Unit = {},
    input: String = "",
    placeholder: @Composable (() -> Unit)? = null,
    showSearchIcon: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier.background(
            color = MaterialTheme.colorScheme.secondaryContainer,
        ).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            modifier = Modifier,
            value = input,
            onValueChange = onQueryChange,
        ) { innerTextField ->
            TextFieldDefaults.DecorationBox(
                value = input,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    focusedTextColor = MaterialTheme.colorScheme.background,
                    unfocusedTextColor = MaterialTheme.colorScheme.background,
                    disabledTextColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.outline,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.outline,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.outline
                ),
                enabled = true,
                singleLine = false,
                innerTextField = innerTextField,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                placeholder = placeholder,
            )
        }
        if (showSearchIcon) {
            Icon(
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { onClick() },
                imageVector = Icons.Outlined.Search, contentDescription = "Search Icon"
            )
        }
    } }

@Preview
@Composable
fun SearchBarPreview() {
    SearchBar(placeholder = { Text("Hello World") })
}
