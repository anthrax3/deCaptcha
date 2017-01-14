package io.ristretto.decaptcha.solver.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.ListAdapter;

import java.util.ArrayList;
import java.util.Collection;

import io.ristretto.decaptcha.R;
import io.ristretto.decaptcha.ui.ChaptchaTileView;

/**
 * Created by coffeemakr on 13.01.17.
 */



public class CFCaptchaAdapter extends BaseAdapter implements ListAdapter {

    static class CaptchaTile implements Checkable{
        final int id;
        final Bitmap bitmap;
        boolean checked;
        public CaptchaTile(Bitmap bitmap, int id) {
            this.bitmap = bitmap;
            this.id = id;
            setChecked(false);
        }

        @Override
        public void setChecked(boolean checked) {
            if(this.checked != checked) {
                this.checked = checked;
            }
        }

        @Override
        public boolean isChecked() {
            return checked;
        }

        @Override
        public void toggle() {
            setChecked(!checked);
        }
    }
    private final ArrayList<CaptchaTile> tiles = new ArrayList<>();

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
        return tiles.get(position).id;
    }

    /*
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CheckBox checkBox;
        if(convertView == null) {
            checkBox = new CheckBox(parent.getContext());
            checkBox.setClickable(false);
            checkBox.setFocusable(false);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                checkBox.setBackgroundResource(R.drawable.captcha_selector);
            }
        } else {
            checkBox = (CheckBox) convertView;
        }
        return checkBox;
    } */

/*

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CheckableImageButton checkableImageButton;
        if(convertView == null) {
            checkableImageButton = new SquaredCheckableImageButton(parent.getContext());
            checkableImageButton.setClickable(false);
            checkableImageButton.setFocusable(false);
            checkableImageButton.setBackgroundResource(R.drawable.captcha_selector);
        } else {
            checkableImageButton = (CheckableImageButton) convertView;
        }
        //checkableImageButton.setChecked(tiles.get(position).isChecked());
        checkableImageButton.setImageBitmap(tiles.get(position).bitmap);
        return checkableImageButton;
    }*/


    @Override
    public View getView(final int position, final View convertView, final ViewGroup parent) {
        ChaptchaTileView view;
        if (convertView == null) {
            Context context = parent.getContext();
            view = new ChaptchaTileView(context);
        } else {
            view = (ChaptchaTileView) convertView;
        }
        CaptchaTile tile = tiles.get(position);
        view.setImageBitmap(tile.bitmap);
        //viewHolder.checkableView.setChecked(tile.isChecked());
        return convertView;
    }

    public void setBitmapy(Collection<Bitmap> tiles) {
        this.tiles.clear();
        int i = 0;
        for(Bitmap bitmap: tiles) {
            this.tiles.add(new CaptchaTile(bitmap, i));
            ++i;
        }
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        final ImageView imageView;
        //final CheckBox checkableView;

        ViewHolder(View view){
            imageView = (ImageView) view.findViewById(R.id.captcha_tile);
            //checkableView = (CheckBox) view.findViewById(R.id.captcha);
        }
    }
}
