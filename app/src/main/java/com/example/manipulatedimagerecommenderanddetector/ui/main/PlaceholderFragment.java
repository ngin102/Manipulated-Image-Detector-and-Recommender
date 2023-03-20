package com.example.manipulatedimagerecommenderanddetector.ui.main;

import static android.content.ContentValues.TAG;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.manipulatedimagerecommenderanddetector.GridAdapter;
import com.example.manipulatedimagerecommenderanddetector.databinding.FragmentMainBinding;
import com.google.android.material.snackbar.Snackbar;
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

        // Upon first boot-up (the user has not uploaded any images yet), we will display 6 random images on the home screen.
        // These images can be either manipulated or authentic, if the fragment is 1.
        // These images can only be authentic, if the fragment is 2.
        ArrayList<String> imageFilenames = new ArrayList<>();
        // For each image in the Firebase Database...
        imagesRef.listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference item : listResult.getItems()) {
                        String filename = item.getName();

                        // Check if the current fragment is fragment 2. If it is, do not display the image if it is manipulated.
                        if (pageViewModel.getIndex() == 2) {
                            // Check the image authenticity status in Firebase Realtime Database.
                            DatabaseReference authenticityRef = FirebaseDatabase.getInstance().getReference().child("Image Authenticity").child(filename);
                            authenticityRef.addValueEventListener(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    if (dataSnapshot.exists()) {
                                        String authenticity = dataSnapshot.getValue(String.class);

                                        // If the image's authenticity is "Most Likely Authentic," add it to the list of potential
                                        // images that can be displayed.
                                        if (authenticity != null && authenticity.equals("au")) {
                                            imageFilenames.add(filename);
                                        }
                                        Log.d("PlaceholderFragment", filename + " authenticity: " + authenticity);

                                        // As long as an image has not just been uploaded, display 6 random images from the list
                                        // of potential images that can be displayed.
                                        if (! imageJustUploaded) {
                                            randomizeGrid(imageFilenames);
                                        }
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    Log.d("PlaceholderFragment", "Can not retrieve image authenticity.");
                                }
                            });

                        } else { // The fragment is fragment 1...
                            imageFilenames.add(filename);
                        }
                    }

                    // As long as an image has not just been uploaded and the fragment is fragment 1, display 6 random images from the list
                    // of potential images that can be displayed.
                    if (! imageJustUploaded && pageViewModel.getIndex() == 1) {
                        randomizeGrid(imageFilenames);
                    }
                })
                .addOnFailureListener(exception -> Log.e(TAG, "Can not retrieve image filenames.", exception));

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Randomize the grid by selecting 6 images for a provided list of filenames.
    // These 6 images will appear on-screen.
    public void randomizeGrid(ArrayList<String> imageFilenames)
    {
        // Randomly select 6 images from the list of filenames.
        ArrayList<String> selectedImageFilenames = new ArrayList<>();
        Collections.shuffle(imageFilenames);
        for (int i = 0; i < 6 && i < imageFilenames.size(); i++) {
            selectedImageFilenames.add(imageFilenames.get(i));
        }

        // Set the adapter for the grid view.
        GridAdapter gridAdapter = new GridAdapter(getActivity(), selectedImageFilenames);
        binding.gridView.setAdapter(gridAdapter);
        gridAdapter.notifyDataSetChanged();
    }

    // Sets the flag to indicate whether an image has just been uploaded, as well as the reference
    // to the uploaded image.
    public void setImageJustUploaded(boolean imageJustUploaded, StorageReference uploadedImageRef) {
        this.imageJustUploaded = imageJustUploaded;
        this.uploadedImageRef = uploadedImageRef;
        Log.d("PlaceholderFragment", "Image just uploaded: " + imageJustUploaded);
        Log.d("PlaceholderFragment", "Uploaded image reference: " + uploadedImageRef);
    }

    // Refresh the grid after an image has been uploaded: calls the method to recommend similar images.
    public void refreshGrid() {
        StorageReference imagesRef = storage.getReference().child("images");
        imagesRef.listAll()
                .addOnSuccessListener(listResult -> {
                    if (imageJustUploaded && uploadedImageRef != null) {
                        String uploadedImageFilename = uploadedImageRef.getName();
                        // Reset both the flag and the reference to the uploaded image.
                        imageJustUploaded = false;
                        uploadedImageRef = null;
                        // Recommend similar images to the uploaded image.
                        recommendImages(uploadedImageFilename);
                    } else {
                        // If no image has been uploaded, we can not refresh the grid.
                        Snackbar.make(binding.gridView, "No image upload detected. Can not retrieve recommendations.", Snackbar.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(exception -> Log.e(TAG, "Can not refresh the grid.", exception));
    }

    // Recommend images for a given image.
    private void recommendImages(@Nullable String currentImageFilename) {
        retrieveTags(currentImageFilename, inputTags -> {
            // Calculate the magnitude of the given input image's tags.
            double magnitude1 = computeMagnitude(inputTags);

            // Retrieve all other tags in the Firebase database and compute cosine similarity between
            // the given image's tags and all other images' tags.
            retrieveAllTags(dataSnapshot -> {
                HashMap<String, Double> similarImages = new HashMap<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String filename = snapshot.getKey();

                    if (!filename.equals(currentImageFilename)) {
                        // Get the tags for the current image
                        String tagsString = snapshot.getValue(String.class);
                        String[] tagArray = tagsString.split(", ");
                        // Create a HashMap that stores each of the image's tags as keys and the count
                        // of how many times each tag appears as their corresponding key's value.
                        HashMap<String, Integer> tags = new HashMap<>();
                        for (String tag : tagArray) {
                            tags.put(tag, tags.getOrDefault(tag, 0) + 1);
                        }

                        // Compute the cosine similarity between the given input image and the current image being
                        // considered from the Database.
                        double similarity = computeCosineSimilarity(inputTags, tags, magnitude1);

                        // If the cosine similarity is above a certain threshold, add the current image to the HashMap of images
                        // considered "similar" to the given input image.
                        similarImages.put(filename, similarity);
                    }
                }

                // Sort similarImages in decesending order.
                List<Map.Entry<String, Double>> list = new ArrayList<>(similarImages.entrySet());
                list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

                ArrayList<String> imageFilenames = new ArrayList<>();

                imageFilenames.add(currentImageFilename);

                // Only select the top (at maximum 5) recommendations from similarImages.
                if (list.size() > 6 || list.size() == 6) {
                    for (int i = 0; i < 5; i++) {
                        imageFilenames.add(list.get(i).getKey());
                    }
                } else {
                    for (int i = 0; i < list.size(); i++) {
                        imageFilenames.add(list.get(i).getKey());
                    }
                }

                // If fragment is the second fragment, we only want to display recommendations that are for
                // images that are "Most Likely Authentic."
                // So we filter out the the recommended images that are "Most Likely Manipulated."
                if (pageViewModel.getIndex() == 2) {
                    // Retrieve the image authenticities of each image that is currently being recommended.
                    retrieveImageAuthenticity(dataSnapshot1 -> {
                        ArrayList<String> filteredImageFilenames = new ArrayList<>();
                        filteredImageFilenames.add(imageFilenames.get(0));

                        for (DataSnapshot snapshot : dataSnapshot1.getChildren()) {
                            String filename = snapshot.getKey();
                            String authenticity = snapshot.getValue(String.class);

                            // As long as the image is considered to be "Most Likely Authentic," add it to the list
                            // of filtered recommendations.
                            if (imageFilenames.contains(filename)) {
                                if (authenticity != null && authenticity.equals("au")) {
                                    filteredImageFilenames.add(filename);
                                    Log.d("PlaceholderFragment", filename + " authenticity: " + authenticity);
                                }
                            }
                        }

                        // Re-order the filtered recommendations list, so that the images appear in the same order
                        // as the do on fragment 1.
                        ArrayList<String> orderedFilteredList = new ArrayList<>();
                        for (String item : imageFilenames) {
                            if (filteredImageFilenames.contains(item)) {
                                orderedFilteredList.add(item);
                            }
                        }

                        // Update the grid view with the filtered, ordered list of recommendations.
                        updateGridView(orderedFilteredList);
                    });
                } else {
                    // Update the grid view with the recommended images.
                    updateGridView(imageFilenames);
                }
            });
        });
    }

    // Retrieve the tags for a given image.
    private void retrieveTags(String currentImageFilename, OnTagsRetrievedListener listener) {
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
                    listener.onTagsRetrieved(inputTags);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("PlaceholderFragment", "Can not retrieve tags.");
            }
        });
    }

    // Retrieve the tags for all the images stored in Firebase Database.
    private void retrieveAllTags(OnAllTagsRetrievedListener listener) {
        DatabaseReference allTagsRef = FirebaseDatabase.getInstance().getReference().child("Image Tags");
        allTagsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    listener.onAllTagsRetrieved(dataSnapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("PlaceholderFragment", "Can not retrieve all tags.");
            }
        });
    }

    // Retrieve the authenticity of an image.
    private void retrieveImageAuthenticity(OnImageAuthenticityRetrievedListener listener) {
        DatabaseReference authenticityRef = FirebaseDatabase.getInstance().getReference().child("Image Authenticity");
        authenticityRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    listener.onImageAuthenticityRetrieved(dataSnapshot);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d("PlaceholderFragment", "Can not retrieve image authenticity.");
            }
        });
    }

    // Update the GridView and display the new image recommendations.
    private void updateGridView(ArrayList<String> imageFilenames) {
        GridAdapter gridAdapter = new GridAdapter(getActivity(), imageFilenames);
        binding.gridView.setAdapter(gridAdapter);
        Snackbar.make(binding.gridView, "Image recommendations retrieved.", Snackbar.LENGTH_LONG).show();
        gridAdapter.notifyDataSetChanged();
    }

    interface OnTagsRetrievedListener {
        void onTagsRetrieved(HashMap<String, Integer> inputTags);
    }

    interface OnAllTagsRetrievedListener {
        void onAllTagsRetrieved(DataSnapshot dataSnapshot);
    }

    interface OnImageAuthenticityRetrievedListener {
        void onImageAuthenticityRetrieved(DataSnapshot dataSnapshot);
    }

    private double computeCosineSimilarity(Map<String, Integer> tags1, Map<String, Integer> tags2, double magnitude1) {
        // Compute magnitude of tags2
        double magnitude2 = computeMagnitude(tags2);

        double dotProduct = 0.0;
        // For each tag in tags1...
        for (String tag : tags1.keySet()) {
            Integer tag_count1 = tags1.get(tag);
            Integer tag_count2 = tags2.get(tag);

            // As long as the tag is also found in tags2...
            if (tag_count2 != null) {
                // Compute the product between the tag's count in tags1 and the tag's count in tags2.
                // Add this product to dotProduct.
                dotProduct += tag_count1 * tag_count2;
            }
        }

        // Return the computed cosine similarity
        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }

    private double computeMagnitude(Map<String, Integer> tags) {
        double magnitude = 0.0;

        // Calculate the magnitude of tags by retrieving the count of every tag in tags
        // and squaring each count before summing them together.
        for (String tag : tags.keySet()) {
            Integer tag_count = tags.get(tag);
            magnitude += tag_count * tag_count;
        }
        return magnitude;
    }
}