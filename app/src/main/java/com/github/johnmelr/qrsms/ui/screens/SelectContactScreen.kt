package com.github.johnmelr.qrsms.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.johnmelr.qrsms.R
import com.github.johnmelr.qrsms.data.contacts.ContactDetails
import com.github.johnmelr.qrsms.ui.components.SearchBar
import com.github.johnmelr.qrsms.ui.model.SelectContactViewModel

@Composable
fun SelectContactScreen(
    modifier: Modifier = Modifier,
    viewModel: SelectContactViewModel = hiltViewModel(),
    onSelectContact: (ContactDetails) -> Unit = {},
) {
    val listState = rememberLazyListState()
    val selectContactState by viewModel.selectContactState.collectAsState()

    val contactList = selectContactState.contactList
    val searchInput: String = viewModel.searchInput.value

    fun String.isNumeric(): Boolean {
        val numRegex = "[0-9]+".toRegex()
        return this.matches(numRegex)
    }

    Column (
        modifier = modifier.fillMaxSize()
    ){
        SearchBar(
            input = searchInput,
            onQueryChange = {
                viewModel.updateSearchInput(it)
            },
            placeholder = { Text("Type names or phone number")}
        )
        if ((searchInput.isNotBlank() || searchInput.isNotEmpty())
            && searchInput.isNumeric()) {
            // Create an Empty Contact Details
            val emptyContact = ContactDetails(
                id = "",
                displayName = "Send to $searchInput",
                photoThumbUriString = null,
                phoneNumber = null,
                normalizedPhoneNumber = searchInput,
            )
            ContactCard(contact = emptyContact, onSelectContact = onSelectContact)
        }


        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                count = contactList.size,
                key = { contactList[it].id },
                itemContent = {
                    val contact = contactList[it]

                    ContactCard(
                        contact = contact,
                        onSelectContact = onSelectContact,
                    )
                }
            )
        }
    }
}

@Composable
fun ContactCard(
    modifier: Modifier = Modifier,
    contact: ContactDetails,
    onSelectContact: (ContactDetails) -> Unit
) {
    Row (
        modifier = modifier
            .height(64.dp)
            .fillMaxWidth()
            .clickable {
                onSelectContact(contact)
            },
        verticalAlignment = Alignment.CenterVertically,
    ){
        // Image
        val circle: Shape = CircleShape

        Image(painter = painterResource(
            R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier
                .padding(12.dp)
                .background(
                    colorResource(R.color.teal_200),
                    circle
                )
                .height(48.dp)
        )

        Column {
            Text(
                fontWeight = FontWeight.Normal,
                text = contact.displayName ?: contact.normalizedPhoneNumber
            )
            Text(
                fontWeight = FontWeight.ExtraLight,
                text = contact.phoneNumber ?: contact.normalizedPhoneNumber
            )
        }
    }
}

@Preview
@Composable
fun ContactCardPreview() {
    val contact = ContactDetails(
        id = "0",
        displayName = "Juan Dela Cruz",
        photoThumbUriString = null,
        phoneNumber = "09991234567",
        normalizedPhoneNumber = "+639991234567"
    )

    ContactCard(
        contact = contact,
        onSelectContact = {},
    )
}