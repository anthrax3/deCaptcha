package io.ristretto.decaptcha.net;

import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.io.IOException;

/**
 *
 * @param <I> Captcha result type
 */
public abstract class Receiver<I> extends AsyncTask<Object, Void, I> {

    @Override
    protected I doInBackground(Object... params) {
        if(params.length < 2) throw new IllegalArgumentException("need at least 2 params");
        if(params[0] == null || !(params[0] instanceof Uri)) {
            throw new IllegalArgumentException("params[0] is not Uri or null");
        }
        if(params[1] == null || !(params[1] instanceof Downloader)) {
            throw new IllegalArgumentException("params[0] is not Uri or null");
        }
        try {
            return doInBackground((Uri) params[0], (Downloader) params[1]);
        } catch (IOException e) {
            return null;

        }
    }

    protected abstract I doInBackground(@NonNull Uri uri, @NonNull Downloader downloader) throws IOException;
}
