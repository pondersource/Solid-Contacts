package com.pondersource.solidcontacts.ui.addressbook

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import com.pondersource.solidcontacts.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pondersource.shared.data.datamodule.contact.Contact
import com.pondersource.shared.data.datamodule.contact.Group
import com.pondersource.solidcontacts.ui.element.ContactItem
import com.pondersource.solidcontacts.ui.element.GroupItem
import com.pondersource.solidcontacts.ui.element.LoadingItem
import com.pondersource.solidcontacts.ui.nav.ContactRoute
import com.pondersource.solidcontacts.ui.nav.GroupRoute
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressBook(
    innerNavController: NavController,
    viewModel: AddressBookViewModel
) {

    val scope = rememberCoroutineScope()
    val tabs = remember {
        listOf(
            Pair("Contacts", R.drawable.ic_contact),
            Pair("Groups", R.drawable.ic_group),
        )
    }
    var pagerState = rememberPagerState { 2 }

    Scaffold (
        modifier = Modifier
            .fillMaxWidth(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(viewModel.addressBookRoute.name) }
            )
        },
    ){ paddings ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddings),
        ) {

            if (viewModel.loadingAddressBookDetails.value) {
                LoadingItem("Loading address book details...")
            } else {
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
                                        .padding(8.dp)
                                        .size(24.dp),
                                )
                                Text(
                                    text = tab.first,
                                    modifier = Modifier
                                        .padding(top = 8.dp)
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
                            ContactList(viewModel.addressBookDetails.value!!.contacts) {
                                innerNavController.navigate(ContactRoute(it.uri))
                            }
                        }

                        1 -> {
                            GroupList(viewModel.addressBookDetails.value!!.groups) {
                                innerNavController.navigate(GroupRoute(it.uri))
                            }
                        }
                    }
                }
            }
        }
    }

    BackHandler() {
        innerNavController.popBackStack()
    }
}

@Composable
fun ContactList(
    contacts: List<Contact>,
    onClick: (Contact) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        items(contacts) {
            ContactItem(it, onClick)
        }
    }
}

@Composable
fun GroupList(
    groups: List<Group>,
    onClick: (Group) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        items(groups) {
            GroupItem(it, onClick)
        }
    }
}