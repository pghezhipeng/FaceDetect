package com.pg.facedetect;

import com.pg.facedetect.bean.DetectResult;
import com.pg.facedetect.bean.UserResult;

import java.io.File;
import java.util.concurrent.TimeUnit;

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
import retrofit2.converter.gson.GsonConverterFactory;

public class FaceDetectPresenter {
    private final static int DEFAULT_TIMEOUT = 15;
    private Retrofit retrofit;
    private Retrofit userInfoRetrofit;
    private String baseUrl = "http://172.16.101.131:5001";
    private String userInfoUrl = "http://172.16.101.131";
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
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        userInfoRetrofit = new Retrofit.Builder()
                .baseUrl(userInfoUrl)
                .client(httpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public void getUserInfo(String idCard){
        Disposable disposable = userInfoRetrofit.create(FaceDetectApi.class).getUserInfo(idCard)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<UserResult>() {
                    @Override
                    public void accept(UserResult userResult) throws Exception {
                        if(userResult.getCode()==0){
                            userResult.setIdNumber(idCard);
                            mainView.showUserInfo(userResult);
                        }else{
                            mainView.toastMsg(userResult.getMsg());
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        mainView.toastMsg(throwable.getMessage());
                    }
                });
    }

    public void detectFace(String filePath) {
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
                            String idNum = detectResult.getId_num().substring(0,detectResult.getId_num().indexOf("."));
                            mainView.faceDetectResult(idNum);
                        }else {
                            mainView.toastMsg(detectResult.getMsg());
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        mainView.toastMsg(throwable.getMessage());
                    }
                });
    }

}
