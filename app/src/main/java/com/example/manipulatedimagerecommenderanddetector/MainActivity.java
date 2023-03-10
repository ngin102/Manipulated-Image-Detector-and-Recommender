package com.example.manipulatedimagerecommenderanddetector;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> activityResultLauncher;
    private StorageReference imagesRef;

    private static final int INPUT_SIZE = 224;
    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 1.0f;

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

        FloatingActionButton fab = binding.fab;
        View rootView = binding.getRoot();
        fab.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            activityResultLauncher.launch(Intent.createChooser(intent, "Select Picture"));
        });

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Get the URI of the selected image
                        assert result.getData() != null;
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

                                // Use the image file with the model to determine its authenticity and save the result to Firebase Database
                                try {
                                    // Load the TFLite model
                                    Interpreter tflite = new Interpreter(loadModelFile(this, "manipulation_detector_v2.tflite"));

                                    // Load the image from URI
                                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);

                                    // Resize the image to the input size of the TFLite model
                                    Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);

                                    // Convert the Bitmap to a 3D float array
                                    float[][][][] input = convertBitmapToFloatArray(resizedBitmap);

                                    // Run the inference
                                    float[][] output = new float[1][1];
                                    tflite.run(input, output);

                                    Log.d("MainActivity", "Output size: " + output.length);

                                    // Determine the authenticity of the image based on the output
                                    String authenticity;
                                    if (output[0][0] > 0.5) {
                                        authenticity = "tp";
                                    } else {
                                        authenticity = "au";
                                    }

                                    // Save the authenticity to Firebase Database
                                    saveImageAuthenticityToDatabase(imageRef.getName(), authenticity);
                                    Snackbar.make(rootView, "Image uploaded successfully", Snackbar.LENGTH_LONG).show();

                                } catch (IOException e) {
                                    Log.d("MainActivity", "Model not run!");
                                    e.printStackTrace();
                                }
                            }).addOnFailureListener(e -> Snackbar.make(rootView, "Failed to get download URL: " + e.getMessage(), Snackbar.LENGTH_LONG).show());


                        }).addOnFailureListener(e -> Snackbar.make(rootView, "Failed to upload image: " + e.getMessage(), Snackbar.LENGTH_LONG).show());
                    }
                });
    }

    private MappedByteBuffer loadModelFile(Context context, String filename) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(filename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void saveImageAuthenticityToDatabase(String filename, String authenticity) {
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Image Authenticity");
        databaseRef.child(filename).setValue(authenticity)
                .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Image authenticity saved to database for filename: " + filename))
                .addOnFailureListener(e -> Log.d("MainActivity", "Failed to save image authenticity to database for filename: " + filename));
    }

    private float[][][][] convertBitmapToFloatArray(Bitmap bitmap) {
        int[] intValues = new int[INPUT_SIZE * INPUT_SIZE];
        float[][][][] floatValues = new float[1][INPUT_SIZE][INPUT_SIZE][3];
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[0][i / INPUT_SIZE][i % INPUT_SIZE][0] = (((val >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            floatValues[0][i / INPUT_SIZE][i % INPUT_SIZE][1] = (((val >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
            floatValues[0][i / INPUT_SIZE][i % INPUT_SIZE][2] = ((val & 0xFF) - IMAGE_MEAN) / IMAGE_STD;
        }
        return floatValues;
    }

}



