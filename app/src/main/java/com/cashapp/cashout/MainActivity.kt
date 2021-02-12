package com.cashapp.cashout

import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    private var billingClient: BillingClient? = null
    private var skuDetails: SkuDetails? = null
    private var userId = ""
    private var fs = Firebase.firestore
    private val db = FirebaseFirestore.getInstance()
    val map = mapOf("99_credits" to 0.99, "499_credits" to 4.99, "999_credits" to 999)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main)
        userId = intent.getStringExtra("user_id")!!
        val emailId = intent.getStringExtra("email_id")
        main_email.text = "Account: $emailId"
        updateAmount()
        setUpBillingClient()
        initListeners()
        button_logout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            startActivity(Intent(this@MainActivity, LoginActivity::class.java))
            finish()
        }
    }

    private fun updateAmount(){
        val docRef = db.collection("users").document(userId)
        docRef.get()
            .addOnSuccessListener { document ->
                if(document != null){
                    amount.text = "Amount Transferred: $${"%.2f".format(document.getDouble("amount_paid"))} CAD"
                }
                else{
                    Log.d("does not exist", "No such document")
                }
            }
    }

    private fun initListeners() {
        txt_product_buy?.setOnClickListener {
            // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
            // Retrieve a value for "skuDetails" by calling querySkuDetailsAsync().
            skuDetails?.let {
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(it)
                    .build()
                billingClient?.launchBillingFlow(this, billingFlowParams)?.responseCode
            } ?: noSKUMessage()

        }
    }

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
                    //This list should contain the products added above
                    updateUI(skuDetails)
                }
            }
        }
    }

    private fun updateUI(skuDetails: SkuDetails?) {
        skuDetails?.let {
            this.skuDetails = it
//            txt_product_name?.text = skuDetails.title
//            txt_product_description?.text = skuDetails.description
            showUIElements()
        }
    }

    private fun showUIElements() {
//        txt_product_name?.visibility = View.VISIBLE
//        txt_product_description?.visibility = View.VISIBLE
        txt_product_buy?.visibility = View.VISIBLE
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
                    fs.collection("users").document(userId).collection("purchases").document(purchase.orderId).set(purch)
                        .addOnSuccessListener { documentReference ->
                            Log.d(
                                ContentValues.TAG,
                                "Purchase added"
                            )
                        }


                    // Update the appropriate tables/databases to grant user the items
                    fs.collection("users").document(userId).update(
                        "amount_paid", FieldValue.increment(
                            if(purchase.sku == "99_credits")
                                0.99
                            else if (purchase.sku == "499_credits")
                                4.99
                            else
                                9.99
                        )
                    )

                    updateAmount()
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
