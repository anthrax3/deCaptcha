package io.ristretto.decaptcha.captcha;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class ParcableCaptchaResult extends CaptchaResult implements Parcelable{

    private static ParcableCaptchaResult fromParcable(Parcel in) {
        List<String> cookies = new ArrayList<>();
        in.readStringList(cookies);
        return new ParcableCaptchaResult(cookies);
    }

    public ParcableCaptchaResult(List<String> cookies) {
        super(cookies);
    }

    public ParcableCaptchaResult(CaptchaResult captchaResult) {
        super(new ArrayList<>(captchaResult.getCookies()));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(getCookies());
    }

    public static final Creator<ParcableCaptchaResult> CREATOR = new Creator<ParcableCaptchaResult>() {
        @Override
        public ParcableCaptchaResult createFromParcel(Parcel in) {
            return ParcableCaptchaResult.fromParcable(in);
        }

        @Override
        public ParcableCaptchaResult[] newArray(int size) {
            return new ParcableCaptchaResult[size];
        }
    };

}
