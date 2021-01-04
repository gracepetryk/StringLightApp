package com.gracepetryk.StringLightControl

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.DragEvent
import android.view.MotionEvent
import android.view.View
import android.os.Handler
import android.os.Looper
import android.os.Message
import java.lang.Exception
import java.lang.Math.atan2
import java.lang.Math.toDegrees
import kotlin.math.abs
import kotlin.math.sqrt

@SuppressLint("ClickableViewAccessibility")
class ColorWheelView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var circleRadius : Float = 0f
    private var centerX : Float = 0f
    private var centerY : Float = 0f

    private val huePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val satPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val colorChangeListeners = ArrayList<ColorChangeListener>()

    private val handler = object : Handler(Looper.getMainLooper()) {}

    init {
        this.setOnTouchListener(object : View.OnTouchListener {
            private var inDrag = false // flag to keep track of weather or not we're in a drag motion
            private var lastColor = 0x000000 // keep track of last color so we don't send duplicates
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                if (event?.action == MotionEvent.ACTION_DOWN) {
                    inDrag = true

                    val colorChangeRunnable = object : Runnable {
                        override fun run() {
                            try {
                                // set x and y to align with view coordinates
                                val viewCords = IntArray(2)
                                this@ColorWheelView.getLocationOnScreen(viewCords)
                                var x : Float = event.x - viewCords[0]
                                var y : Float = event.y - viewCords[1]

                                // transform x and y to a coordinate system with origin at the circles center
                                x -= centerX
                                y = -1 * (y - centerY)

                                // convert units to be in terms of radii
                                x /= circleRadius
                                y /= circleRadius

                                // convert to polar
                                val r = (sqrt(x * x + y * y)).coerceAtMost(1f)
                                var theta: Float =
                                    toDegrees(kotlin.math.atan2(y, x).toDouble()).toFloat()

                                if (theta < 0) {
                                    theta += 360
                                }

                                val color = hsv2rgb(theta, r, 1f)
                                if (color != lastColor) {
                                    notifyColorChangeListeners(color)
                                    lastColor = color
                                }
                            } catch (e: Exception) {
                                System.err.println(e)
                            } finally {
                                // poll every x ms if we're still touching
                                if (inDrag) {
                                    handler.postDelayed(this, 50)
                                }
                            }
                        }
                    }

                    handler.post(colorChangeRunnable)
                    return inDrag
                }
                if (event?.action == MotionEvent.ACTION_UP) {
                    inDrag = false
                    return inDrag
                }
                return false
            }
        })
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val xPad : Float = (paddingLeft  + paddingRight).toFloat()
        val yPad : Float = (paddingTop + paddingBottom).toFloat()

        // ensure circle takes up all of the smaller dimension
        circleRadius = (w - xPad).coerceAtMost(h - yPad) * 0.48f

        centerX = w * 0.5f
        centerY = h * 0.5f

        recomputeShaders(centerX, centerY, circleRadius)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.apply {
            drawCircle(
                centerX,
                centerY,
                circleRadius,
                huePaint)
            drawCircle(
                centerX,
                centerX,
                circleRadius,
                satPaint
            )
        }

    }




    private fun recomputeShaders(cx: Float, cy: Float, radius: Float) {
        // computes shaders for drawing the HSV color wheel
        // https://en.wikipedia.org/wiki/HSL_and_HSV

        val hueShader = SweepGradient(
            cx,
            cy,
            intArrayOf( // int conversions required because of bug in kotlin compiler
                hex2color(0xFF0000), // red
                hex2color(0xFF00FF), // magenta
                hex2color(0x0000FF), // blue
                hex2color(0x00FFFF), // cyan
                hex2color(0x00FF00), // green
                hex2color(0xFFFF00), // yellow
                hex2color(0xFF0000) // back to red
            ),
            floatArrayOf(
                0/6f,
                1/6f,
                2/6f,
                3/6f,
                4/6f,
                5/6f,
                6/6f
            ))

        huePaint.shader = hueShader

        val satShader = RadialGradient(
            cx,
            cy,
            radius,
            0xFFFFFFFF.toInt(),
            0x00FFFFFF,
            Shader.TileMode.CLAMP)

        satPaint.shader = satShader

    }

    /**
     * returns the android hex representation of a [color]
     *
     * @param color a 24 bit rgb color in the format 0xRRGGBB
     */
    private fun hex2color(color : Int) : Int {
        return 0xFF000000.toInt().or(color)
    }

    private fun hsv2rgb(hue: Float, saturation: Float, value: Float): Int {
        /**
         * converts an hsv color to rgba
         *
         * @param hue: [0, 360]
         * @param saturation: [0, 1]
         * @param value: [0, 1]
         * @return: (r, g, b) the color represented in rgb with values [0, 256)
         */



        var r = 0f
        var g = 0f
        var b = 0f

        val chroma = value * saturation
        val  h_prime = hue / 60

        val x : Float = chroma * (1 - abs(h_prime % 2 - 1))

        // find what side of the rgb cube we're on
        when {
            h_prime <= 1 -> {
                r = chroma
                g = x
                b = 0f
            }
            h_prime <= 2 -> {
                r = x
                g = chroma
                b = 0f
            }
            h_prime <= 3 -> {
                r = 0f
                g = chroma
                b = x
            }
            h_prime <= 4 -> {
                r = 0f
                g = x
                b = chroma
            }
            h_prime <= 5 -> {
                r = x
                g = 0f
                b = chroma
            }
            h_prime <= 6 -> {
                r = chroma
                g = 0f
                b = x
            }
        }

        val m = value - chroma
        val rInt = (255 * (m + r)).toInt()
        val gInt = (255 * (m + g)).toInt()
        val bInt = (255 * (m + b)).toInt()

        var color = 0x000000
        color = color.or(rInt.shl(16))
        color = color.or(gInt.shl(8))
        color = color.or(bInt)

        return color
    }

    fun addColorChangeListener(listener: ColorChangeListener) {
        colorChangeListeners.add(listener)
    }

    private fun notifyColorChangeListeners(color: Int) {
        for (listener in colorChangeListeners) {
            listener.onColorChange(color)
        }
    }


}