package io.ristretto.decaptcha.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import io.ristretto.decaptcha.R;
import io.ristretto.decaptcha.captcha.CloudFlareReCaptcha;
import io.ristretto.decaptcha.manager.CaptchaManager;
import io.ristretto.decaptcha.manager.CloudFlareCaptchaManager;
import io.ristretto.decaptcha.net.Downloader;

/**
 * A placeholder fragment containing a simple view.
 */
public class CloudFlareSolverFragment extends CaptchaSolverFragment<CloudFlareReCaptcha.Challenge, CloudFlareReCaptcha> {

    private static final String TAG = "CFSolverFragment";

    private static final int MAX_IMAGE_HEIGHT = 700;
    private static final int MAX_IMAGE_WIDTH = 700;

    private TileAdapter mAdapter;


    private int mShortAnimationDuration;
    private View mCaptchaView;
    private CloudFlareCaptchaManager mCaptchaManager;

    public CloudFlareSolverFragment() {
        // Empty constructor as required for fragments
        super();
    }



    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retrieve and cache the system's default "short" animation time.
        mShortAnimationDuration = getResources().getInteger(
                android.R.integer.config_shortAnimTime);
        mCaptchaManager = new CloudFlareCaptchaManager(getDownloader(), getContext().getCacheDir());
    }


    @Override
    protected CaptchaManager<CloudFlareReCaptcha.Challenge, CloudFlareReCaptcha> getCaptchaManager() {
        return mCaptchaManager;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.cloudflare_solver_fragment, container, false);
        final GridView gridView = (GridView) view.findViewById(R.id.grid_view);
        mAdapter = new TileAdapter();
        mCaptchaView = view.findViewById(R.id.captcha);
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

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Item clicked      : " + position + " - id: " + id);
                Log.d(TAG, "Checked positions : " + gridView.getCheckedItemPositions());
                Log.d(TAG, "Checked item ids  : " + Arrays.toString(gridView.getCheckedItemIds()));
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
        if(selectedAnswers.length == 0) {
            Toast.makeText(getContext(), R.string.tile_selection_required , Toast.LENGTH_SHORT).show();
        } else {
            /*
            for(long answer: selectedAnswers) {
                mAdpater.remove(answer);
            }*/
            hideCaptcha();
            getHandler().post(new Runnable() {
                @Override
                public void run() {
                    try {
                        mCaptchaManager.submitTask(captcha, captcha.getChallenge(), (Object) selectedAnswers);
                    } catch (IOException e) {
                        notifyFailed(R.string.submit_captcha_failed, e);
                    } catch (CaptchaManager.LoaderException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }


    private void hideCaptcha() {
        mCaptchaView.animate()
                .alpha(0f)
                .setDuration(mShortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mCaptchaView.setVisibility(View.GONE);
                    }
                });
    }

    private void showCaptcha() {
        mCaptchaView.setAlpha(0f);
        mCaptchaView.setVisibility(View.VISIBLE);
        mCaptchaView.animate()
                .alpha(1f)
                .setDuration(mShortAnimationDuration)
                .setListener(null);
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

    @NonNull
    private static BitmapFactory.Options checkImageSize(String imagePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);
        int imageHeight = options.outHeight;
        int imageWidth = options.outWidth;
        if(imageHeight > MAX_IMAGE_HEIGHT || imageWidth > MAX_IMAGE_WIDTH) {
            throw new RuntimeException("Payload image to big: " + imageWidth + "x" +  imageHeight);
        }
        return options;
    }

    @NonNull
    private static ArrayList<Bitmap> createChallengeTiles(CloudFlareReCaptcha.Challenge challenge) {
        final int rows = challenge.getImageRows();
        final int columns = challenge.getImageColumns();
        final int numberOfAnswers = challenge.getNumberOfAnswers();

        ArrayList<Bitmap> result = new ArrayList<>(numberOfAnswers);
        String imagePath = challenge.getPayloadImage().getAbsolutePath();
        BitmapFactory.Options options = checkImageSize(imagePath);

        Bitmap wholeImage = BitmapFactory.decodeFile(imagePath);

        int partWidth = options.outWidth / columns;
        int partHeight = options.outHeight / rows;
        for(int i = 0; i < numberOfAnswers; ++i) {
            int x = (i % columns) * partWidth;
            int y = (i / columns) * partHeight;
            Bitmap part = Bitmap.createBitmap(wholeImage, x, y, partWidth, partHeight);
            result.add(part);
        }
        return result;
    }

    private void updateChallenge(CloudFlareReCaptcha.Challenge challenge) {
        if(challenge == null) return;
        View view = getView();
        if(view == null) {
            return;
        }
        GridView gridView = (GridView) view.findViewById(R.id.grid_view);
        gridView.setAdapter(mAdapter); // resets checked state
        TextView textView = (TextView) view.findViewById(R.id.task);
        textView.setText(challenge.getTaskDescription());
        mAdapter.setBitmaps(createChallengeTiles(challenge));
    }

    @Override
    protected void onCaptchaChallengeLoaded(@NonNull CloudFlareReCaptcha captcha, @NonNull CloudFlareReCaptcha.Challenge challenge) {
        super.onCaptchaChallengeLoaded(captcha, challenge);
        Log.d(TAG, "onCaptchaChallengeLoaded " + captcha + " " + challenge);
        updateChallenge(challenge);
        showCaptcha();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reload_captcha:
                onReloadCaptchaClicked();
                break;
            case R.id.captcha_audio:
                onAudioCaptchaClicked();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    private void onReloadCaptchaClicked() {
        hideCaptcha();
        final CloudFlareReCaptcha captcha = getCaptcha();
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCaptchaManager.loadNewChallenge(captcha);
                } catch (CaptchaManager.LoaderException | IOException e) {
                    notifyFailed(R.string.reload_captcha_failed, e);
                }
            }
        });
    }

    private void onAudioCaptchaClicked() {
        hideCaptcha();
        final CloudFlareReCaptcha captcha = getCaptcha();
        getHandler().post(new Runnable() {
            @Override
            public void run() {
                try {
                    mCaptchaManager.loadNewAudioChallenge(captcha);
                } catch (CaptchaManager.LoaderException | IOException e) {
                    notifyFailed(R.string.reload_captcha_failed, e);
                }
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_cloudflare_solver, menu);
    }

    private static class TileAdapter extends BaseAdapter implements ListAdapter {

        private final ArrayList<CaptchaTile> tiles = new ArrayList<>();

        @Override
        public boolean hasStableIds() {
            // It's important to return true otherwise {@link GridView#getCheckedItemIds} returns
            // an empty array!
            return true;
        }

        @Override
        public int getCount() {
            return tiles.size();
        }

        @Override
        public Object getItem(int position) {
            return tiles.get(position);
        }

        @Override
        public long getItemId(int position) {
            if(position >= tiles.size()) {
                // Maybe there is a better way to handle this but when removing tiles
                // with removeTile() this method is called with a unexisting position.
                return -1;
            }
            return tiles.get(position).id;

        }

        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            CaptchaTileView view;
            if (convertView == null) {
                Context context = parent.getContext();
                view = new CaptchaTileView(context);
                view.setFocusable(false);
                view.setClickable(false);
            } else {
                view = (CaptchaTileView) convertView;
            }
            CaptchaTile tile = tiles.get(position);
            view.setImageBitmap(tile.bitmap);
            return view;
        }

        private void setBitmaps(Collection<Bitmap> tiles) {
            this.tiles.clear();
            int i = 0;
            for(Bitmap bitmap: tiles) {
                this.tiles.add(new CaptchaTile(bitmap, i));
                ++i;
            }
            notifyDataSetChanged();
        }

        private void removeTile(long id) {
            for (Iterator<CaptchaTile> iterator = tiles.iterator(); iterator.hasNext(); ) {
                CaptchaTile captchaTile = iterator.next();
                if(captchaTile.id == id) {
                    Log.d(TAG, "Tile removed: " + id);
                    iterator.remove();
                    notifyDataSetChanged();
                    break;
                }
            }
        }

        /**
         * Contains the information of a captcha tile
         */
        private static class CaptchaTile{
            private final int id;
            private final Bitmap bitmap;
            private CaptchaTile(Bitmap bitmap, int id) {
                this.bitmap = bitmap;
                this.id = id;
            }
        }
    }
}
