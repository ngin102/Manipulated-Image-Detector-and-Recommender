package com.example.manipulatedimagerecommenderanddetector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

public class GridAdapter extends BaseAdapter {

    private Context context;
    private int[] imageIDs;

    LayoutInflater inflater;

    public GridAdapter(Context context, int[] imageIDs) {
        this.context = context;
        this.imageIDs = imageIDs;
    }

    @Override
    public int getCount() {
        return imageIDs.length;
    }

    @Override
    public Object getItem(int pos) {
        return null;
    }

    public long getItemId(int pos) {
        return 0;
    }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        if (inflater == null) {
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.grid_item, null);
        }

        ImageView imageView = convertView.findViewById(R.id.grid_image);
        TextView imageLabel = convertView.findViewById(R.id.image_label);

        // Load the image into the image view using Picasso
        Picasso.get().load(imageIDs[pos]).into(imageView);

        // Check the image authenticity status in Firebase Realtime Database
       // String filename = "image" + pos + ".jpg";
        DatabaseReference authenticityRef = FirebaseDatabase.getInstance().getReference().child("Image Authenticity").child("au_ani_30697");
        authenticityRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String authenticity = dataSnapshot.getValue(String.class);

                    // Set the badge image based on the image authenticity status
                    if (authenticity != null && authenticity.equals("au")) {
                        imageLabel.setText("Most Likely Authentic");
                    } else if (authenticity != null && authenticity.equals("tp")) {
                        imageLabel.setText("Most Likely Manipulated");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle errors while retrieving the image authenticity status
                // ...
            }
        });

        return convertView;
    }


}

