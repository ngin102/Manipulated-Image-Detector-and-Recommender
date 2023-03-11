package com.example.manipulatedimagerecommenderanddetector;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import com.example.manipulatedimagerecommenderanddetector.databinding.ActivityMainBinding;
import com.example.manipulatedimagerecommenderanddetector.ui.main.PlaceholderFragment;
import com.example.manipulatedimagerecommenderanddetector.ui.main.SectionsPagerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
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
import java.util.List;

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
            intent.setType("image/jpeg"); // only accept JPG images
            intent.setAction(Intent.ACTION_GET_CONTENT);
            activityResultLauncher.launch(Intent.createChooser(intent, "Select Picture"));
        });

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Get the URI of the selected image
                        assert result.getData() != null;
                        Uri imageUri = result.getData().getData();

                        // Check if the selected image is a JPG
                        String fileType = getContentResolver().getType(imageUri);
                        if (!TextUtils.equals(fileType, "image/jpeg")) {
                            Snackbar.make(rootView, "Only JPG images are accepted", Snackbar.LENGTH_LONG).show();
                            return;
                        }

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
                                    getImageTags(imageRef.getName());
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

    private void getImageTags(String filename) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference imageRef = storage.getReference().child("images/" + filename);

        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Image Tags");

        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                // Use the download URL to load the image in your app
                String downloadUrl = uri.toString();

                Picasso.get().load(downloadUrl).into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        // Create an InputImage object from the Bitmap
                        InputImage image = InputImage.fromBitmap(bitmap, 0);
                        // Use the image for labeling or other tasks
                        ImageLabeler tagger = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
                        tagger.process(image)
                                .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                                    @Override
                                    public void onSuccess(List<ImageLabel> labels) {
                                        String tags = "";
                                        for (int i = 0; i < labels.size(); i++) {
                                            String text = labels.get(i).getText();
                                            float confidence = labels.get(i).getConfidence();
                                            int index = labels.get(i).getIndex();

                                            if (i != labels.size() - 1)
                                            {
                                                tags += text + ", ";
                                            }
                                            else
                                            {
                                                tags += text;
                                            }
                                        }
                                        databaseRef.child(filename).setValue(tags)
                                                .addOnSuccessListener(aVoid -> Log.d("MainActivity", "Image tags saved to database for filename: " + filename))
                                                .addOnFailureListener(e -> Log.d("MainActivity", "Failed to save image tags to database for filename: " + filename));
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
                    }

                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                        // Handle errors loading the image
                        // ...
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                        // Optional placeholder callback
                        // ...
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Handle any errors while retrieving the download URL
                Log.e(TAG, "Error getting download URL", e);
            }
        });
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



