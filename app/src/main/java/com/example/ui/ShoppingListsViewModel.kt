package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Shop
import com.example.data.ShoppingItem
import com.example.data.ShoppingList
import com.example.data.ShoppingListRepository
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShopWithItems(
    val shop: Shop,
    val items: List<ShoppingItem>
)

data class ShoppingListWithShops(
    val list: ShoppingList,
    val shopsWithItems: List<ShopWithItems>
)

class ShoppingListsViewModel(application: Application) : AndroidViewModel(application) {
    private val shoppingListRepository = ShoppingListRepository(AppDatabase.getDatabase(application).shoppingListDao())
    private val firestore = FirebaseFirestore.getInstance()

    private val _syncGroupCode = MutableStateFlow("")
    private val _activeMemberId = MutableStateFlow(-1)
    private val _isActiveAdmin = MutableStateFlow(false)

    private var listsListener: ListenerRegistration? = null
    private val shopListeners = mutableMapOf<String, ListenerRegistration>() // key: list firestoreId
    private val itemListeners = mutableMapOf<String, ListenerRegistration>() // key: shop firestoreId
    private val shopFirestoreIdsByListFirestoreId = mutableMapOf<String, MutableList<String>>()

    val shoppingLists: StateFlow<List<ShoppingList>> = shoppingListRepository.allShoppingLists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shops: StateFlow<List<Shop>> = shoppingListRepository.allShops
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shoppingItems: StateFlow<List<ShoppingItem>> = shoppingListRepository.allShoppingItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shoppingListsWithShops: StateFlow<List<ShoppingListWithShops>> = combine(
        shoppingLists, shops, shoppingItems
    ) { lists, shopList, items ->
        lists.map { list ->
            val shopsForList = shopList.filter { it.listId == list.id }
            ShoppingListWithShops(
                list = list,
                shopsWithItems = shopsForList.map { shop ->
                    ShopWithItems(shop = shop, items = items.filter { it.shopId == shop.id })
                }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSyncGroupCode(code: String) {
        if (_syncGroupCode.value == code) return
        _syncGroupCode.value = code
        registerListeners(code)
    }

    fun setActiveMember(memberId: Int, name: String, isAdmin: Boolean) {
        _activeMemberId.value = memberId
        _isActiveAdmin.value = isAdmin
    }

    fun canModifyList(list: ShoppingList): Boolean =
        _isActiveAdmin.value || _activeMemberId.value == list.createdByMemberId

    fun createShoppingList(name: String) {
        val code = _syncGroupCode.value
        val memberId = _activeMemberId.value
        val cleanName = name.trim()
        if (code.isBlank() || memberId == -1 || cleanName.isBlank()) return

        val data = hashMapOf(
            "name" to cleanName,
            "createdByMemberId" to memberId,
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("families")
            .document(code)
            .collection("shoppingLists")
            .add(data)
    }

    fun renameShoppingList(list: ShoppingList, newName: String) {
        if (!canModifyList(list)) return
        val code = _syncGroupCode.value
        val firestoreId = list.firestoreId ?: return
        val cleanName = newName.trim()
        if (code.isBlank() || cleanName.isBlank()) return

        firestore.collection("families")
            .document(code)
            .collection("shoppingLists")
            .document(firestoreId)
            .update("name", cleanName)
    }

    fun deleteShoppingList(list: ShoppingList) {
        if (!canModifyList(list)) return
        val code = _syncGroupCode.value
        val firestoreId = list.firestoreId ?: return
        if (code.isBlank()) return

        val listDoc = firestore.collection("families")
            .document(code)
            .collection("shoppingLists")
            .document(firestoreId)

        listDoc.collection("shops").get().addOnSuccessListener { shopsSnapshot ->
            val shopDocs = shopsSnapshot.documents
            if (shopDocs.isEmpty()) {
                listDoc.delete()
                return@addOnSuccessListener
            }
            var remaining = shopDocs.size
            shopDocs.forEach { shopDoc ->
                shopDoc.reference.collection("items").get().addOnSuccessListener { itemsSnapshot ->
                    val batch = firestore.batch()
                    for (itemDoc in itemsSnapshot.documents) {
                        batch.delete(itemDoc.reference)
                    }
                    batch.delete(shopDoc.reference)
                    batch.commit().addOnCompleteListener {
                        remaining--
                        if (remaining == 0) {
                            listDoc.delete()
                        }
                    }
                }
            }
        }
    }

    fun addShop(list: ShoppingList, name: String) {
        val code = _syncGroupCode.value
        val listFirestoreId = list.firestoreId ?: return
        val cleanName = name.trim()
        if (code.isBlank() || cleanName.isBlank()) return

        val data = hashMapOf(
            "name" to cleanName,
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("families")
            .document(code)
            .collection("shoppingLists")
            .document(listFirestoreId)
            .collection("shops")
            .add(data)
    }

    fun deleteShop(list: ShoppingList, shop: Shop) {
        val code = _syncGroupCode.value
        val listFirestoreId = list.firestoreId ?: return
        val shopFirestoreId = shop.firestoreId ?: return
        if (code.isBlank()) return

        val shopDoc = firestore.collection("families")
            .document(code)
            .collection("shoppingLists")
            .document(listFirestoreId)
            .collection("shops")
            .document(shopFirestoreId)

        shopDoc.collection("items").get().addOnSuccessListener { itemsSnapshot ->
            val batch = firestore.batch()
            for (itemDoc in itemsSnapshot.documents) {
                batch.delete(itemDoc.reference)
            }
            batch.delete(shopDoc)
            batch.commit()
        }
    }

    fun addItem(list: ShoppingList, shop: Shop, name: String, quantity: Int = 1) {
        val code = _syncGroupCode.value
        val listFirestoreId = list.firestoreId ?: return
        val shopFirestoreId = shop.firestoreId ?: return
        val cleanName = name.trim()
        val cleanQuantity = quantity.coerceAtLeast(1)
        if (code.isBlank() || cleanName.isBlank()) return

        val data = hashMapOf(
            "name" to cleanName,
            "isChecked" to false,
            "quantity" to cleanQuantity,
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("families")
            .document(code)
            .collection("shoppingLists")
            .document(listFirestoreId)
            .collection("shops")
            .document(shopFirestoreId)
            .collection("items")
            .add(data)
    }

    fun updateItemQuantity(list: ShoppingList, shop: Shop, item: ShoppingItem, newQuantity: Int) {
        val code = _syncGroupCode.value
        val listFirestoreId = list.firestoreId ?: return
        val shopFirestoreId = shop.firestoreId ?: return
        val itemFirestoreId = item.firestoreId ?: return
        val cleanQuantity = newQuantity.coerceAtLeast(1)
        if (code.isBlank()) return

        firestore.collection("families")
            .document(code)
            .collection("shoppingLists")
            .document(listFirestoreId)
            .collection("shops")
            .document(shopFirestoreId)
            .collection("items")
            .document(itemFirestoreId)
            .update("quantity", cleanQuantity)
    }

    fun toggleItemChecked(list: ShoppingList, shop: Shop, item: ShoppingItem) {
        val code = _syncGroupCode.value
        val listFirestoreId = list.firestoreId ?: return
        val shopFirestoreId = shop.firestoreId ?: return
        val itemFirestoreId = item.firestoreId ?: return
        if (code.isBlank()) return

        firestore.collection("families")
            .document(code)
            .collection("shoppingLists")
            .document(listFirestoreId)
            .collection("shops")
            .document(shopFirestoreId)
            .collection("items")
            .document(itemFirestoreId)
            .update("isChecked", !item.isChecked)
    }

    fun deleteItem(list: ShoppingList, shop: Shop, item: ShoppingItem) {
        val code = _syncGroupCode.value
        val listFirestoreId = list.firestoreId ?: return
        val shopFirestoreId = shop.firestoreId ?: return
        val itemFirestoreId = item.firestoreId ?: return
        if (code.isBlank()) return

        firestore.collection("families")
            .document(code)
            .collection("shoppingLists")
            .document(listFirestoreId)
            .collection("shops")
            .document(shopFirestoreId)
            .collection("items")
            .document(itemFirestoreId)
            .delete()
    }

    private fun registerListeners(code: String) {
        listsListener?.remove()
        shopListeners.values.forEach { it.remove() }
        shopListeners.clear()
        itemListeners.values.forEach { it.remove() }
        itemListeners.clear()
        shopFirestoreIdsByListFirestoreId.clear()
        if (code.isBlank()) return

        val familyDoc = firestore.collection("families").document(code)

        listsListener = familyDoc.collection("shoppingLists")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                viewModelScope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val data = doc.data
                                val existing = shoppingListRepository.getShoppingListByFirestoreId(doc.id)
                                val list = ShoppingList(
                                    id = existing?.id ?: 0,
                                    name = data["name"] as? String ?: "",
                                    createdByMemberId = (data["createdByMemberId"] as? Long)?.toInt() ?: -1,
                                    createdAt = (data["createdAt"] as? Long) ?: 0L,
                                    firestoreId = doc.id
                                )
                                shoppingListRepository.insertShoppingList(list)
                                registerShopsListener(familyDoc, doc.id)
                            }
                            DocumentChange.Type.REMOVED -> {
                                shoppingListRepository.getShoppingListByFirestoreId(doc.id)?.let {
                                    shoppingListRepository.deleteShoppingListById(it.id)
                                }
                                shopListeners.remove(doc.id)?.remove()
                                shopFirestoreIdsByListFirestoreId.remove(doc.id)?.forEach { shopFirestoreId ->
                                    itemListeners.remove(shopFirestoreId)?.remove()
                                }
                            }
                        }
                    }
                }
            }
    }

    private fun registerShopsListener(familyDoc: DocumentReference, listFirestoreId: String) {
        if (shopListeners.containsKey(listFirestoreId)) return

        val listDoc = familyDoc.collection("shoppingLists").document(listFirestoreId)
        shopListeners[listFirestoreId] = listDoc.collection("shops")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                viewModelScope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val data = doc.data
                                val existing = shoppingListRepository.getShopByFirestoreId(doc.id)
                                val localListId = shoppingListRepository.getShoppingListByFirestoreId(listFirestoreId)?.id ?: return@launch
                                val shop = Shop(
                                    id = existing?.id ?: 0,
                                    listId = localListId,
                                    name = data["name"] as? String ?: "",
                                    createdAt = (data["createdAt"] as? Long) ?: 0L,
                                    firestoreId = doc.id
                                )
                                shoppingListRepository.insertShop(shop)
                                shopFirestoreIdsByListFirestoreId.getOrPut(listFirestoreId) { mutableListOf() }.let {
                                    if (!it.contains(doc.id)) it.add(doc.id)
                                }
                                registerItemsListener(listDoc, doc.id)
                            }
                            DocumentChange.Type.REMOVED -> {
                                shoppingListRepository.getShopByFirestoreId(doc.id)?.let {
                                    shoppingListRepository.deleteShopById(it.id)
                                }
                                itemListeners.remove(doc.id)?.remove()
                                shopFirestoreIdsByListFirestoreId[listFirestoreId]?.remove(doc.id)
                            }
                        }
                    }
                }
            }
    }

    private fun registerItemsListener(listDoc: DocumentReference, shopFirestoreId: String) {
        if (itemListeners.containsKey(shopFirestoreId)) return

        val shopDoc = listDoc.collection("shops").document(shopFirestoreId)
        itemListeners[shopFirestoreId] = shopDoc.collection("items")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener

                viewModelScope.launch {
                    for (change in snapshot.documentChanges) {
                        val doc = change.document
                        when (change.type) {
                            DocumentChange.Type.ADDED, DocumentChange.Type.MODIFIED -> {
                                val data = doc.data
                                val existing = shoppingListRepository.getShoppingItemByFirestoreId(doc.id)
                                val localShopId = shoppingListRepository.getShopByFirestoreId(shopFirestoreId)?.id ?: return@launch
                                val item = ShoppingItem(
                                    id = existing?.id ?: 0,
                                    shopId = localShopId,
                                    name = data["name"] as? String ?: "",
                                    isChecked = data["isChecked"] as? Boolean ?: false,
                                    quantity = (data["quantity"] as? Long)?.toInt() ?: 1,
                                    createdAt = (data["createdAt"] as? Long) ?: 0L,
                                    firestoreId = doc.id
                                )
                                shoppingListRepository.insertShoppingItem(item)
                            }
                            DocumentChange.Type.REMOVED -> {
                                shoppingListRepository.getShoppingItemByFirestoreId(doc.id)?.let {
                                    shoppingListRepository.deleteShoppingItemById(it.id)
                                }
                            }
                        }
                    }
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        listsListener?.remove()
        shopListeners.values.forEach { it.remove() }
        shopListeners.clear()
        itemListeners.values.forEach { it.remove() }
        itemListeners.clear()
        shopFirestoreIdsByListFirestoreId.clear()
    }
}
