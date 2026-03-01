package com.satyam.takesos   // ⚠ apna package name check kar lena

import android.content.Context

class ContactManager(context: Context) {

    private val prefs = context.getSharedPreferences("SOS_PREFS", Context.MODE_PRIVATE)

    fun saveContacts(numbers: List<String>) {
        prefs.edit().putStringSet("contacts", numbers.toSet()).apply()
    }

    fun getContacts(): List<String> {
        return prefs.getStringSet("contacts", emptySet())?.toList() ?: emptyList()
    }
}