package com.pondersource.solidcontacts.ui.addressbook

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import com.pondersource.solidcontacts.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pondersource.shared.data.datamodule.contact.Contact
import com.pondersource.shared.data.datamodule.contact.Group
import com.pondersource.solidcontacts.ui.element.ContactItem
import com.pondersource.solidcontacts.ui.element.GroupItem
import com.pondersource.solidcontacts.ui.element.LoadingItem
import com.pondersource.solidcontacts.ui.nav.AddContactRoute
import com.pondersource.solidcontacts.ui.nav.AddGroupRoute
import com.pondersource.solidcontacts.ui.nav.ContactRoute
import com.pondersource.solidcontacts.ui.nav.GroupRoute
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressBook(
    navController: NavController,
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

    val fabItems = arrayListOf<AddItem>(
        AddItem(R.drawable.ic_contact, "Add Contact"),
        AddItem(R.drawable.ic_add_group, "Add Group"),
    )

    LaunchedEffect(Unit) {
        viewModel.loadData()
    }

    LaunchedEffect(viewModel.deleteAddressBookResult.value) {
        if (viewModel.deleteAddressBookResult.value) {
            innerNavController.popBackStack()
        }
    }

    if (viewModel.loadingAddressBookDetails.value || viewModel.deleteLoading.value) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LoadingItem("Loading address book details...")
        }
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxWidth(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(viewModel.addressBookRoute.name) },
                    navigationIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier
                                .clickable { innerNavController.popBackStack() }
                                .padding(12.dp)
                        )
                    },
                    actions = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier
                                .clickable {
                                    viewModel.deleteAddressBook()
                                }
                                .padding(12.dp),
                            tint = Color.Red
                        )
                    }
                )
            },
            floatingActionButton = {
                if (viewModel.addressBookDetails.value != null) {
                    AddContactAndGroupFab(
                        fabItems,
                    ) {
                        if (it == fabItems[0]) {
                            navController.navigate(AddContactRoute(viewModel.addressBookDetails.value!!.uri))

                        } else if (it == fabItems[1]) {
                            navController.navigate(AddGroupRoute(viewModel.addressBookDetails.value!!.uri))
                        }
                    }
                }
            }
        ) { paddings ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                            ContactList(
                                viewModel.addressBookDetails.value!!.contacts,
                                "You don't have any contact in this address book."
                            ) {
                                innerNavController.navigate(ContactRoute(viewModel.addressBookRoute.addressBookUri, it.uri))
                            }
                        }

                        1 -> {
                            GroupList(
                                viewModel.addressBookDetails.value!!.groups,
                            ) {
                                innerNavController.navigate(
                                    GroupRoute(
                                        viewModel.addressBookDetails.value!!.uri,
                                        it.uri
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    BackHandler() {
        if (!viewModel.deleteLoading.value) {
            innerNavController.popBackStack()
        }
    }
}

@Composable
fun ContactList(
    contacts: List<Contact>,
    emptyText: String,
    onClick: (Contact) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ){
        if (contacts.isEmpty()) {
            item() {
                Text(emptyText)
            }
        } else {
            items(contacts) {
                ContactItem(it, onClick = onClick)
            }
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
        if (groups.isEmpty()) {
            item() {
                Text("You don't have any group in this address book.")
            }
        } else {
            items(groups) {
                GroupItem(it, onClick = onClick)
            }
        }

    }
}

@Composable
private fun AddContactAndGroupFab(
    items: List<AddItem>,
    onItemClick: (AddItem) -> Unit,
) {

    var filterFabState = remember { mutableStateOf(false) }

    val transitionState = remember {
        MutableTransitionState(filterFabState).apply {
            targetState.value = false
        }
    }

    val transition = updateTransition(targetState = transitionState, label = "transition")

    val iconRotationDegree = transition.animateFloat({
        tween(durationMillis = 150, easing = FastOutSlowInEasing)
    }, label = "rotation") {
        if (it.currentState.value == true) 45f else 0f
    }

    Column(
        modifier = Modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp,Alignment.Bottom)
    ) {
        AddFabMenu(
            items = items,
            visible = filterFabState.value,
            onItemClick = onItemClick,
        )
        FloatingActionButton(
            onClick = {
                filterFabState.value = !filterFabState.value
            },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                modifier = Modifier
                    .rotate(iconRotationDegree.value)
            )
        }
    }
}

@Composable
private fun AddFabMenu(
    items: List<AddItem>,
    visible: Boolean,
    onItemClick: (AddItem) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items.forEach { item ->
                AddFabMenuItem(
                    menuItem = item,
                    onItemClick = onItemClick
                )
            }
        }
    }
}

@Composable
private fun AddFabMenuItem(
    menuItem: AddItem,
    onItemClick: (AddItem) -> Unit,
    modifier: Modifier = Modifier
) {

    Row (
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        AddFabMenuLabel(label = menuItem.label)
        AddFabMenuButton(item = menuItem, onClick = onItemClick)

    }
}

@Composable
fun AddFabMenuLabel(
    label: String,
) {
    Surface(
        modifier = Modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color.Black.copy(alpha = 0.8f)
    ) {
        Text(
            text = label, color = Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun AddFabMenuButton(
    item: AddItem,
    onClick: (AddItem) -> Unit,
    modifier: Modifier = Modifier
) {

    SmallFloatingActionButton(
        modifier = modifier,
        onClick = {
            onClick(item)
        },
    ) {
        Icon(
            painter = painterResource(item.icon),
            contentDescription = null,
        )
    }
}


private data class AddItem(
    @DrawableRes
    val icon: Int,
    val label: String,
)
