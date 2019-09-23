package com.bikcrum.flightcheck

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


class MyService : Service() {
    override fun onBind(p0: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    val TAG = "flightcheck" + "MyService";
    val CHANNEL_ID = "com.bikcrum.flightcheck.channel.id"

    val timer = Timer()
    val myTimerTask = MyTimerTask()

    var notificationTitle = "Getting flight info"
    var notificationBody = "Getting flight info"

    override fun onCreate() {
        super.onCreate()

        updateNotification(true)
    }

    private fun updateNotification(startForeground: Boolean) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;

        notificationManager.createNotificationChannel(
            channel
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_flight)
            .setContentTitle(notificationTitle)
            .setContentText(notificationTitle)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(notificationBody)
            ).build()

        if (startForeground) {
            startForeground(1, notification)
        }

        notificationManager.notify(1, notification)
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        Log.d(TAG, "onStartCommand")

        timer.purge()
        timer.schedule(myTimerTask, 0, 60000)

        return START_STICKY;
    }

    private fun updateFlightInfo() {
        Log.d(TAG, "updateFlightInfo")

        val request = object : StringRequest(Request.Method.POST,
            "https://khalti.com/api/v2/service/use/flight/search/",
            Response.Listener { response ->
                Log.d(TAG, "response=" + response)
                processResponse(response)
            },
            Response.ErrorListener { error ->
                processError(error)
            }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val params = HashMap<String, String>()

                params["cookie"] =
                    "_gcl_au=1.1.2100989977.1568820963; _ga=GA1.2.23524732.1568820964; _fbp=fb.1.1568820964264.478809134; _gid=GA1.2.418830125.1568998070; csrftoken=uGbo4WZfSMITHtfjiRnRrOoYRWwTbqrAPoPo1VK4vHxL4yOIcCLTZh589lmEV7Vs; sessionid=g1j960uhh819vv0laik77bqa75hz2swt"

                return params
            }

            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()

                params["flight_type"] = "D"
                params["trip_type"] = "O"
                params["from"] = "KTM"
                params["to"] = "BIR"
                params["flight_date"] = "2019-10-4"
                params["return_date"] = "2019-10-5"
                params["nationality"] = "NP"
                params["adult"] = "1"
                params["child"] = "0"

                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun processError(error: VolleyError?) {
        if (error == null) {
            notificationTitle = "error"
            notificationBody = "error is null"
        } else if (error.networkResponse == null) {
            notificationTitle = "error"
            notificationBody = "error.networkResponse is null"
        } else {
            notificationTitle = "error"
            notificationBody = String(error.networkResponse.data)
        }

        updateNotification(false)
    }

    private fun processResponse(response: String?) {
        if (response == null) {
            notificationTitle = "error"
            notificationBody = "response is null"
        } else {
            try {
                val records =
                    JSONObject(response).optJSONObject("outbound").optJSONArray("records")
                if (records == null) {
                    notificationTitle = "error"
                    notificationBody = "records is null"
                } else if (records.length() > 0) {
                    val record = records.optJSONObject(0)
                    val simpleDateFormat = SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.ENGLISH)
                    notificationTitle = String.format(
                        "%.2f, %.2f, %s, %s",
                        record.optDouble("fare_total"),
                        record.optDouble("bonus"),
                        record.optBoolean("refundable"),
                        simpleDateFormat.format(System.currentTimeMillis())
                    )
                    notificationBody = record.toString()
                } else {
                    notificationTitle = "error"
                    notificationBody = "records are empty"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                notificationTitle = "error"
                notificationBody = e.message.toString()
            }
        }

        updateNotification(false)
    }

    inner class MyTimerTask : TimerTask() {

        override fun run() {
            updateFlightInfo()
        }

    }
}
