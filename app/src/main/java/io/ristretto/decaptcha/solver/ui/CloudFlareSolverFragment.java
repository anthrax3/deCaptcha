package io.ristretto.decaptcha.solver.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.ristretto.decaptcha.R;
import io.ristretto.decaptcha.data.CloudFlareReCaptcha;
import io.ristretto.decaptcha.net.Downloader;
import io.ristretto.decaptcha.net.HttpHeaders;
import io.ristretto.decaptcha.solver.Helper;

import static io.ristretto.decaptcha.data.CloudFlareReCaptcha.Challenge;
import static io.ristretto.decaptcha.solver.Helper.PROTOCOLS_HTTP_AND_HTTPS;

/**
 * A placeholder fragment containing a simple view.
 */
public class CloudFlareSolverFragment extends CaptchaSolverFragment<CloudFlareReCaptcha> {

    private static final String TAG = "CFSolverFragment";
    private CFCaptchaAdapter mAdapter;
    private boolean[] mAnswers = null;

    private static final String NAME_REASON="reason";
    private static final String NAME_CHALLENGE_ID = "c";
    private static final String NAME_RESPONSE = "response";

    private static final String REASON_ANOTHER_CHALLENGE = "r";
    private static final String REASON_AUDIO = "a";

    public CloudFlareSolverFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cloudflare_solver_fragment, container, false);
        final GridView gridView = (GridView) view.findViewById(R.id.grid_view);
        mAdapter = new CFCaptchaAdapter();
        gridView.setAdapter(mAdapter);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemClick: parent=" + parent + " view:" + view + " POSITION:" + position + " id:" + id);
                CFCaptchaAdapter.CaptchaTile tile = (CFCaptchaAdapter.CaptchaTile) gridView.getItemAtPosition(position);
                tile.toggle();

                mAdapter.notifyDataSetChanged();
            }
        });

        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    private static String getChallengeId(Element form) throws IOException {
        return form.getElementsByAttributeValue("name", NAME_CHALLENGE_ID).first().attr("value");
    }



    private static ArrayList<Bitmap> splitPayloadImage(File payloadImage, int numberOfAnswers) throws IOException {
        if(numberOfAnswers != 9) throw new RuntimeException("Can only handle 9!");
        ArrayList<Bitmap> result = new ArrayList<>(numberOfAnswers);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(payloadImage.getAbsolutePath(), options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        String imageType = options.outMimeType;
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

    private void loadTaskFromIFrame(File cacheDir, Document iframe, CloudFlareReCaptcha captcha, Downloader downloader) throws IOException {
        Element label = iframe.select("label[for=response]").first();
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
        notifyLoadingProgress(3,4);
        ArrayList<Bitmap> images = splitPayloadImage(payload, numberOfAnswers);
        Challenge challenge = new Challenge(challengeId, task, numberOfAnswers, images);
        captcha.setChallenge(challenge);
        notifyLoadingProgress(4,4);
    }

    private static File loadPayload(String challengeId, File cacheDir, CloudFlareReCaptcha captcha, Downloader downloader) throws IOException {
        String payloadUrl = captcha.getPayloadUrl(challengeId);
        HttpHeaders payloadHeaders = new HttpHeaders();
        payloadHeaders.setReferer(captcha.getIFrameUrl());
        return downloader.download(cacheDir, new URL(payloadUrl), payloadHeaders);

    }

    @Override
    protected CloudFlareReCaptcha receiveCaptcha(final @NonNull File cacheDir, final @NonNull Downloader downloader, final @NonNull Uri uri) throws IOException {
        URL url = Helper.uriToURL(uri, PROTOCOLS_HTTP_AND_HTTPS);

        Downloader.Result result = downloader.download(url);


        Document document = Jsoup.parse(result.getInputStream(), result.getCharset(), uri.toString());
        Log.d(TAG, "HTTP code: " + result.getStatusCode());
        System.err.println(document.html());


        notifyLoadingProgress(1,4);

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
        notifyLoadingProgress(2,4);

        loadTaskFromIFrame(cacheDir, document, reCaptcha, downloader);
        return reCaptcha;
    }

    @Override
    protected void onCaptchaReceived(@NonNull CloudFlareReCaptcha captcha) {
        super.onCaptchaReceived(captcha);
        Log.d(TAG, "On Captcha receiverd");
        View view = getView();
        if(view == null) {
            return;
        }
        Challenge challenge = captcha.getChallenge();
        mAnswers = new boolean[challenge.getNumberOfAnswers()];
        mAdapter.setBitmapy(challenge.getPayloadImages());
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
