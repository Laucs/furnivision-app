package com.alvarez.furnivisionapp
import android.graphics.BitmapFactory
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.hardware.camera2.CameraManager
import android.icu.text.DecimalFormat
import android.net.Uri
import android.os.Bundle
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
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.alvarez.furnivisionapp.data.AuthUtility
import com.alvarez.furnivisionapp.data.CartItem
import com.alvarez.furnivisionapp.data.Furniture
import com.alvarez.furnivisionapp.data.Shop
import com.alvarez.furnivisionapp.utils.CameraFunctions
import com.alvarez.furnivisionapp.utils.HomePageFunctions
import com.alvarez.furnivisionapp.utils.ShopListAdapter
import com.alvarez.furnivisionapp.data.SessionManager
import com.alvarez.furnivisionapp.data.ShopCart
import com.alvarez.furnivisionapp.utils.CartListAdapter
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.bumptech.glide.Glide

import java.io.ByteArrayOutputStream
import java.io.File

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
        initHomePage(inflatedPage, pageContainer, cartPage)

        homeButton.setOnClickListener {
            activePage = dashboardPage
            pageContainer.removeAllViews()
            inflatedPage = layoutInflater.inflate(dashboardPage, null) as RelativeLayout
            pageContainer.addView(inflatedPage)
            initHomePage(inflatedPage, pageContainer, cartPage)
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
    }

    private fun initHomePage(page: RelativeLayout, pageContainer: ViewGroup, cartPage: Int) {
        homePageFunctions = HomePageFunctions(page, AppCompatImageButton::class.java, this)


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
            val cartItem = shopCart.items?.find { it.furniture.id == selectedFurniture.id }
            if (cartItem != null) {
                // Furniture exists in the list, increment the quantity
                cartItem.quantity += 1
            } else {
                // Furniture does not exist, add it to the list
                if (shopCart.items == null) {
                    shopCart.items = mutableListOf(CartItem(selectedFurniture, 1))
                } else {
                    shopCart.items?.add(CartItem(selectedFurniture, 1))
                }
            }
        } else {
            // Shop does not exist, create a new ShopCart with the furniture item
            cartList?.add(ShopCart(selectedShop, mutableListOf(CartItem(selectedFurniture, 1))))
        }

        // Log the updated cartList for debugging
        Log.d(TAG, cartList.toString())
    }

    private fun initCameraPage() {
        val cameraLayout: RelativeLayout = findViewById(R.id.cameraLayout)
        val galleryLayout: RelativeLayout  = findViewById(R.id.galleryLayout)
        val videoLayout: RelativeLayout  = findViewById(R.id.videoLayout)
        val galleryImageView: ImageView = findViewById(R.id.galleryImageView)

        val galleryBtn: Button = findViewById(R.id.galleryButton)
        val galleryBtn2: Button = findViewById(R.id.vidGalleryButton)
        val cameraBtn: Button = findViewById(R.id.backButton)
        val cameraBtn2: Button = findViewById(R.id.vidCameraButton)
        val captureButton: Button = findViewById(R.id.captureButton)
        val videoBtn: Button = findViewById(R.id.videoButton)
        val galleryPrevBtn: Button = findViewById(R.id.prevButton)
        val galleryNextBtn: Button = findViewById(R.id.nextButton)
        val vidTextureView: TextureView = findViewById(R.id.videoTextureView)
        val vidRecordButton: Button = findViewById(R.id.videoRecButton)
        val textureView: TextureView = findViewById(R.id.textureView)
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        cameraFunctions = CameraFunctions(
            cameraLayout,
            galleryLayout,
            videoLayout,
            galleryImageView,
            galleryBtn,
            galleryBtn2,
            cameraBtn,
            cameraBtn2,
            captureButton,
            videoBtn,
            galleryPrevBtn,
            galleryNextBtn,
            vidTextureView,
            vidRecordButton,
            filesDir,
            textureView,
            cameraManager,
            this
        )
    }

    private fun initCartPage(pageContainer: ViewGroup) {
        val cartListRecyclerView: RecyclerView = findViewById(R.id.cart_recycler_view)
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
        }


    }

    private fun initOrderPage () {

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

        cartButton.setOnClickListener {
            activePage = R.layout.activity_cart
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_cart, null) as RelativeLayout)
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
        }
        refreshProfile()
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
        val nameTV: TextView = findViewById(R.id.nameTV)
        val email = SessionManager.getUserEmail(this)


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

        logoutButton.setOnClickListener {
            AuthUtility.signOut(this)
            val intent = Intent(this, LoginRegistrationActivity::class.java)
            startActivity(intent)
            finish()
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
            initAboutPage()
        }
        rateUsButton.setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=")))
            } catch (e: android.content.ActivityNotFoundException) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store")))
            }
        }
    }
    fun clearCache() {
        try {
            val dir: File = cacheDir
            if (deleteDir(dir)) {
                println("Cache cleared successfully")
            } else {
                println("Cache clearing failed")
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
        backButton.setOnClickListener {
            val pageContainer: ViewGroup = findViewById(R.id.pageContainer)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_settings, null) as RelativeLayout)
            initSettingsPage()
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

            // Build the dialog
            val dialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Confirm") { dialogInterface, which ->
                    val datePicker = dialogView.findViewById<DatePicker>(R.id.datePicker)
                    val year = datePicker.year
                    val month = datePicker.month
                    val day = datePicker.dayOfMonth
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
                }
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
        val paypalLayout = findViewById<RelativeLayout>(R.id.paypalLayout)

        paypalLayout.setOnLongClickListener {
            paypalEditDialog()
            true
        }

        // Retrieve the saved email from SharedPreferences
        val sharedPreferences = getSharedPreferences("paypal", Context.MODE_PRIVATE)
        val savedEmail = sharedPreferences.getString("paypalEmail", "Set Now")

        // Set the saved email in the emailPaypalTextView
        val emailTextView = findViewById<TextView>(R.id.emailPaypalTextView)
        emailTextView.text = savedEmail
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

    fun initEditNamePage(){
        val email = SessionManager.getUserEmail(this)
        val editNameET: TextView = findViewById(R.id.editNameET)
        if (email != null) {
            AuthUtility.getUserName(email,
                onSuccess = { name ->
                    editNameET.text = name
                },
                onFailure = {
                    Log.e("GetUser", "Failed to retrieve user name")
                }
            )
        } else {
            Log.e("GetUser", "Invalid email: $email")
        }
    }

    fun initEditEmailPage(){
        val email = SessionManager.getUserEmail(this)
        val editEmailET: TextView = findViewById(R.id.editEmailET)
        if (email != null) {
            AuthUtility.getUserName(email,
                onSuccess = { name ->
                    editEmailET.text = email
                },
                onFailure = {
                    Log.e("GetUser", "Failed to retrieve user name")
                }
            )
        } else {
            Log.e("GetUser", "Invalid email: $email")
        }
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