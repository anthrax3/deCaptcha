package io.ristretto.decaptcha.solver.ui;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

import io.ristretto.decaptcha.R;
import io.ristretto.decaptcha.data.CloudFlareReCaptcha;
import io.ristretto.decaptcha.net.Downloader;
import io.ristretto.decaptcha.net.HttpHeaders;
import io.ristretto.decaptcha.net.PostDataBuilder;
import io.ristretto.decaptcha.util.UriHelper;

import static io.ristretto.decaptcha.data.CloudFlareReCaptcha.Challenge;
import static io.ristretto.decaptcha.util.UriHelper.PROTOCOLS_HTTP_AND_HTTPS;

/**
 * A placeholder fragment containing a simple view.
 */
public class CloudFlareSolverFragment extends CaptchaSolverFragment<CloudFlareReCaptcha> {

    private static final String TAG = "CFSolverFragment";
    private CFCaptchaAdapter mAdapter;

    private static final String NAME_REASON="reason";
    private static final String NAME_CHALLENGE_ID = "c";
    private static final String NAME_RESPONSE = "response";

    private static final String REASON_ANOTHER_CHALLENGE = "r";
    private static final String REASON_AUDIO = "a";

    public static final int LOADING_STEPS = 4;

    public CloudFlareSolverFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cloudflare_solver_fragment, container, false);
        final GridView gridView = (GridView) view.findViewById(R.id.grid_view);
        mAdapter = new CFCaptchaAdapter();
        gridView.setAdapter(mAdapter);
        gridView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "View " + view + " selected");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Button submitButton = (Button) view.findViewById(R.id.submit_button);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmitClicked();
            }
        });
        setHasOptionsMenu(true);
        return view;
    }



    private void hideTile(int position, boolean isChecked) {
        View view = getView();
        if(view == null) return;
    }


    private long[] getAnswers() {
        View view = getView();
        if(view == null) {
            Log.e(TAG, "View not available", new Throwable());
            return new long[]{};
        }
        GridView gridView = (GridView) view.findViewById(R.id.grid_view);
        return gridView.getCheckedItemIds();
    }

    private void onSubmitClicked() {
        final CloudFlareReCaptcha captcha = getCaptcha();
        if(captcha == null) {
            Log.e(TAG, "Captcha null available", new Throwable());
            return;
        }
        final long[] selectedAnswers = getAnswers();

        if(selectedAnswers.length == 1) {
            Toast.makeText(getContext(), R.string.tile_selection_required , Toast.LENGTH_SHORT).show();
        } else {
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    try {
                        submitCaptcha(selectedAnswers, captcha);
                    } catch (IOException e) {
                        notifyFailed(R.string.submit_captcha_failed, e);
                    }
                }
            });
        }
    }

    private static void logResult(Downloader.Result result) {
        Document document = null;
        try {
            document = Jsoup.parse(result.getInputStream(), result.getCharset(), "www.google.com");
        } catch (IOException e) {
            Log.e(TAG, "Unable to get input stream", e);
        }
        if(document != null) {
            Log.w(TAG, document.html());
        }
        try {
            for(Map.Entry<String, String> header: result.getResponseHeaders().entrySet()) {
                Log.d(TAG, "Header: " + header.getKey() + "=" + header.getValue());
            }
        } catch (IOException e) {
            Log.w(TAG, "Unable to get headers", e);
        }
    }

    private void submitCaptcha(long[] selectedAnswers, final CloudFlareReCaptcha captcha) throws IOException {
        Log.d(TAG, "SUbmitting captcha");
        Downloader downloader = getDownloader();
        URL url = new URL(captcha.getIFrameUrl());
        HttpHeaders header = new HttpHeaders();
        header.setReferer(captcha.getIFrameUrl());
        PostDataBuilder dataBuilder = new PostDataBuilder();
        dataBuilder.add(NAME_RESPONSE, selectedAnswers);
        dataBuilder.add(NAME_CHALLENGE_ID, captcha.getChallenge().getIdentifier());

        Downloader.Result result = downloader.post(url, header, dataBuilder.getBytes());
        if(result.getStatusCode() != HttpURLConnection.HTTP_OK) {
            logResult(result);
            throw new IOException("Failed to submit captcha: " + result.getStatusCode());
        }
        Document document = Jsoup.parse(result.getInputStream(), result.getCharset(), url.toString());
        Log.w(TAG, "Document: " + document);
        String verificationToken = getVerficationToken(document);
        if(verificationToken != null) {
            Log.d(TAG, "Got verification token: " + verificationToken);
        } else {
            loadTaskFromIFrame(getContext().getCacheDir(), document, captcha, downloader);
            Activity activity = getActivity();
            if (activity == null) {
                Log.e(TAG, "Can't submit new task");
                return;
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateChallenge(captcha.getChallenge());
                }
            });
        }
    }


    private static String getChallengeId(Element form) throws IOException {
        return form.getElementsByAttributeValue("name", NAME_CHALLENGE_ID).first().val();
    }



    private static ArrayList<Bitmap> splitPayloadImage(File payloadImage, int numberOfAnswers) throws IOException {
        if(numberOfAnswers != 9) throw new RuntimeException("Can only handle 9!");
        ArrayList<Bitmap> result = new ArrayList<>(numberOfAnswers);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(payloadImage.getAbsolutePath(), options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        if(imageHeight > 700 || imageWidth > 700) {
            throw new IOException("Payload image to big: " + imageWidth + "x" +  imageHeight);
        }

        Bitmap wholeImage = BitmapFactory.decodeFile(payloadImage.getAbsolutePath());

        int partWidth = imageWidth / 3;
        int partHeight = imageHeight / 3;
        for(int i = 0; i < numberOfAnswers; ++i) {
            int x = ((int) (i % 3)) * partWidth;
            int y = ((int) (i / 3)) * partHeight;
            Bitmap part = Bitmap.createBitmap(wholeImage, x, y, partWidth, partHeight);
            result.add(part);
        }
        return result;
    }

    @Nullable
    private String getVerficationToken(Document document) {
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

    private void loadTaskFromIFrame(File cacheDir, Document iframe, CloudFlareReCaptcha captcha, Downloader downloader) throws IOException {
        Element label = iframe.select("label[for=response]").first();
        if(label == null) {
            label = iframe.select(".fbc-imageselect-message-error").first();
        }
        String task = "???";
        if(label == null) {
            Log.e(TAG, "Couldn't find task.");
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
        Elements checkboxes = form.select("input[name=response]");
        int numberOfAnswers = checkboxes.size();
        if(numberOfAnswers != 9) {
            Log.w(TAG, "unexpected answer count!: " + numberOfAnswers);
        }
        String challengeId = getChallengeId(form);
        File payload = loadPayload(challengeId, cacheDir, captcha, downloader);
        notifyLoadingProgress(3,LOADING_STEPS);
        ArrayList<Bitmap> images = splitPayloadImage(payload, numberOfAnswers);
        Challenge challenge = new Challenge(challengeId, task, numberOfAnswers, images);
        captcha.setChallenge(challenge);
        notifyLoadingProgress(4,LOADING_STEPS);
    }

    private static File loadPayload(String challengeId, File cacheDir, CloudFlareReCaptcha captcha, Downloader downloader) throws IOException {
        String payloadUrl = captcha.getPayloadUrl(challengeId);
        HttpHeaders payloadHeaders = new HttpHeaders();
        payloadHeaders.setReferer(captcha.getIFrameUrl());
        return downloader.download(cacheDir, new URL(payloadUrl), payloadHeaders);
    }

    @Override
    protected CloudFlareReCaptcha receiveCaptcha(final @NonNull File cacheDir, final @NonNull Uri uri) throws IOException {
        URL url = UriHelper.uriToURL(uri, PROTOCOLS_HTTP_AND_HTTPS);

        Downloader downloader = getDownloader();
        Downloader.Result result = downloader.download(url);


        Document document = Jsoup.parse(result.getInputStream(), result.getCharset(), uri.toString());
        logResult(result);
        System.err.println(document.html());


        notifyLoadingProgress(1,LOADING_STEPS);

        Elements captchaContainers = document.getElementsByAttribute("data-stoken");
        String siteKey = null;
        String stoken = null;
        if (!captchaContainers.isEmpty()) {
            for (Element element : captchaContainers) {
                siteKey = element.attr("data-sitekey");
                if (!siteKey.isEmpty()) {
                    stoken = element.attr("data-stoken");
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

        CloudFlareReCaptcha reCaptcha = new CloudFlareReCaptcha(uri.toString(), siteKey, stoken);



        String fallbackUrl = reCaptcha.getIFrameUrl();
        HttpHeaders headers = new HttpHeaders();
        headers.setReferer(reCaptcha.getBaseURL());
        result = downloader.download(new URL(fallbackUrl), headers);
        if(result.getStatusCode() != 200) {
            throw new FileNotFoundException("Fallback returned " + result.getStatusCode());
        }

        document = Jsoup.parse(result.getInputStream(), result.getCharset(), fallbackUrl);
        notifyLoadingProgress(2,LOADING_STEPS);

        loadTaskFromIFrame(cacheDir, document, reCaptcha, downloader);
        return reCaptcha;
    }

    private void updateChallenge(Challenge challenge) {
        if(challenge == null) return;
        View view = getView();
        if(view == null) {
            return;
        }
        TextView textView = (TextView) view.findViewById(R.id.task);
        textView.setText(challenge.getTaskDescription());
        mAdapter.setBitmapy(challenge.getPayloadImages());
    }

    @Override
    protected void onCaptchaReceived(@NonNull CloudFlareReCaptcha captcha) {
        super.onCaptchaReceived(captcha);
        Log.d(TAG, "On Captcha received");

        updateChallenge(captcha.getChallenge());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reload_captcha:
                // TODO
                Log.d(TAG, "Reload request");
                break;
            case R.id.captcha_audio:
                // TODO
                Log.d(TAG, "Audio requested");
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_cloudflare_solver, menu);
    }
}
