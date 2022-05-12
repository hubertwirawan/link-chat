// # COMP 4521    #  KENT, KEVIN      20558962        KKENT@CONNECT.UST.HK
// # COMP 4521    # KONSTANTINO, HUBERT ADITYA 20560123 HAK@CONNECT.UST.HK

package com.linkchat.firebase.android.chatapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.linkchat.firebase.android.chatapp.databinding.ActivityUrlSignInBinding
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions


class MenuActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUrlSignInBinding

    private lateinit var fs: FirebaseFirestore
    private var userId = ""
    private var roomId = ""
    private var otherUser = ""
    private var onlineAble = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fs = Firebase.firestore

        // Create user with random id
        val randomNum = (0..999).random()
        val userId = "User${randomNum}"

        this.userId = userId

        binding = ActivityUrlSignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startChatButton.setOnClickListener {
            goToMainActivity()
        }

        binding.copyUrlButton.setOnClickListener {
            Toast.makeText(getApplicationContext(), "User ID copied!", Toast.LENGTH_SHORT)
                .show()
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("UserId", this.userId)
            clipboard.setPrimaryClip(clip)
        }

        binding.urlEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (binding.urlEditText.text.toString() != userId) {
                    otherUser = binding.urlEditText.text.toString()
                    onlineAble = true
                } else {
                    onlineAble = false
                    Toast.makeText(
                        getApplicationContext(),
                        "Cannot fill with own user ID!",
                        Toast.LENGTH_SHORT
                    ).show();

                }
            }

            override fun afterTextChanged(p0: Editable?) {
            }

        })

    }

    public override fun onStart() {
        super.onStart()
        val user = hashMapOf(
            "OnlineStatus" to false
        )

        // Add a new document with a generated ID
        fs.collection("users")
            .document(this.userId)
            .set(user)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: $documentReference")
                val textView: TextView = findViewById<TextView>(R.id.urlId)
                textView.text = userId
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
                Toast.makeText(getApplicationContext(), "Error adding data!", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    public override fun onDestroy() {
        super.onDestroy()
        //remove user on destroy
//        fs.collection("users").document(this.userId).delete()
    }

    private fun goToMainActivity() {

        //Set online status of user to true
        if(onlineAble){
            val data = hashMapOf("OnlineStatus" to true)

            fs.collection("users").document(this.userId)
                .set(data, SetOptions.merge())
        }


        //check if other user is online
        if (this.otherUser != "null" && this.otherUser != "") {
            val docRef = fs.collection("users").document(this.otherUser)


            //Check Other user if online
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        if (document.data != null) {
                            Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                            if (document.data!!["OnlineStatus"] as Boolean) {
                                val intent = Intent(this, MainActivity::class.java)
                                intent.putExtra("userId", this.userId)
                                startActivity(intent)
                                finish()
                            } else {
                                binding.waitingRoom.setVisibility(View.VISIBLE)
                                Toast.makeText(
                                    getApplicationContext(),
                                    "User ${this.otherUser} Not Online Yet!",
                                    Toast.LENGTH_SHORT
                                ).show();
                                //create new room
                                var roomId: String = "null"
                                fs.collection("rooms").add(
                                    hashMapOf(
                                        "users" to listOf<String>(
                                            this.userId,
                                            this.otherUser
                                        )
                                    )
                                ).addOnSuccessListener { document ->
                                    roomId = document.id
                                    this.roomId = roomId

                                    val data = hashMapOf("roomId" to roomId)

                                    //set room id to each users
                                    fs.collection("users").document(this.userId)
                                        .set(data, SetOptions.merge())

                                    fs.collection("users").document(this.otherUser)
                                        .set(data, SetOptions.merge())
                                }.addOnFailureListener { e ->
                                    Log.w(TAG, "Error adding document", e)
                                    Toast.makeText(
                                        getApplicationContext(),
                                        "Error adding data!",
                                        Toast.LENGTH_SHORT
                                    )
                                        .show()
                                }

                            }
                        } else {
                            Log.d(TAG, "No such document")
                            Toast.makeText(
                                getApplicationContext(),
                                "User not found!",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "get failed with ", exception)
                }


            //wait for other user to go online
            docRef.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    if (snapshot.data != null) {
                        Log.d(TAG, "Current data: ${snapshot.data}")

                        if (snapshot.data!!["OnlineStatus"] as Boolean) {
                            binding.waitingRoom.setVisibility(View.GONE)
                            val intent = Intent(this, MainActivity::class.java)
                            this.roomId = snapshot.data!!["roomId"] as String
                            if (this.roomId != "" && this.roomId != "null" && this.roomId != null) {
                                intent.putExtra("roomId", this.roomId)
                                intent.putExtra("userId", this.userId)
                                intent.putExtra("otherUser", this.otherUser)
                                startActivity(intent)
                                finish()
                            }
                        }

                    }

                } else {
                    Log.d(TAG, "Current data: null")
                    Toast.makeText(
                        getApplicationContext(),
                        "User no longer exists!",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    binding.waitingRoom.setVisibility(View.GONE)
                }
            }
        } else {
            onlineAble = false
            Toast.makeText(getApplicationContext(), "Fill other user ID!", Toast.LENGTH_SHORT)
                .show()
        }

    }

    companion object {
        private const val TAG = "MenuActivity"
    }

}