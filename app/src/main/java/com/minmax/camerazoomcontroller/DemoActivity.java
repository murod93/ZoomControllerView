package com.minmax.camerazoomcontroller;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.minmax.zoom.ZoomView;

public class DemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_demo);

        ZoomView zoomView = findViewById(R.id.zoom_controller_view);
        TextView textView = findViewById(R.id.textView);

        textView.setOnClickListener(view -> {
            zoomView.setProgress(0);
        });

        zoomView.setOnSliderProgressChangeListener(progress -> {
            Log.e("DemoActivity", String.format("progress: %s", String.valueOf(progress)));
            textView.setText(String.format("%.1f",4*progress));

        });
    }
}
