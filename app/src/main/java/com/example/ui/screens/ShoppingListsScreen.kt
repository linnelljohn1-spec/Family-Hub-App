package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.FamilyMember
import com.example.data.Shop
import com.example.data.ShoppingItem
import com.example.data.ShoppingList
import com.example.ui.ShopWithItems
import com.example.ui.ShoppingListWithShops
import com.example.ui.ShoppingListsViewModel
import com.example.ui.theme.FeatureColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListsScreen(
    shoppingListsViewModel: ShoppingListsViewModel,
    members: List<FamilyMember>,
    activeMemberId: Int,
    isActiveAdmin: Boolean,
    onNavigateBack: () -> Unit
) {
    val listsWithShops by shoppingListsViewModel.shoppingListsWithShops.collectAsStateWithLifecycle()

    var showAddListDialog by remember { mutableStateOf(false) }
    var listPendingDelete by remember { mutableStateOf<ShoppingList?>(null) }
    var listPendingRename by remember { mutableStateOf<ShoppingList?>(null) }
    var shopPendingDelete by remember { mutableStateOf<Pair<ShoppingList, Shop>?>(null) }
    var addShopForList by remember { mutableStateOf<ShoppingList?>(null) }
    var addItemForShop by remember { mutableStateOf<Pair<ShoppingList, Shop>?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Shopping Lists",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("shopping_lists_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Family Hub"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddListDialog = true },
                        modifier = Modifier.testTag("shopping_lists_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Create New Shopping List",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddListDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New List") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .testTag("shopping_lists_fab")
                    .padding(bottom = 16.dp)
            )
        }
    ) { paddingValues ->
        if (listsWithShops.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(FeatureColors.shoppingListsContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = FeatureColors.shoppingLists,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Text(
                        text = "No Shopping Lists Yet",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Create a list, add the shops you need to visit, then add items under each shop. Tap 'New List' to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                items(
                    items = listsWithShops,
                    key = { it.list.id }
                ) { listWithShops ->
                    val canModify = shoppingListsViewModel.canModifyList(listWithShops.list)

                    ShoppingListCard(
                        listWithShops = listWithShops,
                        canModify = canModify,
                        onRename = { listPendingRename = listWithShops.list },
                        onDelete = { listPendingDelete = listWithShops.list },
                        onAddShop = { addShopForList = listWithShops.list },
                        onDeleteShop = { shop -> shopPendingDelete = listWithShops.list to shop },
                        onAddItem = { shop -> addItemForShop = listWithShops.list to shop },
                        onToggleItemChecked = { shop, item ->
                            shoppingListsViewModel.toggleItemChecked(listWithShops.list, shop, item)
                        },
                        onDeleteItem = { shop, item ->
                            shoppingListsViewModel.deleteItem(listWithShops.list, shop, item)
                        }
                    )
                }
            }
        }
    }

    if (showAddListDialog) {
        SingleTextFieldDialog(
            title = "Create a Shopping List",
            label = "List name",
            placeholder = "e.g. Weekly groceries",
            confirmLabel = "Create",
            testTagPrefix = "shopping_list",
            onDismiss = { showAddListDialog = false },
            onConfirm = { name ->
                shoppingListsViewModel.createShoppingList(name)
                showAddListDialog = false
            }
        )
    }

    listPendingRename?.let { list ->
        SingleTextFieldDialog(
            title = "Rename Shopping List",
            label = "List name",
            initialValue = list.name,
            confirmLabel = "Save",
            testTagPrefix = "rename_shopping_list",
            onDismiss = { listPendingRename = null },
            onConfirm = { newName ->
                shoppingListsViewModel.renameShoppingList(list, newName)
                listPendingRename = null
            }
        )
    }

    addShopForList?.let { list ->
        SingleTextFieldDialog(
            title = "Add a Shop",
            label = "Shop name",
            placeholder = "e.g. Tesco",
            confirmLabel = "Add",
            testTagPrefix = "shop",
            onDismiss = { addShopForList = null },
            onConfirm = { name ->
                shoppingListsViewModel.addShop(list, name)
                addShopForList = null
            }
        )
    }

    addItemForShop?.let { (list, shop) ->
        SingleTextFieldDialog(
            title = "Add an Item",
            label = "Item name",
            placeholder = "e.g. Milk",
            confirmLabel = "Add",
            testTagPrefix = "item",
            onDismiss = { addItemForShop = null },
            onConfirm = { name ->
                shoppingListsViewModel.addItem(list, shop, name)
                addItemForShop = null
            }
        )
    }

    listPendingDelete?.let { list ->
        ConfirmDeleteDialog(
            title = "Delete Shopping List?",
            message = "This will delete \"${list.name}\" and every shop and item under it. This can't be undone.",
            testTagPrefix = "delete_shopping_list",
            onDismiss = { listPendingDelete = null },
            onConfirm = {
                shoppingListsViewModel.deleteShoppingList(list)
                listPendingDelete = null
            }
        )
    }

    shopPendingDelete?.let { (list, shop) ->
        ConfirmDeleteDialog(
            title = "Delete Shop?",
            message = "This will delete \"${shop.name}\" and every item under it. This can't be undone.",
            testTagPrefix = "delete_shop",
            onDismiss = { shopPendingDelete = null },
            onConfirm = {
                shoppingListsViewModel.deleteShop(list, shop)
                shopPendingDelete = null
            }
        )
    }
}

@Composable
fun ShoppingListCard(
    listWithShops: ShoppingListWithShops,
    canModify: Boolean,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddShop: () -> Unit,
    onDeleteShop: (Shop) -> Unit,
    onAddItem: (Shop) -> Unit,
    onToggleItemChecked: (Shop, ShoppingItem) -> Unit,
    onDeleteItem: (Shop, ShoppingItem) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    var showMenu by remember { mutableStateOf(false) }

    val list = listWithShops.list
    val allItems = listWithShops.shopsWithItems.flatMap { it.items }
    val checkedCount = allItems.count { it.isChecked }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("shopping_list_card_${list.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = list.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${listWithShops.shopsWithItems.size} shop${if (listWithShops.shopsWithItems.size == 1) "" else "s"} • $checkedCount/${allItems.size} checked",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (canModify) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.testTag("list_menu_button_${list.id}")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "List options")
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                onClick = { showMenu = false; onRename() },
                                modifier = Modifier.testTag("list_rename_item_${list.id}")
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = { showMenu = false; onDelete() },
                                modifier = Modifier.testTag("list_delete_item_${list.id}")
                            )
                        }
                    }
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listWithShops.shopsWithItems.forEach { shopWithItems ->
                        ShopCard(
                            shopWithItems = shopWithItems,
                            onDelete = { onDeleteShop(shopWithItems.shop) },
                            onAddItem = { onAddItem(shopWithItems.shop) },
                            onToggleItemChecked = { item -> onToggleItemChecked(shopWithItems.shop, item) },
                            onDeleteItem = { item -> onDeleteItem(shopWithItems.shop, item) }
                        )
                    }
                    TextButton(
                        onClick = onAddShop,
                        modifier = Modifier.testTag("add_shop_button_${list.id}")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add shop")
                    }
                }
            }
        }
    }
}

@Composable
fun ShopCard(
    shopWithItems: ShopWithItems,
    onDelete: () -> Unit,
    onAddItem: () -> Unit,
    onToggleItemChecked: (ShoppingItem) -> Unit,
    onDeleteItem: (ShoppingItem) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }
    val shop = shopWithItems.shop
    val checkedCount = shopWithItems.items.count { it.isChecked }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("shop_card_${shop.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shop.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$checkedCount/${shopWithItems.items.size} checked",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("shop_delete_button_${shop.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Shop",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            if (expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    shopWithItems.items.forEach { item ->
                        ItemRow(
                            item = item,
                            onToggleChecked = { onToggleItemChecked(item) },
                            onDelete = { onDeleteItem(item) }
                        )
                    }
                    TextButton(
                        onClick = onAddItem,
                        modifier = Modifier.testTag("add_item_button_${shop.id}")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add item", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun ItemRow(
    item: ShoppingItem,
    onToggleChecked: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("item_row_${item.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = { onToggleChecked() },
            modifier = Modifier.testTag("item_checkbox_${item.id}")
        )
        Text(
            text = item.name,
            style = MaterialTheme.typography.bodyMedium,
            textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None,
            color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        IconButton(
            onClick = onDelete,
            modifier = Modifier.testTag("item_delete_button_${item.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete Item",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun SingleTextFieldDialog(
    title: String,
    label: String,
    testTagPrefix: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    placeholder: String? = null,
    initialValue: String = "",
    confirmLabel: String = "Save"
) {
    var value by remember { mutableStateOf(initialValue) }
    val canConfirm = value.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("${testTagPrefix}_dialog"),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(label) },
                    placeholder = placeholder?.let { { Text(it) } },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${testTagPrefix}_input_name")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("${testTagPrefix}_dialog_cancel")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { if (canConfirm) onConfirm(value.trim()) },
                        enabled = canConfirm,
                        modifier = Modifier.testTag("${testTagPrefix}_dialog_confirm"),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text(confirmLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    title: String,
    message: String,
    testTagPrefix: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .testTag("${testTagPrefix}_dialog"),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("${testTagPrefix}_dialog_cancel")
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("${testTagPrefix}_dialog_confirm"),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}
