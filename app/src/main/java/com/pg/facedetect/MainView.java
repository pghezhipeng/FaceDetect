package com.pg.facedetect;

import com.pg.facedetect.bean.DetectResult;
import com.pg.facedetect.bean.UserResult;

public interface MainView {
    void faceDetectResult(String idCard);
    void toastMsg(String msg);
    void showUserInfo(UserResult userResult);
}
