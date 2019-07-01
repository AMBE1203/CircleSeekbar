package com.ambe.circleseekbar;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CircleSeekbar mySeekBar = findViewById(R.id.mySeekbar);
        final TextView txtProgress = findViewById(R.id.txtProgress);

        mySeekBar.setOnSeekbarCircleChangeListener(new CircleSeekbar.OnSeekbarCircleChangeListener() {
            @Override
            public void onProgressChanged(CircleSeekbar seekbar, int progress, boolean fromUser) {
                if (fromUser) {
                    txtProgress.setText(progress + "");
                }
            }

            @Override
            public void onStartTrackingTouch(CircleSeekbar seekbar) {

            }

            @Override
            public void onStopTrackingTouch(CircleSeekbar seekbar) {

            }
        });


    }
}
