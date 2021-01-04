package com.gracepetryk.StringLightControl

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatDelegate
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var queue : RequestQueue
    private val baseUrl = "http://192.168.0.23:5000"

    private var async = false

    init {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // disable night mode
        setContentView(R.layout.activity_main)

        queue = Volley.newRequestQueue(this)
        readPowerState(findViewById(R.id.onOffSwitch))


        findViewById<ColorWheelView>(R.id.colorWheelView).addColorChangeListener(object : ColorChangeListener {
            override fun onColorChange(color: Int) {
                // set mode to solid
                findViewById<RadioButton>(R.id.radioButton).isChecked = true

                // extract individual colors using bitmasks
                val r = color.and(0xFF0000).shr(16)
                val g = color.and(0x00FF00).shr(8)
                val b = color.and(0x0000FF)

                // send request to raspberry pi to change light color
                val endpoint = "/setcolor"
                val opts = "?r=$r&g=$g&b=$b"

                val url = baseUrl + endpoint + opts

                val stringRequest = StringRequest(Request.Method.GET, url,
                    Response.Listener <String>{},
                    Response.ErrorListener { error -> println(error) })
                queue.add(stringRequest)

                val rEditText : EditText = findViewById(R.id.rColorInput)
                val gEditText : EditText = findViewById(R.id.gColorInput)
                val bEditText : EditText = findViewById(R.id.bColorInput)

                rEditText.setText(r.toString())
                gEditText.setText(g.toString())
                bEditText.setText(b.toString())

            }

        })



    }

    fun togglePower(view: View) {
        val switch: SwitchMaterial = findViewById(R.id.onOffSwitch)
        send_request(if (switch.isChecked) "/on" else "/off")
    }


    private fun readPowerState(switch: SwitchMaterial) {

        val endpoint = "/powerstate"

        val url = baseUrl + endpoint

        val stringRequest = StringRequest(Request.Method.GET, url,
            Response.Listener <String>{ response ->
                if (response == "True") {
                    switch.isChecked = true
                } else if (response == "False") {
                    switch.isChecked = false
                }
            },
            Response.ErrorListener { _ ->
                println("ERROR!")
            })

        queue.add(stringRequest)
    }

    fun setModeSolid(view: View) {
        setAsync(false)
        send_request("/mode_solid")
    }

    fun setModeFade(view: View) {
        if (async) {
            send_request("/mode_fade_async")
        } else {
            send_request("/mode_fade")
        }
    }

    fun setModeJump(view: View) {
        if (async) {
            send_request("/mode_jump_async")
        } else {
            send_request("/mode_jump")
        }
    }

    fun toggleAsync(view: View) {
        setAsync(findViewById<SwitchMaterial>(R.id.asyncSwitch).isChecked)
    }

    private fun setAsync(state : Boolean) {
        async = state

        findViewById<SwitchMaterial>(R.id.asyncSwitch).isChecked = async

        val fadeButton = findViewById<RadioButton>(R.id.radioButton2)
        val jumpButton = findViewById<RadioButton>(R.id.radioButton3)

        if (fadeButton.isChecked) {
            setModeFade(fadeButton)
        } else if (jumpButton.isChecked) {
            setModeJump(jumpButton)
        }
    }

    private fun send_request(endpoint: String) {
        val url = baseUrl + endpoint

        val stringRequest = StringRequest(Request.Method.GET, url,
            Response.Listener <String>{},
            Response.ErrorListener { e ->
                println(e)
            })

        queue.add(stringRequest)
    }




}