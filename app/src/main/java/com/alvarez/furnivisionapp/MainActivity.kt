package com.alvarez.furnivisionapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.icu.text.DecimalFormat
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
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
import com.alvarez.furnivisionapp.data.SessionManager
import com.google.firebase.auth.FirebaseAuth
import java.io.File
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
        val cartItemList = cartFurniture.split(',').toString().trim(' ')

        Log.d("d", cartItemList.toString())

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

        settingsButton.setOnClickListener {
            activePage = (R.layout.activity_settings)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_settings, null) as RelativeLayout)
            initBackButton()
            initSettingsPage()
        }

        toPayButton.setOnClickListener {
            activePage = (R.layout.activity_to_pay)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_pay, null) as RelativeLayout)
            initBackButton()
        }

        toShipButton.setOnClickListener {
            activePage = (R.layout.activity_to_ship)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_ship, null) as RelativeLayout)
            initBackButton()
        }

        toReceiveButton.setOnClickListener {
            activePage = (R.layout.activity_to_receive)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_receive, null) as RelativeLayout)
            initBackButton()
        }

        toRateButton.setOnClickListener {
            activePage = (R.layout.activity_to_rate)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_to_rate, null) as RelativeLayout)
            initBackButton()
        }

        accountInfoButton.setOnClickListener {
            activePage = (R.layout.activity_edit_account)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_edit_account, null) as RelativeLayout)
            initBackButton()
            initProfileEditPage()
        }

        paymentMethodsButton.setOnClickListener {
            activePage = (R.layout.activity_payment_methods)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_payment_methods, null) as RelativeLayout)
            initBackButton()
        }

        deliveryAddressButton.setOnClickListener {
            activePage = (R.layout.activity_delivery_address)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_delivery_address, null) as RelativeLayout)
            initBackButton()
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
        val changeBioButton: RelativeLayout = findViewById(R.id.changeBioButton)
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
        }

        changeBioButton.setOnClickListener {
            activePage = (R.layout.activity_edit_bio)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_edit_bio, null) as RelativeLayout)
            initProfileBackButton()
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
        }

        saveButton.setOnClickListener {
            activePage = (R.layout.activity_profile)
            pageContainer.removeAllViews()
            pageContainer.addView(layoutInflater.inflate(R.layout.activity_profile, null) as RelativeLayout)
            initProfilePage()
        }
    }

    fun initEditNamePage(){

    }
    fun initEditBioPage(){

    }
    fun initEditEmailPage(){

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