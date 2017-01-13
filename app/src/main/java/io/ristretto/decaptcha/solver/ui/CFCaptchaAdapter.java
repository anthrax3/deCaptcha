package io.ristretto.decaptcha.solver.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Checkable;
import android.widget.ImageView;
import android.widget.ListAdapter;

import java.util.ArrayList;
import java.util.Collection;

import io.ristretto.decaptcha.R;

/**
 * Created by coffeemakr on 13.01.17.
 */



public class CFCaptchaAdapter extends BaseAdapter implements ListAdapter {

    static class CaptchaTile implements Checkable{
        Bitmap bitmap;
        boolean checked;
        public CaptchaTile(Bitmap bitmap) {
            this.bitmap = bitmap;
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
        return tiles.get(position).hashCode();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder;
        if (convertView == null) {
            Context context = parent.getContext();
            convertView = LayoutInflater.from(context).inflate(R.layout.cloudflare_captcha_item, parent, false);
        }
        viewHolder = new ViewHolder(convertView);
        CaptchaTile tile = tiles.get(position);
        viewHolder.imageView.setImageBitmap(tile.bitmap);
        viewHolder.checkableView.setChecked(tile.isChecked());
        return convertView;
    }

    public void setBitmapy(Collection<Bitmap> tiles) {
        this.tiles.clear();
        for(Bitmap bitmap: tiles) {
            this.tiles.add(new CaptchaTile(bitmap));
        }
        notifyDataSetChanged();
    }

    static class ViewHolder {
        final ImageView imageView;
        final Checkable checkableView;

        ViewHolder(View view){
            imageView = (ImageView) view.findViewById(R.id.captcha_tile);
            checkableView = (Checkable) view.findViewById(R.id.captcha_checkable);
        }
    }
}
