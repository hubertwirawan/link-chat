// # COMP 4521    #  KENT, KEVIN      20558962        KKENT@CONNECT.UST.HK
// # COMP 4521    # KONSTANTINO, HUBERT ADITYA 20560123 HAK@CONNECT.UST.HK
package com.linkchat.firebase.android.chatapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.downloader.PRDownloader
import com.firebase.ui.auth.AuthUI.IdpConfig.*
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.linkchat.firebase.android.chatapp.databinding.ActivityMainBinding
import com.linkchat.firebase.android.chatapp.model.FriendlyMessage
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import com.downloader.PRDownloaderConfig


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var manager: LinearLayoutManager

    // Firebase instance variables
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase
    private lateinit var fs: FirebaseFirestore
    private lateinit var adapter: FriendlyMessageAdapter

    private lateinit var userId: String
    private lateinit var roomId: String

    private val openDocument = registerForActivityResult(MyOpenDocumentContract()) { uri ->
        onImageSelected(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        PRDownloader.initialize(getApplicationContext());



// Setting timeout globally for the download network requests:
        val config = PRDownloaderConfig.newBuilder()
            .setReadTimeout(30000)
            .setConnectTimeout(30000)
            .build()
        PRDownloader.initialize(applicationContext, config)

        // This codelab uses View Binding
        // See: https://developer.android.com/topic/libraries/view-binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Realtime Database
        db = Firebase.database

        //Initialize firestore
        fs = Firebase.firestore

        // Initialize Firebase Auth and check if the user is signed in
        auth = Firebase.auth

        this.userId = intent.getStringExtra("userId").toString()
        this.roomId = intent.getStringExtra("roomId").toString()

        //check if userId exist
        if (this.userId != "null") {
            val docRef = fs.collection("users").document(this.userId)
            var userExist = false
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                        userExist = true
                        //set room id
                        this.roomId = document.data?.get("roomId") as String
                    } else {
                        Log.d(TAG, "No such document")
                    }

                    if (!userExist) {
                        // Not signed in, launch the Sign In activity
                        startActivity(Intent(this, MenuActivity::class.java))
                        finish()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
        } else {
            // Not signed in, launch the Sign In activity
            startActivity(Intent(this, MenuActivity::class.java))
            finish()
            return
        }


        val messagesRef = db.reference.child(this.roomId).child(MESSAGES_CHILD)

        // The FirebaseRecyclerAdapter class and options come from the FirebaseUI library
        // See: https://github.com/firebase/FirebaseUI-Android
        val options = FirebaseRecyclerOptions.Builder<FriendlyMessage>()
            .setQuery(messagesRef, FriendlyMessage::class.java)
            .build()
        adapter = FriendlyMessageAdapter(options, getUserName())
        manager = LinearLayoutManager(this)
        manager.stackFromEnd = true
        binding.messageRecyclerView.layoutManager = manager
        binding.messageRecyclerView.adapter = adapter

        // Scroll down when a new message arrives
        // See MyScrollToBottomObserver for details
        adapter.registerAdapterDataObserver(
            MyScrollToBottomObserver(binding.messageRecyclerView, adapter, manager)
        )

        // Disable the send button when there's no text in the input field
        // See MyButtonObserver for details
        binding.messageEditText.addTextChangedListener(MyButtonObserver(binding.sendButton))

        // When the send button is clicked, send a text message
        binding.sendButton.setOnClickListener {
            val friendlyMessage = FriendlyMessage(
                binding.messageEditText.text.toString(),
                this.userId,
                getPhotoUrl(),
                null
            )

            db.reference.child(this.roomId).child(MESSAGES_CHILD).push().setValue(friendlyMessage)
            binding.messageEditText.setText("")
        }

        // When the image button is clicked, launch the image picker
        binding.addMessageImageView.setOnClickListener {
            openDocument.launch(arrayOf("image/*", "video/*", "/*"))
        }


    }

    public override fun onStart() {
        super.onStart()
        getSupportActionBar()?.setTitle(intent.getStringExtra("otherUser") + " - Online")
        //check if userId exist
        if (this.userId != "null") {
            val docRef = fs.collection("users").document(this.userId)
            var userExist = false
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                        userExist = true
                    } else {
                        Log.d(TAG, "No such document")
                    }

                    if (!userExist) {
                        // Not signed in, launch the Sign In activity
                        startActivity(Intent(this, MenuActivity::class.java))
                        finish()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }
        } else {
            // Not signed in, launch the Sign In activity
            startActivity(Intent(this, MenuActivity::class.java))
            finish()
            return
        }

        val docRef = fs.collection("users").document(intent.getStringExtra("otherUser").toString())

        docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                if (snapshot.data != null) {
                    if (snapshot.data!!["OnlineStatus"] as Boolean) {
                        getSupportActionBar()?.setTitle(intent.getStringExtra("otherUser") + " - Online")
                    } else {
                        getSupportActionBar()?.setTitle(intent.getStringExtra("otherUser") + " - Offline")
                    }

                }
            } else {
                getSupportActionBar()?.setTitle(intent.getStringExtra("otherUser") + " - Offline")
            }
        }

    }

    public override fun onPause() {
        adapter.stopListening()
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        adapter.startListening()
    }

    public override fun onDestroy() {
        super.onDestroy()
        //remove user on destroy
        fs.collection("users").document(this.userId).delete()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sign_out_menu -> {
                signOut()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onImageSelected(uri: Uri) {
        Log.d(TAG, "Uri: $uri")
        val user = auth.currentUser
        val tempMessage = FriendlyMessage(null, this.userId, getPhotoUrl(), LOADING_IMAGE_URL)
        db.reference
            .child(this.roomId)
            .child(MESSAGES_CHILD)
            .push()
            .setValue(
                tempMessage,
                DatabaseReference.CompletionListener { databaseError, databaseReference ->
                    if (databaseError != null) {
                        Log.w(
                            TAG, "Unable to write message to database.",
                            databaseError.toException()
                        )
                        return@CompletionListener
                    }

                    // Build a StorageReference and then upload the file
                    val storageRef =
                        Firebase.storage.reference.child(this.roomId!!).child(uri.lastPathSegment!!)

                    val key = databaseReference.key
//                            val storageReference = Firebase.storage
//                                    .getReference(user!!.uid)
//                                    .child(key!!)
//                                    .child(uri.lastPathSegment!!)
                    putImageInStorage(storageRef, uri, key)
                })
    }

    private fun putImageInStorage(storageReference: StorageReference, uri: Uri, key: String?) {
        // First upload the image to Cloud Storage
        storageReference.putFile(uri)
            .addOnSuccessListener(
                this
            ) { taskSnapshot -> // After the image loads, get a public downloadUrl for the image
                // and add it to the message.
                taskSnapshot.metadata!!.reference!!.downloadUrl
                    .addOnSuccessListener { uri ->
                        val friendlyMessage =
                            FriendlyMessage(null, this.userId, getPhotoUrl(), uri.toString())
                        db.reference
                            .child(this.roomId)
                            .child(MESSAGES_CHILD)
                            .child(key!!)
                            .setValue(friendlyMessage)
                    }
            }
            .addOnFailureListener(this) { e ->
                Log.w(
                    TAG,
                    "Image upload task was unsuccessful.",
                    e
                )
                Toast.makeText(
                    getApplicationContext(),
                    "Image upload unsuccessful!",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
    }

    private fun signOut() {
        fs.collection("users").document(this.userId).delete()
        startActivity(Intent(this, MenuActivity::class.java))
        finish()
    }

    private fun getPhotoUrl(): String? {
        val user = auth.currentUser
        return user?.photoUrl?.toString()
    }

    private fun getUserName(): String? {
        val user = auth.currentUser
        return if (user != null) {
            user.displayName
        } else ANONYMOUS
    }

    companion object {
        private const val TAG = "MainActivity"
        const val MESSAGES_CHILD = "messages"
        const val ANONYMOUS = "anonymous"
        private const val LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif"
    }
}
