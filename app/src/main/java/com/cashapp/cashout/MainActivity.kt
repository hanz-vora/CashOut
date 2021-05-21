package com.cashapp.cashout

import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.billingclient.api.*
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.skydoves.balloon.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList
import java.text.DateFormatSymbols
import kotlin.collections.HashMap

class MainActivity : AppCompatActivity() {
    private var billingClient: BillingClient? = null
    private var skuDetails: SkuDetails? = null
    private var userId = ""
    private var fs = Firebase.firestore
    private val db = fs.collection("users")
    var monthMap = HashMap<String, String>()
    val doc_name  = Calendar.getInstance().get(Calendar.YEAR).toString() + "-" + DateFormatSymbols.getInstance().getMonths()[Calendar.getInstance().get(Calendar.MONTH)]
    val map = mapOf("99_credits" to 0.99, "499_credits" to 4.99, "999_credits" to 9.99)
    lateinit var mAdView : AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main)
        userId = intent.getStringExtra("user_id")!!
        val emailId = intent.getStringExtra("email_id")
        main_email.text = "$emailId"
        updateAmount()
        setUpBillingClient()
        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        val balloon = createBalloon(applicationContext) {
            setArrowSize(20)
            setArrowOrientation(ArrowOrientation.BOTTOM)
            setArrowVisible(true)
            setArrowPosition(0.5f)
            setWidthRatio(0.8f)
            setHeight(240)
            setPadding(10)
            setTextGravity(0)
            setTextSize(11f)
            setCornerRadius(10f)
            setAlpha(1f)
            setText("<pre><strong>[1]</strong> <strong>60%</strong> of purchased amount will be sent to your PayPal, subject to transfer " +
                    "fees<br /><br /><strong>[2]</strong> All transfers are made on the <strong>16th</strong> of the <em>next</em> month " +
                    "after purchase<br /><br /><strong>[3]</strong> Payments will not be made to users with multiple CashOut accounts, or " +
                    "who hold invalid PayPal accounts<br /><br /><strong>[4]</strong> A maximum of <strong>$25</strong> can be transferred " +
                    "each month<br /><br /><strong>[5]</strong> Users who fail to follow these terms will not be refunded any monies</pre> <p>&nbsp;</p>")
            setTextIsHtml(true)
            setTextColorResource(R.color.black)
            setBackgroundColorResource(R.color.colorPrimary)
            setBalloonAnimation(BalloonAnimation.FADE)
            setLifecycleOwner(lifecycleOwner)
        }

        tooltip.setOnClickListener{
            balloon.showAlignTop(tooltip)
        }

        year_picker.minValue = 2021
        year_picker.maxValue = Calendar.getInstance().get(Calendar.YEAR)
        year_picker.wrapSelectorWheel = true
        month_picker.minValue = 0
        month_picker.maxValue = 11
        month_picker.displayedValues = arrayOf<String>("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        month_picker.wrapSelectorWheel = true

        //initListeners()
        button_logout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val preferences = getSharedPreferences("checkbox", MODE_PRIVATE)
            val editor: SharedPreferences.Editor  = preferences.edit()
            editor.putString("remember", "false")
            editor.putString("google", "false")
            editor.apply()
            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            Firebase.auth.signOut()
            finish()
        }

        updateMap()

        month_picker.setOnValueChangedListener{month_picker, old_month, new_month ->
            val key = year_picker.value.toString() + "-" + DateFormatSymbols.getInstance().getMonths()[new_month]
            populateAmounts(key, monthMap)
        }
        year_picker.setOnValueChangedListener{year_picker, old_month, new_month ->
            val key = new_month.toString() + "-" + DateFormatSymbols.getInstance().getMonths()[month_picker.value]
            populateAmounts(key, monthMap)
        }
    }

    private fun updateMap() {
        val possible_dates = db.document(userId).collection("purchases_by_month")
        possible_dates.get()
            .addOnSuccessListener { document ->
                if(document != null){
                    for (month in document){
                        val total = month.get("total").toString()
                        monthMap[month.id] = total
                    }
                    month_picker.setValue(Calendar.getInstance().get(Calendar.MONTH))
                    year_picker.setValue(Calendar.getInstance().get(Calendar.YEAR))
                    populateAmounts(doc_name, monthMap)
                }
                else{
                    Log.d("does not exist", "No such document")
                }
            }
    }

    private fun populateAmounts(key: String, monthMap: HashMap<String, String>) {
        var amt = "0"
        if (monthMap.containsKey(key))
            amt = monthMap[key]!!
        amount_monthly.text = "$key: $${"%.2f".format(amt.toDouble())}"
    }

    private fun updateAmount(){
        val doc_ref = db.document(userId)
        doc_ref.get()
            .addOnSuccessListener { document ->
                if(document != null){
                    amount.text = "Total: $${"%.2f".format(document.getDouble("amount_paid"))}"
                }
                else{
                    Log.d("does not exist", "No such document")
                }
            }
    }


//    private fun initListeners() {
//        product_099?.setOnClickListener {
//            // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
//            // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
//            skuDetails?.let {
//                val billingFlowParams = BillingFlowParams.newBuilder()
//                    .setSkuDetails(it)
//                    .build()
//                billingClient?.launchBillingFlow(this, billingFlowParams)?.responseCode
//            } ?: noSKUMessage()
//
//        }
//    }

    private fun noSKUMessage() {

    }

    private fun setUpBillingClient() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(purchaseUpdateListener)
            .enablePendingPurchases()
            .build()
        startConnection()
    }

    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // The BillingClient is ready. You can query purchases here.
                    Log.v("TAG_INAPP", "Setup Billing Done")
                    queryAvaliableProducts()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        })
    }

    private fun queryAvaliableProducts() {
        val skuList = ArrayList<String>()
        skuList.add("99_credits")
        skuList.add("499_credits")
        skuList.add("999_credits")
        val params = SkuDetailsParams.newBuilder()
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP)

        billingClient?.querySkuDetailsAsync(params.build()) { billingResult, skuDetailsList ->
            // Process the result.
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !skuDetailsList.isNullOrEmpty()) {
                for (skuDetails in skuDetailsList) {
                    Log.v("TAG_INAPP", "skuDetailsList : ${skuDetailsList}")

                    if(skuDetails.sku == "99_credits"){
                        updateUI(skuDetails)
                        product_099.setOnClickListener {
                            val billingFlowParams = BillingFlowParams
                                .newBuilder()
                                .setSkuDetails(skuDetails)
                                .build()
                            billingClient?.launchBillingFlow(this, billingFlowParams)
                        }
                    }

                    if(skuDetails.sku == "499_credits"){
                        updateUI(skuDetails)
                        product_499.setOnClickListener {
                            val billingFlowParams = BillingFlowParams
                                .newBuilder()
                                .setSkuDetails(skuDetails)
                                .build()
                            billingClient?.launchBillingFlow(this, billingFlowParams)
                        }
                    }

                    if(skuDetails.sku == "999_credits"){
                        updateUI(skuDetails)
                        product_999.setOnClickListener {
                            val billingFlowParams = BillingFlowParams
                                .newBuilder()
                                .setSkuDetails(skuDetails)
                                .build()
                            billingClient?.launchBillingFlow(this, billingFlowParams)
                        }
                    }

                    //This list should contain the products added above
                }
            }
        }
    }

    private fun updateUI(skuDetails: SkuDetails?) {
        skuDetails?.let {
            this.skuDetails = it
//            txt_product_name?.text = skuDetails.title
//            txt_product_description?.text = skuDetails.description
            showUIElements(skuDetails.sku)
        }
    }

    private fun showUIElements(sku: String) {
//        txt_product_name?.visibility = View.VISIBLE
//        txt_product_description?.visibility = View.VISIBLE
        if(sku == "99_credits")
            product_099?.visibility = View.VISIBLE
        if(sku == "499_credits")
            product_499?.visibility = View.VISIBLE
        if(sku == "999_credits")
            product_999?.visibility = View.VISIBLE
    }

    private val purchaseUpdateListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            Log.v("TAG_INAPP", "billingResult responseCode : ${billingResult.responseCode}")
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                for (purchase in purchases) {
//                        handlePurchase(purchase)
                    handleConsumedPurchases(purchase)
                }
            } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
                // Handle an error caused by a user cancelling the purchase flow.
            } else {
                // Handle any other error codes.
            }
        }

    private fun handleConsumedPurchases(purchase: Purchase) {
        Log.d("TAG_INAPP", "handleConsumablePurchasesAsync foreach it is $purchase")
        val params =
            ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        billingClient?.consumeAsync(params) { billingResult, purchaseToken ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {

                    val purch = hashMapOf(
                        "amount" to map[purchase.sku],
                        "purchase_time" to Date(purchase.purchaseTime),
                        "purchase_token" to purchase.purchaseToken,
                        "order_id" to purchase.orderId,
                        "account_identifiers" to purchase.accountIdentifiers
                    )
                    val doc_ref = db.document(userId)
                    doc_ref.collection("purchases_by_month").document(doc_name).collection("purchases").document(purchase.orderId).set(purch)
                    doc_ref.collection("purchases").document(purchase.orderId).set(purch)
                        .addOnSuccessListener { documentReference ->
                            Log.d(
                                ContentValues.TAG,
                                "Purchase added"
                            )
                        }

                    var new_total = 0.00
                    var purchase_amount = 0.00

                    if(monthMap.containsKey(doc_name)){
                        new_total = monthMap[doc_name]!!.toDouble()
                    }
                    
                    if(purchase.sku == "99_credits")
                        purchase_amount = 0.99
                    else if (purchase.sku == "499_credits")
                        purchase_amount = 4.99
                    else
                        purchase_amount = 9.99

                    new_total += purchase_amount


                    // Update the appropriate tables/databases to grant user the items
                    doc_ref.update(
                        "amount_paid", FieldValue.increment(
                            purchase_amount
                    ))

                    doc_ref.collection("purchases_by_month").document(doc_name).set(hashMapOf("total" to new_total))
                    updateAmount()
                    updateMap()
                    Log.d(
                        "TAG_INAPP",
                        " Update the appropriate tables/databases to grant user the items"
                    )
                }
                else -> {
                    Log.w("TAG_INAPP", billingResult.debugMessage)
                }
            }
        }
    }

    private fun handleNonConsumablePurchase(purchase: Purchase) {
        Log.v("TAG_INAPP", "handlePurchase : ${purchase}")
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken).build()
                billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    val billingResponseCode = billingResult.responseCode
                    val billingDebugMessage = billingResult.debugMessage

                    Log.v("TAG_INAPP", "response code: $billingResponseCode")
                    Log.v("TAG_INAPP", "debugMessage : $billingDebugMessage")

                }
            }
        }
    }


}
