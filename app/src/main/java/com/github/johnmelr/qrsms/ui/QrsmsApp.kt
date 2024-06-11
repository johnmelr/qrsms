package com.github.johnmelr.qrsms.ui

import android.telephony.PhoneNumberUtils
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.johnmelr.qrsms.data.messages.QrsmsMessage
import com.github.johnmelr.qrsms.ui.model.GenerateQrViewModel
import com.github.johnmelr.qrsms.ui.model.InboxViewModel
import com.github.johnmelr.qrsms.ui.model.QrsmsAppViewModel
import com.github.johnmelr.qrsms.ui.model.SelectContactViewModel
import com.github.johnmelr.qrsms.ui.screens.ConversationScreen
import com.github.johnmelr.qrsms.ui.screens.GenerateQrScreen
import com.github.johnmelr.qrsms.ui.screens.InboxScreen
import com.github.johnmelr.qrsms.ui.screens.ScanQrScreen
import com.github.johnmelr.qrsms.ui.screens.SelectContactScreen

enum class QrsmsAppScreens(val title: String) {
    Inbox(title = "QRSMS"),
    Conversation(title = "Conversation"),
    GenerateQR(title = "Generating QR Code"),
    GenerateForContact(title = "Generate QR Code"),
    NewConversation(title = "New Conversation"),
    ScanQR(title = "Scan QR Code"),
}

@Composable
fun QrsmsApp(
    modifier: Modifier = Modifier,
    qrsmsAppViewModel: QrsmsAppViewModel,
    selectContactViewModel: SelectContactViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
){
    // The defaultPhoneNumber is needed for generating the qr code as it will be encoded in the
    // qr code.
    val defaultPhoneNumber = qrsmsAppViewModel.phoneNumberFlow.collectAsState("loading...").value

    val qrsmsAppUiState by qrsmsAppViewModel.qrsmsAppUiState.collectAsStateWithLifecycle()

    val hasSmsReadPermission = qrsmsAppUiState.hasSmsReadPermission
    val hasSmsSendPermission = qrsmsAppUiState.hasSmsSendPermission
    val hasContactReadPermission = qrsmsAppUiState.hasContactReadPermission

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = QrsmsAppScreens
        .valueOf(backStackEntry?.destination?.route ?: QrsmsAppScreens.Inbox.name)

    val topBarTitle = remember { mutableStateOf(currentScreen.title) }

    Scaffold (
        topBar = {
            QrsmsAppBar(
                topBarTitle = topBarTitle.value,
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() },
            )
        }
    ) { innerPadding ->
        if (!hasSmsReadPermission && !hasSmsSendPermission && !hasContactReadPermission) {
            return@Scaffold
        }

        val inboxViewModel: InboxViewModel = viewModel()

        NavHost(
            navController = navController,
            startDestination = QrsmsAppScreens.Inbox.name,
            modifier = modifier
                .padding(innerPadding)
        ) {
            composable(route = QrsmsAppScreens.Inbox.name) {
                val inboxUiState by inboxViewModel.qrsmsInboxUiState.collectAsStateWithLifecycle()
                val messageList: List<QrsmsMessage> = inboxViewModel.messageList.collectAsStateWithLifecycle().value

                InboxScreen(
                    messageList = messageList,
                    onNavigateToConversations = {
                        navController.navigate(it)
                    },
                    onSelectGenerateQr = { navController.navigate(it) },
                    onSelectScanQr = { navController.navigate(it) },
                    onSelectMessage = { threadId: String, address: String ->
                        inboxViewModel.setSelectedConversation(threadId, address)
                    },
                    onSelectNewMessage = {
                        navController.navigate(it)
                    }
                )
            }
            composable(route = QrsmsAppScreens.Conversation.name) {
                ConversationScreen(
                    threadIdOfConversation = inboxViewModel.selectedThread,
                    addressOfConversation = inboxViewModel.selectedAddress,
                    modifier = modifier,
                    onConversationLoad = { contact: String -> topBarTitle.value = contact },    // Modifies the top bar title to reflect the current contact selected
                )
            }
            composable(route = QrsmsAppScreens.GenerateForContact.name) {
                SelectContactScreen(
                    viewModel = selectContactViewModel,
                    onSelectContact = {
                        selectContactViewModel.setSelectedContactDetails(it)
                        navController.navigate(QrsmsAppScreens.GenerateQR.name)
                    },
                )
            }
            composable(route = QrsmsAppScreens.NewConversation.name) {
                SelectContactScreen(
                    viewModel = selectContactViewModel,
                    onSelectContact = { contactDetails ->
                        selectContactViewModel.setSelectedContactDetails(contactDetails)
                        inboxViewModel.setSelectedConversation("", contactDetails.normalizedPhoneNumber)
                        navController.navigate(QrsmsAppScreens.Conversation.name)
                    },
                )
            }
            composable(route = QrsmsAppScreens.GenerateQR.name) {
                val selectContactState by selectContactViewModel
                    .selectContactState.collectAsStateWithLifecycle()

                if (selectContactState.selectedContact == null) {
                    navController.popBackStack()
                    return@composable
                }

                val selectedContact = remember { selectContactState.selectedContact!! }
                val normalizedNumber = PhoneNumberUtils
                    .formatNumberToE164(defaultPhoneNumber, "PH")

                val generateQrViewModel: GenerateQrViewModel = hiltViewModel(
                    creationCallback = { factory:
                         GenerateQrViewModel.GenerateQrViewModelFactory ->
                            factory.create(selectedContact, normalizedNumber)
                    }
                )

                val hasExistingKey by generateQrViewModel.hasExistingKey.collectAsStateWithLifecycle()
                val publicKeyString by generateQrViewModel.publicKeyString.collectAsStateWithLifecycle()

                val imageBitmap: ImageBitmap? =  if (publicKeyString.isEmpty()) null else
                    generateQrViewModel.generateQrCode(
                    "$normalizedNumber:$publicKeyString"
                )

                GenerateQrScreen(
                    selectedContact = selectedContact,
                    hasExistingKey = hasExistingKey,
                    generateKeyPair = { generateQrViewModel.generate(selectedContact , defaultPhoneNumber)},
                    imageBitmap = imageBitmap
                )
            }
            composable(route = QrsmsAppScreens.ScanQR.name) {
                ScanQrScreen(
                    navController = navController,
                    onScanSuccess = {
                        navController.popBackStack(QrsmsAppScreens.Inbox.name, false)
                    },
                    defaultPhoneNumber = defaultPhoneNumber
                )
            }
        }
    }
}

@Composable
fun QrsmsAppBar(
    topBarTitle: String,
    currentScreen: QrsmsAppScreens,
    modifier: Modifier = Modifier,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
) {
    TopAppBar(
        title = { Text( text =
            if (currentScreen.title != "Conversation")  currentScreen.title
            else topBarTitle,
            fontSize = 16.sp
        ) },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back Button"
                    )
                }
            }
        }
    )
}