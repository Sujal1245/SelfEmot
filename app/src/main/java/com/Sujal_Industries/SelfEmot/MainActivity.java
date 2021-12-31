package com.Sujal_Industries.SelfEmot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST = 7899;
    private static final int REQUEST_TAKE_PHOTO = 420;
    private static final String savedFile = "SETTINGS";
    private final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private ImageView capturedImage;
    private String mCurrentPhotoPath;
    private RecyclerView recyclerView;
    private RecyclerView.LayoutManager layoutManager;
    private ResultAdapter resultAdapter;
    private ArrayList<Result> resultArrayList;
    private File file;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    LinearLayout info;
    Toolbar toolbar;
    CollapsingToolbarLayout collapsingToolbarLayout;
    ActionBar actionBar;
    FloatingActionButton fab;
    FloatingActionButton settings;
    ImageButton darkTog;
    CoordinatorLayout coordinatorLayout;
    AppBarLayout appBarLayout;
    boolean on;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Preparing theme as per the settings..
        sp = getSharedPreferences(savedFile, MODE_PRIVATE);
        if (sp.getBoolean("Night", false)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Getting elements ready..
        toolbar = findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar_layout);
        capturedImage = findViewById(R.id.captured);
        recyclerView = findViewById(R.id.recyclerView);
        fab = findViewById(R.id.floating_action_button);
        settings = findViewById(R.id.setting_fab);
        darkTog = findViewById(R.id.darkToggle);
        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        info = findViewById(R.id.info);
        appBarLayout = findViewById(R.id.appBarLayout);

        //Setting up layout design..
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle("SelfEmot");
            actionBar.setDisplayHomeAsUpEnabled(false);
        }

        //Asking for permissions if not done..
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, MY_PERMISSIONS_REQUEST);
        }

        fab.setOnClickListener(v -> {
            if (hasPermissions(MainActivity.this, PERMISSIONS)) {
                openCamera();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, MY_PERMISSIONS_REQUEST);
            }
        });

        //Animations
        final Animation show_fab_1 = AnimationUtils.loadAnimation(getApplication(), R.anim.fab1_show);
        final Animation hide_fab_1 = AnimationUtils.loadAnimation(getApplication(), R.anim.fab1_hide);

        on = false;
        settings.setOnClickListener(view -> {
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) darkTog.getLayoutParams();
            if (!on) {
                layoutParams.rightMargin += (int) (darkTog.getWidth() * 0.75);
                layoutParams.topMargin += (int) (darkTog.getHeight() * 0.25);
                darkTog.setLayoutParams(layoutParams);
                darkTog.startAnimation(show_fab_1);
                darkTog.setClickable(true);
                on = true;
            } else {
                layoutParams.rightMargin -= (int) (darkTog.getWidth() * 0.75);
                layoutParams.topMargin -= (int) (darkTog.getHeight() * 0.25);
                darkTog.setLayoutParams(layoutParams);
                darkTog.startAnimation(hide_fab_1);
                darkTog.setClickable(false);
                on = false;
            }
        });

        darkTog.setOnClickListener(view -> {
            editor = sp.edit();

            switch (AppCompatDelegate.getDefaultNightMode()) {
                case AppCompatDelegate.MODE_NIGHT_YES: {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    editor.putBoolean("Night", false);
                    break;
                }
                case AppCompatDelegate.MODE_NIGHT_NO: {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    editor.putBoolean("Night", true);
                    break;
                }
            }
            editor.apply();
        });

        if (savedInstanceState != null) {
            mCurrentPhotoPath = savedInstanceState.getString("CurrentPath");
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.Sujal_Industries.SelfEmot", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(intent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_PHOTO) {
                file = new File(mCurrentPhotoPath);
                Bitmap bitmap;
                try {
                    bitmap = MediaStore.Images.Media
                            .getBitmap(getContentResolver(), Uri.fromFile(file));
                    RotateImage rotateImage = new RotateImage();
                    rotateImage.execute(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String imageFileName = "Photo7899";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    //Class for doing background tasking.
    private class RotateImage extends AsyncTask<Bitmap, Void, Bitmap> {
        private ProgressBar progressBar;
        private Bitmap rotatedBitmap;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar = findViewById(R.id.progress);
            progressBar.setVisibility(View.VISIBLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }

        @Override
        protected Bitmap doInBackground(Bitmap... imgs) {
            Bitmap bitmap = imgs[0];

            if (bitmap != null) {
                //Rotating the image captured...
                ExifInterface ei = null;
                try {
                    ei = new ExifInterface(mCurrentPhotoPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (file.exists()) {
                    if (file.delete()) {
                        Log.i("Process", "DONE!");
                    }
                }

                int orientation = 0;
                if (ei != null) {
                    orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_UNDEFINED);
                }

                float angle = 0.0f;

                //Saving angle as per the condition.
                try {
                    switch (orientation) {

                        case ExifInterface.ORIENTATION_ROTATE_90:
                            angle = 90.0f;
                            break;

                        case ExifInterface.ORIENTATION_ROTATE_180:
                            angle = 180.0f;
                            break;

                        case ExifInterface.ORIENTATION_ROTATE_270:
                            angle = 270.0f;
                            break;

                        case ExifInterface.ORIENTATION_NORMAL:
                        default:
                            rotatedBitmap = bitmap;
                    }
                } catch (Exception e) {
                    Log.e("Error", "Error OCCURRED!");
                }
                //Rotating Image...
                Matrix matrix = new Matrix();
                matrix.postRotate(angle);
                rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                return rotatedBitmap;
            } else {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            assert rotatedBitmap != null;
            //DisplayingImage
            Glide.with(MainActivity.this)
                    .load(rotatedBitmap)
                    .into(capturedImage);
            //Processing Image...
            InputImage image = InputImage.fromBitmap(rotatedBitmap, 0);
            ImageLabeler detector = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
            @SuppressLint("NotifyDataSetChanged") Task<List<ImageLabel>> result =
                    detector.process(image)
                            .addOnSuccessListener(
                                    labels -> {
                                        // Task completed successfully
                                        // Updating RecyclerView...
                                        resultArrayList = new ArrayList<>();
                                        resultAdapter = new ResultAdapter(resultArrayList);
                                        layoutManager = new LinearLayoutManager(getApplicationContext());

                                        info.setVisibility(View.GONE);

                                        recyclerView.setVisibility(View.VISIBLE);
                                        recyclerView.setHasFixedSize(false);
                                        recyclerView.setLayoutManager(layoutManager);
                                        recyclerView.setAdapter(resultAdapter);

                                        for (ImageLabel label : labels) {
                                            String text = label.getText();
                                            float confidence = label.getConfidence() * 100;
                                            String con = ((double) Math.round(confidence * 100d) / 100d) + "%";
                                            resultArrayList.add(new Result(text, con));
                                        }

                                        resultAdapter.notifyDataSetChanged();

                                        Snackbar snak = Snackbar.make(coordinatorLayout, "Success!", Snackbar.LENGTH_SHORT);
                                        snak.show();
                                    })
                            .addOnFailureListener(
                                    e -> {
                                        // Task failed with an exception
                                        // Giving Warning!
                                        Snackbar snak = Snackbar.make(coordinatorLayout, "Failure!", Snackbar.LENGTH_SHORT);
                                        snak.show();
                                    });
            progressBar.setVisibility(View.INVISIBLE);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("CurrentPath", mCurrentPhotoPath);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mCurrentPhotoPath = savedInstanceState.getString("CurrentPath");
    }
}