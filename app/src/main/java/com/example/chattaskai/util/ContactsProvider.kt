package com.example.chattaskai.util

import android.content.Context
import android.provider.ContactsContract
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

data class ContactInfo(
    val name: String,
    val displayName: String
)

object ContactsProvider {
    fun getPhoneContacts(context: Context): List<ContactInfo> {
        // Check READ_CONTACTS permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        val contacts = mutableListOf<ContactInfo>()
        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            ),
            null,
            null,
            ContactsContract.Contacts.DISPLAY_NAME + " COLLATE NOCASE ASC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val displayName = it.getString(
                    it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
                )
                if (displayName.isNotBlank()) {
                    contacts.add(ContactInfo(displayName, displayName))
                }
            }
        }

        return contacts.distinctBy { it.name }
    }
}
