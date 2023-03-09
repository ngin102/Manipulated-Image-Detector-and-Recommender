package com.example.manipulatedimagerecommenderanddetector;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.example.manipulatedimagerecommenderanddetector.databinding.ActivityMainBinding;
import com.example.manipulatedimagerecommenderanddetector.ui.main.PlaceholderFragment;
import com.example.manipulatedimagerecommenderanddetector.ui.main.SectionsPagerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> activityResultLauncher;
    private static final int REQUEST_IMAGE = 101;
    private FirebaseStorage storage;
    private StorageReference imagesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        storage = FirebaseStorage.getInstance();
        imagesRef = storage.getReference().child("images");

        // Create the adapter that will return a fragment for each of the
        // primary sections of the activity.
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);

        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);

        FloatingActionButton fab = binding.fab;
        View rootView = binding.getRoot();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                activityResultLauncher.launch(Intent.createChooser(intent, "Select Picture"));
            }
        });

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Get the URI of the selected image
                        Uri imageUri = result.getData().getData();

                        // Get a reference to the image file in Firebase Storage
                        String filename = imageUri.getLastPathSegment();
                        StorageReference imageRef = imagesRef.child(filename);

                        // Upload the image file to Firebase Storage
                        UploadTask uploadTask = imageRef.putFile(imageUri);
                        uploadTask.addOnSuccessListener(taskSnapshot -> {
                            // Get the download URL of the uploaded image file
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                // Refresh both placeholder fragments to show the newly uploaded image
                                PlaceholderFragment placeholderFragment1 = (PlaceholderFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.view_pager + ":0");
                                PlaceholderFragment placeholderFragment2 = (PlaceholderFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.view_pager + ":1");
                                if (placeholderFragment1 != null && placeholderFragment2 != null) {
                                    placeholderFragment1.setImageJustUploaded(true, imageRef);
                                    placeholderFragment1.refreshGrid();
                                    placeholderFragment2.setImageJustUploaded(true, imageRef);
                                    placeholderFragment2.refreshGrid();
                                }

                                Snackbar.make(rootView, "Image uploaded successfully", Snackbar.LENGTH_LONG).show();
                            }).addOnFailureListener(e -> {
                                Snackbar.make(rootView, "Failed to get download URL: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                            });
                        }).addOnFailureListener(e -> {
                            Snackbar.make(rootView, "Failed to upload image: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                        });
                    }
                });
        }
    }

