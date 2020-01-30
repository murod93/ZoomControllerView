package com.minmax.camerazoomcontroller

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.minmax.zoom.ZoomView
import java.nio.channels.FileLock

class MainActivity : AppCompatActivity() {

    val zoomControlView by lazy { findViewById<ZoomView>(R.id.zoom_controller_view) }
    val textView by lazy { findViewById<TextView>(R.id.textView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView.setOnClickListener {  }
    }
}
