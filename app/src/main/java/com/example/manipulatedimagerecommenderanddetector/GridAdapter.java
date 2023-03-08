package com.example.manipulatedimagerecommenderanddetector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class GridAdapter extends BaseAdapter {

    Context context;
    int[] image;

    LayoutInflater inflater;

    public GridAdapter(Context context, int[] image)
    {
        this.context = context;
        this.image = image;
    }

    @Override
    public int getCount()
    {
        return image.length;
    }

    @Override
    public Object getItem(int pos)
    {
        return null;
    }

    public long getItemId(int pos)
    {
        return 0;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent)
    {
        if (inflater == null) {
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.grid_item, null);
        }

        ImageView imageView = convertView.findViewById(R.id.grid_image);

        imageView.setImageResource(image[pos]);

        return convertView;
    }

}
