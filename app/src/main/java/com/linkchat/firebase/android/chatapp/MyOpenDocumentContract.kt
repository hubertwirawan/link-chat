// # COMP 4521    #  KENT, KEVIN      20558962        KKENT@CONNECT.UST.HK
// # COMP 4521    # KONSTANTINO, HUBERT ADITYA 20560123 HAK@CONNECT.UST.HK
package com.linkchat.firebase.android.chatapp

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContracts

/**
 * See:
 * https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.OpenDocument
 */
class MyOpenDocumentContract : ActivityResultContracts.OpenDocument() {

    override fun createIntent(context: Context, input: Array<out String>): Intent {
        val intent = super.createIntent(context, input)
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        return intent;
    }
}