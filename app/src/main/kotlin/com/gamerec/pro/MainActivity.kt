package com.gamerec.pro

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gamerec.pro.databinding.ActivityMainBinding
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var projectionManager: MediaProjectionManager
    
    private var rewardedAd: RewardedAd? = null
    private val AD_UNIT_ID = "ca-app-pub-9464901744553188/5313805969"
    
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001
    
    private var lastAdWatchTime: Long = 0
    private val AD_VALIDITY_PERIOD = 3600000L // 1 hour in ms
    private var isUserPremium = false
    
    private var lastClickTime: Long = 0
    private val CLICK_THRESHOLD = 1000L // Debounce threshold

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            startCountdown(result.resultCode, result.data!!)
        } else {
            showError("Screen record permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Manual Firebase Initialization from config
        initializeFirebase()
        auth = Firebase.auth
        db = Firebase.firestore

        setupGoogleSignIn()

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this) {
            detectAdBlocker()
            loadRewardedAd()
            loadBannerAd()
        }

        projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        binding.btnStartRecorder.setOnClickListener {
            if (System.currentTimeMillis() - lastClickTime < CLICK_THRESHOLD) return@setOnClickListener
            lastClickTime = System.currentTimeMillis()
            
            checkAdAndRecord()
        }

        binding.cardUnlockPro.setOnClickListener {
            showPremiumDialogue()
        }

        binding.btnLogin.setOnClickListener {
            signIn()
        }

        updateUI()
        setupButtonListeners()
    }

    private fun initializeFirebase() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setProjectId("gen-lang-client-0480385688")
                .setApplicationId("1:808050884226:web:9646f00486b24bcab3fe9b") // Using web ID for auth consistency in preview
                .setApiKey("AIzaSyAvO8ukKdhq4AM45ZwUWgGdqJmIjvCuyS0")
                .setStorageBucket("gen-lang-client-0480385688.firebasestorage.app")
                .build()
            FirebaseApp.initializeApp(this, options)
        }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("808050884226-eb5r43r6sh3p6lql06vsh7h6q7sh8qsk.apps.googleusercontent.com") // This is usually provided, using placeholder web client ID logic
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                showError("Google sign in failed: ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    saveUserProfile(user)
                    updateUI()
                    Toast.makeText(this, "Welcome ${user?.displayName}", Toast.LENGTH_SHORT).show()
                } else {
                    showError("Firebase authentication failed")
                }
            }
    }

    private fun saveUserProfile(user: com.google.firebase.auth.FirebaseUser?) {
        user?.let {
            val docRef = db.collection("users").document(it.uid)
            docRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    isUserPremium = document.getBoolean("isPremium") ?: false
                } else {
                    val userMap = hashMapOf(
                        "uid" to it.uid,
                        "email" to it.email,
                        "displayName" to it.displayName,
                        "photoURL" to it.photoUrl.toString(),
                        "isPremium" to false,
                        "createdAt" to Date(),
                        "updatedAt" to Date()
                    )
                    docRef.set(userMap)
                }
            }
        }
    }

    private fun updateUI() {
        val user = auth.currentUser
        if (user != null) {
            binding.btnLogin.text = user.displayName?.uppercase() ?: "LOGGED IN"
            binding.btnLogin.isEnabled = false
        } else {
            binding.btnLogin.text = "LOGIN"
            binding.btnLogin.isEnabled = true
        }
        
        listenToAppConfig()
    }

    private var premiumPrice = "₹499"
    private var qrCodeUrl = ""

    private fun listenToAppConfig() {
        db.collection("app_config").document("settings")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    premiumPrice = snapshot.getString("premiumPrice") ?: "₹499"
                    qrCodeUrl = snapshot.getString("qrCodeUrl") ?: ""

                    val overlayVisible = snapshot.getBoolean("updateOverlayVisible") ?: false
                    val message = snapshot.getString("updateMessage") ?: ""
                    if (overlayVisible && message.isNotEmpty()) {
                        showUpdateOverlay(message)
                    }

                    val adVisible = snapshot.getBoolean("overlayAdVisible") ?: false
                    val adUrl = snapshot.getString("overlayAdUrl") ?: ""
                    if (adVisible && adUrl.isNotEmpty()) {
                        // Logic to show a popup image ad could go here
                    }
                }
            }
    }

    private fun showUpdateOverlay(message: String) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("System Update")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("UPDATE NOW") { _, _ ->
                // Typically open Play Store or download link
                Toast.makeText(this, "Checking for updates...", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showPremiumDialogue() {
        val user = auth.currentUser
        if (user == null) {
            showError("Please login first to upgrade.")
            return
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        val tvPrice = TextView(this).apply {
            text = "Upgrade to Pro: $premiumPrice"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }
        
        val tvInstruction = TextView(this).apply {
            text = "Scan QR code to pay and enter Transaction ID below:"
            textSize = 12f
            gravity = android.view.Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }

        val etTxnId = android.widget.EditText(this).apply {
            hint = "Enter Transaction ID"
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        container.addView(tvPrice)
        container.addView(tvInstruction)
        container.addView(etTxnId)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Go Premium")
            .setView(container)
            .setPositiveButton("SUBMIT REQUEST") { _, _ ->
                val txnId = etTxnId.text.toString()
                if (txnId.isNotEmpty()) {
                    submitPremiumRequest(txnId)
                }
            }
            .setNeutralButton("WATCH AD (1 HR)") { _, _ ->
                showRewardedAd {
                    lastAdWatchTime = System.currentTimeMillis()
                    Toast.makeText(this, "Premium features active for 1 hour!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun submitPremiumRequest(txnId: String) {
        val user = auth.currentUser ?: return
        val request = hashMapOf(
            "userId" to user.uid,
            "userEmail" to user.email,
            "status" to "pending",
            "transactionId" to txnId,
            "createdAt" to Date()
        )
        db.collection("premium_requests").add(request)
            .addOnSuccessListener {
                Toast.makeText(this, "Request submitted. Processing usually takes 24h.", Toast.LENGTH_LONG).show()
            }
    }
    private fun setupButtonListeners() {
        binding.btnHelp.setOnClickListener {
            Toast.makeText(this, "Opening Help & Support...", Toast.LENGTH_SHORT).show()
        }

        binding.cardPerformance.setOnClickListener {
            Toast.makeText(this, "System optimized for peak performance.", Toast.LENGTH_SHORT).show()
        }

        binding.btnDashboard.setOnClickListener {
            Toast.makeText(this, "Dashboard Syncing...", Toast.LENGTH_SHORT).show()
        }

        binding.btnRecordingsView.setOnClickListener {
            startActivity(Intent(this, RecordingsActivity::class.java))
        }

        binding.btnNavDashboard.setOnClickListener {
            binding.btnDashboard.performClick()
        }

        binding.btnNavRecord.setOnClickListener {
            binding.btnStartRecorder.performClick()
        }

        binding.btnNavSettings.setOnClickListener {
            Toast.makeText(this, "Settings module coming soon!", Toast.LENGTH_SHORT).show()
        }

        binding.btnAdminWeb.setOnClickListener {
            showError("Admin Panel is available at /web/admin in the web interface. Log in with singhrajnish52741@gmail.com to manage.")
        }
    }

    private fun loadBannerAd() {
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)
        binding.adView.adListener = object : AdListener() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                // If banner fails to load, it might be due to ad blocker
                // Error code 3 is NO_FILL, but others like network errors could be blockers
            }
        }
    }

    private fun detectAdBlocker() {
        // Simple detection: Try to resolve a known ad-serving host
        Thread {
            try {
                val address = java.net.InetAddress.getByName("googleads.g.doubleclick.net")
                // Success - likely no DNS level blocker
            } catch (e: Exception) {
                runOnUiThread {
                    binding.tvAdBlockWarning.visibility = View.VISIBLE
                }
            }
        }.start()
    }

    private fun checkAdAndRecord() {
        if (!checkAllPermissions()) return

        if (isUserPremium) {
            requestScreenCapture()
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastAdWatchTime < AD_VALIDITY_PERIOD) {
            requestScreenCapture()
        } else {
            showRewardedAd {
                lastAdWatchTime = System.currentTimeMillis()
                requestScreenCapture()
            }
        }
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                rewardedAd = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }
        })
    }

    private fun showRewardedAd(onReward: () -> Unit) {
        rewardedAd?.let { ad ->
            ad.show(this) { rewardItem ->
                onReward()
                
                val user = auth.currentUser
                if (user != null) {
                    db.collection("users").document(user.uid)
                        .update("isPremium", true, "updatedAt", Date())
                        .addOnSuccessListener {
                            isUserPremium = true
                            Toast.makeText(this, "Profile updated to Premium!", Toast.LENGTH_SHORT).show()
                        }
                }
            }
        } ?: run {
            showError("Internal Error: Ad not ready. Please check internet connection or disable ad block.")
            loadRewardedAd()
        }
    }

    private fun showError(message: String) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Notice")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun checkAllPermissions(): Boolean {
        // 1. Audio and Notifications
        val needsAudio = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        val needsNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        } else false

        if (needsAudio || needsNotifications) {
            val list = mutableListOf<String>()
            if (needsAudio) list.add(Manifest.permission.RECORD_AUDIO)
            if (needsNotifications) list.add(Manifest.permission.POST_NOTIFICATIONS)
            ActivityCompat.requestPermissions(this, list.toTypedArray(), 100)
            return false
        }

        // 2. Overlay Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            Toast.makeText(this, "Enable Overlay Permission for Gaming Controls", Toast.LENGTH_LONG).show()
            return false
        }

        return true
    }

    private fun requestScreenCapture() {
        val captureIntent = projectionManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(captureIntent)
    }

    private fun startCountdown(resultCode: Int, data: Intent) {
        binding.tvCountdown.visibility = View.VISIBLE
        binding.btnStartRecorder.isEnabled = false
        
        object : CountDownTimer(3000, 1000) {
            override fun onTick(ms: Long) {
                binding.tvCountdown.text = ((ms / 1000) + 1).toString()
            }
            override fun onFinish() {
                binding.tvCountdown.visibility = View.GONE
                val intent = Intent(this@MainActivity, ScreenRecordService::class.java).apply {
                    action = ScreenRecordService.ACTION_START
                    putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
                    putExtra(ScreenRecordService.EXTRA_DATA, data)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                binding.tvStatus.text = "RECORDING GAMEPLAY"
                binding.tvStatus.setTextColor(ContextCompat.getColor(this@MainActivity, android.R.color.holo_red_light))
            }
        }.start()
    }
}
