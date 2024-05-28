package com.alvarez.furnivisionapp
import com.alvarez.furnivisionapp.utils.DeliveryAddressAdapter
import com.alvarez.furnivisionapp.utils.CartListAdapter
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraManager
import android.icu.text.DecimalFormat
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Im
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
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alvarez.furnivisionapp.data.AuthUtility
import com.alvarez.furnivisionapp.data.CartItem
import com.alvarez.furnivisionapp.data.Furniture
import com.alvarez.furnivisionapp.data.Order
import com.alvarez.furnivisionapp.data.Shop
import com.alvarez.furnivisionapp.data.DeliveryAddress
import com.alvarez.furnivisionapp.utils.CameraFunctions
import com.alvarez.furnivisionapp.utils.HomePageFunctions
import com.alvarez.furnivisionapp.utils.ShopListAdapter
import com.alvarez.furnivisionapp.data.SessionManager
import com.alvarez.furnivisionapp.data.ShopCart
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var homePageFunctions: HomePageFunctions
    private lateinit var cameraFunctions: CameraFunctions
    private var activePage: Int? = null
    private lateinit var activeButton: LinearLayout
    private lateinit var database: FirebaseFirestore
    private var cartList: MutableList<ShopCart>? = mutableListOf()

    companion object {
        private const val ONE_MEGABYTE: Long = 1024 * 1024
        private val PRICE_FORMAT = DecimalFormat("#0.00")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Navigation Buttons
        val homeButton: LinearLayout = findViewById(R.id.home_menu)
        val shopButton: LinearLayout = findViewById(R.id.shop_menu)
        val cameraButton: LinearLayout = findViewById(R.id.try_ar_menu)
        val cartButton: LinearLayout = findViewById(R.id.cart_menu)
        val profileButton: LinearLayout = findViewById(R.id.profile_menu)

        activeButton = homeButton

        // Layouts
        val dashboardPage = R.layout.activity_dashboard
        val shopPage = R.layout.activity_shop
        val cameraPage = R.layout.activity_camera
        val cartPage = R.layout.activity_cart
        val profilePage = R.layout.activity_profile
        activePage = dashboardPage

        // Navigation Logic
        val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
        var inflatedPage = layoutInflater.inflate(dashboardPage, null) as RelativeLayout
        pageContainer.addView(inflatedPage)
        initHomePage(inflatedPage, pageContainer)

        homeButton.setOnClickListener {
            activePage = dashboardPage
            pageContainer.removeAllViews()
            inflatedPage = layoutInflater.inflate(dashboardPage, null) as RelativeLayout
            pageContainer.addView(inflatedPage)
            initHomePage(inflatedPage, pageContainer)
        }
        shopButton.setOnClickListener {
            activePage = shopPage
            pageContainer.removeAllViews()
            inflatedPage = layoutInflater.inflate(shopPage, null) as RelativeLayout
            pageContainer.addView(inflatedPage)
            initShopPage(pageContainer)
        }
        cameraButton.setOnClickListener {
            activePage = cameraPage
            pageContainer.removeAllViews()
            inflatedPage = layoutInflater.inflate(cameraPage, null) as RelativeLayout
            pageContainer.addView(inflatedPage)
            initCameraPage()
        }
        cartButton.setOnClickListener {
            activePage = cartPage
            pageContainer.removeAllViews()
            inflatedPage = layoutInflater.inflate(cartPage, null) as RelativeLayout
            pageContainer.addView(inflatedPage)
            initCartPage(pageContainer)
        }
        profileButton.setOnClickListener {
            activePage = profilePage
            pageContainer.removeAllViews()
            inflatedPage = layoutInflater.inflate(profilePage, null) as RelativeLayout
            pageContainer.addView(inflatedPage)
            initProfilePage()
        }
        //toggle change favorite icons
        val heartFill1 : ImageButton = findViewById(R.id.fav1_heart)
        val heartFill2 : ImageButton = findViewById(R.id.fav2_heart)
        val heartFill3 : ImageButton = findViewById(R.id.fav3_heart)
        val heart1 : ImageButton = findViewById(R.id.pop1_heart)
        val heart2 : ImageButton = findViewById(R.id.pop2_heart)
        val heart3 : ImageButton = findViewById(R.id.pop3_heart)

        heartFill1.setOnClickListener { toggleHeart(heartFill1) }
        heartFill2.setOnClickListener { toggleHeart(heartFill2) }
        heartFill3.setOnClickListener { toggleHeart(heartFill3) }
        heart1.setOnClickListener { toggleHeart(heart1) }
        heart2.setOnClickListener { toggleHeart(heart2) }
        heart3.setOnClickListener { toggleHeart(heart3) }
    }

    private fun initHomePage(page: RelativeLayout, pageContainer: ViewGroup) {
        homePageFunctions = HomePageFunctions(page, AppCompatImageButton::class.java, this)

    }
    fun toggleHeart(heartButton: ImageButton) {
        val isFilled = heartButton.tag as? Boolean ?: false
        if (isFilled) {
            heartButton.setImageResource(R.drawable.heart_empty)
            heartButton.tag = false
        } else {
            heartButton.setImageResource(R.drawable.heart)
            heartButton.tag = true
        }
    }

    private fun initShopPage(pageContainer: ViewGroup) {
        database = FirebaseFirestore.getInstance()
        database.collection("shops")
            .get()
            .addOnSuccessListener { result: QuerySnapshot ->
                val shopList: List<Shop> = result.documents.mapNotNull { document ->
                    document.toObject(Shop::class.java)?.apply {
                        id = document.id
                    }
                }
                val shopsView: RecyclerView = findViewById(R.id.shop_list)

                val adapter = ShopListAdapter(shopList)

                adapter.setOnItemClickListener (object : ShopListAdapter.OnItemClickListener {
                    @SuppressLint("InflateParams")
                    override fun onItemClick(shopID: String) {
                        pageContainer.removeAllViews()
                        var inflatedPage = layoutInflater.inflate(R.layout.activity_furniture_selection, null) as RelativeLayout
                        pageContainer.addView(inflatedPage)
                        initFurniSelectPage(shopID, pageContainer)
                    }
                })

                shopsView.adapter = adapter
                val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
                shopsView.layoutManager = layoutManager

            }
            .addOnFailureListener { exception: Exception ->
                // Handle the error here
                Log.d(TAG,"Error fetching documents: $exception")
            }



    }

    @SuppressLint("SetTextI18n", "DefaultLocale", "CommitPrefEdits", "InflateParams")
    private fun initFurniSelectPage(shopID: String, pageContainer: ViewGroup){
        var index: Int = 0
        database = FirebaseFirestore.getInstance()

        database.collection("furniture").whereEqualTo("shopID", shopID).get()
            .addOnSuccessListener { result: QuerySnapshot ->
                val furnitures: List<Furniture> = result.documents.mapNotNull { document ->
                    document.toObject(Furniture::class.java)?.apply {
                        id = document.id
                    }
                }

                Log.d("Open Cart", cartList.toString())

                var addToCartBtn: Button = findViewById(R.id.addToCartButton)
                var nextBtn: ImageButton = findViewById(R.id.nextBtn)
                var prevBtn: ImageButton = findViewById(R.id.previousBtn)

                var imageView: ImageView = findViewById(R.id.furnitureImage)
                var nameTextView: TextView = findViewById(R.id.furnitureName)
                var descTextView: TextView = findViewById(R.id.furnitureDesc)
                var priceTextView: TextView = findViewById(R.id.furniturePrice)
                var dimensionsTextView: TextView = findViewById(R.id.furnitureDimensions)
                var stocksTextView: TextView = findViewById(R.id.furnitureStocks)



                val storageReference = furnitures[index].img.let { it?.let { it1 ->
                    Firebase.storage.getReferenceFromUrl(
                        it1
                    )
                } }

                storageReference?.getBytes(ONE_MEGABYTE)?.addOnSuccessListener { bytes ->
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    imageView.setImageBitmap(bitmap)
                }
                nameTextView.text = furnitures[index].name
                descTextView.text = furnitures[index].description
                priceTextView.text = "$ " + PRICE_FORMAT.format(furnitures[index].price)
                dimensionsTextView.text = getString(R.string.dimensions) + " " + furnitures[index].dimensions
                stocksTextView.text = getString(R.string.stock) + " " + furnitures[index].stocks.toString()


                nextBtn.setOnClickListener {
                    if (index != furnitures.size-1) {
                        index++
                        val storageRef = furnitures[index].img.let { it?.let { it1 ->
                            Firebase.storage.getReferenceFromUrl(
                                it1
                            )
                        } }

                        storageRef?.getBytes(ONE_MEGABYTE)?.addOnSuccessListener { bytes ->
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            imageView.setImageBitmap(bitmap)
                        }

                        val price = furnitures[index].price?.toDouble() ?: 0.0
                        nameTextView.text = furnitures[index].name
                        descTextView.text = furnitures[index].description
                        priceTextView.text = "$ " + PRICE_FORMAT.format(price)
                        dimensionsTextView.text = getString(R.string.dimensions) + " " + furnitures[index].dimensions
                        stocksTextView.text = getString(R.string.stock) + " " + furnitures[index].stocks.toString()
                    }
                }
                prevBtn.setOnClickListener {
                    if (index > 0) {
                        index--
                        val storageRef1 = furnitures[index].img.let { it?.let { it1 ->
                            Firebase.storage.getReferenceFromUrl(
                                it1
                            )
                        } }

                        storageRef1?.getBytes(ONE_MEGABYTE)?.addOnSuccessListener { bytes ->
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            imageView.setImageBitmap(bitmap)
                        }

                        val price = furnitures[index].price?.toDouble() ?: 0.0
                        nameTextView.text = furnitures[index].name
                        descTextView.text = furnitures[index].description
                        priceTextView.text = "$ " + PRICE_FORMAT.format(price)
                        dimensionsTextView.text = getString(R.string.dimensions) + " " + furnitures[index].dimensions
                        stocksTextView.text = getString(R.string.stock) + " " + furnitures[index].stocks.toString()
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
                                Log.e("ERRO", "Error getting document", exception)
                            }
                    }

                    Toast.makeText(this, "Furniture is added to Cart", Toast.LENGTH_SHORT).show()
                }
            }

        val backBtn: ImageButton = findViewById(R.id.backBtn)

        backBtn.setOnClickListener{
            pageContainer.removeAllViews()
            val inflatedPage: RelativeLayout = layoutInflater.inflate(R.layout.activity_shop, null) as RelativeLayout
            pageContainer.addView(inflatedPage)
            initShopPage(pageContainer)
        }
    }
    private fun addItemToCart(selectedShop: Shop, selectedFurniture: Furniture) {
        // Find the ShopCart in the list
        val shopCart = cartList?.find { it.shop.id == selectedShop.id }

        if (shopCart != null) {
            // Shop exists in the list, check for the furniture item
            val cartItem = shopCart.items.find { it.furniture.id == selectedFurniture.id }
            if (cartItem != null) {
                // Furniture exists in the list, increment the quantity
                cartItem.quantity += 1
            } else {
                // Furniture does not exist, add it to the list
                if (shopCart.items == null) {
                    shopCart.items = mutableListOf(CartItem(selectedFurniture, 1))
                } else {
                    shopCart.items.add(CartItem(selectedFurniture, 1))
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
        var totalPrice = cartList?.let { calculateTotalPrice(it) }

        totalPriceTextView.text = PRICE_FORMAT.format(totalPrice)


        val adapter = cartList?.let { CartListAdapter(it) }

        cartListRecyclerView.adapter = adapter
        cartListRecyclerView.apply {
            layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        }


        val checkoutBtn: Button = findViewById(R.id.checkout_button)

        checkoutBtn.setOnClickListener {
            activePage = (R.layout.activity_orders)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_orders, null) as RelativeLayout)
            initOrderPage(pageContainer)
        }


    }

    fun calculateTotalPrice(shoppingCarts: MutableList<ShopCart>): Double {
        var totalPrice = 0.0
        for (cart in shoppingCarts) {
            for (item in cart.items) {
                totalPrice += (item.furniture.price?.times(item.quantity) ?: 0.0)
            }
        }
        return totalPrice
    }

    private fun initOrderPage (pageContainer: ViewGroup) {
        val orderListRecyclerView: RecyclerView = findViewById(R.id.order_recycler_view)
        val orderProductProtectTextView: TextView = findViewById(R.id.prod_protect_subtotal_value)
        val ordershipSubtotalTextView: TextView = findViewById(R.id.ship_subtotal_value)
        val merchSubTotalTextView: TextView = findViewById(R.id.merch_subtotal_value)
        val orderTotalPaymentTextView: TextView = findViewById(R.id.total_payment_value)
        val totalValueTextView: TextView = findViewById(R.id.total_value)

        val productProtectSubtotal = 0
        val shipSubtotal = 0
        var merchSubTotal = cartList?.let { calculateTotalPrice(it) }
        var totalPayment = productProtectSubtotal + shipSubtotal + merchSubTotal!!

        orderProductProtectTextView.text = productProtectSubtotal.toString()
        ordershipSubtotalTextView.text = shipSubtotal.toString()
        merchSubTotalTextView.text = merchSubTotal.toString()

        orderTotalPaymentTextView.text = totalPayment.toString()
        totalValueTextView.text = totalPayment.toString()
        val adapter = cartList?.let { CartListAdapter(it) }

        orderListRecyclerView.apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        }

        val placeOrderButton: Button = findViewById(R.id.place_order_button)
        placeOrderButton.setOnClickListener {

            val furnitureIds = mutableListOf<String>()
            val today = Date()
            cartList?.forEach { shopCart ->
                shopCart.items.forEach { cartItem ->
                    repeat(cartItem.quantity) {
                        cartItem.furniture.id?.let { it1 -> furnitureIds.add(it1) }
                    }
                }
            }

            var order = hashMapOf (
                "timestamp" to today,
                "furnitures" to furnitureIds
            )

            val email = SessionManager.getUserEmail(this)
            database.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { result: QuerySnapshot ->
                    if (!result.isEmpty) {
                        val document = result.documents.first()
                        val userId = document.id
                        order.put("userID", userId)
                    }
                }
                .addOnFailureListener { e: Exception ->
                    Log.w(TAG, "Error getting phone number", e)
                }

            database.collection("orders")
                .add(order)
                .addOnSuccessListener {
                    Toast.makeText(this, "Order is successful", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener{
                    Toast.makeText(this, "Order isn't successful", Toast.LENGTH_SHORT).show()
                }
            cartList = mutableListOf()

            activePage = R.layout.activity_dashboard
            pageContainer.removeAllViews()
            val inflatedPage = layoutInflater.inflate(R.layout.activity_dashboard, null) as RelativeLayout
            pageContainer.addView(inflatedPage)
            initHomePage(inflatedPage, pageContainer)
        }

    }

    private fun initProfilePage() {
        val emailTextView: TextView = findViewById(R.id.email)
        val email = SessionManager.getUserEmail(this)
        emailTextView.text = email
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
        }
        profilePic.setOnClickListener {
            activePage = R.layout.activity_edit_account
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_edit_account, null) as RelativeLayout)
            initProfileEditPage()
        }
        cartButton.setOnClickListener {
            activePage = R.layout.activity_cart
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_cart, null) as RelativeLayout)
            initCartPage(pageContainer)
        }

        settingsButton.setOnClickListener {
            activePage = R.layout.activity_settings
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_settings, null) as RelativeLayout)
            initBackButton()
            initSettingsPage()
        }

        toPayButton.setOnClickListener {
            activePage = R.layout.activity_to_pay
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_pay, null) as RelativeLayout)
            initToPayPage()
        }

        toShipButton.setOnClickListener {
            activePage = R.layout.activity_to_ship
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_ship, null) as RelativeLayout)
            initBackButton()
            initToShipPage()
        }

        toReceiveButton.setOnClickListener {
            activePage = R.layout.activity_to_receive
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_receive, null) as RelativeLayout)
            initBackButton()
            initToReceivePage()
        }

        toRateButton.setOnClickListener {
            activePage = R.layout.activity_to_rate
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_rate, null) as RelativeLayout)
            initBackButton()
            initToRatePage()

        }

        accountInfoButton.setOnClickListener {
            activePage = R.layout.activity_edit_account
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_edit_account, null) as RelativeLayout)
            initBackButton()
            initProfileEditPage()
        }

        paymentMethodsButton.setOnClickListener {
            activePage = R.layout.activity_payment_methods
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_payment_methods, null) as RelativeLayout)
            initBackButton()
            initPaymentMethodsPage()
        }

        deliveryAddressButton.setOnClickListener {
            activePage = R.layout.activity_delivery_address
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_delivery_address, null) as RelativeLayout)
            initBackButton()
            initDeliveryAddressPage()
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
            val editCheckBox = addDialogView.findViewById<CheckBox>(R.id.editCheckBox)
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
                    val isChecked = editCheckBox.isChecked

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
                            isChecked
                        )

                        val email = SessionManager.getUserEmail(this)
                        // Update the delivery addresses field within the user's document
                        if (email != null) {
                            val deliveryAddressData = mapOf(
                                "name" to newDeliveryAddress.name,
                                "phone" to newDeliveryAddress.phone,
                                "region" to newDeliveryAddress.region,
                                "barangay" to newDeliveryAddress.barangay,
                                "streetName" to newDeliveryAddress.streetName,
                                "postalCode" to newDeliveryAddress.postalCode,
                                "isChecked" to newDeliveryAddress.isChecked
                            )

                            firestore.collection("users")
                                .whereEqualTo("email", email)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    for (document in querySnapshot.documents) {
                                        document.reference
                                            .update("deliveryAddresses", FieldValue.arrayUnion(deliveryAddressData))
                                            .addOnSuccessListener {
                                                // Fetch the updated delivery addresses from Firestore
                                                initRefreshDeliveryAddress()
                                                Toast.makeText(this, "Delivery address added successfully.", Toast.LENGTH_SHORT).show()
                                                addDialog.dismiss()
                                                initDeliveryAddressPage()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(this, "Failed to add delivery address: ${e.message}", Toast.LENGTH_SHORT).show()
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

                // Check if more than one address is marked as default
                if (newIsChecked && deliveryAddresses.filter { it.isChecked }.size > 1) {
                    Toast.makeText(this@MainActivity, "Only one delivery address can be marked as default.", Toast.LENGTH_SHORT).show()
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
                                        val deliveryAddressList = userDocument.get("deliveryAddresses") as? List<Map<String, Any>>
                                        if (deliveryAddressList != null) {
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
                                                }
                                                .addOnFailureListener { e ->
                                                    Toast.makeText(this@MainActivity, "Failed to update delivery address: ${e.message}", Toast.LENGTH_SHORT).show()
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

        }

        val toRateButton: Button = findViewById(R.id.toRateButton)
        toRateButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_rate, null) as RelativeLayout)
            initToRatePage()
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
        }

        val toReceiveButton: Button = findViewById(R.id.toReceiveButton)
        toReceiveButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_receive, null) as RelativeLayout)
            initToReceivePage()

        }

        val toRateButton: Button = findViewById(R.id.toRateButton)
        toRateButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_rate, null) as RelativeLayout)
            initToRatePage()
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
        }

        val toShipButton: Button = findViewById(R.id.toShipButton)
        toShipButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_ship, null) as RelativeLayout)
            initToShipPage()
        }

        val toRateButton: Button = findViewById(R.id.toRateButton)
        toRateButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_rate, null) as RelativeLayout)
            initToRatePage()
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
        }

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

        }
        changePassButton.setOnClickListener {
            activePage = (R.layout.activity_change_password)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_change_password, null) as RelativeLayout)
            initChangePassPage()
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
        }
    }

    fun initProfileBackButton(){
        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_edit_account, null) as RelativeLayout)
            initProfileEditPage()
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
            AuthUtility.getUserName(email,
                onSuccess = { name ->
                    nameTV.text = name
                    emailTV.text = email
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

            // Build the dialog
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
                        phoneTV.text = "0$phoneNum" // Display the phone number in the phoneTV TextView
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

        editButton.setOnClickListener(){
            if (email != null) {
                showImageSelectionDialog(email)
            } else {
                Log.e("GetUser", "Invalid email: $email")
            }
        }



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
        val masterCardLayout = findViewById<RelativeLayout>(R.id.masterCardLayout)
        val gcashLayout = findViewById<RelativeLayout>(R.id.gcashLayout)


        paypalLayout.setOnLongClickListener {
            paypalEditDialog()
            true
        }

    }

    fun paypalEditDialog() {
        val dialogView = layoutInflater.inflate(R.layout.edit_paypal_dialog, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Save") { dialogInterface, which ->
                val newEmail = dialogView.findViewById<EditText>(R.id.editPaypalEmail).text.toString()
                val emailTextView = findViewById<TextView>(R.id.emailPaypalTextView)
                emailTextView.text = newEmail

                val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString("paypalEmail", newEmail)
                editor.apply()
            }
            .setNegativeButton("Cancel") { dialogInterface, which ->
                dialogInterface.dismiss()
            }
            .create()

        // Retrieve the saved email from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val savedEmail = sharedPreferences.getString("paypalEmail", "")

        // Set the saved email in the emailPaypalTextView
        val emailEditText = dialogView.findViewById<EditText>(R.id.editPaypalEmail)
        emailEditText.setText(savedEmail)

        dialog.show()
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
            editEmailET.text = email

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
                                                    Toast.makeText(this, "Failed to update email: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(this, "Error querying document: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(this, "Email is already used in another account.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error querying document: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "No new changes to the email.", Toast.LENGTH_SHORT).show()
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
                            editPhoneET.setText(phoneNum)
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
                                Toast.makeText(this, "No new changes to the phone number.", Toast.LENGTH_SHORT).show()
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