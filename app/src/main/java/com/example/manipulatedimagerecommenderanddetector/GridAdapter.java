package com.example.manipulatedimagerecommenderanddetector;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.List;


public class GridAdapter extends BaseAdapter {

    private Context context; // Context of the app.
    private List<String> imageFilenames; // List of filenames of the images to be displayed in the grid.

    LayoutInflater inflater;

    // Constructor to initialize the context and image filenames.
    public GridAdapter(Context context, List<String> imageFilenames) {
        this.context = context;
        this.imageFilenames = imageFilenames;
    }

    // Returns the number of images in the grid.
    @Override
    public int getCount() {
        return imageFilenames.size();
    }

    @Override
    public Object getItem(int pos) {
        return null;
    }

    public long getItemId(int pos) {
        return 0;
    }

    // Creates and returns the view for each image in the grid.
    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        if (inflater == null) {
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.grid_item, null);
        }

        // Get each component of the view that will be displayed in the grid.
        ImageView imageView = convertView.findViewById(R.id.grid_image);
        TextView imageLabel = convertView.findViewById(R.id.image_label);
        TextView imageTags = convertView.findViewById(R.id.image_tags);

        String filename = imageFilenames.get(pos);

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference imageRef = storage.getReference().child("images/" + filename);

        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            // Use the download URL to load the image into the app.
            String downloadUrl = uri.toString();
            Picasso.get().load(downloadUrl).into(imageView);
        }).addOnFailureListener(e -> Log.e(TAG, "Can not download URL.", e));

        // Consider the current image's filename without its extension.
        String filenameWithoutExtension = filename.replaceAll("\\.[^.]+$", "");

        // Check the image authenticity status for this particular image in Firebase Realtime Database.
        DatabaseReference authenticityRef = FirebaseDatabase.getInstance().getReference().child("Image Authenticity").child(filenameWithoutExtension);
        authenticityRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String authenticity = dataSnapshot.getValue(String.class);

                    // Set the imageLabel based on the retrieved image authenticity status.
                    if (authenticity != null && authenticity.equals("au")) {
                        imageLabel.setText("Most Likely Authentic");
                    } else if (authenticity != null && authenticity.equals("tp")) {
                        imageLabel.setText("Most Likely Manipulated");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("GridAdapter", "Can not retrieve image authenticity.");
            }
        });

        // Retrieve the tags for the current image.
        DatabaseReference tagsRef = FirebaseDatabase.getInstance().getReference().child("Image Tags").child(filenameWithoutExtension);
        tagsRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String tags = dataSnapshot.getValue(String.class);

                    // Set the imageTags based on the retrieved tags.
                    if (tags != null) {
                        imageTags.setText("ML Generated Tags: " + tags);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("GridAdapter", "Can not retrieve image tags.");
            }
        });

        return convertView;
    }

}