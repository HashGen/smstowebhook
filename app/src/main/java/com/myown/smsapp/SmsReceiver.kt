package com.myown.smsapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("SmsForwarderPrefs", Context.MODE_PRIVATE)
        val webhookUrl = prefs.getString("webhook_url", null)

        if (webhookUrl.isNullOrEmpty()) {
            Log.d("SmsReceiver", "Webhook URL not set. Ignoring SMS.")
            return
        }

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                forwardSmsToWebhook(webhookUrl, sms.originatingAddress, sms.messageBody)
            }
        }
    }

    private fun forwardSmsToWebhook(url: String, sender: String?, message: String?) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.doOutput = true
                val jsonPayload = JSONObject().apply {
                    put("sender", sender)
                    put("message", message)
                }.toString()
                OutputStreamWriter(connection.outputStream).use { it.write(jsonPayload) }
                Log.d("SmsReceiver", "Webhook Response Code: ${connection.responseCode}")
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error forwarding SMS: ${e.message}")
            }
        }
    }
}
