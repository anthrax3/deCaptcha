package io.ristretto.decaptcha.ui;

import android.content.Context;

/**
 * Created by coffeemakr on 14.01.17.
 */

public class SquaredCheckableImageButton extends CheckableImageButton {
    public SquaredCheckableImageButton(Context context) {
        super(context);
    }

    @SuppressWarnings("all") // (widthMeasureSpec, widthMeasureSpec)
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
