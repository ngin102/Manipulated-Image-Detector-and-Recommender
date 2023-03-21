package com.example.manipulatedimagerecommenderanddetector;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.example.manipulatedimagerecommenderanddetector.databinding.ActivityMainBinding;
import com.example.manipulatedimagerecommenderanddetector.ui.main.PlaceholderFragment;
import com.example.manipulatedimagerecommenderanddetector.ui.main.SectionsPagerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> activityResultLauncher;
    private StorageReference imagesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.example.manipulatedimagerecommenderanddetector.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseStorage storage = FirebaseStorage.getInstance();
        imagesRef = storage.getReference().child("images");

        // Create the adapter that will return a fragment for each of the
        // primary sections of the activity.
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);

        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);

        View rootView = binding.getRoot();

        // Set up floating action button that is used to upload images.
        FloatingActionButton uploadFab = findViewById(R.id.uploadFab);
        uploadFab.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/jpeg"); // only accept JPG images
            intent.setAction(Intent.ACTION_GET_CONTENT);
            activityResultLauncher.launch(Intent.createChooser(intent, "Select Picture"));
        });

        // Set up floating action button that is used to refresh the grid.
        FloatingActionButton refreshFab = findViewById(R.id.refreshFab);
        refreshFab.setOnClickListener(view -> {
            PlaceholderFragment placeholderFragment = (PlaceholderFragment) sectionsPagerAdapter.instantiateItem(viewPager, 0);
            placeholderFragment.refreshGrid();
            placeholderFragment = (PlaceholderFragment) sectionsPagerAdapter.instantiateItem(viewPager, 1);
            placeholderFragment.refreshGrid();
        });

        // After uploading an image...
        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Get the URI of the selected image
                        assert result.getData() != null;
                        Uri imageUri = result.getData().getData();

                        // Check if the selected image is a JPG
                        String fileType = getContentResolver().getType(imageUri);
                        if (!TextUtils.equals(fileType, "image/jpeg")) {
                            Snackbar.make(rootView, "Please only upload JPEG images.", Snackbar.LENGTH_LONG).show();
                            return;
                        }

                        // Get a reference to the image in Firebase Storage.
                        String filename = imageUri.getLastPathSegment();
                        StorageReference imageRef = imagesRef.child(filename);

                        // Upload the image file to Firebase Storage.
                        UploadTask uploadTask = imageRef.putFile(imageUri);
                        uploadTask.addOnSuccessListener(taskSnapshot -> {
                            // Get the download URL of the uploaded image.
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                // Indicate that an image has just been uploaded.
                                PlaceholderFragment placeholderFragment1 = (PlaceholderFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.view_pager + ":0");
                                PlaceholderFragment placeholderFragment2 = (PlaceholderFragment) getSupportFragmentManager().findFragmentByTag("android:switcher:" + R.id.view_pager + ":1");
                                if (placeholderFragment1 != null && placeholderFragment2 != null) {
                                    placeholderFragment1.setImageJustUploaded(true, imageRef);
                                    placeholderFragment2.setImageJustUploaded(true, imageRef);
                                }

                                // Use the model to determine the uploaded image's authenticity and save the result to the Firebase Database.
                                try {
                                    // Load the TFLite model.
                                    Interpreter tflite = new Interpreter(loadModel(this));

                                    // Load the image from URI/
                                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                                    // Resize the image to the input size of the TFLite model (256 x 256).
                                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true);

                                    // Convert the Bitmap to a 3D float array.
                                    float[][][][] input = new float[1][256][256][3];
                                    for (int x = 0; x < 256; x++) {
                                        for (int y = 0; y < 256; y++) {
                                            int pixel = resizedBitmap.getPixel(x, y);
                                            input[0][x][y][0] = Color.red(pixel) / 255.0f;
                                            input[0][x][y][1] = Color.green(pixel) / 255.0f;
                                            input[0][x][y][2] = Color.blue(pixel) / 255.0f;
                                        }
                                    }

                                    // Run the model on the image.
                                    float[][] output = new float[1][1];
                                    tflite.run(input, output);

                                    Log.d("MainActivity", "Output size: " + output.length);
                                    Log.d("MainActivity", "Authenticity score: " + output[0][0]);

                                    // Determine the authenticity of the image based on the output of running the model on the image.
                                    String authenticity;
                                    if (output[0][0] < 0.5) {
                                        authenticity = "tp";
                                    } else {
                                        authenticity = "au";
                                    }

                                    // Save the determined authenticity to the Firebase Database.
                                    saveImageAuthenticityToDatabase(imageRef.getName(), authenticity);
                                    getImageTags(imageRef.getName());
                                    Snackbar.make(rootView, "Image uploaded successfully. Retrieving recommendations...", Snackbar.LENGTH_LONG).show();

                                    // Refresh both fragments to display the newly grid by clicking the refresh floating action button.
                                    // There is a 2 second delay between after image has been uploaded and the button is clicked; to ensure that all proper
                                    // info can be retrieved from Firebase to refresh the grid.
                                    refreshFab.postDelayed(refreshFab::performClick, 2000);

                                } catch (IOException e) {
                                    Log.d("MainActivity", "Can not run model.");
                                    e.printStackTrace();
                                }

                            }).addOnFailureListener(e -> Snackbar.make(rootView, "Can not get download URL: " + e.getMessage(), Snackbar.LENGTH_LONG).show());


                        }).addOnFailureListener(e -> Snackbar.make(rootView, "Can not upload image: " + e.getMessage(), Snackbar.LENGTH_LONG).show());
                    }
                });
    }

    // Load a given model.
    private MappedByteBuffer loadModel(Context context) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd("manipulation_detector_v5.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.getStartOffset(), fileDescriptor.getDeclaredLength());
    }

    // Save image authenticity to Firebase.
    private void saveImageAuthenticityToDatabase(String filename, String authenticity) {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Image Authenticity");
        databaseRef.child(filename).setValue(authenticity)
                .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Image authenticity saved to database for filename: " + filename))
                .addOnFailureListener(e -> Log.d("MainActivity", "Failed to save image authenticity to database for filename: " + filename));
    }

    // Get a given image's tags.
    private void getImageTags(String filename) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference imageRef = storage.getReference().child("images/" + filename);

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Image Tags");

        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
            // Use the download URL to load the image into the app.
            String downloadUrl = uri.toString();

            Picasso.get().load(downloadUrl).into(new Target() {
                @Override
                public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                    // Create an InputImage object from the Bitmap.
                    InputImage image = InputImage.fromBitmap(bitmap, 0);
                    // Use ML Kit to label the image with tags.
                    ImageLabeler tagger = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
                    tagger.process(image)
                            .addOnSuccessListener(labels -> {
                                StringBuilder tags = new StringBuilder();
                                for (int i = 0; i < labels.size(); i++) {
                                    String text = labels.get(i).getText();
                                    // float confidence = labels.get(i).getConfidence();
                                    // int index = labels.get(i).getIndex();
                                    if (i != labels.size() - 1)
                                    {
                                        tags.append(text).append(", ");
                                    }
                                    else
                                    {
                                        tags.append(text);
                                    }
                                }
                                databaseRef.child(filename).setValue(tags.toString())
                                        .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Image tags saved to " +
                                                "database for filename: " + filename))
                                        .addOnFailureListener(e -> Log.d("MainActivity", "Failed to save image tags " +
                                                "to database for filename: " + filename));
                            })
                            .addOnFailureListener(e -> Log.e(TAG, "Can not process and tag the image.", e));
                }

                @Override
                public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                    Log.e(TAG, "Can not load the Bitmap.", e);
                }

                @Override
                public void onPrepareLoad(Drawable placeHolderDrawable) {
                }
            });
        }).addOnFailureListener(e -> {
            // Handle any errors while retrieving the download URL
            Log.e(TAG, "Can not retrieve download URL.", e);
        });
    }

}



