package io.ristretto.decaptcha.captcha;

import android.os.Parcel;
import android.os.Parcelable;

public class CaptchaResult implements Parcelable{


    public CaptchaResult() {

    }

    protected CaptchaResult(Parcel in) {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    public static final Creator<CaptchaResult> CREATOR = new Creator<CaptchaResult>() {
        @Override
        public CaptchaResult createFromParcel(Parcel in) {
            return new CaptchaResult(in);
        }

        @Override
        public CaptchaResult[] newArray(int size) {
            return new CaptchaResult[size];
        }
    };
}
