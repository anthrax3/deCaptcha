package io.ristretto.decaptcha.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.support.annotation.Nullable;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.ImageView;

import io.ristretto.decaptcha.R;


public class ChaptchaTileView extends FrameLayout implements Checkable{

    private static final int[] DRAWABLE_STATE_CHECKED = new int[]{android.R.attr.state_checked};

    private boolean mChecked;


    public ChaptchaTileView(Context context) {
        this(context, null);
    }

    public ChaptchaTileView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onInitializeAccessibilityEvent(host, event);
                event.setChecked(isChecked());
            }

            @Override
            public void onInitializeAccessibilityNodeInfo(View host,
                                                          AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setCheckable(true);
                info.setChecked(isChecked());
            }
        });

        View.inflate(context, R.layout.cloudflare_captcha_item, this);
    }


    @Override
    public void setChecked(boolean checked) {
        if (mChecked != checked) {
            mChecked = checked;
            refreshDrawableState();
            sendAccessibilityEvent(AccessibilityEventCompat.TYPE_WINDOW_CONTENT_CHANGED);
            ImageView imageView = (ImageView) findViewById(R.id.captcha_tile);
            if(mChecked) {
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0.5f);
                ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
                imageView.setColorFilter(f);
            } else {
                imageView.setColorFilter(null);
            }
        }
    }

    @Override
    public boolean isChecked() {
        return mChecked;
    }

    @Override
    public void toggle() {
        setChecked(!mChecked);
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        if (mChecked) {
            return mergeDrawableStates(
                    super.onCreateDrawableState(extraSpace + DRAWABLE_STATE_CHECKED.length),
                    DRAWABLE_STATE_CHECKED);
        } else {
            return super.onCreateDrawableState(extraSpace);
        }
    }

    public void setImageBitmap(Bitmap imageBitmap) {
        ImageView imageView = (ImageView) findViewById(R.id.captcha_tile);
        imageView.setImageBitmap(imageBitmap);
    }
}
