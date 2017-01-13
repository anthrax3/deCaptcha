package io.ristretto.decaptcha.data;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class CloudFlareReCaptcha extends CaptchaImpl {

    private String siteKey;
    private String secureToken;
    private Challenge challenge;

    public CloudFlareReCaptcha(String baseURL, String siteKey, String secureToken) {
        super(baseURL);
        this.siteKey = siteKey;
        this.secureToken = secureToken;
        this.challenge = null;
    }

    @NonNull
    public String getIFrameUrl() {
        return "https://www.google.com/recaptcha/api/fallback?k=" + siteKey + "&stoken=" + secureToken;
    }

    public String getPayloadUrl(String challengeId) {
        return "https://www.google.com/recaptcha/api2/payload?c=" + challengeId + "&k=" + siteKey;
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
    }

    public Challenge getChallenge() {
        return challenge;
    }

    public static class Challenge {
        private final String identifier;
        private ArrayList<Bitmap> payloadImages;
        private String taskDescription;
        private int numberOfAnswers;

        public Challenge(String identifier, String taskDescription, int numberOfAnswers, ArrayList<Bitmap> payload) {
            this.identifier = identifier;
            this.taskDescription = taskDescription;
            this.numberOfAnswers = numberOfAnswers;
            this.payloadImages = payload;
        }

        public String getIdentifier() {
            return identifier;
        }

        public int getNumberOfAnswers() {
            return numberOfAnswers;
        }

        public List<Bitmap> getPayloadImages() {
            return new ArrayList<>(payloadImages);
        }
    }
}
