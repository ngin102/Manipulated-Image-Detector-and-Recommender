package com.example.manipulatedimagerecommenderanddetector.ui.main;

import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import com.example.manipulatedimagerecommenderanddetector.GridAdapter;
import com.example.manipulatedimagerecommenderanddetector.MainActivity;
import com.example.manipulatedimagerecommenderanddetector.R;
import com.example.manipulatedimagerecommenderanddetector.databinding.FragmentMainBinding;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaceholderFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "section_number";
    private PageViewModel pageViewModel;
    private FragmentMainBinding binding;
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private StorageReference imagesRef = storage.getReference().child("images");
    private boolean imageJustUploaded = false;
    private StorageReference uploadedImageRef;

    public static PlaceholderFragment newInstance(int index) {
        PlaceholderFragment fragment = new PlaceholderFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        ArrayList<String> imageFilenames = new ArrayList<>();
        imagesRef.listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference item : listResult.getItems()) {
                        String filename = item.getName();

                        // Check if the current page is the second page and skip manipulated images
                        if (pageViewModel.getIndex() == 2) {
                            // Check the image authenticity status in Firebase Realtime Database
                            DatabaseReference authenticityRef = FirebaseDatabase.getInstance().getReference().child("Image Authenticity").child(filename);
                            authenticityRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        String authenticity = dataSnapshot.getValue(String.class);

                                        // Set the badge image based on the image authenticity status
                                        if (authenticity != null && authenticity.equals("au")) {
                                            imageFilenames.add(filename);
                                            Log.d("PlaceholderFragment", filename + " authenticity: " + authenticity);
                                        } else if (authenticity != null && authenticity.equals("tp")) {
                                            Log.d("PlaceholderFragment", filename + " authenticity: " + authenticity);
                                        }
                                        // Check if an image has just been uploaded
                                        if (imageJustUploaded) {
                                            // If an image has just been uploaded, make sure it's the first image on the screen
                                            String uploadedImageFilename = uploadedImageRef.getName();
                                            int index = imageFilenames.indexOf(uploadedImageFilename);
                                            if (index > 0) {
                                                imageFilenames.remove(index);
                                                imageFilenames.add(0, uploadedImageFilename);
                                            }

                                            // Reset the flag and the reference to the uploaded image
                                            imageJustUploaded = false;
                                            uploadedImageRef = null;
                                        }

                                        // Randomly select 6 images from the list of filenames
                                        ArrayList<String> selectedImageFilenames = new ArrayList<>();
                                        Collections.shuffle(imageFilenames);
                                        for (int i = 0; i < 6 && i < imageFilenames.size(); i++) {
                                            selectedImageFilenames.add(imageFilenames.get(i));
                                        }

                                        // Set the adapter for the grid view
                                        GridAdapter gridAdapter = new GridAdapter(getActivity(), selectedImageFilenames);
                                        binding.gridView.setAdapter(gridAdapter);
                                        gridAdapter.notifyDataSetChanged();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    // Handle errors while retrieving the image authenticity status
                                    // ...
                                }
                            });

                        } else {
                            imageFilenames.add(filename);

                            // Check if an image has just been uploaded
                            if (imageJustUploaded) {
                                // If an image has just been uploaded, make sure it's the first image on the screen
                                String uploadedImageFilename = uploadedImageRef.getName();
                                int index = imageFilenames.indexOf(uploadedImageFilename);
                                if (index > 0) {
                                    imageFilenames.remove(index);
                                    imageFilenames.add(0, uploadedImageFilename);
                                }

                                // Reset the flag and the reference to the uploaded image
                                imageJustUploaded = false;
                                uploadedImageRef = null;
                            }

                            // Randomly select 6 images from the list of filenames
                            ArrayList<String> selectedImageFilenames = new ArrayList<>();
                            Collections.shuffle(imageFilenames);
                            for (int i = 0; i < 6 && i < imageFilenames.size(); i++) {
                                selectedImageFilenames.add(imageFilenames.get(i));
                            }

                            // Set the adapter for the grid view
                            GridAdapter gridAdapter = new GridAdapter(getActivity(), selectedImageFilenames);
                            binding.gridView.setAdapter(gridAdapter);
                            gridAdapter.notifyDataSetChanged();
                        }
                    }
                })
                .addOnFailureListener(exception -> {
                    // Handle errors while retrieving the list of image filenames from Firebase Storage
                    // ...
                });

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void setImageJustUploaded(boolean justUploaded, StorageReference uploadedImageRef) {
        imageJustUploaded = justUploaded;
        this.uploadedImageRef = uploadedImageRef;
        Log.d("PlaceholderFragment", "Image just uploaded: " + justUploaded);
        Log.d("PlaceholderFragment", "Uploaded image reference: " + uploadedImageRef);
    }

    public void refreshGrid() {
        StorageReference imagesRef = storage.getReference().child("images");
        imagesRef.listAll()
                .addOnSuccessListener(listResult -> {
                    // Check if an image has just been uploaded
                    if (imageJustUploaded && uploadedImageRef != null) {
                        // If an image has just been uploaded, make sure it's the first image on the screen
                        String uploadedImageFilename = uploadedImageRef.getName();
                        // recommendImage sets the adapter for the grid view
                        recommendImages(uploadedImageFilename);
                    }

                })
                .addOnFailureListener(exception -> {
                    // Handle errors while retrieving the list of image filenames from Firebase Storage
                    // ...
                });
    }

    private void recommendImages(String currentImageFilename) {
        DatabaseReference tagsRef = FirebaseDatabase.getInstance().getReference().child("Image Tags").child(currentImageFilename);
        tagsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    HashMap<String, Integer> inputTags = new HashMap<>();
                    String tagsString = dataSnapshot.getValue(String.class);
                    String[] tagArray = tagsString.split(", ");
                    for (String tag : tagArray) {
                        inputTags.put(tag, inputTags.getOrDefault(tag, 0) + 1);
                    }

                    double magnitude1 = computeMagnitude(inputTags);

                    // Get the list of all image filenames and their tags from Firebase Realtime Database
                    DatabaseReference allTagsRef = FirebaseDatabase.getInstance().getReference().child("Image Tags");
                    allTagsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                HashMap<String, Double> similarImages = new HashMap<>();

                                // Compute the cosine similarity between the input image and all other images
                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                    String filename = snapshot.getKey();

                                    if (!filename.equals(currentImageFilename)) {
                                        // Get the tags for the current image
                                        String tagsString = snapshot.getValue(String.class);
                                        String[] tagArray = tagsString.split(", ");
                                        HashMap<String, Integer> tags = new HashMap<>();
                                        for (String tag : tagArray) {
                                            tags.put(tag, tags.getOrDefault(tag, 0) + 1);
                                        }

                                        // Compute the cosine similarity between the input image and the current image
                                        double similarity = computeCosineSimilarity(inputTags, tags, magnitude1);

                                        // If the cosine similarity is above a certain threshold, add the current image to the list of similar images
                                        similarImages.put(filename, similarity);
                                    }
                                }

                                // Sort the map by decreasing value
                                List<Map.Entry<String, Double>> list = new ArrayList<>(similarImages.entrySet());
                                list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

                                ArrayList<String> imageFilenames = new ArrayList<>();

                                imageFilenames.add(currentImageFilename);

                                if (list.size() > 6 || list.size() == 6) {
                                    for (int i = 0; i < 5; i++) {
                                        imageFilenames.add(list.get(i).getKey());
                                    }
                                }
                                else {
                                    for (int i = 0; i < list.size(); i++) {
                                        imageFilenames.add(list.get(i).getKey());
                                    }
                                }

                                StringBuilder sb = new StringBuilder();
                                for (Map.Entry<String, Double> entry : list) {
                                    sb.append(entry.getKey()).append(": ").append(String.format("%.2f", entry.getValue())).append(", ");
                                }
                                String similarityScoresString = sb.toString();

                                Log.d("PlaceholderFragment", "Similarity scores: " + similarityScoresString);
                                Log.d("PlaceholderFragment", "# of Recommended images: " + (imageFilenames.size() - 1));

                                if (pageViewModel.getIndex() == 2) {
                                    // Check the image authenticity status in Firebase Realtime Database
                                    DatabaseReference authenticityRef = FirebaseDatabase.getInstance().getReference().child("Image Authenticity");
                                    authenticityRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.exists()) {
                                                ArrayList<String> filteredImageFilenames = new ArrayList<>();

                                                filteredImageFilenames.add(imageFilenames.get(0));

                                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                    String filename = snapshot.getKey();
                                                    String authenticity = snapshot.getValue(String.class);

                                                    if (imageFilenames.contains(filename)) {
                                                        if (authenticity != null && authenticity.equals("au")) {
                                                            filteredImageFilenames.add(filename);
                                                            Log.d("PlaceholderFragment", filename + " authenticity: " + authenticity);
                                                        }
                                                    }
                                                }

                                                ArrayList<String> orderedFilteredList = new ArrayList<>();
                                                for (String item : imageFilenames) {
                                                    if (filteredImageFilenames.contains(item)) {
                                                        orderedFilteredList.add(item);
                                                    }
                                                }

                                                // Set the adapter for the grid view
                                                GridAdapter gridAdapter = new GridAdapter(getActivity(), orderedFilteredList);
                                                binding.gridView.setAdapter(gridAdapter);
                                                gridAdapter.notifyDataSetChanged();
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            // Handle errors while retrieving the image authenticity status
                                            // ...
                                        }
                                    });
                                }
                                else
                                {
                                    // Display the recommended images in the grid view
                                    GridAdapter gridAdapter = new GridAdapter(getActivity(), imageFilenames);
                                    binding.gridView.setAdapter(gridAdapter);
                                    gridAdapter.notifyDataSetChanged();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            // Handle errors while retrieving the image tags from Firebase Realtime Database
                            // ...
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle errors while retrieving the tags for the current image from Firebase Realtime Database
                // ...
            }
        });
    }

    private double computeCosineSimilarity(Map<String, Integer> tags1, Map<String, Integer> tags2, double magnitude1) {
        double magnitude2 = 0.0;
        for (String tag : tags2.keySet()) {
            Integer count2 = tags2.get(tag);
            magnitude2 += count2 * count2;
        }

        double dotProduct = 0.0;
        for (String tag : tags1.keySet()) {
            Integer count1 = tags1.get(tag);
            Integer count2 = tags2.get(tag);
            if (count2 != null) {
                dotProduct += count1 * count2;
            }
        }

        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }

    private double computeMagnitude(Map<String, Integer> tags1) {
        double magnitude1 = 0.0;
        for (String tag : tags1.keySet()) {
            Integer count = tags1.get(tag);
            magnitude1 += count * count;
        }
        return magnitude1;
    }



}

