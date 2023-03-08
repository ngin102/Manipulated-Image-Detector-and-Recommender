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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

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

        // Get an array of all drawable resources
        Field[] drawableFields = R.drawable.class.getFields();

        // Filter the drawable resources to get only the images we want
        ArrayList<Integer> drawableImageIds = new ArrayList<>();
        for (Field field : drawableFields) {
            String name = field.getName();
            if (! name.equals("ic_launcher_background") && ! name.equals("ic_launcher_foreground"))  {
                try {
                    int drawableId = field.getInt(null);
                    drawableImageIds.add(drawableId);
                } catch (IllegalAccessException e) {
                    Log.e(TAG, "Error getting drawable ID", e);
                }
            }
        }

        // Randomly select 6 images from the filtered drawable resources
        ArrayList<Integer> selectedImageIds = new ArrayList<>();
        Collections.shuffle(drawableImageIds);
        for (int i = 0; i < 6 && i < drawableImageIds.size(); i++) {
            selectedImageIds.add(drawableImageIds.get(i));
        }

        // Convert the selected image IDs to an array and set it as the adapter for the grid view
        int[] images = new int[selectedImageIds.size()];
        for (int i = 0; i < selectedImageIds.size(); i++) {
            images[i] = selectedImageIds.get(i);
        }
        GridAdapter gridAdapter = new GridAdapter(getActivity(), images);
        binding.gridView.setAdapter(gridAdapter);

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}