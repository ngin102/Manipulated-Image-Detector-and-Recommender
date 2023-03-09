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
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.manipulatedimagerecommenderanddetector.GridAdapter;
import com.example.manipulatedimagerecommenderanddetector.databinding.FragmentMainBinding;
import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;

public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;
    private FragmentMainBinding binding;

    private ImageView[] imageViews;

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

        // Initialize Firebase
        FirebaseApp.initializeApp(getContext());

        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);

        // Retrieve the images from Firebase Storage
        retrieveImagesFromFirebaseStorage();
    }

    private void retrieveImagesFromFirebaseStorage() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        StorageReference imagesRef = storageRef.child("images");

        // Retrieve the image URLs from Firebase Storage
        imagesRef.listAll()
                .addOnSuccessListener(listResult -> {
                    ArrayList<String> imageUrls = new ArrayList<>();
                    for (StorageReference item : listResult.getItems()) {
                        item.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    imageUrls.add(uri.toString());
                                    // Check if we have retrieved enough images
                                    if (imageUrls.size() == 6) {
                                        // Display the images
                                        displayImages(imageUrls);
                                    }
                                })
                                .addOnFailureListener(exception -> {
                                    // Handle unsuccessful downloads
                                });
                    }
                })
                .addOnFailureListener(exception -> {
                    // Handle unsuccessful listing
                });
    }

    private void displayImages(ArrayList<String> imageUrls) {
        // Convert the image URLs to an array
        String[] urlsArray = new String[imageUrls.size()];
        urlsArray = imageUrls.toArray(urlsArray);

        // Set the modified GridAdapter as the adapter for the GridView
        GridAdapter gridAdapter = new GridAdapter(getContext(), urlsArray);
        binding.gridView.setAdapter(gridAdapter);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);
        View rootView = binding.getRoot();

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
