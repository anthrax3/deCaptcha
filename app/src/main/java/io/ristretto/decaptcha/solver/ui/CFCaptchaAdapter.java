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

    private static class CaptchaTile{
        final int id;
        final Bitmap bitmap;
        public CaptchaTile(Bitmap bitmap, int id) {
            this.bitmap = bitmap;
            this.id = id;
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
        return view;
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
}
