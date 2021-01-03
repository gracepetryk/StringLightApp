package com.gracepetryk.StringLightControl

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ColorWheelView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var circleRadius : Float = 0f

    private val huePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val satPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val xPad : Float = (paddingLeft  + paddingRight).toFloat()
        val yPad : Float = (paddingTop + paddingBottom).toFloat()

        // ensure circle takes up all of the smaller dimension
        circleRadius = (w - xPad).coerceAtMost(h - yPad) * 0.48f

        recomputeShaders(w * 0.5f, h * 0.5f, circleRadius)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.apply {
            drawCircle(
                width.toFloat() / 2,
                height.toFloat() / 2,
                circleRadius,
                huePaint)
            drawCircle(
                width.toFloat() / 2,
                height.toFloat() / 2,
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
}