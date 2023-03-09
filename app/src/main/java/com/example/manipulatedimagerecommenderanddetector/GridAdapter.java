package com.example.manipulatedimagerecommenderanddetector;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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

    private Context context; // Context of the app
    private List<String> imageFilenames; // List of filenames of the images to be displayed in the grid

    LayoutInflater inflater;

    // Constructor to initialize the context and image filenames
    public GridAdapter(Context context, List<String> imageFilenames) {
        this.context = context;
        this.imageFilenames = imageFilenames;
    }

    // Returns the number of images in the grid
    @Override
    public int getCount() {
        return imageFilenames.size();
    }

    // Returns null
    @Override
    public Object getItem(int pos) {
        return null;
    }

    // Returns 0
    public long getItemId(int pos) {
        return 0;
    }

    // Creates and returns the view for each image in the grid
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

        String filename = imageFilenames.get(pos);

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference imageRef = storage.getReference().child("images/" + filename);

        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                // Use the download URL to load the image in your app
                String downloadUrl = uri.toString();
                Picasso.get().load(downloadUrl).into(imageView);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Handle any errors while retrieving the download URL
                Log.e(TAG, "Error getting download URL", e);
            }
        });

        String filenameWithoutExtension = filename.replaceAll("\\.[^.]+$", "");

        // Check the image authenticity status in Firebase Realtime Database
        DatabaseReference authenticityRef = FirebaseDatabase.getInstance().getReference().child("Image Authenticity").child(filenameWithoutExtension);
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