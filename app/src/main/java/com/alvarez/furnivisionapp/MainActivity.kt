package com.alvarez.furnivisionapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.icu.text.DecimalFormat
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alvarez.furnivisionapp.data.AuthUtility
import com.alvarez.furnivisionapp.data.CartItem
import com.alvarez.furnivisionapp.data.Furniture
import com.alvarez.furnivisionapp.data.Shop
import com.alvarez.furnivisionapp.utils.CameraFunctions
import com.alvarez.furnivisionapp.utils.CartListAdapter
import com.alvarez.furnivisionapp.utils.HomePageFunctions
import com.alvarez.furnivisionapp.utils.ShopListAdapter
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.log

class MainActivity : AppCompatActivity() {

    private lateinit var shops: Array<Shop>
    private lateinit var homePageFunctions: HomePageFunctions
    private lateinit var cameraFunctions: CameraFunctions
    private var activePage: Int? = null
    private lateinit var activeButton: LinearLayout
    private var furniIndex: Int = 0
    private var cartShop: Shop? = null
    private var cartFurniture: StringBuilder = StringBuilder()
    private var cartSummary: HashMap<String, Int>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Navigation Buttons
        val homeButton: LinearLayout = findViewById(R.id.home_menu)
        val shopButton: LinearLayout = findViewById(R.id.shop_menu)
        val cameraButton: LinearLayout = findViewById(R.id.ar_menu)
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
        initHomePage(inflatedPage)

        homeButton.setOnClickListener {
            activePage = dashboardPage
            pageContainer.removeAllViews()
            inflatedPage = layoutInflater.inflate(dashboardPage, null) as RelativeLayout
            pageContainer.addView(inflatedPage)
            initHomePage(inflatedPage)
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

    private fun initHomePage(page: RelativeLayout) {
        Log.d("MainActivity", "Initializing Home Page")
        homePageFunctions = HomePageFunctions(page, AppCompatImageButton::class.java, this)
        Log.d("MainActivity", "Home Page Initialized")
    }

    private fun initShopPage(pageContainer: ViewGroup) {
        shops = arrayOf(
            Shop(
            "1",
            R.drawable.shop_1,
            "Furniture World",
            "Your one-stop destination for all types of furniture.",
                arrayOf(
                    Furniture("1", R.drawable.f_1,"Sofa", "Living Room", 499.99, "42cm by 42cm by 70cm",24),
                    Furniture("2", R.drawable.f_2,"Dining Table", "Dining Room", 299.99, "12cm by 30cm by 70cm",9),
                    Furniture("3", R.drawable.f_3,"Bed", "Bedroom", 599.99, "12cm by 30cm by 70cm",123)
            ),
            4.5,
            1000
        ),
        Shop(
            "2",
            R.drawable.shop_2,
            "Home Furnishings Emporium",
            "Transform your home with our wide range of furnishings.",
            arrayOf(
            Furniture("4", R.drawable.f_3 ,"Coffee Table", "Living Room", 149.99, "12cm by 30cm by 20cm",21),
            Furniture("5", R.drawable.f_2 ,"Wardrobe", "Bedroom", 399.99, "55cm by 55cm by 70cm",90),
            Furniture("6", R.drawable.f_1 ,"Desk", "Study Room", 199.99, "12cm by 30cm by 90cm",661)
            ),
            4.2,
            800
        ),
        Shop(
            "3",
            R.drawable.shop_3,
            "Modern Living",
            "Experience modern living with our contemporary furniture.",
            arrayOf(
                Furniture("7", R.drawable.f_1 ,"Armchair", "Living Room", 249.99, "14cm by 51cm by 100cm",2),
                Furniture("8", R.drawable.f_3 ,"Bookshelf", "Study Room", 179.99, "41cm by 51cm by 120cm",42),
                Furniture("9", R.drawable.f_2 ,"Nightstand", "Bedroom", 99.99, "12cm by 30cm by 70cm",111)
            ),
            4.7,
            1200
        ),
        Shop(
            id = "4",
            logo = R.drawable.shop_4, // Provide the logo yourself
            name = "Classic Comforts",
            description = "Discover timeless elegance with our classic furniture pieces.",
            furnitures = arrayOf(
                Furniture("10", R.drawable.f_2,"Chaise Lounge", "Living Room", 799.99, "20cm by 20cm by 100cm",300),
                Furniture("11", R.drawable.f_1,"Dresser", "Bedroom", 349.99,"20cm by 20cm by 100cm",12),
                Furniture("12", R.drawable.f_3,"Bar Stool", "Kitchen", 79.99, "20cm by 20cm by 100cm",231)
            ),
            reviews = 4.4,
            views = 950
        ))


        val shopListView: RecyclerView = findViewById(R.id.shop_list)
        val adapter = ShopListAdapter(shops)

        adapter.setOnItemClickListener (object : ShopListAdapter.OnItemClickListener {
            @SuppressLint("InflateParams")
            override fun onItemClick(shop: Shop) {
                pageContainer.removeAllViews()
                var inflatedPage = layoutInflater.inflate(R.layout.activity_furniture_selection, null) as RelativeLayout
                pageContainer.addView(inflatedPage)
                initFurniSelectPage(shop, pageContainer)
            }
        })

        shopListView.apply {
            this.adapter = adapter
            layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
        }





    }

    @SuppressLint("SetTextI18n", "DefaultLocale", "CommitPrefEdits")
    private fun initFurniSelectPage(shop: Shop, pageContainer: ViewGroup){

        var backBtn: ImageButton = findViewById(R.id.backBtn)

        backBtn.setOnClickListener{
            pageContainer.removeAllViews()
            var inflatedPage: RelativeLayout = layoutInflater.inflate(R.layout.activity_shop, null) as RelativeLayout
            pageContainer.addView(inflatedPage)
            initShopPage(pageContainer)
            furniIndex = 0
        }

        val nextBtn: ImageButton = findViewById(R.id.nextBtn)
        val prevBtn: ImageButton = findViewById(R.id.previousBtn)
        val addToCartBtn: Button = findViewById(R.id.addToCartButton)

        val imageView: ImageView = findViewById(R.id.furnitureImage)
        val nameTextView: TextView = findViewById(R.id.furnitureName)
        val descTextView: TextView = findViewById(R.id.furnitureDesc)
        val priceTextView: TextView = findViewById(R.id.furniturePrice)
        val dimensionsTextView: TextView = findViewById(R.id.furnitureDimensions)
        val stocksTextView: TextView = findViewById(R.id.furnitureStocks)

        val df = DecimalFormat("#0.00")

        imageView.setImageResource(shop.furnitures[furniIndex].img)
        nameTextView.text = getString(R.string.name_title) + shop.furnitures[furniIndex].name
        descTextView.text = getString(R.string.description) + shop.furnitures[furniIndex].description
        priceTextView.text = getString(R.string.price) + df.format(shop.furnitures[furniIndex].stocks)
        dimensionsTextView.text = getString(R.string.dimensions) + shop.furnitures[furniIndex].dimensions
        stocksTextView.text = getString(R.string.stock) + shop.furnitures[furniIndex].stocks.toString()


        nextBtn.setOnClickListener {
            if (furniIndex != shop.furnitures.size-1) {
                furniIndex++
                imageView.setImageResource(shop.furnitures[furniIndex].img)
                nameTextView.text = getString(R.string.name_title) + shop.furnitures[furniIndex].name
                descTextView.text = getString(R.string.description) + shop.furnitures[furniIndex].description
                priceTextView.text = getString(R.string.price) + df.format(shop.furnitures[furniIndex].stocks)
                dimensionsTextView.text = getString(R.string.dimensions) + shop.furnitures[furniIndex].dimensions
                stocksTextView.text = getString(R.string.stock) + shop.furnitures[furniIndex].stocks.toString()
            }
        }

        prevBtn.setOnClickListener {
            if (furniIndex > 0) {
                furniIndex--
                imageView.setImageResource(shop.furnitures[furniIndex].img)
                nameTextView.text = getString(R.string.name_title) + shop.furnitures[furniIndex].name
                descTextView.text = getString(R.string.description) + shop.furnitures[furniIndex].description
                priceTextView.text = getString(R.string.price) + df.format(shop.furnitures[furniIndex].stocks)
                dimensionsTextView.text = getString(R.string.dimensions) + shop.furnitures[furniIndex].dimensions
                stocksTextView.text = getString(R.string.stock) + shop.furnitures[furniIndex].stocks.toString()            }
        }

        addToCartBtn.setOnClickListener {
            // If the cart shop is null, set it to the current shop
            if (cartShop == null) {
                cartShop = shop
            }

            // If the cart shop matches the current shop and the cart shop is not null
            if (cartShop?.id == shop.id) {
                // Extract the current furniture ID
                val currentFurnitureId = shop.furnitures[furniIndex].id.toString()

                // Append the current furniture ID to the cartFurniture string
                cartFurniture.append(currentFurnitureId).append(",")

                Log.d("id", currentFurnitureId)
            }

            // Split the cartFurniture string, trim each item, and count occurrences
            val cart: List<String> = cartFurniture.trim().split(",").map { it.trim() }

            Log.d("cartF", cartFurniture.split(",").toString())
            cartSummary = countFurnitureOccurrences(cart.toTypedArray())
            Log.d("cartS", cartSummary!!.toString())
        }
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
        val furnitures = cartShop?.furnitures
        val cartItemList = cartFurniture?.split(',').toString().trim(' ')

        Log.d("d", cartItemList!!.toString())

        if (cartItemList != null) {
            val cartItems = cartSummary?.mapNotNull { (id, count) ->
                val furniture = furnitures?.find { it.id == id }
                furniture?.let { CartItem(it, count) }
            }?.toTypedArray()

            if (cartItems != null) {
                Log.d("cartItems", cartItems.toList().toString())
            }

            val cartListing: RecyclerView = findViewById(R.id.cart_recycler_view)
            val adapter = CartListAdapter(cartItems ?: emptyArray())
            cartListing.adapter = adapter
            cartListing.apply {
                this.adapter = adapter
                layoutManager = LinearLayoutManager(this.context, LinearLayoutManager.VERTICAL, false)
            }
        }
    }

    private fun initProfilePage() {

        val settingsButton: ImageButton = findViewById(R.id.settingsButton)
        val cartButton: ImageButton = findViewById(R.id.cartButton)
        val toPayButton: Button = findViewById(R.id.toPayButton)
        val toShipButton: Button = findViewById(R.id.toShipButton)
        val toReceiveButton: Button = findViewById(R.id.toReceiveButton)
        val toRateButton: Button = findViewById(R.id.toRateButton)
        val accountInfoButton: RelativeLayout = findViewById(R.id.accountInfoButton)
        val paymentMethodsButton: RelativeLayout = findViewById(R.id.paymentMethodsButton)
        val deliveryAddressButton: RelativeLayout = findViewById(R.id.deliveryAddressButton)
        val logoutButton: RelativeLayout = findViewById(R.id.logout_button)

        // Navigation Logic
        val pageContainer: ViewGroup = findViewById(R.id.pageContainer)

        settingsButton.setOnClickListener {
            activePage = (R.layout.activity_settings)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_settings, null) as RelativeLayout)
        }

        toPayButton.setOnClickListener {
            // Add your functionality for the "To Pay" button here
        }

        toShipButton.setOnClickListener {
            // Add your functionality for the "To Ship" button here
        }

        toReceiveButton.setOnClickListener {
            // Add your functionality for the "To Receive" button here
        }

        toRateButton.setOnClickListener {
            // Add your functionality for the "To Rate" button here
        }

        accountInfoButton.setOnClickListener {
            // Add your functionality for the "Account Info" button here
        }

        paymentMethodsButton.setOnClickListener {
            // Add your functionality for the "Payment Methods" button here
        }

        deliveryAddressButton.setOnClickListener {
            // Add your functionality for the "Delivery Address" button here
        }

        logoutButton.setOnClickListener {
            AuthUtility.signOut(this)
            val intent = Intent(this, LoginRegistrationActivity::class.java)
            startActivity(intent)
        }
        logoutButton.setOnClickListener {
            AuthUtility.signOut(this)
            val intent = Intent(this, LoginRegistrationActivity::class.java)
            startActivity(intent)
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