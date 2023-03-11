package com.example.manipulatedimagerecommenderanddetector.ui.main;

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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections;

public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final int REQUEST_IMAGE = 100;

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
                                            Log.d("PlaceholderFragment", "Image we are looking at B: " + authenticity);
                                        } else if (authenticity != null && authenticity.equals("tp")) {
                                            Log.d("PlaceholderFragment", "Image we are looking at B: " + authenticity);
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
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {
                                    // Handle errors while retrieving the image authenticity status
                                    // ...
                                }
                            });
                            continue;
                        }

                        else
                        {
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
        ArrayList<String> imageFilenames = new ArrayList<>();
        StorageReference imagesRef = storage.getReference().child("images");
        imagesRef.listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference item : listResult.getItems()) {
                        String filename = item.getName();
                        imageFilenames.add(filename);
                    }

                    // Randomly select 6 images from the list of filenames
                    ArrayList<String> selectedImageFilenames = new ArrayList<>();
                    Collections.shuffle(imageFilenames);

                    for (int i = 0; i < 6 && i < imageFilenames.size(); i++) {
                        selectedImageFilenames.add(imageFilenames.get(i));
                    }

                    // Check if an image has just been uploaded
                    if (imageJustUploaded && uploadedImageRef != null) {
                        // If an image has just been uploaded, make sure it's the first image on the screen
                        String uploadedImageFilename = uploadedImageRef.getName();
                        Log.d("PlaceholderFragment", "Uploaded image filename: " + uploadedImageFilename);
                        int index = imageFilenames.indexOf(uploadedImageFilename);
                        Log.d("PlaceholderFragment", "Uploaded image index: " + index);
                        if (index >= 0 && index < 6) {
                            // If the uploaded image is already in the selected images, move it to the first position
                            selectedImageFilenames.remove(uploadedImageFilename);
                            selectedImageFilenames.add(0, uploadedImageFilename);
                        } else if (index >= 6) {
                            // If the uploaded image is not in the selected images but is in the full list, replace the last selected image with it
                            selectedImageFilenames.remove(selectedImageFilenames.size() - 1);
                            selectedImageFilenames.add(0, uploadedImageFilename);
                        } else {
                            // If the uploaded image is not in the full list, do nothing
                        }

                        // Reset the flag and the reference to the uploaded image
                        imageJustUploaded = false;
                        uploadedImageRef = null;
                    }

                    // Set the adapter for the grid view
                    GridAdapter gridAdapter = new GridAdapter(getActivity(), selectedImageFilenames);
                    binding.gridView.setAdapter(gridAdapter);
                })
                .addOnFailureListener(exception -> {
                    // Handle errors while retrieving the list of image filenames from Firebase Storage
                    // ...
                });
    }





}

