// # COMP 4521    #  KENT, KEVIN      20558962        KKENT@CONNECT.UST.HK
// # COMP 4521    # KONSTANTINO, HUBERT ADITYA 20560123 HAK@CONNECT.UST.HK

package com.linkchat.firebase.android.chatapp

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView.*
import com.bumptech.glide.Glide
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.linkchat.firebase.android.chatapp.MainActivity.Companion.ANONYMOUS
import com.linkchat.firebase.android.chatapp.databinding.FileMessageBinding
import com.linkchat.firebase.android.chatapp.databinding.ImageMessageBinding
import com.linkchat.firebase.android.chatapp.model.FriendlyMessage
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.linkchat.firebase.android.chatapp.databinding.MessageBinding


// The FirebaseRecyclerAdapter class and options come from the FirebaseUI library
// See: https://github.com/firebase/FirebaseUI-Android
class FriendlyMessageAdapter(
    private val options: FirebaseRecyclerOptions<FriendlyMessage>,
    private val currentUserName: String?
) :
    FirebaseRecyclerAdapter<FriendlyMessage, ViewHolder>(options) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_TEXT) {
            val view = inflater.inflate(R.layout.message, parent, false)
            val binding = MessageBinding.bind(view)
            MessageViewHolder(binding)
        } else {
//            val view = inflater.inflate(R.layout.image_message, parent, false)
//            val binding = ImageMessageBinding.bind(view)
//            ImageMessageViewHolder(binding)
            val view = inflater.inflate(R.layout.file_message, parent, false)
            val binding = FileMessageBinding.bind(view)
            FileMessageViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, model: FriendlyMessage) {
        if (options.snapshots[position].text != null) {
            (holder as MessageViewHolder).bind(model)
        } else {
            (holder as FileMessageViewHolder).bind(model)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (options.snapshots[position].text != null) VIEW_TYPE_TEXT else VIEW_TYPE_IMAGE
    }

    inner class MessageViewHolder(private val binding: MessageBinding) : ViewHolder(binding.root) {
        fun bind(item: FriendlyMessage) {
            binding.messageTextView.text = item.text
            setTextColor(item.name, binding.messageTextView)

            fun createNotificationChannel() {
                // Create the NotificationChannel, but only on API 26+ because
                // the NotificationChannel class is new and not in the support library
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val importance = NotificationManager.IMPORTANCE_DEFAULT
                    val channel = NotificationChannel(
                        "linkchat_channel",
                        "New LinkChat Message",
                        importance
                    ).apply {
                        description = "You have new message from LinkChat!"
                    }
                    // Register the channel with the system
                    val notificationManager: NotificationManager =
                        binding.messageTextView.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.createNotificationChannel(channel)
                }
            }

            val builder = NotificationCompat.Builder(binding.messageTextView.context, "linkchat_channel")
                .setSmallIcon(R.drawable.ic_account_circle_black_36dp)
                .setContentTitle("New Message")
                .setContentText(item.text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            createNotificationChannel()

            with(NotificationManagerCompat.from(binding.messageTextView.context)){
                notify(101, builder.build())
            }

            binding.messengerTextView.text = if (item.name == null) ANONYMOUS else item.name
            if (item.photoUrl != null) {
                loadImageIntoView(binding.messengerImageView, item.photoUrl!!)
            } else {
                binding.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp)
            }

        }

        private fun setTextColor(userName: String?, textView: TextView) {
            if (userName != ANONYMOUS && currentUserName == userName && userName != null) {
                textView.setBackgroundResource(R.drawable.rounded_message_blue)
                textView.setTextColor(Color.WHITE)
            } else {
                textView.setBackgroundResource(R.drawable.rounded_message_gray)
                textView.setTextColor(Color.BLACK)
            }
        }

    }

    inner class ImageMessageViewHolder(private val binding: ImageMessageBinding) :
        ViewHolder(binding.root) {
        fun bind(item: FriendlyMessage) {
            loadImageIntoView(binding.messageImageView, item.imageUrl!!)

            binding.messengerTextView.text = if (item.name == null) ANONYMOUS else item.name
            if (item.photoUrl != null) {
                loadImageIntoView(binding.messengerImageView, item.photoUrl!!)
            } else {
                binding.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp)
            }
        }
    }

    inner class FileMessageViewHolder(private val binding: FileMessageBinding) :
        ViewHolder(binding.root) {
        fun bind(item: FriendlyMessage) {
            var downloadid: Long = 0

            binding.fileMessageTextView.setOnClickListener {
                if (item.imageUrl!! != "https://www.google.com/images/spin-32.gif") {
                    var storageRef = Firebase.storage.getReferenceFromUrl(item.imageUrl!!)

                    storageRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            val downloadUrl = uri.toString()

                            var request = DownloadManager.Request(Uri.parse(downloadUrl))
                                .setTitle("File Download")
                                .setDescription("File Downloading...")
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setAllowedOverMetered(true)
                            var dm = binding.fileMessageTextView.context.getSystemService(
                                Context.DOWNLOAD_SERVICE
                            ) as DownloadManager

                            downloadid = dm.enqueue(request)

                            Log.w(TAG, downloadid.toString())
                        }
                        .addOnFailureListener { e ->
                            Log.w(
                                TAG,
                                "Getting download url was not successful.",
                                e
                            )
                        }

                }
            }

            var br = object : BroadcastReceiver() {
                override fun onReceive(p0: Context?, p1: Intent?) {
                    var id = p1?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadid) {
                        Toast.makeText(
                            binding.fileMessageTextView.context.applicationContext,
                            "File Download Completed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                }
            }

            binding.fileMessageTextView.context.registerReceiver(
                br,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )

            binding.fileMessengerTextView.text = if (item.name == null) ANONYMOUS else item.name
            if (item.photoUrl != null) {
                loadImageIntoView(binding.messengerImageView, item.photoUrl!!)
            } else {
                binding.messengerImageView.setImageResource(R.drawable.ic_account_circle_black_36dp)
            }
        }

        private fun createNotificationChannel() {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(
                    "linkchat_channel",
                    "New LinkChat Message",
                    importance
                ).apply {
                    description = "You have new message from LinkChat!"
                }
                // Register the channel with the system
                val notificationManager: NotificationManager =
                    binding.fileMessageTextView.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    private fun loadImageIntoView(view: ImageView, url: String) {
        if (url.startsWith("gs://")) {
            val storageReference = Firebase.storage.getReferenceFromUrl(url)
            storageReference.downloadUrl
                .addOnSuccessListener { uri ->
                    val downloadUrl = uri.toString()
                    Glide.with(view.context)
                        .load(downloadUrl)
                        .into(view)
                }
                .addOnFailureListener { e ->
                    Log.w(
                        TAG,
                        "Getting download url was not successful.",
                        e
                    )
                }
        } else {
            Glide.with(view.context).load(url).into(view)
        }
    }


    companion object {
        const val TAG = "MessageAdapter"
        const val VIEW_TYPE_TEXT = 1
        const val VIEW_TYPE_IMAGE = 2
    }
}
