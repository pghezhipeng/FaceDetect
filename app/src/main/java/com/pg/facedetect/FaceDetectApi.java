package com.pg.facedetect;

import com.pg.facedetect.bean.DetectResult;
import com.pg.facedetect.bean.UserResult;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface FaceDetectApi {
    @Multipart
    @POST("/recognition")
    Observable<DetectResult> recognition(@Part MultipartBody.Part part);

    @FormUrlEncoded
    @POST("/userInfo")
    Observable<UserResult> getUserInfo(@Field("idCard")String idCard);
}
