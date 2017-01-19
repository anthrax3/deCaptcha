package io.ristretto.decaptcha.manager;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import io.ristretto.decaptcha.captcha.CaptchaResult;
import io.ristretto.decaptcha.captcha.CloudFlareReCaptcha;
import io.ristretto.decaptcha.captcha.CloudFlareReCaptcha.Challenge;
import io.ristretto.decaptcha.net.Downloader;
import io.ristretto.decaptcha.net.HttpHeaders;
import io.ristretto.decaptcha.net.PostDataBuilder;


public class CloudFlareCaptchaManager extends AbstractCaptchaManager<Challenge, CloudFlareReCaptcha> {

    public static final String ATTRIBUTE_STOKEN = "data-stoken";
    private static final int EXPECTED_TOKEN_LENGTH = 420;
    public static final int EXPECTED_NUMBER_OF_ANSWERS = 9;
    public static final String ATTRIBUTE_SITE_KEY = "data-sitekey";
    private final Logger logger;

    private static final int IMAGE_COLUMNS = 3;
    private static final int IMAGE_ROWS = 3;

    private static final String NAME_REASON="reason";
    private static final String NAME_CHALLENGE_ID = "c";
    private static final String NAME_RESPONSE = "response";
    private static final String NAME_VERIFICATION_TOKEN = "g-recaptcha-response";

    private static final String REASON_ANOTHER_CHALLENGE = "r";
    private static final String REASON_AUDIO = "a";

    public CloudFlareCaptchaManager(Downloader downloader, File cacheDir) {
        super(downloader, cacheDir);
        logger = Logger.getLogger("CloudFlareCaptchaManager");
        logger.setLevel(Level.FINEST);

    }

    @NotNull
    @Override
    public CloudFlareReCaptcha loadCaptcha(URL url) throws IOException, LoaderException {

        Downloader downloader = getDownloader();
        Downloader.Result result = downloader.download(url);

        List<HttpCookie> cookieList = result.getCookies();

        Document document = Jsoup.parse(result.getInputStream(), result.getCharset(), url.toString());
        logger.fine(document.html());

        Element form = document.getElementById("challenge-form");
        if(form == null) {
            throw new LoaderException("Challenge form not found", document.html());
        }
        String validationPath = form.attr("action");
        Elements captchaContainers = form.getElementsByAttribute(ATTRIBUTE_STOKEN);
        String stoken = null;
        String siteKey = null;
        if (!captchaContainers.isEmpty()) {
            for (Element element : captchaContainers) {
                siteKey = element.attr(ATTRIBUTE_SITE_KEY);
                if (!siteKey.isEmpty()) {
                    stoken = element.attr(ATTRIBUTE_STOKEN);
                    break;
                }
            }
        }
        if (siteKey == null || siteKey.isEmpty()) {
            throw new IOException("siteKey = null");
        }
        if (stoken == null) {
            throw new IOException("stoken = null");
        }

        return new CloudFlareReCaptcha(url.toString(), siteKey, stoken, validationPath, cookieList);
    }

    @NotNull
    @Override
    public Challenge loadTask(CloudFlareReCaptcha captcha) throws IOException, LoaderException {
        String fallbackUrl = captcha.getIFrameUrl();
        HttpHeaders headers = new HttpHeaders();
        headers.setReferer(captcha.getBaseURL());
        Downloader.Result result = getDownloader().download(new URL(fallbackUrl), headers);
        if(result.getStatusCode() != HttpURLConnection.HTTP_OK) {
            throw new FileNotFoundException("Fallback returned " + result.getStatusCode());
        }
        Document document = Jsoup.parse(result.getInputStream(), result.getCharset(), fallbackUrl);
        return loadTaskFromIFrame(document, captcha);
    }

    @Override
    public Challenge submitAndGetNewTask(CloudFlareReCaptcha captcha, Challenge task, Object... answers) throws IOException, LoaderException {
        return submitTask(captcha, (long[]) answers[0]);
    }


    private String getVerificationToken(Document document) {
        Element element = document.getElementsByClass("fbc-verification-token").first();
        if(element == null) {
            return null;
        }
        Element textArea = element.getElementsByTag("textarea").first();
        if(textArea == null) {
            throw new RuntimeException("TextArea not found in " + element.html());
        }
        return textArea.val();
    }

    private static String getChallengeId(Element form) throws IOException {
        return form.getElementsByAttributeValue("name", NAME_CHALLENGE_ID).first().val();
    }

    private Challenge loadTaskFromIFrame(Document iframe, CloudFlareReCaptcha captcha) throws IOException, LoaderException {
        Element label = iframe.select("label[for=" + NAME_RESPONSE + "]").first();
        if(label == null) {
            label = iframe.select(".fbc-imageselect-message-error").first();
        }
        String task = "???";
        if(label == null) {
            throw new LoaderException("Couldn't find task", iframe.html());
        } else {
            task = label.text();
        }
        Elements forms = iframe.select(".fbc-imageselect-challenge form");
        if(forms.size() > 1) {
            throw new RuntimeException("Expected only 1 <form> got " + forms.size());
        }
        Element form = forms.first();
        if(form == null) {
            throw new IOException("Form not found");
        }
        Elements checkboxes = form.select("input[name=" + NAME_RESPONSE + "]");
        int numberOfAnswers = checkboxes.size();
        if(numberOfAnswers != EXPECTED_NUMBER_OF_ANSWERS) {
            throw new LoaderException("Unexpected answer count!: " + numberOfAnswers, iframe.html());
        }
        String challengeId = getChallengeId(form);
        File payload = loadPayload(challengeId, getCacheDir(), captcha, getDownloader());
        Challenge challenge;
        challenge = new Challenge(challengeId, payload, task, numberOfAnswers, IMAGE_COLUMNS, IMAGE_ROWS);
        return challenge;
    }

    private static File loadPayload(String challengeId, File cacheDir, CloudFlareReCaptcha captcha, Downloader downloader) throws IOException {
        String payloadUrl = captcha.getPayloadUrl(challengeId);
        HttpHeaders payloadHeaders = new HttpHeaders();
        payloadHeaders.put(HttpHeaders.HEADER_ACCEPT, HttpHeaders.ACCEPT_IMAGES);
        payloadHeaders.setReferer(captcha.getIFrameUrl());
        return downloader.download(cacheDir, new URL(payloadUrl), payloadHeaders);
    }


    @Contract("null, _, _, _, _ -> fail;" +
            "_, null, _, _, _ -> fail;" +
            "_, _, null, _, _ -> fail")
    private static Document postCaptcha(Downloader downloader,
                                        URL iframeURL,
                                        String challengeId,
                                        long[] answers,
                                        String reason) throws IOException {
        if(downloader == null) throw new NullPointerException("downlaoder is null");
        if(iframeURL == null) throw new NullPointerException("iframeURL is null");
        if(challengeId == null) throw new NullPointerException("challengeId  is null");
        PostDataBuilder dataBuilder = new PostDataBuilder();
        if(answers != null) {
            dataBuilder.add(NAME_RESPONSE, answers);
        }
        if(reason != null) {
            dataBuilder.add(NAME_REASON, reason);
        }
        dataBuilder.add(NAME_CHALLENGE_ID, challengeId);
        HttpHeaders headers = new HttpHeaders();
        headers.setReferer(iframeURL);
        Downloader.Result result = downloader.post(iframeURL, headers, dataBuilder.getBytes());
        if(result.getStatusCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to submit captcha: " + result.getStatusCode());
        }
        return Jsoup.parse(result.getInputStream(), result.getCharset(), iframeURL.toString());
    }

    private void postReloadCaptcha(final String reason, final CloudFlareReCaptcha captcha) throws IOException, LoaderException {
        Downloader downloader = getDownloader();
        URL url = new URL(captcha.getIFrameUrl());
        Document document = postCaptcha(downloader, url, captcha.getChallenge().getIdentifier(),
                null, reason);
        logger.warning("Document: " + document);
        String verificationToken = getVerificationToken(document);
        if(verificationToken != null) {
            foundVerificationToken(captcha, verificationToken);
        } else {
            loadTaskFromIFrame(document, captcha);
        }
    }

    private Challenge submitTask(final CloudFlareReCaptcha captcha, long[] selectedAnswers) throws IOException, LoaderException {
        logger.finer("Submitting captcha");
        Downloader downloader = getDownloader();
        URL url = new URL(captcha.getIFrameUrl());
        Document document = postCaptcha(downloader, url, captcha.getChallenge().getIdentifier(),
                selectedAnswers, null);
        String verificationToken = getVerificationToken(document);
        if(verificationToken != null) {
            foundVerificationToken(captcha, verificationToken);
            return null;
        } else {
            return loadTaskFromIFrame(document, captcha);
        }
    }

    private void foundVerificationToken(CloudFlareReCaptcha captcha, String verificationToken) throws IOException {
        logger.finer("Got verification token: " + verificationToken);
        if(verificationToken.length() != EXPECTED_TOKEN_LENGTH) {
            logger.warning("Expected length of token to be " + EXPECTED_TOKEN_LENGTH + " but got " + verificationToken.length());
        }
        Downloader downloader = getDownloader();
        HttpHeaders headers = new HttpHeaders();
        headers.setReferer(captcha.getBaseURL());
        PostDataBuilder dataBuilder = new PostDataBuilder();
        dataBuilder.add(NAME_VERIFICATION_TOKEN, verificationToken);
        // TODO: Make this always working
        URL url = new URL(captcha.getValidationURL() + "?" + dataBuilder.toString());
        System.err.println(url.toString());
        headers.setCookies(captcha.getBaseCookies());
        Downloader.Result result = downloader.download(url, headers);
        logger.finer("Response: " + result.getStatusCode());
        if(result.getStatusCode() != HttpURLConnection.HTTP_MOVED_TEMP) {
            throw new IOException("Got invalid response " + result.getStatusCode());
        }
        HttpHeaders httpHeaders = new HttpHeaders(result.getResponseHeaders());
        CookieStore cookieStore = httpHeaders.getCookieStore(captcha.getBaseURL());
        CaptchaResult captchaResult = new CaptchaResult();
        captchaResult.setCookiesFromHttpCookies(cookieStore.getCookies());
        foundResult(captcha, captchaResult);
    }


    public void loadNewChallenge(final CloudFlareReCaptcha captcha) throws IOException, LoaderException {
        postReloadCaptcha(REASON_ANOTHER_CHALLENGE, captcha);
    }

    public void loadNewAudioChallenge(final CloudFlareReCaptcha captcha) throws IOException, LoaderException {
        postReloadCaptcha(REASON_AUDIO, captcha);
    }
}
