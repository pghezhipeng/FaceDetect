package com.pg.facedetect;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private MTCNN mtcnn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mtcnn = new MTCNN(getAssets());

//        mtcnn.detectFaces(bm,40);
    }
    @Override
    protected void onResume() {
        super.onResume();
    }
}
