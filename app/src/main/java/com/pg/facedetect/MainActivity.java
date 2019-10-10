package com.pg.facedetect;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.pg.facedetect.face.Box;
import com.pg.facedetect.face.MTCNN;

import java.util.Vector;

import io.fotoapparat.Fotoapparat;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.preview.Frame;
import io.fotoapparat.preview.FrameProcessor;
import io.fotoapparat.selector.LensPositionSelectorsKt;
import io.fotoapparat.selector.ResolutionSelectorsKt;
import io.fotoapparat.view.CameraView;

public class MainActivity extends AppCompatActivity {
    private CameraView cameraView;
    private Fotoapparat fotoapparat;
    private MTCNN mtcnn;

    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;

    private final static int CAMERA_OK = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraView = findViewById(R.id.camera_view);
        mtcnn = new MTCNN(getAssets());
        rs = RenderScript.create(this);
        yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
        fotoapparat = Fotoapparat.with(this)
                .into(cameraView)
                .previewScaleType(ScaleType.CenterCrop)
                .photoResolution(ResolutionSelectorsKt.highestResolution())
                .lensPosition(LensPositionSelectorsKt.front())
                .frameProcessor(new FaceDetectProcess())
                .build();
    }

    private boolean checkCameraPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(ActivityCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{android.Manifest.permission.CAMERA},CAMERA_OK);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case CAMERA_OK:
                if (grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    //这里已经获取到了摄像头的权限，想干嘛干嘛了可以
                    fotoapparat.start();
                }else {
                    //这里是拒绝给APP摄像头权限，给个提示什么的说明一下都可以。
                    Toast.makeText(MainActivity.this,"请手动打开相机权限",Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(checkCameraPermission()) {
            fotoapparat.start();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        fotoapparat.stop();
    }

    public class FaceDetectProcess implements FrameProcessor{

        @Override
        public void process(Frame frame) {
            if (yuvType == null) {
                yuvType = new Type.Builder(rs, Element.U8(rs)).setX(frame.getImage().length);
                in = Allocation.createTyped(rs, yuvType.create(), Allocation.USAGE_SCRIPT);

                rgbaType = new Type.Builder(rs, Element.RGBA_8888(rs)).setX(frame.getSize().width).setY(frame.getSize().height);
                out = Allocation.createTyped(rs, rgbaType.create(), Allocation.USAGE_SCRIPT);
            }

            in.copyFrom(frame.getImage());

            yuvToRgbIntrinsic.setInput(in);
            yuvToRgbIntrinsic.forEach(out);
            Bitmap bm =Bitmap.createBitmap(frame.getSize().width, frame.getSize().height, Bitmap.Config.ARGB_8888);
//            Bitmap bm = BitmapFactory.decodeByteArray(frame.getImage(),0,frame.getImage().length);
            out.copyTo(bm);
            if(bm!=null) {
                Vector<Box> boxes = mtcnn.detectFaces(bm, 60);
                Log.e("test", boxes.size() + "");
            }
        }
    }
}
