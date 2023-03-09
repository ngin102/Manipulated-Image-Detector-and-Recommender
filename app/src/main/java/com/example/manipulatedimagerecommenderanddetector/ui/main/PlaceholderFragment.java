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
import android.widget.ImageView;
import android.widget.Toast;

import com.example.manipulatedimagerecommenderanddetector.GridAdapter;
import com.example.manipulatedimagerecommenderanddetector.R;
import com.example.manipulatedimagerecommenderanddetector.databinding.FragmentMainBinding;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class PlaceholderFragment extends Fragment {

    private static final String ARG_SECTION_NUMBER = "section_number";

    private PageViewModel pageViewModel;
    private FragmentMainBinding binding;

    private ImageView[] imageViews;
    private FirebaseStorage storage = FirebaseStorage.getInstance();

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
        StorageReference imagesRef = storage.getReference().child("images");
        imagesRef.listAll()
                .addOnSuccessListener(listResult -> {
                    for (StorageReference item : listResult.getItems()) {
                        String filename = item.getName();
                        if (filename.endsWith(".jpg")) {
                            imageFilenames.add(filename);
                        }
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
}