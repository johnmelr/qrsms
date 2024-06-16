package com.github.johnmelr.qrsms.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.johnmelr.qrsms.R
import com.github.johnmelr.qrsms.data.contacts.ContactDetails
import com.github.johnmelr.qrsms.ui.model.ManageKeysViewModel

@Composable
fun ManageKeysScreen(
    modifier: Modifier = Modifier,
    manageKeysViewModel: ManageKeysViewModel = hiltViewModel(),
    onViewQrCode: (String) -> Unit = {},
) {
    val aliases by manageKeysViewModel.aliases.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    val openDialog = remember { mutableStateOf(false) }
    val keyToDelete = remember { mutableStateOf("" ) }

    KeysList(
        modifier = modifier
            .fillMaxSize(),
        aliases = aliases,
        listState = listState,
        checkSecretKey = { manageKeysViewModel.phoneNumberHasSecretKey(it) },
        onViewQrCode = { onViewQrCode(it) },
        onDeleteKey = {
            openDialog.value = true
            keyToDelete.value = it
        }
    )

    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = { openDialog.value = false },
            confirmButton = { TextButton(onClick = {
                openDialog.value = false
                manageKeysViewModel.deleteKey(keyToDelete.value)
            }) {
                Text("Confirm")
            }},
            dismissButton = { TextButton(onClick = { openDialog.value = false }) {
               Text("Cancel")
            }},
            icon = { Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete") },
            title = { Text("Delete Key") },
            text = { Text("Are you sure you want to delete this key? " +
                    "Deleting this entry will also delete the key used for end-to-end encryption.")
            },
        )
    }
}

@Composable
fun KeysList(
    modifier: Modifier = Modifier,
    aliases: Map<String, ContactDetails?>,
    listState: LazyListState = rememberLazyListState(),
    checkSecretKey: (String) -> Boolean = { _: String -> false },
    onViewQrCode: (String) -> Unit = {},
    onDeleteKey: (String) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier,
        state = listState
    ) {
        items(
            count = aliases.size,
            key = { aliases.keys.toList()[it] }
        ) {
            val alias = remember { aliases.keys.toList()[it] }

            val hasSecretKey = checkSecretKey(alias)

            KeyEntry(
                modifier = Modifier
                    .fillMaxWidth(),
                keyAlias = alias,
                hasSecretKey = hasSecretKey,
                associatedContact = aliases.getValue(alias),
                onViewQrCode = { onViewQrCode(it) },
                onDeleteKey = { onDeleteKey(it) },
            )
        }
    }
}


@Composable
fun KeyEntry(
    modifier: Modifier = Modifier,
    keyAlias: String = "",
    hasSecretKey: Boolean = false,
    associatedContact: ContactDetails? = null,
    onViewQrCode: (String) -> Unit = {},
    onDeleteKey: (String) -> Unit = {}
) {
    Card(
        modifier = modifier.padding(1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(
                    vertical = 10.dp,
                    horizontal = 12.dp
                )
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                val availability = if (hasSecretKey) "Available" else "None"

                var text = keyAlias
                if (associatedContact?.displayName != null) {
                    text = "${associatedContact.displayName} ($keyAlias)"
                }

                Text(
                    fontWeight = FontWeight.Bold,
                    text = text
                )
                Text(
                    fontWeight = FontWeight.Light,
                    fontSize = 12.sp,
                    text = "E2E Encryption: $availability")
            }

            Row {
                val qrIcon = ImageVector
                    .vectorResource(R.drawable.qr_code_24dp_fill0_wght400_grad0_opsz24)
                IconButton(onClick = { onViewQrCode(keyAlias) }) {
                    Icon(imageVector = qrIcon, contentDescription = "View QR")
                }
                IconButton(onClick = { onDeleteKey(keyAlias) }) {
                    Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete Keys")
                }
            }
        }
    }
}

@Preview(apiLevel = 33)
@Composable
fun ListPreview() {
    val aliases = mapOf(
        "+639151231234" to null,
        "+639993210998" to null,
        "+639984388234" to null,
        "+639998766872" to null,
        "+639158898361" to null,
    )

    KeysList(modifier = Modifier.fillMaxSize(), aliases = aliases)
}