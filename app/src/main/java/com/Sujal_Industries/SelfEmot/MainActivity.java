package com.Sujal_Industries.SelfEmot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.core.splashscreen.SplashScreen;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.snackbar.Snackbar;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST = 7899;
    private static final String savedFile = "SETTINGS";
    private String[] PERMISSIONS;
    private ShapeableImageView capturedImage;
    private String mCurrentPhotoPath;
    private RecyclerView recyclerView;
    private ResultAdapter resultAdapter;
    private ArrayList<Result> resultArrayList;
    private File file;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    private Disposable d;

    LinearLayoutCompat info;
    FloatingActionButton fab;
    FloatingActionButton darkTog;
    CoordinatorLayout coordinatorLayout;

    private LinearProgressIndicator progressBar;
    private Bitmap rotatedBitmap;

    ActivityResultLauncher<Intent> mGetContent = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == RESULT_OK) {
            file = new File(mCurrentPhotoPath);
            Bitmap bitmap;
            try {
                bitmap = MediaStore.Images.Media
                        .getBitmap(getContentResolver(), Uri.fromFile(file));
                startAsyncTask(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    });

    @SuppressLint("SwitchIntDef")
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

        SplashScreen.installSplashScreen(this);

        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT < 33) {
            if (Build.VERSION.SDK_INT > 28) {
                PERMISSIONS = new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE};
            } else {
                PERMISSIONS = new String[]{Manifest.permission.CAMERA,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE};
            }
        } else {
            PERMISSIONS = new String[]{Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES};
        }

        //Getting elements ready..
        capturedImage = findViewById(R.id.captured);
        recyclerView = findViewById(R.id.recyclerView);
        fab = findViewById(R.id.floating_action_button);
        darkTog = findViewById(R.id.darkToggle);
        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        info = findViewById(R.id.info);

        float radius = 50.0f;
        capturedImage.setShapeAppearanceModel(capturedImage.getShapeAppearanceModel()
                .toBuilder()
                .setBottomLeftCorner(CornerFamily.ROUNDED, radius)
                .setBottomRightCorner(CornerFamily.ROUNDED, radius)
                .build());

        resultArrayList = new ArrayList<>();
        resultAdapter = new ResultAdapter(resultArrayList);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(resultAdapter);

        //Asking for permissions if not done..
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, MY_PERMISSIONS_REQUEST);
        }

        fab.setOnClickListener(v -> {
            if (hasPermissions(MainActivity.this, PERMISSIONS)) {
                openCamera();
            } else {
                Log.i("EMERGENCY", "Asking for permission...");
                ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, MY_PERMISSIONS_REQUEST);
            }
        });

        darkTog.setOnClickListener(view -> {
            editor = sp.edit();

            switch (AppCompatDelegate.getDefaultNightMode()) {
                case AppCompatDelegate.MODE_NIGHT_YES -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    editor.putBoolean("Night", false);
                }
                case AppCompatDelegate.MODE_NIGHT_NO -> {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    editor.putBoolean("Night", true);
                }
            }
            editor.apply();
        });
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
        Log.v("EMERGENCY", "Tried to open camera");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        try {
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
                mGetContent.launch(intent);
                Log.v("EMERGENCY", "PASSED");
            } else {
                Log.v("EMERGENCY", "FAILURE");
            }
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Log.v("EMERGENCY", "FAILED");
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

    private void startAsyncTask(Bitmap bitmap) {
        d = Observable.just(bitmap)
                .map(this::doInBackground)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(this::onPreExecute)
                .subscribe(this::onPostExecute);
    }

    private void onPreExecute(Disposable disposable) {
        progressBar = findViewById(R.id.progress);
        progressBar.show();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
    }


    private Bitmap doInBackground(Bitmap... imgs) {
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
                orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
            }

            float angle = 0.0f;

            //Saving angle as per the condition.
            try {
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90 -> angle = 90.0f;
                    case ExifInterface.ORIENTATION_ROTATE_180 -> angle = 180.0f;
                    case ExifInterface.ORIENTATION_ROTATE_270 -> angle = 270.0f;
                    default -> rotatedBitmap = bitmap;
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

    @SuppressLint("NotifyDataSetChanged")
    private void onPostExecute(Bitmap bitmap) {
        assert rotatedBitmap != null;
        //DisplayingImage
        Glide.with(MainActivity.this)
                .load(rotatedBitmap)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(capturedImage);
        //Processing Image...
        InputImage image = InputImage.fromBitmap(rotatedBitmap, 0);
        ImageLabeler detector = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
        detector.process(image)
                .addOnSuccessListener(
                        labels -> {
                            // Task completed successfully
                            // Updating RecyclerView...
                            info.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);

                            resultArrayList.clear();

                            for (ImageLabel label : labels) {
                                String text = label.getText();
                                float confidence = label.getConfidence() * 100;
                                String con = ((double) Math.round(confidence * 100d) / 100d) + "%";
                                resultArrayList.add(new Result(text, con));
                            }

                            resultAdapter.notifyDataSetChanged();
                            animateRecyclerView();

                            Snackbar.make(coordinatorLayout, "Success!", Snackbar.LENGTH_SHORT).show();

                            progressBar.hide();
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        })
                .addOnFailureListener(
                        e -> {
                            // Task failed with an exception
                            // Giving Warning!
                            Snackbar.make(coordinatorLayout, "Failure!", Snackbar.LENGTH_SHORT).show();
                            progressBar.hide();
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                        });
    }

    public void animateRecyclerView() {
        recyclerView.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        recyclerView.getViewTreeObserver().removeOnPreDrawListener(this);

                        for (int i = 0; i < recyclerView.getChildCount(); i++) {
                            View v = recyclerView.getChildAt(i);
                            v.setAlpha(0.0f);
                            v.animate().alpha(1.0f)
                                    .setDuration(300)
                                    .setStartDelay(i * 100L)
                                    .start();
                        }

                        return true;
                    }
                });
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (d != null) {
            d.dispose();
        }
    }
}