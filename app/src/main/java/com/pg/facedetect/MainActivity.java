package com.pg.facedetect;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
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
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Toast;

import com.pg.facedetect.face.Box;
import com.pg.facedetect.face.MTCNN;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

import io.fotoapparat.Fotoapparat;
import io.fotoapparat.parameter.ScaleType;
import io.fotoapparat.preview.Frame;
import io.fotoapparat.preview.FrameProcessor;
import io.fotoapparat.selector.LensPositionSelectorsKt;
import io.fotoapparat.selector.ResolutionSelectorsKt;
import io.fotoapparat.view.CameraView;

public class MainActivity extends AppCompatActivity implements MainView{
    private CameraView cameraView;
    private Fotoapparat fotoapparat;
    private MTCNN mtcnn;

    private RenderScript rs;
    private ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic;
    private Type.Builder yuvType, rgbaType;
    private Allocation in, out;

    private final static int CAMERA_OK = 101;

    private static boolean DETECTING = false;
    private FaceDetectPresenter faceDetectPresenter;

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
        faceDetectPresenter = new FaceDetectPresenter(this);
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

    @Override
    public void faceDetectResult(DetectResult detectResult) {
        DETECTING = false;
    }

    @Override
    public void toastMsg(String msg) {
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }

    public class FaceDetectProcess implements FrameProcessor{

        @Override
        public void process(Frame frame) {
            if(DETECTING){
                return;
            }
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
            out.copyTo(bm);
            if(bm!=null) {
                Matrix m = new Matrix();
                int orientationDegree = getRotation();
                m.setRotate(orientationDegree, (float) bm.getWidth() / 2, (float) bm.getHeight() / 2);
                try {
                    Bitmap bm1 = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
                    Vector<Box> boxes = mtcnn.detectFaces(bm1, 60);
                    if(boxes.size()>0&&!DETECTING){
                        DETECTING = true;
                        File externalFilesDir = getExternalFilesDir("Caches");
                        if(!externalFilesDir.exists()){
                            externalFilesDir.mkdirs();
                        }
                        String filePath = externalFilesDir.getAbsolutePath()+"/1.jpg";
                        File file = new File(filePath);
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(file);
                            bm1.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            fos.flush();
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        faceDetectPresenter.detectFace(bm1,filePath);
                    }
                    Log.e("test", boxes.size() + "");
                } catch (OutOfMemoryError ex) {
                }
            }
        }

        private int getRotation(){
            int facingCamera = CameraCharacteristics.LENS_FACING_FRONT;
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                for (int i=0;i<manager.getCameraIdList().length;i++) {
                    String cameraId = manager.getCameraIdList()[i];
                    CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (facing!=null&&facing != facingCamera) {
                        continue;
                    }

                    Integer mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    int rotation = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
                    int degrees = 0;
                    switch (rotation){
                        case Surface.ROTATION_0:
                            degrees = 0;
                            break;
                        case Surface.ROTATION_90:
                            degrees = 90;
                            break;
                        case Surface.ROTATION_180:
                            degrees = 180;
                            break;
                        case Surface.ROTATION_270:
                            degrees = 270;
                            break;
                    }
                    int result;
                    if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        result = (mSensorOrientation + degrees - 360) % 360;
                        result = (360 + result) % 360;
                    } else {
                        // back-facing
                        result = (mSensorOrientation - degrees + 360) % 360;
                    }
                    return result;
                }
            } catch (Exception e){
                e.printStackTrace();
            }
            return 0;
        }
    }
}
