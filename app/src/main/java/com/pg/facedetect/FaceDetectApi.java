package com.pg.facedetect;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface FaceDetectApi {
    @Multipart
    @POST("/recognition")
    Observable<DetectResult> recognition(@Part MultipartBody.Part part);
}
