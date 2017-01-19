package io.ristretto.decaptcha.captcha;


import java.io.File;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CloudFlareReCaptcha extends AbstractCaptcha<CloudFlareReCaptcha.Challenge>{

    private final String baseURL;
    private final String siteKey;
    private final String secureToken;
    private final String validationURL;
    private final List<HttpCookie> baseCookies;

    public CloudFlareReCaptcha(String baseURL, String siteKey, String secureToken, String validationPath, List<HttpCookie> cookieList) {
        super();
        this.baseURL = checkUrl(baseURL);
        this.siteKey = checkSiteKey(siteKey);
        this.secureToken = checkSecureToken(secureToken);
        this.validationURL = getValidationUrlFromPath(baseURL, validationPath);
        this.baseCookies = new ArrayList<>(cookieList);
    }

    private static String getValidationUrlFromPath(String baseURL, String validationPath) {
        URL baseUrl = null;
        try {
            baseUrl = new URL(baseURL);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("baseURL not an url", e);
        }
        URL validationURL;
        try {
            validationURL = new URL(baseUrl.getProtocol(), baseUrl.getHost(), validationPath);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("can't construct url with validationPath", e);
        }
        if(validationURL.getQuery() != null) {
            throw new IllegalArgumentException("validationURL with existing query not implemented");
        }
        return validationURL.toString();
    }

    private static String checkUrl(String url) {
        if(url.isEmpty()) {
            throw new IllegalArgumentException("url is empty");
        }
        return url;
    }

    private static String checkSiteKey(String siteKey) {
        if(siteKey.isEmpty()) {
            throw new IllegalArgumentException("siteKey is empty");
        }
        return siteKey;
    }

    private static String checkSecureToken(String secureToken) {
        if(secureToken.isEmpty()) {
            throw new IllegalArgumentException("secureToken is empty");
        }
        return secureToken;
    }

    public String getIFrameUrl() {
        return "https://www.google.com/recaptcha/api/fallback?k=" + siteKey + "&stoken=" + secureToken;
    }

    public String getPayloadUrl(String challengeId) {
        return "https://www.google.com/recaptcha/api2/payload?c=" + challengeId + "&k=" + siteKey;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public String getValidationURL() {
        return validationURL;
    }

    public List<HttpCookie> getBaseCookies() {
        return new ArrayList<>(baseCookies);
    }

    public static class Challenge implements CaptchaChallenge {

        private final String identifier;
        private final File payloadImage;
        private final int imageColumns;
        private final int imageRows;
        private final String taskDescription;
        private final int numberOfAnswers;

        public Challenge(String identifier, File payloadImage, String taskDescription, int numberOfAnswers, int imageColumns, int imageRows) {
            if(imageColumns * imageRows < numberOfAnswers) {
                throw new IllegalArgumentException("imageColumns * imageRows < numberOfAnswers");
            }
            if(taskDescription.isEmpty()) {
                throw new IllegalArgumentException("taskDescription is empty");
            }
            if(numberOfAnswers <= 0) {
                throw new IllegalArgumentException("numberOfAnswers must be greater than 0");
            }
            this.identifier = identifier;
            this.taskDescription = taskDescription;
            this.numberOfAnswers = numberOfAnswers;
            this.imageColumns = imageColumns;
            this.imageRows = imageRows;
            this.payloadImage = payloadImage;
        }

        public String getIdentifier() {
            return identifier;
        }

        public int getNumberOfAnswers() {
            return numberOfAnswers;
        }

        public String getTaskDescription() {
            return taskDescription;
        }

        public int getImageColumns() {
            return imageColumns;
        }

        public int getImageRows() {
            return imageRows;
        }

        public File getPayloadImage() {
            return payloadImage;
        }
    }
}
