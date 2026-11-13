package com.example.colorlight

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var root: FrameLayout
    private lateinit var panel: LinearLayout
    private lateinit var seekR: SeekBar
    private lateinit var seekG: SeekBar
    private lateinit var seekB: SeekBar
    private lateinit var torchBtn: Button

    private var r = 255
    private var g = 0
    private var b = 0
    private var torchOn = false

    private val presets = intArrayOf(
        Color.RED, Color.rgb(255, 128, 0), Color.YELLOW, Color.GREEN,
        Color.CYAN, Color.BLUE, Color.MAGENTA, Color.WHITE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = 1f }

        root = FrameLayout(this)
        root.setOnClickListener {
            panel.visibility = if (panel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        panel = buildPanel()
        root.addView(
            panel,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
            )
        )
        setContentView(root)
        applyColor()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun buildPanel(): LinearLayout {
        val p = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(170, 0, 0, 0))
            setPadding(dp(16), dp(12), dp(16), dp(20))
            isClickable = true
        }

        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (c in presets) {
            val dot = View(this).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(c)
                    setStroke(dp(2), Color.WHITE)
                }
                setOnClickListener { setRgb(Color.red(c), Color.green(c), Color.blue(c)) }
            }
            row.addView(dot, LinearLayout.LayoutParams(0, dp(40), 1f).apply {
                setMargins(dp(3), 0, dp(3), 0)
            })
        }
        p.addView(row)

        seekR = addSlider(p, "R", Color.RED) { r = it; applyColor() }
        seekG = addSlider(p, "G", Color.GREEN) { g = it; applyColor() }
        seekB = addSlider(p, "B", Color.rgb(80, 140, 255)) { b = it; applyColor() }

        torchBtn = Button(this).apply {
            text = "Вспышка (LED): выкл"
            setOnClickListener { toggleTorch() }
        }
        p.addView(torchBtn)

        val hint = TextView(this).apply {
            text = "Тап по экрану — скрыть/показать панель"
            setTextColor(Color.LTGRAY)
            textSize = 12f
            gravity = Gravity.CENTER
        }
        p.addView(hint)
        return p
    }

    private fun addSlider(
        parent: LinearLayout, label: String, tint: Int, onChange: (Int) -> Unit
    ): SeekBar {
        val line = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        line.addView(TextView(this).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 16f
        }, LinearLayout.LayoutParams(dp(24), ViewGroup.LayoutParams.WRAP_CONTENT))

        val sb = SeekBar(this).apply {
            max = 255
            progressTintList = ColorStateList.valueOf(tint)
            thumbTintList = ColorStateList.valueOf(tint)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, value: Int, fromUser: Boolean) {
                    if (fromUser) onChange(value)
                }
                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        line.addView(sb, LinearLayout.LayoutParams(0, dp(44), 1f))
        parent.addView(line)
        return sb
    }

    private fun setRgb(nr: Int, ng: Int, nb: Int) {
        r = nr; g = ng; b = nb
        seekR.progress = r; seekG.progress = g; seekB.progress = b
        applyColor()
    }

    private fun applyColor() {
        root.setBackgroundColor(Color.rgb(r, g, b))
        if (::seekR.isInitialized && seekR.progress != r) seekR.progress = r
        if (::seekG.isInitialized && seekG.progress != g) seekG.progress = g
        if (::seekB.isInitialized && seekB.progress != b) seekB.progress = b
    }

    private fun toggleTorch() {
        try {
            val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull() ?: run {
                Toast.makeText(this, "Вспышка не найдена", Toast.LENGTH_SHORT).show()
                return
            }
            torchOn = !torchOn
            cm.setTorchMode(id, torchOn)
            torchBtn.text = "Вспышка (LED): " + if (torchOn) "вкл" else "выкл"
        } catch (e: Exception) {
            torchOn = false
            Toast.makeText(this, "Не удалось включить вспышку", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        if (torchOn) toggleTorch()
    }
}
