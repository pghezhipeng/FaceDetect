package com.pg.facedetect;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pg.facedetect.bean.DetectResult;
import com.pg.facedetect.bean.UserResult;
import com.pg.facedetect.face.Box;
import com.pg.facedetect.face.ImageUtils;
import com.pg.facedetect.face.MTCNN;
import com.pg.facedetect.face.RecycleBitmap;

import java.io.File;
import java.io.FileInputStream;
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
    private AlertDialog alertDialog;
    private ImageView headView;
    private TextView userNameView;

    private String idNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        idNumber = getIntent().getStringExtra("ID_NUM");
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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = View.inflate(getApplication(),R.layout.dialog_userinfo,null);
        builder.setView(view);
        builder.setCancelable(false);
        builder.setTitle("识别结果");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                alertDialog.dismiss();
            }
        });
        alertDialog = builder.create();
        headView = view.findViewById(R.id.iv_user);
        userNameView = view.findViewById(R.id.tv_user);
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                DETECTING = false;
                RecycleBitmap.recycleImageView(headView);
                headView.setImageDrawable(null);
            }
        });
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
    public void faceDetectResult(String idCard) {
        faceDetectPresenter.getUserInfo(idCard);
    }

    @Override
    public void toastMsg(String msg) {
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
        DETECTING = false;
    }

    @Override
    public void showUserInfo(UserResult userResult) {
        String userName = userResult.getName();
        String head = getFilePath();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(head);
            Bitmap bitmap  = BitmapFactory.decodeStream(fis);
            headView.setImageBitmap(bitmap);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(userResult.getIdNumber().equals(idNumber)) {
            userNameView.setText("用户：" + userName);
        }else{
            userNameView.setText("身份证不匹配");
        }
        alertDialog.show();
    }

    private String getFilePath(){
        File externalFilesDir = getExternalFilesDir("Caches");
        if(!externalFilesDir.exists()){
            externalFilesDir.mkdirs();
        }
        String filePath = externalFilesDir.getAbsolutePath()+"/1.jpg";
        return filePath;
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
                        int maxArea = boxes.get(0).area();
                        Rect rect = getFaceRect(boxes.get(0),bm1.getWidth(),bm1.getHeight());
                        if(boxes.size()>1)
                        for (Box box:boxes){
                            if(box.area()>maxArea){
                                maxArea = box.area();
                                rect = getFaceRect(box,bm1.getWidth(),bm1.getHeight());
                            }
                        }
                        Bitmap cropBitmap = ImageUtils.cropBitmap(bm1, rect, false);
                        DETECTING = true;

                        File file = new File(getFilePath());
                        if(file.exists()){
                            file.delete();
                        }
                        FileOutputStream fos = null;
                        try {
                            fos = new FileOutputStream(file);
                            cropBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                            fos.flush();
                            fos.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        cropBitmap.recycle();
                        cropBitmap = null;
                        faceDetectPresenter.detectFace(getFilePath());
                    }
                    Log.e("test", boxes.size() + "");
                } catch (OutOfMemoryError ex) {
                }
            }
        }

        private Rect getFaceRect(Box box,int maxWidth,int maxHeight){
            double scaleSize = 1.5;
            int scaleLength = box.width();
            if(box.height()>scaleLength){
                scaleLength = box.height();
            }
            if(scaleSize*scaleLength>=maxHeight){
                scaleSize = maxHeight/scaleLength;
            }
            double newX = box.left() - (((scaleSize-1)*scaleLength)/2);
            if(newX<0){
                newX = 0;
            }
            if((newX+(scaleSize*scaleLength))>maxWidth){
                newX = maxWidth - (scaleSize*scaleLength);
            }
            double newY = box.top() - (((scaleSize-1)*scaleLength)/2);
            if(newY < 0){
                newY = 0;
            }
            if(newY + (scaleSize*scaleLength)>maxHeight){
                newY = maxHeight - (scaleSize*scaleLength);
            }
            Rect rect = new Rect((int)newX,(int)newY,(int)(newX+(scaleSize*scaleLength)),(int)(newY+(scaleSize*scaleLength)));
            return rect;
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
