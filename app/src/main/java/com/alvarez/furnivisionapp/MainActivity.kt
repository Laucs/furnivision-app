package com.alvarez.furnivisionapp
import UserAccount
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.hardware.camera2.CameraManager
import android.icu.text.DecimalFormat
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.DatePicker
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alvarez.furnivisionapp.data.AuthUtility
import com.alvarez.furnivisionapp.data.CartItem
import com.alvarez.furnivisionapp.data.DeliveryAddress
import com.alvarez.furnivisionapp.data.Furniture
import com.alvarez.furnivisionapp.data.Order
import com.alvarez.furnivisionapp.data.SessionManager
import com.alvarez.furnivisionapp.data.Shop
import com.alvarez.furnivisionapp.data.ShopCart
import com.alvarez.furnivisionapp.utils.CameraFunctions
import com.alvarez.furnivisionapp.utils.CartListAdapter
import com.alvarez.furnivisionapp.utils.DBFurnitureAdapter
import com.alvarez.furnivisionapp.utils.DeliveryAddressAdapter
import com.alvarez.furnivisionapp.utils.HomePageFunctions
import com.alvarez.furnivisionapp.utils.OrdersAdapter
import com.alvarez.furnivisionapp.utils.SearchListAdapter
import com.alvarez.furnivisionapp.utils.ShopListAdapter
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Calendar
import java.util.Date


class MainActivity : AppCompatActivity() {

    private lateinit var homePageFunctions: HomePageFunctions
    private lateinit var cameraFunctions: CameraFunctions
    private var activePage: Int? = null
    private lateinit var activeButton: LinearLayout
    private lateinit var database: FirebaseFirestore
    private var cartList: MutableList<ShopCart>? = mutableListOf<ShopCart>()
    private var shops: List<Shop> = mutableListOf<Shop>()
    private var furnitures: MutableList<Furniture> = mutableListOf<Furniture>()
    private var orders: List<Order> = listOf<Order>()
    private var userDetails: UserAccount = UserAccount()
    private lateinit var email: String

    companion object {
        const val ONE_MEGABYTE: Long = 1024 * 1024
        val PRICE_FORMAT = DecimalFormat("#0.00")
    }

    // Icons
    private lateinit var imgHome: ImageView
    private lateinit var imgShop: ImageView
    private lateinit var imgCamera: ImageView
    private lateinit var imgCart: ImageView
    private lateinit var imgProfile: ImageView

    // Layouts
    private val dashboardPage = R.layout.activity_dashboard
    private val shopPage = R.layout.activity_shop
    private val cameraPage = R.layout.activity_camera
    private val cartPage = R.layout.activity_cart
    private val profilePage = R.layout.activity_profile


    private fun getOrders() {
        getUserDetails()
        val db = FirebaseFirestore.getInstance()
        Log.d("email", userDetails.id.toString())
        db.collection("orders")
            .whereEqualTo("userId", userDetails.id)
            .get()
            .addOnSuccessListener { result: QuerySnapshot ->
                val orderList = result.documents.mapNotNull { document ->
                    document.toObject(Order::class.java)?.apply {
                        id = document.id
                    }
                }
                Log.d("reuslt", orderList.toString() )
                orders = orderList.toMutableList()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Error getting orders", exception)
            }
        Log.d("asd", orders.toString())
    }


    private fun getUserDetails() {
        email = SessionManager.getUserEmail(this).toString()
        database = FirebaseFirestore.getInstance()
        database.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { result: QuerySnapshot ->
                if (!result.isEmpty) {
                    val document = result.documents[0]
                    userDetails = document.toObject(UserAccount::class.java)?.apply {
                        id = document.id
                    }!!
                }
            }
            .addOnFailureListener { e: Exception ->
                Log.w(TAG, "Error getting phone number", e)
            }
    }
    private fun getShops() {
        database = FirebaseFirestore.getInstance()
        database.collection("shops").get()
            .addOnSuccessListener { result: QuerySnapshot ->
                shops = result.documents.mapNotNull { document ->
                    document.toObject(Shop::class.java)?.apply {
                        id = document.id
                    }
                }
            }
            .addOnFailureListener { e: Exception ->
                Log.d(TAG, "Error fetching documents: $e")
            }
    }

    private fun getFurniture() {
        database = FirebaseFirestore.getInstance()
        database.collection("furniture")
            .orderBy("name", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result: QuerySnapshot ->
                furnitures = result.documents.mapNotNull { document ->
                    document.toObject(Furniture::class.java)?.apply {
                        id = document.id
                    }

                }.toMutableList()
            }
            .addOnFailureListener { e: Exception ->
                Log.d(TAG, "Error fetching documents: $e")
            }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = FirebaseFirestore.getInstance()
        email = SessionManager.getUserEmail(this) ?: ""
        getShops()
        getFurniture()
        getUserDetails()
        getOrders()

        // Navigation Buttons
        val homeButton: LinearLayout = findViewById(R.id.home_menu)
        val shopButton: LinearLayout = findViewById(R.id.shop_menu)
        val cameraButton: LinearLayout = findViewById(R.id.try_ar_menu)
        val cartButton: LinearLayout = findViewById(R.id.cart_menu)
        val profileButton: LinearLayout = findViewById(R.id.profile_menu)

        // Initialize Icons
        imgHome = findViewById(R.id.imgHome)
        imgShop = findViewById(R.id.imgShop)
        imgCamera = findViewById(R.id.imgAR)
        imgCart = findViewById(R.id.imgCart)
        imgProfile = findViewById(R.id.imgProfile)

        // Navigation Logic
        val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
        var inflatedPage = layoutInflater.inflate(dashboardPage, null) as RelativeLayout
        pageContainer.addView(inflatedPage)
        initHomePage(inflatedPage, pageContainer)

        // Set initial active page icon
        setActivePageIcon(dashboardPage)

        // Refresh dashboard twice
        refreshDashboard(pageContainer)

        // Button Click Listeners
        homeButton.setOnClickListener {
            activePage = dashboardPage
            pageContainer.removeAllViews()
            inflatedPage = layoutInflater.inflate(dashboardPage, null) as RelativeLayout
            pageContainer.addView(inflatedPage)
            initHomePage(inflatedPage, pageContainer)
            setActivePageIcon(dashboardPage)
        }

        shopButton.setOnClickListener {
            activePage = shopPage
            pageContainer.removeAllViews()
            inflatedPage = layoutInflater.inflate(shopPage, null) as RelativeLayout
            pageContainer.addView(inflatedPage)
            initShopPage(pageContainer)
            setActivePageIcon(shopPage)
        }

        cameraButton.setOnClickListener {
            activePage = cameraPage
            pageContainer.removeAllViews()
            inflatedPage = layoutInflater.inflate(cameraPage, null) as RelativeLayout
            pageContainer.addView(inflatedPage)
            initCameraPage()
            setActivePageIcon(cameraPage)
        }

        cartButton.setOnClickListener {
            activePage = cartPage
            pageContainer.removeAllViews()
            inflatedPage = layoutInflater.inflate(cartPage, null) as RelativeLayout
            pageContainer.addView(inflatedPage)
            initCartPage(pageContainer)
            setActivePageIcon(cartPage)
        }

        profileButton.setOnClickListener {
            activePage = profilePage
            pageContainer.removeAllViews()
            inflatedPage = layoutInflater.inflate(profilePage, null) as RelativeLayout
            pageContainer.addView(inflatedPage)
            initProfilePage()
            setActivePageIcon(profilePage)
        }
    }

    // Helper function to reset icons to default
    private fun resetMenuIcons() {
        imgHome.setImageResource(R.drawable.home_logo)
        imgShop.setImageResource(R.drawable.shop_logo)
        imgCamera.setImageResource(R.drawable.ar_logo)
        imgCart.setImageResource(R.drawable.cart_logo)
        imgProfile.setImageResource(R.drawable.profile_logo)
    }

    // Helper function to set the active page icon
    private fun setActivePageIcon(page: Int) {
        resetMenuIcons()
        when (page) {
            dashboardPage -> {
                imgHome.setImageResource(R.drawable.home_logo_fill)
            }
            shopPage -> {
                imgShop.setImageResource(R.drawable.shop_logo_fill)
            }
            cameraPage -> {
                imgCamera.setImageResource(R.drawable.ar_logo_fill)
            }
            cartPage -> {
                imgCart.setImageResource(R.drawable.cart_logo_fill)
            }
            profilePage -> {
                imgProfile.setImageResource(R.drawable.profile_logo_fill)
            }
        }
    }


    private fun refreshDashboard(pageContainer: ViewGroup) {
        pageContainer.removeAllViews()
        val inflatedPage = layoutInflater.inflate(R.layout.activity_dashboard, null) as RelativeLayout
        pageContainer.addView(inflatedPage)
        initHomePage(inflatedPage, pageContainer)
        setActivePageIcon(dashboardPage)
    }


    private fun initHomePage(page: RelativeLayout, pageContainer: ViewGroup) {

        database = FirebaseFirestore.getInstance()
        database.collection("furniture").whereLessThan("sold", 10).get()
            .addOnSuccessListener { result: QuerySnapshot ->
                var newArrivalList: MutableList<Furniture> = mutableListOf()

                for (document in result.documents) {
                    // Create a Furniture object with the document data and document ID
                    val furniture = Furniture(
                        id = document.id,
                        img = document.getString("imgPNG"),
                        name = document.getString("name"),
                        description = document.getString("description"),
                        price = document.getDouble("price"),
                        dimensions = document.getString("dimensions"),
                        stocks = document.getLong("stocks")?.toInt(),
                        rating = document.getDouble("rating"),
                        sold = document.getLong("sold")?.toInt(),
                        shopID = document.getString("shopID"),
                        colors = document.get("colors") as? List<String>
                    )
                    // Add the Furniture object to the list
                    newArrivalList.add(furniture)
                }
                if (newArrivalList.size == 0) {
                    val newArrivalContent: RelativeLayout = findViewById(R.id.new_arrival_content)
                    newArrivalContent.visibility = View.GONE
                }

                val newArrivalListView: RecyclerView = findViewById(R.id.new_arrival_recyclerview)
                val newArrivalAdapter = DBFurnitureAdapter(newArrivalList)

                newArrivalAdapter.setOnItemClickListener(object : DBFurnitureAdapter.OnItemClickListener {
                    @SuppressLint("InflateParams")
                    //for whole item
                    override fun onItemClick(item: Furniture) {
                        pageContainer.removeAllViews()
                        val inflatedPage = layoutInflater.inflate(R.layout.activity_furniture_selection, null) as ViewGroup
                        pageContainer.addView(inflatedPage)
                        item.shopID?.let { initFurniSelectPage(it, pageContainer, item.name) }
                        setActivePageIcon(shopPage)
                    }
                    //for button
                    override fun onLinkButtonClick(item: Furniture) {
                        pageContainer.removeAllViews()
                        val inflatedPage = layoutInflater.inflate(R.layout.activity_furniture_selection, null) as ViewGroup
                        pageContainer.addView(inflatedPage)
                        item.shopID?.let { initFurniSelectPage(it, pageContainer, item.name) }
                        setActivePageIcon(shopPage)
                    }
                    //for image
                    override fun onItemImageClick(item: Furniture) {
                        pageContainer.removeAllViews()
                        val inflatedPage = layoutInflater.inflate(R.layout.activity_furniture_selection, null) as ViewGroup
                        pageContainer.addView(inflatedPage)
                        item.shopID?.let { initFurniSelectPage(it, pageContainer, item.name) }
                        setActivePageIcon(shopPage)
                    }
                })



                newArrivalListView.apply {
                    adapter = newArrivalAdapter
                    layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.HORIZONTAL, false)
                }
            }
            .addOnFailureListener { e: Exception ->

                Log.d(TAG, "Error fetching documents: $e")
            }


        database.collection("furniture")
            .orderBy("sold", Query.Direction.DESCENDING)
            .limit(7)
            .get()
            .addOnSuccessListener { result: QuerySnapshot ->
                val popularsRecyclerView: RecyclerView = findViewById(R.id.popular_recycler_view)
                val topSellingList = mutableListOf<Furniture>()
                for (document in result.documents) {
                    // Create a Furniture object with the document data and document ID
                    val furniture = Furniture(
                        id = document.id,
                        img = document.getString("imgPNG"),
                        name = document.getString("name"),
                        description = document.getString("description"),
                        price = document.getDouble("price"),
                        dimensions = document.getString("dimensions"),
                        stocks = document.getLong("stocks")?.toInt(),
                        rating = document.getDouble("rating"),
                        sold = document.getLong("sold")?.toInt(),
                        shopID = document.getString("shopID"),
                        colors = document.get("colors") as? List<String>
                    )
                    // Add the Furniture object to the list
                    topSellingList.add(furniture)
                    Log.d("List", furniture.toString())
                }
                val popularsAdapter = DBFurnitureAdapter(topSellingList)
                if (popularsAdapter.itemCount == 0) {
                val popularContent: RelativeLayout = findViewById(R.id.popular_content)

                popularContent.visibility = View.GONE
            }
                popularsAdapter.setOnItemClickListener(object : DBFurnitureAdapter.OnItemClickListener {
                    @SuppressLint("InflateParams")
                    //for whole item
                    override fun onItemClick(item: Furniture) {
                        pageContainer.removeAllViews()
                        val inflatedPage = layoutInflater.inflate(R.layout.activity_furniture_selection, null) as ViewGroup
                        pageContainer.addView(inflatedPage)
                        item.shopID?.let { initFurniSelectPage(it, pageContainer, item.name) }
                        setActivePageIcon(shopPage)
                    }
                    //for button
                    override fun onLinkButtonClick(item: Furniture) {
                        pageContainer.removeAllViews()
                        val inflatedPage = layoutInflater.inflate(R.layout.activity_furniture_selection, null) as ViewGroup
                        pageContainer.addView(inflatedPage)
                        item.shopID?.let { initFurniSelectPage(it, pageContainer, item.name) }
                        setActivePageIcon(shopPage)
                    }
                    //for image
                    override fun onItemImageClick(item: Furniture) {
                        pageContainer.removeAllViews()
                        val inflatedPage = layoutInflater.inflate(R.layout.activity_furniture_selection, null) as ViewGroup
                        pageContainer.addView(inflatedPage)
                        item.shopID?.let { initFurniSelectPage(it, pageContainer, item.name) }
                        setActivePageIcon(shopPage)
                    }
                })

                popularsRecyclerView.apply {
                    adapter = popularsAdapter
                    layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.HORIZONTAL, false)
                }
            }
            .addOnFailureListener { e: Exception ->
                Log.d(TAG, "Error fetching documents: $e")
            }

        val searchBar: SearchView = findViewById(R.id.search_bar)
        val contentScroll: ScrollView = findViewById(R.id.scroll_content)
        val searchListView: ListView = findViewById(R.id.search_list_view)
        var furnitureList: List<Furniture> = furnitures


        val searchAdapter = SearchListAdapter(this, furnitureList)
        searchListView.adapter = searchAdapter

        searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle search action (optional)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val isSearchEmpty = newText.isNullOrEmpty()
                Log.d(TAG, "Search text changed: $newText")

                contentScroll.visibility = if (isSearchEmpty) View.VISIBLE else View.GONE
                searchListView.visibility = if (isSearchEmpty) View.GONE else View.VISIBLE

                if (!isSearchEmpty) {
                    newText?.let {
                        val filteredList = furnitureList.filter { furniture ->
                            furniture.name?.contains(it, ignoreCase = true) ?: false
                        }
                        (searchListView.adapter as SearchListAdapter).updateList(filteredList)
                        Log.d(TAG, "Filtered list updated with ${filteredList.size} items")
                    }

                }

                searchListView.setOnItemClickListener { parent, view, position, id  ->
                    // Get the Furniture object corresponding to the clicked item
                    var clickedFurniture: Furniture? = null
                    val furnitureName: TextView = view.findViewById(R.id.search_item)

                    for (furniture in furnitureList) {
                        if  (furnitureName.text.toString() == furniture.name) {
                            clickedFurniture = furniture
                        }
                    }

                    // Now you can use the data from the clicked Furniture object as needed
                    val furnitureShopID = clickedFurniture?.shopID

                    // Perform further actions based on the clicked item's data
                    pageContainer.removeAllViews()
                    val inflatedPage = layoutInflater.inflate(R.layout.activity_furniture_selection, null) as ViewGroup
                    pageContainer.addView(inflatedPage)
                    if (furnitureShopID != null) {
                        setActivePageIcon(shopPage)
                        initFurniSelectPage(furnitureShopID, pageContainer, furnitureName.text.toString())
                    }
                    refreshDashboard(pageContainer)
                }

                return true

            }
        })
    }

    private fun initShopPage(pageContainer: ViewGroup) {
        val database = FirebaseFirestore.getInstance()
        val shopsView: RecyclerView = findViewById(R.id.shop_list)
        val searchBar: SearchView = findViewById(R.id.search_store)

        val adapter = ShopListAdapter(emptyList())

        adapter.setOnItemClickListener(object : ShopListAdapter.OnItemClickListener {
            @SuppressLint("InflateParams")
            override fun onItemClick(shopID: String) {
                pageContainer.removeAllViews()
                val inflatedPage = layoutInflater.inflate(R.layout.activity_furniture_selection, null) as ViewGroup
                pageContainer.addView(inflatedPage)
                initFurniSelectPage(shopID, pageContainer, null)
            }
        })

        var originalList: List<Shop> = emptyList()

        shopsView.adapter = adapter
        shopsView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        database.collection("shops")
            .get()
            .addOnSuccessListener { result: QuerySnapshot ->
                originalList = result.documents.mapNotNull { document ->
                    document.toObject(Shop::class.java)?.apply {
                        id = document.id
                    }
                }
                adapter.updateList(originalList)
            }
            .addOnFailureListener { exception: Exception ->
                Log.d(TAG, "Error fetching documents: $exception")
            }

        searchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Handle search action (optional)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = if (newText.isNullOrEmpty()) {
                    // If search query is empty, show original list
                    originalList
                } else {
                    // Filter the list based on search query
                    adapter.filter(newText)
                }
                adapter.updateList(filteredList)
                return true
            }
        })
    }


    @SuppressLint("SetTextI18n", "DefaultLocale", "CommitPrefEdits", "InflateParams")
    private fun initFurniSelectPage(shopID: String, pageContainer: ViewGroup, furnitureName: String? = null) {
        var index: Int = 0

        database = FirebaseFirestore.getInstance()

        database.collection("furniture").whereEqualTo("shopID", shopID).get()
            .addOnSuccessListener { result: QuerySnapshot ->
                val furnitures: List<Furniture> = result.documents.mapNotNull { document ->
                    document.toObject(Furniture::class.java)?.apply {
                        id = document.id
                    }

                }

                var fIndex = 0
                // Find the index of the furnitureID in the list
                for (furnitureItem in furnitures) {
                    if (furnitureName == furnitureItem.name) {
                        index = fIndex
                        Log.d("name", furnitures[index].id + "-" + furnitures[index].name)
                        break
                    }else fIndex++
                }

                Log.d("Open Cart", cartList.toString())


                val shopName: TextView = findViewById(R.id.shopName)
                val addToCartBtn: Button = findViewById(R.id.addToCartButton)
                val nextBtn: ImageButton = findViewById(R.id.nextBtn)
                val prevBtn: ImageButton = findViewById(R.id.previousBtn)

                val imageView: ImageView = findViewById(R.id.furnitureImage)
                val nameTextView: TextView = findViewById(R.id.furnitureName)
                val descTextView: TextView = findViewById(R.id.furnitureDesc)
                val priceTextView: TextView = findViewById(R.id.furniturePrice)
                val rateTextView: TextView = findViewById(R.id.rateTV)
                val dimensionsTextView: TextView = findViewById(R.id.furnitureDimensions)
                val stocksTextView: TextView = findViewById(R.id.furnitureStocks)

                //for colors
                val color1TextView: TextView = findViewById(R.id.color1)
                val color2TextView: TextView = findViewById(R.id.color2)

                fun updateUI() {
                    val storageReference = furnitures[index].img.let { it?.let { it1 ->
                        Firebase.storage.getReferenceFromUrl(
                            it1
                        )
                    } }

                    storageReference?.getBytes(ONE_MEGABYTE)?.addOnSuccessListener { bytes ->
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        imageView.setImageBitmap(bitmap)
                    }
                    database.collection("shops").document(shopID).get()
                        .addOnSuccessListener { shopDocument ->
                            if (shopDocument.exists()) {
                                val shop = shopDocument.toObject(Shop::class.java)
                                shop?.id = shopDocument.id

                                shopName.text = shop?.name ?: "Unknown Shop"

                            }
                        }
                        .addOnFailureListener { exception ->
                            // Handle the failure
                            Log.e("ERROR", "Error getting shop document", exception)
                        }
                    nameTextView.text = furnitures[index].name
                    rateTextView.text = furnitures[index].rating.toString()
                    descTextView.text = furnitures[index].description
                    priceTextView.text = "₱ " + String.format("%,.2f",furnitures[index].price)
                    dimensionsTextView.text = furnitures[index].dimensions
                    stocksTextView.text = getString(R.string.stock) + " " + furnitures[index].stocks.toString()

                    // Set the colors
                    val colors = furnitures[index].colors
                    if (colors != null && colors.size >= 2) {
                        color1TextView.setBackgroundColor(Color.parseColor(colors[0]))
                        color2TextView.setBackgroundColor(Color.parseColor(colors[1]))
                    }

                    prevBtn.visibility = if (index == 0) View.GONE else View.VISIBLE
                    nextBtn.visibility = if (index == furnitures.size - 1) View.GONE else View.VISIBLE

                    val addBtn: ImageButton = findViewById(R.id.addBtn)
                    // Retrieve the image URL of the furniture the user is currently viewing
                    val currentFurnitureImageURL = furnitures[index].img


                    //for add button to direct int he camerapage
                    // Set click listener for the add button

                    addBtn.setOnClickListener {
                        // Navigate to the camera page and pass the image URL of the current furniture
                        val inflatedPage: RelativeLayout = layoutInflater.inflate(R.layout.activity_camera, null) as RelativeLayout
                        pageContainer.removeAllViews()
                        pageContainer.addView(inflatedPage)
                        initCameraPage()
                        setActivePageIcon(cameraPage)
                        Toast.makeText(this@MainActivity, "Trying Furniture In AR Camera....", Toast.LENGTH_SHORT).show()
                    }
                }

                updateUI()

                nextBtn.setOnClickListener {
                    if (index < furnitures.size - 1) {
                        index++
                        updateUI()
                    }
                }

                prevBtn.setOnClickListener {
                    if (index > 0) {
                        index--
                        updateUI()
                    }
                }

                addToCartBtn.setOnClickListener {
                    val shopID = furnitures[index].shopID
                    if (shopID != null) {
                        database.collection("shops").document(shopID).get()
                            .addOnSuccessListener { result: DocumentSnapshot? ->
                                if (result != null && result.exists()) {
                                    val shop = result.toObject(Shop::class.java)
                                    if (shop != null) {
                                        shop.id = result.id
                                        addItemToCart(shop, furnitures[index])
                                    }
                                }
                            }
                            .addOnFailureListener { exception ->
                                // Handle the failure
                                Log.e("ERROR", "Error getting document", exception)
                            }
                    }

                    Toast.makeText(this, "Furniture is added to Cart", Toast.LENGTH_SHORT).show()
                }
            }



        val backBtn: ImageButton = findViewById(R.id.backBtn)

        backBtn.setOnClickListener {
            index = 0
            pageContainer.removeAllViews()
            val inflatedPage: RelativeLayout = layoutInflater.inflate(R.layout.activity_dashboard, null) as RelativeLayout
            activePage = R.layout.activity_dashboard
            pageContainer.removeAllViews()
            pageContainer.addView(inflatedPage)
            initHomePage(inflatedPage, pageContainer)
            setActivePageIcon(dashboardPage)
        }

        val shopImg: ImageButton = findViewById(R.id.shopImg)
        shopImg.setOnClickListener {
            pageContainer.removeAllViews()
            val inflatedPage: RelativeLayout = layoutInflater.inflate(R.layout.activity_shop, null) as RelativeLayout
            activePage = R.layout.activity_shop
            pageContainer.removeAllViews()
            pageContainer.addView(inflatedPage)
            initShopPage(pageContainer)
            setActivePageIcon(shopPage)
        }
    }

    private fun addItemToCart(selectedShop: Shop, selectedFurniture: Furniture) {
        // Find the ShopCart in the list
        val shopCart = cartList?.find { it.shop?.id == selectedShop.id }

        if (shopCart != null) {
            // Shop exists in the list, check for the furniture item
            val cartItem = shopCart.items?.find { it.furniture?.id == selectedFurniture.id }
            if (cartItem != null) {
                // Furniture exists in the list, increment the quantity
                cartItem.quantity = cartItem.quantity?.plus(1)
            } else {
                // Furniture does not exist, add it to the list
                if (shopCart.items == null) {
                    shopCart.items = mutableListOf(CartItem(selectedFurniture, 1))
                } else {
                    shopCart.items!!.add(CartItem(selectedFurniture, 1))
                }
            }
        } else {
            // Shop does not exist, create a new ShopCart with the furniture item
            cartList?.add(ShopCart(selectedShop, mutableListOf(CartItem(selectedFurniture, 1))))
        }

        // Log the updated cartList for debugging
    }

    private fun initCameraPage() {
        val cameraLayout: RelativeLayout = findViewById(R.id.cameraLayout)
        val galleryLayout: RelativeLayout  = findViewById(R.id.galleryLayout)
        val galleryImageView: ImageView = findViewById(R.id.galleryImageView)

        val galleryBtn: Button = findViewById(R.id.galleryButton)
        val galleryBtn2: Button = findViewById(R.id.vidGalleryButton)
        val cameraBtn: Button = findViewById(R.id.backButton)
        val cameraBtn2: Button = findViewById(R.id.vidCameraButton)
        val captureButton: Button = findViewById(R.id.captureButton)
        val galleryPrevBtn: Button = findViewById(R.id.prevButton)
        val galleryNextBtn: Button = findViewById(R.id.nextButton)
        val textureView: TextureView = findViewById(R.id.textureView)
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager


        cameraFunctions = CameraFunctions(
            cameraLayout,
            galleryLayout,
            galleryImageView,
            galleryBtn,
            galleryBtn2,
            cameraBtn,
            cameraBtn2,
            captureButton,
            galleryPrevBtn,
            galleryNextBtn,
            filesDir,
            textureView,
            cameraManager,
            this
        )
    }

    private fun initCartPage(pageContainer: ViewGroup) {
        val cartListRecyclerView: RecyclerView = findViewById(R.id.cart_recycler_view)
        val totalPriceTextView: TextView = findViewById(R.id.total_amount_textview)

        // Calculate and display the initial total price
        val totalPrice = calculateTotalPrice(cartList ?: mutableListOf())
        totalPriceTextView.text = "₱ ${String.format("%,.2f", totalPrice)}"

        // Set up CartListAdapter
        val adapter = CartListAdapter(cartList ?: mutableListOf()) { newTotalPrice ->
            totalPriceTextView.text = "₱ ${String.format("%,.2f", newTotalPrice)}"
        }
        cartListRecyclerView.adapter = adapter
        cartListRecyclerView.layoutManager = LinearLayoutManager(this)

        // Checkout button click listener
        val checkoutBtn: Button = findViewById(R.id.checkout_button)
        checkoutBtn.setOnClickListener {
            if (cartList.isNullOrEmpty()) {
                Toast.makeText(this, "No items to checkout.", Toast.LENGTH_SHORT).show()
            } else {
                activePage = R.layout.activity_orders
                pageContainer.removeAllViews()
                pageContainer.addView(layoutInflater.inflate(R.layout.activity_orders, null) as RelativeLayout)
                initOrderPage(pageContainer)
            }
        }
    }

    fun calculateTotalPrice(shoppingCarts: MutableList<ShopCart>): Double {
        var totalPrice = 0.0
        for (cart in shoppingCarts) {
            for (item in cart.items!!) {
                totalPrice += (item.quantity?.let { item.furniture?.price?.times(it) } ?: 0.0)
            }
        }
        return totalPrice
    }

    //store in the database
    fun afterCheckoutCompletion(
        productProtectSubtotal: Double,
        shipSubtotal: Double,
        merchSubTotalValue: Double,
        paymentMethod: String,
        phoneNumber: String
    ) {
        val totalPrice = productProtectSubtotal + shipSubtotal + merchSubTotalValue

        val furnitureIds = mutableListOf<String>()
        cartList?.forEach { shopCart ->
            shopCart.items?.forEach { cartItem ->
                cartItem.quantity?.let { quantity ->
                    repeat(quantity) {
                        cartItem.furniture?.id?.let { id -> furnitureIds.add(id) }
                    }
                }
            }
        }
        // Get current timestamp
        val today = Date()
        val calendar = Calendar.getInstance()
        calendar.time = today
        calendar.add(Calendar.DAY_OF_MONTH, 10)
        val arrivalDate = calendar.time

        // Get the user's email
        val email = SessionManager.getUserEmail(this)

        // Query the database to get the user document based on email
        database.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { result: QuerySnapshot ->
                val document = result.documents.firstOrNull()
                if (document != null) {
                    val userId = document.id

                        // Create a hashmap to store order details including user ID
                        val order: Order = Order("", userId, cartList, totalPrice, today, arrivalDate, paymentMethod, phoneNumber)

                    // Add the order to the "orders" collection along with the user ID
                    database.collection("orders")
                        .add(order)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Order is successful", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Order isn't successful", Toast.LENGTH_SHORT).show()
                        }

                    // Clear the cart after placing the order
                    cartList = mutableListOf()

                    // Navigate to the dashboard or any other desired page after placing the order
                    val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
                    pageContainer.removeAllViews()
                    val inflatedPage = layoutInflater.inflate(R.layout.activity_after_checkout_page, null) as RelativeLayout
                    pageContainer.addView(inflatedPage)
                    initAfterCheckOutPage()

                } else {
                    // Handle case where user document is not found
                    Log.e(TAG, "User document not found for email: $email")
                    Toast.makeText(this, "User not found. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e: Exception ->
                Log.w(TAG, "Error getting user document", e)
                Toast.makeText(this, "Failed to place order. Please try again.", Toast.LENGTH_SHORT).show()
            }
    }



    private fun initOrderPage(pageContainer: ViewGroup) {
        val orderListRecyclerView: RecyclerView = findViewById(R.id.order_recycler_view)
        val orderProductProtectTextView: TextView = findViewById(R.id.prod_protect_subtotal_value)
        val ordershipSubtotalTextView: TextView = findViewById(R.id.ship_subtotal_value)
        val merchSubTotalTextView: TextView = findViewById(R.id.merch_subtotal_value)
        val orderTotalPaymentTextView: TextView = findViewById(R.id.total_payment_value)
        val totalValueTextView: TextView = findViewById(R.id.total_value)
        val backButton: ImageButton = findViewById(R.id.backButton)

        fetchAndDisplayDeliveryAddress()

        backButton.setOnClickListener {
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_cart, null) as RelativeLayout)
            initCartPage(pageContainer)
        }

        // Define shipping costs
        val STANDARD_SHIPPING_COST = 50.0
        val EXPRESS_SHIPPING_COST = 100.0
        val OVERNIGHT_SHIPPING_COST = 200.0

        // Initialize shipping subtotal
        var shipSubtotal = STANDARD_SHIPPING_COST

        // Calculate and display subtotals
        val productProtectSubtotal = 1000.0
        var merchSubTotalValue = cartList?.let { calculateTotalPrice(it) } ?: 0.0
        val merchSubTotal = "₱ " + String.format("%,.2f", merchSubTotalValue)

        // Function to update total payment
        fun updateTotalPayment() {
            val totalPaymentValue = productProtectSubtotal + shipSubtotal + merchSubTotalValue
            val totalPayment = "₱ " + String.format("%,.2f", totalPaymentValue)
            orderTotalPaymentTextView.text = totalPayment
            totalValueTextView.text = totalPayment
        }

        // Initialize views with default values
        orderProductProtectTextView.text = "₱ " + String.format("%,.2f", productProtectSubtotal)
        ordershipSubtotalTextView.text = "₱ " + String.format("%,.2f", shipSubtotal)
        merchSubTotalTextView.text = merchSubTotal
        updateTotalPayment()

        // Select shipping method
        val orderShippingMethod: CardView = findViewById(R.id.order_ship_method)
        val shippingMethodTV: TextView = findViewById(R.id.shipping_method_text_view)

        orderShippingMethod.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.shipping_method_dialog, null)
            val radioGroup: RadioGroup = dialogView.findViewById(R.id.radioGroup)

            val builder = AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Confirm") { dialog, _ ->
                    val selectedRadioButtonId = radioGroup.checkedRadioButtonId
                    if (selectedRadioButtonId != -1) {
                        val selectedRadioButton: RadioButton = dialogView.findViewById(selectedRadioButtonId)
                        shippingMethodTV.text = selectedRadioButton.text
                        shippingMethodTV.setTextColor(ContextCompat.getColor(this, R.color.lighter_gray))

                        // Update shipping subtotal based on selected method
                        shipSubtotal = when (selectedRadioButton.text.toString()) {
                            "Standard Shipping" -> STANDARD_SHIPPING_COST
                            "Express Shipping" -> EXPRESS_SHIPPING_COST
                            "Overnight Shipping" -> OVERNIGHT_SHIPPING_COST
                            else -> STANDARD_SHIPPING_COST
                        }
                        ordershipSubtotalTextView.text = "₱ " + String.format("%,.2f", shipSubtotal)
                        updateTotalPayment()

                        Toast.makeText(this, "Shipping Method Set!", Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }

            val dialog = builder.create()
            dialog.show()
        }

        // Select payment method
        val orderPaymentMethod: CardView = findViewById(R.id.order_payment_method)
        val paymentMethodTV: TextView = findViewById(R.id.payment_method_text_view)

        orderPaymentMethod.setOnClickListener {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.payment_method_dialog, null)
            val radioGroup: RadioGroup = dialogView.findViewById(R.id.payment_method_radio_group)

            val builder = AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Confirm") { dialog, _ ->
                    val selectedRadioButtonId = radioGroup.checkedRadioButtonId
                    if (selectedRadioButtonId != -1) {
                        val selectedRadioButton: RadioButton = dialogView.findViewById(selectedRadioButtonId)
                        paymentMethodTV.text = selectedRadioButton.text
                        paymentMethodTV.setTextColor(ContextCompat.getColor(this, R.color.lighter_gray))
                        Toast.makeText(this, "Payment Method Set!", Toast.LENGTH_SHORT).show()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.cancel()
                }

            val dialog = builder.create()
            dialog.show()
        }

        // Set up CartListAdapter
        val adapter = CartListAdapter(cartList ?: mutableListOf()) { newTotalPrice ->
            merchSubTotalValue = newTotalPrice
            merchSubTotalTextView.text = "₱ ${String.format("%,.2f", newTotalPrice)}"
            updateTotalPayment()
        }
        orderListRecyclerView.adapter = adapter
        orderListRecyclerView.layoutManager = LinearLayoutManager(this)

        val nameDeliveryTV: TextView = findViewById(R.id.nameDeliveryTV)
        val phoneDeliveryTV: TextView = findViewById(R.id.phoneDeliveryTV)
        val regionTV: TextView = findViewById(R.id.regionTV)
        val barangayTV: TextView = findViewById(R.id.barangayTV)
        val streetNameTV: TextView = findViewById(R.id.streetNameTV)
        val postalCodeTV: TextView = findViewById(R.id.postalCodeTV)

        val placeOrderButton: Button = findViewById(R.id.place_order_button)
        placeOrderButton.setOnClickListener {
            val nameDelivery = nameDeliveryTV.text.toString()
            val phoneDelivery = phoneDeliveryTV.text.toString()
            val region = regionTV.text.toString()
            val barangay = barangayTV.text.toString()
            val streetName = streetNameTV.text.toString()
            val postalCode = postalCodeTV.text.toString()

            if (nameDelivery.isEmpty() || phoneDelivery.isEmpty() ||
                region.isEmpty() || barangay.isEmpty() ||
                streetName.isEmpty() || postalCode.isEmpty()
            ) {
                Toast.makeText(this, "Please add a delivery address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (shippingMethodTV.text == "Select") {
                Toast.makeText(this, "Please select a shipping method", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (paymentMethodTV.text == "Select") {
                Toast.makeText(this, "Please select a payment method", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }



            // Handle different payment methods using when statement
            when (paymentMethodTV.text.toString()) {
                "PayPal"-> {

                    initEmailVerification(pageContainer,productProtectSubtotal, shipSubtotal, merchSubTotalValue,"PayPal")


                }
                "GCash" -> {

                    initEmailVerification(pageContainer, productProtectSubtotal, shipSubtotal, merchSubTotalValue,"GCash")

                }
                "COD" -> {
                    //straight forward
                    afterCheckoutCompletion(productProtectSubtotal, shipSubtotal, merchSubTotalValue, paymentMethodTV.text.toString(), phoneDelivery)
                }
                else -> Toast.makeText(this, "Unsupported Payment Method", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun initEmailVerification(
        pageContainer: ViewGroup,
        productProtectSubtotal: Double,
        shipSubtotal: Double,
        merchSubTotalValue: Double,
        paymentMethod: String
    ) {
        val emailVerificationLayout = layoutInflater.inflate(R.layout.activity_email_verification, null) as RelativeLayout
        pageContainer.removeAllViews()
        pageContainer.addView(emailVerificationLayout)

        val email = SessionManager.getUserEmail(this)
        val totalPaymentEmail: TextView = emailVerificationLayout.findViewById(R.id.total_payment_value_email)
        val paymentMethodEmail: TextView = emailVerificationLayout.findViewById(R.id.payment_method_TV_email)
        val emailOrPhone: EditText = emailVerificationLayout.findViewById(R.id.emailET)
        val payButton: Button = emailVerificationLayout.findViewById(R.id.payButton)

        // Set payment method
        paymentMethodEmail.text = paymentMethod
        backToOrders(emailVerificationLayout)

        // Get user name
        if (email != null) {
            AuthUtility.getUserName(email,
                onSuccess = { _ ->
                    emailOrPhone.setText(email)
                },
                onFailure = {
                    Log.e("GetUser", "Failed to retrieve user name")
                }
            )
        }

        val TOTAL_AMOUNT = "₱ ${String.format("%,.2f", productProtectSubtotal + shipSubtotal + merchSubTotalValue)}"
        totalPaymentEmail.text = TOTAL_AMOUNT

        payButton.setOnClickListener {
            val emailOrPhoneText = emailOrPhone.text.toString().trim()
            if (emailOrPhoneText.isEmpty()) {
                Toast.makeText(this, "Invalid Email Format", Toast.LENGTH_SHORT).show()
            } else {
                when (paymentMethodEmail.text.toString()) {
                    "PayPal" -> {
                        initPaypalVerification(pageContainer, TOTAL_AMOUNT, productProtectSubtotal, shipSubtotal, merchSubTotalValue)
                    }
                    "GCash" -> {
                        initGcashVerification(pageContainer, TOTAL_AMOUNT, productProtectSubtotal, shipSubtotal, merchSubTotalValue)
                    }
                    else -> Toast.makeText(this, "Unsupported Payment Method", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun initPaypalVerification(pageContainer: ViewGroup, totalAmount: String, productProtectSubtotal: Double, shipSubtotal: Double, merchSubTotalValue: Double) {
        val paypalVerification = layoutInflater.inflate(R.layout.activity_paypal_verification, null) as RelativeLayout
        pageContainer.removeAllViews()
        pageContainer.addView(paypalVerification)

        backToOrders(paypalVerification)
        val paypalEmailET: EditText = paypalVerification.findViewById(R.id.editPhoneET)
        val paypalAmountDueTextView: TextView = paypalVerification.findViewById(R.id.paypal_amount_due)
        paypalAmountDueTextView.text = totalAmount

        val confirmButton: Button = paypalVerification.findViewById(R.id.confirmButton)
        confirmButton.setOnClickListener {
            if (paypalAmountDueTextView.text.isEmpty()){
                Toast.makeText(this, "Please enter your PayPal Email Address", Toast.LENGTH_SHORT).show()
            }else {
                afterCheckoutCompletion(productProtectSubtotal, shipSubtotal, merchSubTotalValue, "PayPal" , paypalEmailET.text.toString())
            }
        }
    }

    fun initGcashVerification(pageContainer: ViewGroup, totalAmount: String, productProtectSubtotal: Double, shipSubtotal: Double, merchSubTotalValue: Double) {
        val gcashVerificationLayout = layoutInflater.inflate(R.layout.activity_gcash_verification, null) as ScrollView
        pageContainer.removeAllViews()
        pageContainer.addView(gcashVerificationLayout)

        backToOrders(gcashVerificationLayout)
        val gcashNumTV: EditText = gcashVerificationLayout.findViewById(R.id.editPhoneET)
        val gcashAmountDueTextView: TextView = gcashVerificationLayout.findViewById(R.id.gcash_amount_due)
        gcashAmountDueTextView.text = totalAmount

        val confirmButton: Button = gcashVerificationLayout.findViewById(R.id.confirmButton)
        confirmButton.setOnClickListener {
            // Call afterCheckoutCompletion with the appropriate parameters
            if (gcashAmountDueTextView.text.isEmpty()){
                Toast.makeText(this, "Please enter your GCash Mobile Number", Toast.LENGTH_SHORT).show()
            }else {
                afterCheckoutCompletion(productProtectSubtotal, shipSubtotal, merchSubTotalValue, "GCash", gcashNumTV.text.toString())
            }
        }
    }

    fun backToOrders(layout: ViewGroup){
        val backButton: ImageButton = layout.findViewById(R.id.backButton)
        val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
        backButton.setOnClickListener {
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_orders, null) as RelativeLayout)
            initOrderPage(pageContainer)
            setActivePageIcon(cartPage)
        }
    }
    fun initAfterCheckOutPage(){
        getOrders()
        val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
        val homeButton: Button = findViewById(R.id.homeButton)
        homeButton.setOnClickListener {

            val inflatedPage: RelativeLayout = layoutInflater.inflate(R.layout.activity_dashboard, null) as RelativeLayout
            activePage = R.layout.activity_dashboard
            pageContainer.removeAllViews()
            pageContainer.addView(inflatedPage)
            initHomePage(inflatedPage, pageContainer)
            setActivePageIcon(dashboardPage)

        }

        val myPurchasesButton: Button = findViewById(R.id.myPurchasesButton)
        myPurchasesButton.setOnClickListener {

            val inflatedPage: RelativeLayout = layoutInflater.inflate(R.layout.activity_to_pay, null) as RelativeLayout
            activePage = R.layout.activity_to_pay
            pageContainer.removeAllViews()
            pageContainer.addView(inflatedPage)
            initToPayPage()
        }
    }
    fun fetchAndDisplayDeliveryAddress() {
        val firestore = FirebaseFirestore.getInstance()
        val email = SessionManager.getUserEmail(this)
        val nameDeliveryTV: TextView = findViewById(R.id.nameDeliveryTV)
        val phoneDeliveryTV: TextView = findViewById(R.id.phoneDeliveryTV)
        val regionTV: TextView = findViewById(R.id.regionTV)
        val barangayTV: TextView = findViewById(R.id.barangayTV)
        val streetNameTV: TextView = findViewById(R.id.streetNameTV)
        val postalCodeTV: TextView = findViewById(R.id.postalCodeTV)

        if (email != null) {
            firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val userDocument = querySnapshot.documents.first()
                        val deliveryAddressesData = userDocument.get("deliveryAddresses") as? List<HashMap<String, Any>>

                        if (deliveryAddressesData != null && deliveryAddressesData.isNotEmpty()) {
                            val deliveryAddressData = deliveryAddressesData.first()

                            val name = deliveryAddressData["name"] as? String
                            val phone = "(+63)" + " " + deliveryAddressData["phone"] as? String
                            val region = deliveryAddressData["region"] as? String
                            val barangay = deliveryAddressData["barangay"] as? String
                            val streetName = deliveryAddressData["streetName"] as? String
                            val postalCode = deliveryAddressData["postalCode"] as? String

                            if (name != null && phone != null && region != null && barangay != null && streetName != null && postalCode != null) {
                                nameDeliveryTV.text = name
                                phoneDeliveryTV.text = phone
                                regionTV.text = region
                                barangayTV.text = barangay
                                streetNameTV.text = streetName
                                postalCodeTV.text = postalCode
                            } else {
                                Log.e("MainActivity", "One or more fields are null")
                            }
                        } else {
                            Log.e("MainActivity", "No delivery addresses found")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Failed to fetch delivery addresses: ${e.message}")
                }
        } else {
            Log.e("GetUser", "Invalid email: $email")
        }
    }

    private fun initProfilePage() {
        val emailTextView: TextView = findViewById(R.id.email)
        val email = SessionManager.getUserEmail(this)
        if (email != null) {
            val censoredEmail = censorEmail(email)
            emailTextView.text = censoredEmail
        } else {
            // Handle the case where email is null
            Log.e("GetUserEmail", "Email is null")
        }
        val nameTextView: TextView = findViewById(R.id.name)

        val settingsButton: ImageButton = findViewById(R.id.settingsButton)
        val cartButton: ImageButton = findViewById(R.id.cartButton)
        val toPayButton: RelativeLayout = findViewById(R.id.toPayButton)
        val toShipButton: RelativeLayout = findViewById(R.id.toShipButton)
        val toReceiveButton: RelativeLayout = findViewById(R.id.toReceiveButton)
        val toRateButton: RelativeLayout = findViewById(R.id.toRateButton)
        val accountInfoButton: RelativeLayout = findViewById(R.id.accountInfoButton)
        val paymentMethodsButton: RelativeLayout = findViewById(R.id.paymentMethodsButton)
        val deliveryAddressButton: RelativeLayout = findViewById(R.id.deliveryAddressButton)
        val editButton: ImageButton = findViewById(R.id.editButton)
        val profilePic: ImageButton = findViewById(R.id.profilePic)
        // Navigation Logic
        val pageContainer: ViewGroup = findViewById(R.id.pageContainer)

        if (email != null) {
            AuthUtility.getUserName(email,
                onSuccess = { name ->
                    nameTextView.text = name
                },
                onFailure = {
                    Log.e("GetUser", "Failed to retrieve user name")
                }
            )
        } else {
            Log.e("GetUser", "Invalid email: $email")
        }

        editButton.setOnClickListener {
            activePage = R.layout.activity_edit_account
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_edit_account, null) as RelativeLayout)
            initProfileEditPage()
            setActivePageIcon(profilePage)
        }
        profilePic.setOnClickListener {
            activePage = R.layout.activity_edit_account
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_edit_account, null) as RelativeLayout)
            initProfileEditPage()
            setActivePageIcon(profilePage)
        }
        cartButton.setOnClickListener {
            activePage = R.layout.activity_cart
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_cart, null) as RelativeLayout)
            initCartPage(pageContainer)
            setActivePageIcon(cartPage)
        }

        settingsButton.setOnClickListener {
            activePage = R.layout.activity_settings
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_settings, null) as RelativeLayout)
            initBackButton()
            initSettingsPage()
            setActivePageIcon(profilePage)
        }

        toPayButton.setOnClickListener {
            activePage = R.layout.activity_to_pay
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_pay, null) as RelativeLayout)
            initToPayPage()
            setActivePageIcon(profilePage)
        }

        toShipButton.setOnClickListener {
            activePage = R.layout.activity_to_ship
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_ship, null) as RelativeLayout)
            initBackButton()
            initToShipPage()
            setActivePageIcon(profilePage)
        }

        toReceiveButton.setOnClickListener {
            activePage = R.layout.activity_to_receive
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_receive, null) as RelativeLayout)
            initBackButton()
            initToReceivePage()
            setActivePageIcon(profilePage)
        }

        toRateButton.setOnClickListener {
            activePage = R.layout.activity_to_rate
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_rate, null) as RelativeLayout)
            initBackButton()
            initToRatePage()
            setActivePageIcon(profilePage)

        }

        accountInfoButton.setOnClickListener {
            activePage = R.layout.activity_edit_account
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_edit_account, null) as RelativeLayout)
            initBackButton()
            initProfileEditPage()
            setActivePageIcon(profilePage)
        }

        paymentMethodsButton.setOnClickListener {
            activePage = R.layout.activity_payment_methods
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_payment_methods, null) as RelativeLayout)
            initBackButton()
            initPaymentMethodsPage()
            setActivePageIcon(profilePage)
        }

        deliveryAddressButton.setOnClickListener {
            activePage = R.layout.activity_delivery_address
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_delivery_address, null) as RelativeLayout)
            initBackButton()
            initDeliveryAddressPage()
            setActivePageIcon(profilePage)
        }
        refreshProfile()
    }




    fun initDeliveryAddressPage() {
        val firestore = FirebaseFirestore.getInstance()
        val addNewDeliveryAddressButton = findViewById<RelativeLayout>(R.id.addNewDeliveryAddressButton)

        // Initialize the delivery address page
        initRefreshDeliveryAddress()

        addNewDeliveryAddressButton.setOnClickListener {
            val addDialogView = layoutInflater.inflate(R.layout.add_delivery_address_dialog, null)
            val addNameDeliveryET = addDialogView.findViewById<EditText>(R.id.addNameDeliveryET)
            val addPhoneDeliveryET = addDialogView.findViewById<EditText>(R.id.addPhoneDeliveryET)
            val addRegionET = addDialogView.findViewById<EditText>(R.id.addRegionET)
            val addBarangayET = addDialogView.findViewById<EditText>(R.id.addBarangayET)
            val addStreetNameET = addDialogView.findViewById<EditText>(R.id.addStreetNameET)
            val addPostalCodeET = addDialogView.findViewById<EditText>(R.id.addPostalCodeET)
            val closeButton = addDialogView.findViewById<ImageButton>(R.id.closeButton)

            val addDialog = AlertDialog.Builder(this)
                .setView(addDialogView)
                .setPositiveButton("Save", null)
                .setNegativeButton("Cancel") { _, _ -> }
                .create()

            closeButton.setOnClickListener {
                addDialog.dismiss()
            }

            addDialog.setOnShowListener { dialog ->
                val saveButton = (dialog as AlertDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                saveButton.setOnClickListener {
                    val name = addNameDeliveryET.text?.toString()?.trim()
                    val phone = addPhoneDeliveryET.text?.toString()?.trim()
                    val region = addRegionET.text?.toString()?.trim()
                    val barangay = addBarangayET.text?.toString()?.trim()
                    val streetName = addStreetNameET.text?.toString()?.trim()
                    val postalCode = addPostalCodeET.text?.toString()?.trim()

                    if (name.isNullOrEmpty() || phone.isNullOrEmpty() || region.isNullOrEmpty() ||
                        barangay.isNullOrEmpty() || streetName.isNullOrEmpty() || postalCode.isNullOrEmpty()) {
                        Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                    } else {
                        val newDeliveryAddress = DeliveryAddress(
                            name,
                            phone,
                            region,
                            barangay,
                            streetName,
                            postalCode,
                            isChecked = false // No checkbox, so default to false
                        )

                        val email = SessionManager.getUserEmail(this)
                        if (email != null) {
                            firestore.collection("users")
                                .whereEqualTo("email", email)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    for (document in querySnapshot.documents) {
                                        val userRef = document.reference
                                        userRef.get()
                                            .addOnSuccessListener { userDocument ->
                                                val deliveryAddressList = userDocument.get("deliveryAddresses") as? MutableList<Map<String, Any>> ?: mutableListOf()

                                                // Add the new address to the end of the list
                                                val updatedDeliveryAddressList = deliveryAddressList.toMutableList().apply {
                                                    add(mapOf(
                                                        "name" to newDeliveryAddress.name,
                                                        "phone" to newDeliveryAddress.phone,
                                                        "region" to newDeliveryAddress.region,
                                                        "barangay" to newDeliveryAddress.barangay,
                                                        "streetName" to newDeliveryAddress.streetName,
                                                        "postalCode" to newDeliveryAddress.postalCode,
                                                        "isChecked" to newDeliveryAddress.isChecked
                                                    ))
                                                }

                                                // Update the database with the modified list
                                                userRef.update("deliveryAddresses", updatedDeliveryAddressList)
                                                    .addOnSuccessListener {
                                                        // Refresh the local list and notify the adapter
                                                        initRefreshDeliveryAddress()
                                                        Toast.makeText(this, "Delivery address added successfully.", Toast.LENGTH_SHORT).show()
                                                        addDialog.dismiss()
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(this, "Failed to add delivery address: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(this, "Error retrieving document: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error querying document: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Log.e("YourActivity", "Invalid email: $email")
                        }
                    }
                }
            }
            addDialog.show()
        }
    }

    fun initRefreshDeliveryAddress() {
        val firestore = FirebaseFirestore.getInstance()
        val email = SessionManager.getUserEmail(this)
        val deliveryAddresses = mutableListOf<DeliveryAddress>()
        val deliveryAddressRecyclerView = findViewById<RecyclerView>(R.id.deliveryAddressRecyclerView)
        deliveryAddressRecyclerView.layoutManager = LinearLayoutManager(this)
        val deliveryAddressAdapter = DeliveryAddressAdapter(deliveryAddresses)
        deliveryAddressRecyclerView.adapter = deliveryAddressAdapter

        if (email != null) {
            firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val userDocument = querySnapshot.documents.first()
                        val deliveryAddressesData = userDocument.get("deliveryAddresses") as? List<HashMap<String, Any>>

                        if (deliveryAddressesData != null) {
                            deliveryAddressesData.forEach { deliveryAddressData ->
                                val name = deliveryAddressData["name"] as? String
                                val phone = deliveryAddressData["phone"] as? String
                                val region = deliveryAddressData["region"] as? String
                                val barangay = deliveryAddressData["barangay"] as? String
                                val streetName = deliveryAddressData["streetName"] as? String
                                val postalCode = deliveryAddressData["postalCode"] as? String
                                val isChecked = deliveryAddressData["isChecked"] as? Boolean

                                if (name != null && phone != null && region != null && barangay != null && streetName != null && postalCode != null && isChecked != null) {
                                    val deliveryAddress = DeliveryAddress(
                                        name,
                                        phone,
                                        region,
                                        barangay,
                                        streetName,
                                        postalCode,
                                        isChecked
                                    )
                                    deliveryAddresses.add(deliveryAddress)
                                } else {
                                    Log.e("MainActivity", "One or more fields are null")
                                }
                            }
                        }
                    }
                    deliveryAddressAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Failed to fetch delivery addresses: ${e.message}")
                }
        } else {
            Log.e("GetUser", "Invalid email: $email")
        }
        deliveryAddressAdapter.setOnItemClickListener(object : DeliveryAddressAdapter.OnItemClickListener {
            override fun onItemClick(deliveryAddress: DeliveryAddress) {

                showEditDialog(deliveryAddress, deliveryAddresses, deliveryAddressAdapter)
            }
        })

    }

    fun showEditDialog(deliveryAddress: DeliveryAddress, deliveryAddresses: MutableList<DeliveryAddress>, deliveryAddressAdapter: DeliveryAddressAdapter) {
        val editDialogView = layoutInflater.inflate(R.layout.edit_delivery_address_dialog, null)
        val editNameDeliveryET = editDialogView.findViewById<EditText>(R.id.nameDeliveryET)
        val editPhoneDeliveryET = editDialogView.findViewById<EditText>(R.id.phoneDeliveryET)
        val editRegionET = editDialogView.findViewById<EditText>(R.id.regionET)
        val editBarangayET = editDialogView.findViewById<EditText>(R.id.barangayET)
        val editStreetNameET = editDialogView.findViewById<EditText>(R.id.streetNameET)
        val editPostalCodeET = editDialogView.findViewById<EditText>(R.id.postalCodeET)
        val editCheckBox = editDialogView.findViewById<CheckBox>(R.id.editCheckBox)
        val closeButton = editDialogView.findViewById<ImageButton>(R.id.closeButton)
        val firestore = FirebaseFirestore.getInstance()

        // Populate the EditText fields with current delivery address details
        editNameDeliveryET.setText(deliveryAddress.name)
        editPhoneDeliveryET.setText(deliveryAddress.phone)
        editRegionET.setText(deliveryAddress.region)
        editBarangayET.setText(deliveryAddress.barangay)
        editStreetNameET.setText(deliveryAddress.streetName)
        editPostalCodeET.setText(deliveryAddress.postalCode)
        editCheckBox.isChecked = deliveryAddress.isChecked

        val editDialog = AlertDialog.Builder(this@MainActivity)
            .setView(editDialogView)
            .setPositiveButton("Update", null)
            .setNegativeButton("Delete", null)
            .create()

        closeButton.setOnClickListener {
            editDialog.dismiss()
        }

        editDialog.setOnShowListener { dialog ->
            val alertDialog = dialog as AlertDialog

            val updateButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
            updateButton.setOnClickListener {
                val newName = editNameDeliveryET.text?.toString()?.trim()
                val newPhone = editPhoneDeliveryET.text?.toString()?.trim()
                val newRegion = editRegionET.text?.toString()?.trim()
                val newBarangay = editBarangayET.text?.toString()?.trim()
                val newStreetName = editStreetNameET.text?.toString()?.trim()
                val newPostalCode = editPostalCodeET.text?.toString()?.trim()
                val newIsChecked = editCheckBox.isChecked

                // Validation: Check if any field is empty
                if (newName.isNullOrEmpty() || newPhone.isNullOrEmpty() || newRegion.isNullOrEmpty() ||
                    newBarangay.isNullOrEmpty() || newStreetName.isNullOrEmpty() || newPostalCode.isNullOrEmpty()) {
                    Toast.makeText(this@MainActivity, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // Validation: Check if phone number is valid
                if (!validatePhoneNumber(newPhone)) {
                    Toast.makeText(this@MainActivity, "Please enter a valid phone number.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val email = SessionManager.getUserEmail(this)
                if (email != null) {
                    firestore.collection("users")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            for (document in querySnapshot.documents) {
                                val userRef = document.reference
                                userRef.get()
                                    .addOnSuccessListener { userDocument ->
                                        val deliveryAddressList = userDocument.get("deliveryAddresses") as? MutableList<Map<String, Any>> ?: mutableListOf()

                                        // Update the delivery address list
                                        val updatedDeliveryAddressList = deliveryAddressList.toMutableList().apply {
                                            val index = indexOfFirst { it["name"] == deliveryAddress.name && it["phone"] == deliveryAddress.phone }
                                            if (index != -1) {
                                                this[index] = mapOf(
                                                    "name" to newName,
                                                    "phone" to newPhone,
                                                    "region" to newRegion,
                                                    "barangay" to newBarangay,
                                                    "streetName" to newStreetName,
                                                    "postalCode" to newPostalCode,
                                                    "isChecked" to newIsChecked
                                                )
                                            }
                                        }

                                        // If the new address is checked, move it to the top and uncheck others
                                        if (newIsChecked) {
                                            val updatedAddresses = updatedDeliveryAddressList.map {
                                                if (it["isChecked"] == true && it["name"] != newName) it.toMutableMap().apply { this["isChecked"] = false } else it
                                            }.toMutableList()

                                            // Find the updated address and move it to the top
                                            val index = updatedAddresses.indexOfFirst { it["name"] == newName && it["phone"] == newPhone }
                                            if (index != -1) {
                                                val updatedAddress = updatedAddresses.removeAt(index)
                                                updatedAddresses.add(0, updatedAddress)
                                            }

                                            // Update the database with the reordered list
                                            userRef.update("deliveryAddresses", updatedAddresses)
                                                .addOnSuccessListener {
                                                    // Update the delivery address in the local list
                                                    deliveryAddress.apply {
                                                        name = newName
                                                        phone = newPhone
                                                        region = newRegion
                                                        barangay = newBarangay
                                                        streetName = newStreetName
                                                        postalCode = newPostalCode
                                                        isChecked = newIsChecked
                                                    }
                                                    // Notify adapter that the data has changed
                                                    deliveryAddressAdapter.notifyDataSetChanged()
                                                    Toast.makeText(this@MainActivity, "Delivery address updated successfully.", Toast.LENGTH_SHORT).show()
                                                    alertDialog.dismiss()
                                                    // Refresh the page to view the changes
                                                    initDeliveryAddressPage()
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(this@MainActivity, "Failed to update delivery address: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        } else {
                                            // Update the database with the modified list without reordering
                                            userRef.update("deliveryAddresses", updatedDeliveryAddressList)
                                                .addOnSuccessListener {
                                                    // Update the delivery address in the local list
                                                    deliveryAddress.apply {
                                                        name = newName
                                                        phone = newPhone
                                                        region = newRegion
                                                        barangay = newBarangay
                                                        streetName = newStreetName
                                                        postalCode = newPostalCode
                                                        isChecked = newIsChecked
                                                    }
                                                    // Notify adapter that the data has changed
                                                    deliveryAddressAdapter.notifyDataSetChanged()
                                                    Toast.makeText(this@MainActivity, "Delivery address updated successfully.", Toast.LENGTH_SHORT).show()
                                                    alertDialog.dismiss()
                                                    // Refresh the page to view the changes
                                                    initDeliveryAddressPage()
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(this@MainActivity, "Failed to update delivery address: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this@MainActivity, "Error retrieving document: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this@MainActivity, "Error querying document: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Log.e("YourActivity", "Invalid email: $email")
                }
            }

        val deleteButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
            deleteButton.setOnClickListener {

                // Show the confirmation delete dialog
                val confirmDeleteDialogView = layoutInflater.inflate(R.layout.confirm_delete_dialog, null)
                val cancelButton = confirmDeleteDialogView.findViewById<Button>(R.id.cancelButton)
                val confirmDeleteButton = confirmDeleteDialogView.findViewById<Button>(R.id.confirmDeleteButton)
                val confirmDeleteDialog = AlertDialog.Builder(this@MainActivity)
                    .setView(confirmDeleteDialogView)
                    .create()

                cancelButton.setOnClickListener {
                    // Dismiss the confirmation dialog
                    confirmDeleteDialog.dismiss()
                }

                confirmDeleteButton.setOnClickListener {
                    // Proceed with deletion
                    val email = SessionManager.getUserEmail(this@MainActivity)
                    if (email != null) {
                        firestore.collection("users")
                            .whereEqualTo("email", email)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                for (document in querySnapshot.documents) {
                                    val userRef = document.reference
                                    userRef.get()
                                        .addOnSuccessListener { userDocument ->
                                            val deliveryAddressList = userDocument.get("deliveryAddresses") as? List<Map<String, Any>>
                                            if (deliveryAddressList != null) {
                                                val updatedDeliveryAddressList = deliveryAddressList.toMutableList().apply {
                                                    val index = indexOfFirst { it["name"] == deliveryAddress.name && it["phone"] == deliveryAddress.phone }
                                                    if (index != -1) {
                                                        removeAt(index)
                                                    }
                                                }
                                                userRef.update("deliveryAddresses", updatedDeliveryAddressList)
                                                    .addOnSuccessListener {
                                                        // Remove the delivery address from the local list
                                                        deliveryAddresses.remove(deliveryAddress)
                                                        // Notify adapter that the data has changed
                                                        deliveryAddressAdapter.notifyDataSetChanged()
                                                        editDialog.dismiss()
                                                        Toast.makeText(this@MainActivity, "Delivery address deleted successfully.", Toast.LENGTH_SHORT).show()

                                                    }
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(this@MainActivity, "Failed to delete delivery address: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                            } else {
                                                Toast.makeText(this@MainActivity, "No matching delivery address found.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this@MainActivity, "Error retrieving document: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@MainActivity, "Error querying document: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Log.e("YourActivity", "Invalid email: $email")
                    }

                    // Dismiss the confirmation dialog
                    confirmDeleteDialog.dismiss()
                }

                confirmDeleteDialog.show()
            }
        }

        editDialog.show()
    }




    fun deleteDeliveryAddress(deliveryAddress: DeliveryAddress, deliveryAddresses: MutableList<DeliveryAddress>, deliveryAddressAdapter: DeliveryAddressAdapter) {
        val firestore = FirebaseFirestore.getInstance()
        val email = SessionManager.getUserEmail(this)

        if (email != null) {
            firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot.documents) {
                        val userRef = document.reference
                        userRef.get()
                            .addOnSuccessListener { userDocument ->
                                val deliveryAddressList = userDocument.get("deliveryAddresses") as? List<Map<String, Any>>
                                if (deliveryAddressList != null) {
                                    val updatedDeliveryAddressList = deliveryAddressList.toMutableList().apply {
                                        val index = indexOfFirst {
                                            it["name"] == deliveryAddress.name &&
                                                    it["phone"] == deliveryAddress.phone &&
                                                    it["region"] == deliveryAddress.region &&
                                                    it["barangay"] == deliveryAddress.barangay &&
                                                    it["streetName"] == deliveryAddress.streetName &&
                                                    it["postalCode"] == deliveryAddress.postalCode
                                        }
                                        if (index != -1) {
                                            removeAt(index)
                                        }
                                    }
                                    userRef.update("deliveryAddresses", updatedDeliveryAddressList)
                                        .addOnSuccessListener {
                                            // Remove the delivery address from the local list
                                            deliveryAddresses.remove(deliveryAddress)
                                            // Notify adapter that the data has changed
                                            deliveryAddressAdapter.notifyDataSetChanged()
                                            Toast.makeText(this@MainActivity, "Delivery address deleted successfully.", Toast.LENGTH_SHORT).show()
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this@MainActivity, "Failed to delete delivery address: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    Toast.makeText(this@MainActivity, "No matching delivery address found.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@MainActivity, "Error retrieving document: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this@MainActivity, "Error querying document: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.e("YourActivity", "Invalid email: $email")
        }
    }



    fun initToPayPage() {
        getOrders()

        Log.d("asda", orders.size.toString())
        val ordersContentView: ScrollView = findViewById(R.id.orders_content)
        val noOrdersContentView: RelativeLayout = findViewById(R.id.no_orders_section)

        ordersContentView.visibility = View.VISIBLE
        noOrdersContentView.visibility = View.GONE
        val ordersRecyclerView: RecyclerView = findViewById(R.id.orders_recycler_view)
        val ordersAdapter = OrdersAdapter(orders)

        ordersRecyclerView.apply {
            adapter = ordersAdapter
            layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
        }


        initBackButton()

        val toShipButton: Button = findViewById(R.id.toShipButton)
        toShipButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_ship, null) as RelativeLayout)
            initToShipPage()

        }

        val toReceiveButton: Button = findViewById(R.id.toReceiveButton)
        toReceiveButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_receive, null) as RelativeLayout)
            initToReceivePage()
            setActivePageIcon(profilePage)
        }

        val toRateButton: Button = findViewById(R.id.toRateButton)
        toRateButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_rate, null) as RelativeLayout)
            initToRatePage()
            setActivePageIcon(profilePage)
        }
    }

    fun initToShipPage() {
        initBackButton()
        val toPayButton: Button = findViewById(R.id.toPayButton)
        toPayButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_pay, null) as RelativeLayout)
            initToPayPage()
            setActivePageIcon(profilePage)
        }

        val toReceiveButton: Button = findViewById(R.id.toReceiveButton)
        toReceiveButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_receive, null) as RelativeLayout)
            initToReceivePage()
            setActivePageIcon(profilePage)

        }

        val toRateButton: Button = findViewById(R.id.toRateButton)
        toRateButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_rate, null) as RelativeLayout)
            initToRatePage()
            setActivePageIcon(profilePage)
        }
    }

    fun initToReceivePage() {
        initBackButton()
        val toPayButton: Button = findViewById(R.id.toPayButton)
        toPayButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_pay, null) as RelativeLayout)
            initToReceivePage()
            setActivePageIcon(profilePage)
        }

        val toShipButton: Button = findViewById(R.id.toShipButton)
        toShipButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_ship, null) as RelativeLayout)
            initToShipPage()
            setActivePageIcon(profilePage)
        }

        val toRateButton: Button = findViewById(R.id.toRateButton)
        toRateButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_rate, null) as RelativeLayout)
            initToRatePage()
            setActivePageIcon(profilePage)
        }
        
    }

    fun initToRatePage() {
        initBackButton()
        val toPayButton: Button = findViewById(R.id.toPayButton)
        toPayButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_pay, null) as RelativeLayout)
            initToPayPage()
            setActivePageIcon(profilePage)
        }

        val toShipButton: Button = findViewById(R.id.toShipButton)
        toShipButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_ship, null) as RelativeLayout)
            initToShipPage()
            setActivePageIcon(profilePage)
        }

        val toReceiveButton: Button = findViewById(R.id.toReceiveButton)
        toReceiveButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_receive, null) as RelativeLayout)
            initToReceivePage()
            setActivePageIcon(profilePage)
        }
    }

    fun initSettingsPage(){
        val logoutButton: RelativeLayout = findViewById(R.id.logout_button)
        val aboutButton: RelativeLayout = findViewById(R.id.aboutButton)
        val rateUsButton: RelativeLayout = findViewById(R.id.rateUsButton)
        val changePassButton: RelativeLayout = findViewById(R.id.changePasswordButton)
        val email = SessionManager.getUserEmail(this)
        val nameTV: TextView = findViewById(R.id.nameTV)

        if (email != null) {
            AuthUtility.getUserName(email,
                onSuccess = { name ->
                    nameTV.text = name
                },
                onFailure = {
                    Log.e("GetUser", "Failed to retrieve user name")
                }
            )
        } else {
            Log.e("GetUser", "Invalid email: $email")
        }

        initBackButton()
        // Navigation Logic
        val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
        val confirmLogoutDialogView = layoutInflater.inflate(R.layout.confirm_logout_dialog, null)
        val cancelButton = confirmLogoutDialogView.findViewById<Button>(R.id.cancelButton)
        val confirmLogoutButton = confirmLogoutDialogView.findViewById<Button>(R.id.confirmLogoutButton)

        logoutButton.setOnClickListener {

            val confirmLogoutDialogView = layoutInflater.inflate(R.layout.confirm_logout_dialog, null)
            val cancelButton = confirmLogoutDialogView.findViewById<Button>(R.id.cancelButton)
            val confirmLogoutButton = confirmLogoutDialogView.findViewById<Button>(R.id.confirmLogoutButton)

            val confirmLogoutDialog = AlertDialog.Builder(this@MainActivity)
                .setView(confirmLogoutDialogView)
                .create()

            cancelButton.setOnClickListener {
                confirmLogoutDialog.dismiss()
            }

            confirmLogoutButton.setOnClickListener {
                confirmLogoutDialog.dismiss()
                AuthUtility.signOut(this@MainActivity)
                val intent = Intent(this@MainActivity, LoginRegistrationActivity::class.java)
                startActivity(intent)
                finish()
            }

            confirmLogoutDialog.show()
        }


        aboutButton.setOnClickListener {
            activePage = (R.layout.activity_about)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_about, null) as RelativeLayout)
            initAboutPage()
            setActivePageIcon(profilePage)

        }
        changePassButton.setOnClickListener {
            activePage = (R.layout.activity_change_password)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_change_password, null) as RelativeLayout)
            initChangePassPage()
            setActivePageIcon(profilePage)
        }
        rateUsButton.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=")))
            } catch (e: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store")))
            }
        }
    }
    fun initChangePassPage() {
        val backButton: ImageButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_settings, null) as RelativeLayout)
            initSettingsPage()
            setActivePageIcon(profilePage)
        }

        val editCurrentPass: EditText = findViewById(R.id.editCurrentPass)
        val viewPasswordButton: ImageButton = findViewById(R.id.viewPasswordButton)
        var isPasswordVisible = false

        val editNewPassword: EditText = findViewById(R.id.editNewPassword)
        val viewPasswordButton1: ImageButton = findViewById(R.id.viewPasswordButton1)
        var isNewPasswordVisible = false

        val editConfirmNewPassword: EditText = findViewById(R.id.logoutLabel)
        val viewPasswordButton2: ImageButton = findViewById(R.id.viewPasswordButton2)
        var isConfirmPasswordVisible = false

        viewPasswordButton.setOnClickListener {
            togglePasswordVisibility(editCurrentPass, viewPasswordButton, isPasswordVisible)
            isPasswordVisible = !isPasswordVisible
        }

        viewPasswordButton1.setOnClickListener {
            togglePasswordVisibility(editNewPassword, viewPasswordButton1, isNewPasswordVisible)
            isNewPasswordVisible = !isNewPasswordVisible
        }

        viewPasswordButton2.setOnClickListener {
            togglePasswordVisibility(editConfirmNewPassword, viewPasswordButton2, isConfirmPasswordVisible)
            isConfirmPasswordVisible = !isConfirmPasswordVisible
        }
    }

    private fun togglePasswordVisibility(editText: EditText, button: ImageButton, isVisible: Boolean) {
        if (isVisible) {
            // Hide the password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            button.setBackgroundResource(R.drawable.close_eye_icon)
        } else {
            // Show the password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            button.setBackgroundResource(R.drawable.open_eye_icon)
        }
        // Move cursor to the end of the text
        editText.setSelection(editText.text.length)
    }

    fun clearCache() {
        try {
            val dir: File = cacheDir
            if (deleteDir(dir)) {
                println("Cache cleared successfully")
                Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show()
            } else {
                println("Cache clearing failed")
                Toast.makeText(this, "Cache clearing failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children: Array<String> = dir.list() ?: return false
            for (child in children) {
                val success = deleteDir(File(dir, child))
                if (!success) {
                    return false
                }
            }
            return dir.delete()
        } else if (dir != null && dir.isFile) {
            return dir.delete()
        } else {
            return false
        }
    }

    fun initAboutPage(){
        val backButton: ImageButton = findViewById(R.id.backButton)
        val clearCacheButton: Button = findViewById(R.id.clearCacheButton)
        val verUpdateButton: Button = findViewById(R.id.verUpdateButton)

        backButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_settings, null) as RelativeLayout)
            initSettingsPage()
            setActivePageIcon(profilePage)
        }

        clearCacheButton.setOnClickListener {
            clearCache()
        }

        verUpdateButton.setOnClickListener {
            Toast.makeText(this, "No Updates Available!", Toast.LENGTH_SHORT).show()
        }

    }

    fun initBackButton(){
        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_profile, null) as RelativeLayout)
            initProfilePage()
            setActivePageIcon(profilePage)
        }
    }

    fun initProfileBackButton(){
        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_edit_account, null) as RelativeLayout)
            initProfileEditPage()
            setActivePageIcon(profilePage)
        }
    }

    fun initProfileEditPage(){
        val changeNameButton: RelativeLayout = findViewById(R.id.changeNameButton)
        val changeGenderButton: RelativeLayout = findViewById(R.id.changeGenderButton)
        val changeBdayButton: RelativeLayout = findViewById(R.id.changeBdayButton)
        val changePhoneButton: RelativeLayout = findViewById(R.id.changePhoneButton)
        val changeEmailButton: RelativeLayout = findViewById(R.id.changeEmailButton)
        val saveButton: ImageButton = findViewById(R.id.applyChangesBtn)
        val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
        val nameTV: TextView = findViewById(R.id.nameTV)
        val bdayTV: TextView = findViewById(R.id.bdayTV)
        val genderTV: TextView = findViewById(R.id.genderTV)
        val phoneTV: TextView = findViewById(R.id.phoneTV)
        val emailTV: TextView = findViewById(R.id.emailTV)
        val email = SessionManager.getUserEmail(this)

        if (email != null) {
            val censoredEmail = censorEmail(email)
            AuthUtility.getUserName(email,
                onSuccess = { name ->
                    nameTV.text = name
                    emailTV.text = censoredEmail
                },
                onFailure = {
                    Log.e("GetUser", "Failed to retrieve user name")
                }
            )
        } else {
            Log.e("GetUser", "Invalid email: $email")
        }

        initBackButton()
        changeNameButton.setOnClickListener {
            activePage = (R.layout.activity_edit_name)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_edit_name, null) as RelativeLayout)
            initProfileBackButton()
            initEditNamePage()
        }


        changeGenderButton.setOnClickListener {
            // Inflate the custom layout/view
            val dialogView = layoutInflater.inflate(R.layout.activity_edit_gender_dialog, null)

            // Build the dialog
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Confirm") { dialogInterface, which ->
                    val radioGroup = dialogView.findViewById<RadioGroup>(R.id.genderRadioGroup)
                    val selectedGenderId = radioGroup.checkedRadioButtonId
                    val selectedGender = when (selectedGenderId) {
                        R.id.maleButton -> "Male"
                        R.id.femaleButton -> "Female"
                        R.id.otherButton -> "Other"
                        else -> "Unknown"
                    }

                    val firestore = FirebaseFirestore.getInstance()

                    val userData = hashMapOf(
                        "gender" to selectedGender
                    )

                    val email = SessionManager.getUserEmail(this)
                    if (email != null) {
                        firestore.collection("users")
                            .whereEqualTo("email", email)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                for (document in querySnapshot.documents) {
                                    document.reference.update(userData as Map<String, Any>)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Gender updated successfully.", Toast.LENGTH_SHORT).show()
                                            genderTV.text = selectedGender // Display the selected gender in the genderTV TextView
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this, "Failed to update gender: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error querying document: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Log.e("GetUser", "Invalid email: $email")
                    }
                }
                .setNegativeButton("Cancel") { dialogInterface, which ->
                    dialogInterface.dismiss()
                }
                .create()

            // Show the dialog
            dialog.show()
        }

        changeBdayButton.setOnClickListener {
            // Inflate the custom layout/view
            val dialogView = layoutInflater.inflate(R.layout.activity_edit_birthday_dialog, null)

            // Declare the datePicker variable
            val datePicker = dialogView.findViewById<DatePicker>(R.id.datePicker)

            // Calculate the maximum date (current year - 13 years)
            val cal = Calendar.getInstance()
            cal.add(Calendar.YEAR, -13)
            val maxDate = cal.time.time

            // Set the minimum date to January 1, 1900
            val minDate = Calendar.getInstance()
            minDate.set(1900, Calendar.JANUARY, 1)
            val minDateMillis = minDate.time.time

            // Set the minimum and maximum dates for the DatePicker
            datePicker.minDate = minDateMillis
            datePicker.maxDate = maxDate

            // Retrieve the saved birthday from the database
            val firestore = FirebaseFirestore.getInstance()
            val email = SessionManager.getUserEmail(this)
            if (email != null) {
                firestore.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val document = querySnapshot.documents.first()
                            val savedBirthday = document.getString("birthday")
                            if (savedBirthday != null) {
                                val savedBirthdayParts = savedBirthday.split("/")
                                if (savedBirthdayParts.size == 3) {
                                    val savedMonth = savedBirthdayParts[0].toInt()
                                    val savedDay = savedBirthdayParts[1].toInt()
                                    val savedYear = savedBirthdayParts[2].toInt()

                                    // Set the initial date on the DatePicker
                                    datePicker.updateDate(savedYear, savedMonth - 1, savedDay)
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("GetUser", "Error retrieving birthday: ${e.message}", e)
                    }
            } else {
                Log.e("GetUser", "Invalid email: $email")
            }

            // Create and show the AlertDialog
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Confirm") { dialogInterface, which ->
                    val month = datePicker.month + 1 // DatePicker months are zero-based, so we add 1 to get the correct month
                    val day = datePicker.dayOfMonth
                    val year = datePicker.year % 100 // Get the last two digits of the year

                    val birthday = String.format("%02d/%02d/%02d", month, day, year) // Format the birthday as "mm/dd/yy"

                    val firestore = FirebaseFirestore.getInstance()

                    val userData = hashMapOf(
                        "birthday" to birthday
                    )

                    val email = SessionManager.getUserEmail(this)
                    if (email != null) {
                        firestore.collection("users")
                            .whereEqualTo("email", email)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                for (document in querySnapshot.documents) {
                                    document.reference.update(userData as Map<String, Any>)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Birthday updated successfully.", Toast.LENGTH_SHORT).show()
                                            bdayTV.text = birthday // Display the birthday in the bdayTV TextView
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this, "Failed to update birthday: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error querying document: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Log.e("GetUser", "Invalid email: $email")
                    }
                }
                .setNegativeButton("Cancel") { dialogInterface, which ->
                    dialogInterface.dismiss()
                }
                .create()

            // Show the dialog
            dialog.show()
        }


        changePhoneButton.setOnClickListener {
            activePage = (R.layout.activity_edit_phone)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_edit_phone, null) as RelativeLayout)
            initProfileBackButton()
            initEditPhonePage()


        }

        changeEmailButton.setOnClickListener {
            activePage = (R.layout.activity_edit_email)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_edit_email, null) as RelativeLayout)
            initBackButton()
            initProfileBackButton()
            initEditEmailPage()
        }

        saveButton.setOnClickListener {
            activePage = (R.layout.activity_profile)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_profile, null) as RelativeLayout)
            initProfilePage()
        }

        val profilePic = findViewById<ImageButton>(R.id.profilePic)

        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents.first()
                    val image = document.getString("image")
                    if (image != null) {
                        Glide.with(this)
                            .load(image)
                            .into(profilePic)
                    }
                    val phoneNum = document.getString("phoneNumber")
                    if (phoneNum != null) {
                        phoneTV.text = censorPhoneNumber(phoneNum)
                    }
                    val gender = document.getString("gender")
                    if (gender != null) {
                        genderTV.text = gender // Display the saved gender in the genderTV TextView
                    }
                    val birthday = document.getString("birthday")
                    if (birthday != null) {
                        val birthdayParts = birthday.split("/")
                        if (birthdayParts.size == 3) {
                            val month = birthdayParts[0].toInt()
                            val day = birthdayParts[1].toInt()
                            val year = birthdayParts[2].toInt()
                            val formattedBirthday = String.format("%02d/%02d/%02d", month, day, year) // Format the saved birthday as "mm/dd/yy"
                            bdayTV.text = formattedBirthday // Display the saved birthday in the bdayTV TextView
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("GetUser", "Error getting user details: ${e.message}", e)
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error getting user name", e)
            }
        profilePic.setOnClickListener {
            val email = SessionManager.getUserEmail(this)

            if (email != null) {
                showImageSelectionDialog(email)
            } else {
                Log.e("GetUser", "Invalid email: $email")
            }
        }

        val editButton: ImageButton = findViewById(R.id.editButton)

        editButton.setOnClickListener {
            if (email != null) {
                showImageSelectionDialog(email)
            } else {
                Log.e("GetUser", "Invalid email: $email")
            }
        }



    }
    fun censorEmail(email: String): String {
        if (email.length <= 6) return email // Email too short to censor
        val atIndex = email.indexOf('@')
        if (atIndex == -1 || atIndex <= 2) return email // Invalid email format

        val firstLetter = email.first()
        val lastLetter = email[atIndex - 1]
        val domain = email.substring(atIndex)

        return "$firstLetter*****$lastLetter$domain"
    }

    fun refreshProfile(){
        val profilePic = findViewById<ImageButton>(R.id.profilePic)
        val email = SessionManager.getUserEmail(this)
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents.first()
                    val image = document.getString("image")
                    if (image != null) {
                        Glide.with(this)
                            .load(image)
                            .into(profilePic)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("TAG", "Error getting user name", e)
            }
    }


    fun showImageSelectionDialog(userEmail: String) {
        val images = arrayOf(
            R.drawable.profile_pic1,
            R.drawable.profile_pic2,
            R.drawable.profile_pic3,
            R.drawable.profile_pic4,
            R.drawable.profile_pic5,
            R.drawable.profile_pic6,
            R.drawable.profile_pic7,
            R.drawable.profile_pic8,
            R.drawable.profile_pic9,
            R.drawable.profile_pic10
        )

        val firestore = FirebaseFirestore.getInstance()

        class ProfileImageAdapter(private val images: Array<Int>, private val onItemClick: (position: Int) -> Unit) :
            RecyclerView.Adapter<ProfileImageAdapter.ViewHolder>() {

            var selectedPosition: Int? = null

            inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                private val imageView: ImageView = itemView.findViewById(R.id.profileImage)
                private val checkBox: CheckBox = itemView.findViewById(R.id.checkbox1)

                fun bind(image: Int) {
                    imageView.setImageResource(image)
                    val position = adapterPosition
                    val isSelected = position == selectedPosition
                    itemView.isActivated = isSelected
                    checkBox.isChecked = isSelected

                    itemView.setOnClickListener {
                        if (isSelected) {
                            checkBox.isChecked = false
                            selectedPosition = null
                        } else {
                            selectedPosition = position
                            onItemClick(position)
                            notifyDataSetChanged()
                        }
                    }
                }
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.profile_image_item, parent, false)
                return ViewHolder(view)
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                val image = images[position]
                holder.bind(image)
            }

            override fun getItemCount(): Int {
                return images.size
            }
        }

        val dialogView = layoutInflater.inflate(R.layout.profile_selection_dialog, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.profileImageRecyclerView)

        val adapter = ProfileImageAdapter(images) { position ->
            // Handle the onItemClick event here if needed
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Save") { dialogInterface, which ->
                val selectedImageDrawable = images[adapter.selectedPosition ?: return@setPositiveButton]
                val selectedBitmap = BitmapFactory.decodeResource(resources, selectedImageDrawable)

                // Convert Bitmap to byte array
                val baos = ByteArrayOutputStream()
                selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val imageData = baos.toByteArray()

                // Upload image data to Firestore
                val storageRef = Firebase.storage.reference.child("profile_images/${userEmail}.png")
                val uploadTask = storageRef.putBytes(imageData)

                uploadTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            // Save the image URL to Firestore
                            val imageUrl = uri.toString()
                            saveSelectedImage(userEmail, imageUrl, firestore)
                        }.addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to get download URL: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Failed to upload image: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    // Function to save selected image URL to Firestore
    private fun saveSelectedImage(userEmail: String, imageUrl: String, firestore: FirebaseFirestore) {
        val selectedImageData = mapOf(
            "image" to imageUrl
        )

        firestore.collection("users")
            .whereEqualTo("email", userEmail)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val documentSnapshot = querySnapshot.documents[0]
                    val documentId = documentSnapshot.id
                    firestore.collection("users")
                        .document(documentId)
                        .update(selectedImageData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Image saved successfully.", Toast.LENGTH_SHORT).show()
                            refreshProfile()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "User with email $userEmail not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error retrieving user document: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    fun initPaymentMethodsPage() {
        val cashLayout = findViewById<RelativeLayout>(R.id.cashLayout)
        val paypalLayout = findViewById<RelativeLayout>(R.id.paypalLayout)

        val gcashLayout = findViewById<RelativeLayout>(R.id.gcashLayout)

    }



    fun initEditNamePage() {
        val email = SessionManager.getUserEmail(this)
        val editNameET: EditText = findViewById(R.id.editNameET)
        val applyChangesBtn: ImageButton = findViewById(R.id.applyChangesBtn)

        if (email != null) {
            AuthUtility.getUserName(email,
                onSuccess = { name ->
                    editNameET.setText(name) // Set the user's current name in the EditText
                },
                onFailure = {
                    Log.e("GetUser", "Failed to retrieve user name")
                }
            )

            applyChangesBtn.setOnClickListener {
                val newName = editNameET.text.toString() // Get the updated name from the EditText
                val firestore = FirebaseFirestore.getInstance()

                if (newName.isNullOrEmpty() || newName.length > 100) {
                    Toast.makeText(this, "Please enter a valid name (1-100 characters).", Toast.LENGTH_SHORT).show()
                } else {
                    val userData = hashMapOf(
                        "name" to newName
                    )

                    firestore.collection("users")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            for (document in querySnapshot.documents) {
                                document.reference.update(userData as Map<String, Any>)
                                    .addOnSuccessListener {
                                        if (newName != document.getString("name")) {
                                            Toast.makeText(this, "Name updated successfully.", Toast.LENGTH_SHORT).show()
                                            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
                                            pageContainer.removeAllViews()
                                            pageContainer.addView(layoutInflater.inflate(R.layout.activity_edit_account, null) as RelativeLayout)
                                            initProfileEditPage()
                                        } else {
                                            Toast.makeText(this, "No new changes to the name.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Failed to update name: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error querying document: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        } else {
            Log.e("GetUser", "Invalid email: $email")
        }
    }

    fun initEditEmailPage() {
        val email = SessionManager.getUserEmail(this)
        val editEmailET: TextView = findViewById(R.id.editEmailET)
        val changeEmailButton: Button = findViewById(R.id.changeEmailButton)

        if (email != null) {
            changeEmailButton.setOnClickListener {
                val newEmail = editEmailET.text.toString().trim() // Get the updated email from the TextView and remove leading/trailing whitespaces

                if (newEmail.isNullOrEmpty() || !isEmailValid(newEmail)) {
                    Toast.makeText(this, "Invalid email format. Please enter a valid email.", Toast.LENGTH_SHORT).show()
                } else if (newEmail != email) {
                    val firestore = FirebaseFirestore.getInstance()

                    // Check if the new email is already used in another account
                    firestore.collection("users")
                        .whereEqualTo("email", newEmail)
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            if (querySnapshot.isEmpty) {
                                // Update email in Firebase Authentication
                                val user = FirebaseAuth.getInstance().currentUser
                                user?.updateEmail(newEmail)?.addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val userData = hashMapOf(
                                            "email" to newEmail
                                        )

                                        firestore.collection("users")
                                            .whereEqualTo("email", email)
                                            .get()
                                            .addOnSuccessListener { querySnapshot ->
                                                for (document in querySnapshot.documents) {
                                                    document.reference.update(userData as Map<String, Any>)
                                                        .addOnSuccessListener {
                                                            Toast.makeText(this, "Email updated successfully.", Toast.LENGTH_SHORT).show()
                                                            // Update the email in the session manager
                                                            SessionManager.setUserEmail(this, newEmail)
                                                            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
                                                            pageContainer.removeAllViews()
                                                            pageContainer.addView(layoutInflater.inflate(R.layout.activity_edit_account, null) as RelativeLayout)
                                                            initProfileEditPage()
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Toast.makeText(this, "Failed to update email in Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                }
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(this, "Error querying document in Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    } else {
                                        Toast.makeText(this, "Failed to update email in Authentication: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(this, "Email is already used in another account.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error querying document in Firestore: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Input email is the same as the current email.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Log.e("GetUser", "Invalid email: $email")
        }
    }


    private fun isEmailValid(email: String): Boolean {
        val pattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        return pattern.matches(email)
    }

    fun initEditPhonePage() {
        val email = SessionManager.getUserEmail(this)
        val editPhoneET: EditText = findViewById(R.id.editPhoneET)
        val changePhoneButton: Button = findViewById(R.id.changePhoneButton)

        if (email != null) {
            val firestore = FirebaseFirestore.getInstance()

            firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents.first()
                        val phoneNum = document.getString("phoneNumber")
                        if (phoneNum != null) {
//                            editPhoneET.setText(phoneNum)
                        }

                        changePhoneButton.setOnClickListener {
                            val newPhoneNumber = editPhoneET.text.toString().trim() // Get the updated phone number from the EditText
                            val isValidPhoneNumber = validatePhoneNumber(newPhoneNumber)

                            if (newPhoneNumber.isNullOrEmpty()) {
                                Toast.makeText(this, "Phone number cannot be empty.", Toast.LENGTH_SHORT).show()
                            } else if (!isValidPhoneNumber) {
                                Toast.makeText(this, "Invalid phone number format. Please enter a valid phone number.", Toast.LENGTH_SHORT).show()
                            } else if (newPhoneNumber != phoneNum) {
                                val firestore = FirebaseFirestore.getInstance()

                                // Check if the new phone number is already used in another account
                                firestore.collection("users")
                                    .whereEqualTo("phoneNumber", newPhoneNumber)
                                    .get()
                                    .addOnSuccessListener { querySnapshot ->
                                        if (querySnapshot.isEmpty) {
                                            val userData = hashMapOf(
                                                "phoneNumber" to newPhoneNumber
                                            )

                                            document.reference.update(userData as Map<String, Any>)
                                                .addOnSuccessListener {
                                                    Toast.makeText(this, "Phone number updated successfully.", Toast.LENGTH_SHORT).show()
                                                    val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
                                                    pageContainer.removeAllViews()
                                                    pageContainer.addView(layoutInflater.inflate(R.layout.activity_edit_account, null) as RelativeLayout)
                                                    initProfileEditPage()
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(this, "Failed to update phone number: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        } else {
                                            Toast.makeText(this, "Phone number is already used in another account.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error querying document: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(this, "Input phone number is same current phone number.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Log.e("GetUser", "No document found for email: $email")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("GetUser", "Error getting phone number: ${e.message}", e)
                }
        } else {
            Log.e("GetUser", "Invalid email: $email")
        }
    }
    // Function to censor phone number
    fun censorPhoneNumber(phoneNumber: String): String {
        return if (phoneNumber.length > 3) {
            val visiblePart = phoneNumber.takeLast(3)
            val censoredPart = "*".repeat(phoneNumber.length - 3)
            "$censoredPart$visiblePart"
        } else {
            phoneNumber // If the phone number is too short to censor, return as is.
        }
    }


    private fun validatePhoneNumber(phoneNumber: String): Boolean {
        val phonePattern = Regex("[0-9]{10}") // Assumes a 10-digit phone number format
        return phonePattern.matches(phoneNumber)
    }
    fun countFurnitureOccurrences(furnitureArray: Array<String>): HashMap<String, Int> {
        val furnitureCountMap = HashMap<String, Int>()

        for (furniture in furnitureArray) {
            if (furniture.isNotBlank()) {
                val count = furnitureCountMap.getOrDefault(furniture, 0)
                furnitureCountMap[furniture] = count + 1
            }
        }

        return furnitureCountMap
    }


}