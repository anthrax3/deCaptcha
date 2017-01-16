package io.ristretto.decaptcha.captcha;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

public class CaptchaResult implements Parcelable{

    private List<String> cookies;

    protected CaptchaResult(Parcel in) {
        cookies = new ArrayList<>();
        in.readStringList(cookies);
    }

    public CaptchaResult(List<HttpCookie> cookies) {
        this.cookies = new ArrayList<>(cookies.size());
        for(HttpCookie cookie: cookies) {
            this.cookies.add(cookie.toString());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(cookies);
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

    @Override
    public String toString() {
        return "CaptchaResult{" +
                "cookies=" + cookies +
                '}';
    }
}
