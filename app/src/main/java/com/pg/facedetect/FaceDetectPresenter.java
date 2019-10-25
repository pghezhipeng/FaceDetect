package com.pg.facedetect;

import android.graphics.Bitmap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;

public class FaceDetectPresenter {
    private final static int DEFAULT_TIMEOUT = 15;
    private Retrofit retrofit;
    private String baseUrl = "http://172.16.101.131:5001";
    private MainView mainView;

    public FaceDetectPresenter(MainView mainView){
        this.mainView = mainView;
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .retryOnConnectionFailure(true)
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }

    public void detectFace(Bitmap img,String filePath) {
        final File file = new File(filePath);
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"),file);
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file",file.getName(),requestFile);
        Disposable disposable = retrofit.create(FaceDetectApi.class).recognition(filePart)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<DetectResult>() {
                    @Override
                    public void accept(DetectResult detectResult) throws Exception {
                        if(detectResult.getCode()==0){
                            mainView.toastMsg(detectResult.getId_num());
                        }else {
                            mainView.toastMsg(detectResult.getMsg());
                        }
                        file.delete();
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        mainView.toastMsg(throwable.getMessage());
                        file.delete();
                    }
                });
    }

}
