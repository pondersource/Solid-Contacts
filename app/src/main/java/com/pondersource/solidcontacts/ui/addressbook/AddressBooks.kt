package com.pondersource.solidcontacts.ui.addressbook

import android.widget.Space
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.pondersource.shared.data.datamodule.contact.AddressBook
import com.pondersource.solidcontacts.R
import com.pondersource.solidcontacts.ui.element.AddressBookItem
import com.pondersource.solidcontacts.ui.element.LoadingItem
import com.pondersource.solidcontacts.ui.nav.AddressBookRoute
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressBooks(
    navController: NavController,
    innerNavController: NavController,
    viewModel: AddressBooksViewModel
) {

    val scope = rememberCoroutineScope()
    val tabs = remember {
        listOf(
            Pair("Private", R.drawable.ic_lock),
            Pair("Public", R.drawable.ic_public),
        )
    }
    var pagerState = rememberPagerState { 2 }

    val addAddressBookDialogState = remember { mutableStateOf(false) }
    val dismissAddAddressBookDialog = {
        addAddressBookDialogState.value = false
    }

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    Scaffold (
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Your Address Books") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    addAddressBookDialogState.value = true
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = null,
                )
            }
        }
    ){ paddings ->

        if (viewModel.addressBooksLoadingState.value) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddings),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoadingItem("Loading address books...")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddings),
            ) {

                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth(),
                    divider = {
                        HorizontalDivider(
                            thickness = 1.dp,
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                        ) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(
                                    painter = painterResource(tab.second),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(8.dp, 4.dp)
                                        .size(24.dp),
                                )
                                Text(
                                    text = tab.first,
                                    modifier = Modifier
                                        .padding(8.dp, 4.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxSize()
                ) { index ->
                    when (index) {
                        0 -> {
                            AddressBookList(
                                addressBooks = viewModel.privateAddressBooks.value,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                innerNavController.navigate(AddressBookRoute(it.uri, it.title))
                            }
                        }

                        1 -> {
                            AddressBookList(
                                addressBooks = viewModel.publicAddressBooks.value,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                innerNavController.navigate(AddressBookRoute(it.uri, it.title))
                            }
                        }
                    }
                }
            }
        }

        when {
            addAddressBookDialogState.value -> {
                Dialog(
                    onDismissRequest = dismissAddAddressBookDialog
                ) {
                    Card(
                        modifier = Modifier
                            .widthIn(min = 200.dp)
                            .padding(16.dp)
                    ) {
                        Text(
                            "Create new Address Book",
                            modifier = Modifier.padding(12.dp)
                        )
                        OutlinedTextField(
                            value = viewModel.newAddressBookTitle.value,
                            onValueChange = {
                                viewModel.newAddressBookTitle.value = it
                            },
                            modifier = Modifier.padding(12.dp),
                            label = {
                                Text("Title")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        )

                        Row(
                            Modifier.padding(12.dp, 4.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                        ){
                            Checkbox(
                                checked = viewModel.newAddressBookPrivate.value,
                                onCheckedChange = {
                                    viewModel.newAddressBookPrivate.value = it
                                }
                            )
                            Text("Private")
                        }

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = {
                                    viewModel.createNewAddressBook()
                                    addAddressBookDialogState.value = false
                                }
                            ) {
                                Text("Create")
                            }
                        }
                    }
                }
            }
        }
    }

    BackHandler() {
        navController.popBackStack()
    }
}

@Composable
private fun AddressBookList(
    addressBooks: List<AddressBook>?,
    modifier: Modifier = Modifier,
    onClick: (AddressBook) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (addressBooks == null || addressBooks.isEmpty()) {
            item() {
                Text("You don't have any address book.")
            }
        } else {
            items(addressBooks.size) {
                AddressBookItem(addressBooks[it]) { it ->
                    onClick(it)
                }
            }
        }
    }
}