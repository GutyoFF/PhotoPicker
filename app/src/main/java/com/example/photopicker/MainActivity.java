package com.example.photopicker;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.impl.utils.Exif;
import androidx.core.content.ContextCompat;

import com.example.photopicker.databinding.ActivityMainBinding;
import com.squareup.picasso.Picasso;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity implements
        BottomSheetOptions.OnSheetOptionListener,
        BottomSheetCamera.OnSheetCaptureListener, ImageAdapter.OnResourceClickListener,
        BottomSheetQr.OnQRScanned
{
    private final String TAG = this.getClass().getSimpleName();

    private ArrayList<Uri> resources;
    private ImageAdapter imageAdapter;

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        resources = new ArrayList<Uri>();
        imageAdapter = new ImageAdapter(resources, this);
        binding.rvImages.setAdapter(imageAdapter);
    }

    private void setListeners() {
        binding.btnImageAdd.setOnClickListener((view -> {
            BottomSheetOptions sheet = new BottomSheetOptions();
            sheet.show(getSupportFragmentManager(), "SheetOptions");
        }));
    }

    // Take photo
    private final ActivityResultLauncher<String[]> requestWriteLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                AtomicReference<Boolean> areGranted = new AtomicReference<>(true);
                result.forEach((s, b) -> {
                    areGranted.set(b);
                });
                if (areGranted.get()) {
                    openCamera();
                } else {
                    Toast.makeText(this, "Se requieren ambos permisos para continuar",
                            Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void checkWritePermission(Boolean forQR) {
        if(
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED
        ) {
            if (forQR) {
                openScanner();
            } else {
                openCamera();
            }
        } else {
            requestWriteLauncher.launch(
                    new String[]{
                            Manifest.permission.CAMERA
                    }
            );
        }
    }

    private void openScanner() {
        BottomSheetQr sheetQr = new BottomSheetQr();
        sheetQr.show(getSupportFragmentManager(), "QR");
    }

    private void openCamera() {
        BottomSheetCamera sheetCamera = new BottomSheetCamera();
        sheetCamera.show(getSupportFragmentManager(), "Camera");
    }

    @Override
    public void takePhoto() {
        Toast.makeText(this, "Take Photo", Toast.LENGTH_SHORT).show();
        checkWritePermission(false);
    }

    // Pick image
    private final ActivityResultLauncher<String> requestReadLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), (granted) -> {
                if (granted) {
                    pickImage();
                } else {
                    Toast.makeText(this, "El permiso es requerido para el manejo de imagenes",
                            Toast.LENGTH_SHORT).show();
                }
            }
    );

    private void checkReadPermission(){
        if(ContextCompat
                .checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
        ) {
            pickImage();
        } else {
            requestReadLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private final ActivityResultLauncher<Intent> startForActivityGallery = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), (data) -> {
                if (data.getResultCode() == Activity.RESULT_OK) {
                    Uri uri = data.getData().getData();
                    setPreview(uri);
                    imageAdapter.addUri(uri);
                }
            }
    );

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startForActivityGallery.launch(intent);
    }


    @Override
    public void pickImageFromGallery() {
        Toast.makeText(this, "Pick Image", Toast.LENGTH_SHORT).show();
        checkReadPermission();
    }

    @Override
    public void scanQR() {
        Toast.makeText(this, "Scan QR", Toast.LENGTH_SHORT).show();
        checkWritePermission(true);
    }

    @SuppressLint("RestrictedApi")
    private void setPreview(Uri fileUri) {
        //Only to check if image is rotated
        int rotation = 0;
        try {
            InputStream x = getContentResolver().openInputStream(fileUri);
            Exif exif = Exif.createFromInputStream(x);
            x.close();
            rotation = exif.getRotation();
            Log.i(TAG, "onBindViewHolder: "+rotation);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Picasso.get()
                .load(fileUri)
                .rotate(rotation)
                .error(R.drawable.ic_baseline_broken_image_24)
                .into(binding.ivPreview);
    }

    @Override
    public void captureImage(Uri photoUri) {
        setPreview(photoUri);
        imageAdapter.addUri(photoUri);
    }


    @Override
    public void onResourceClick(Uri resourceUri) {
        setPreview(resourceUri);
    }

    @Override
    public void onQRScanned(String qr) {
        binding.tvQr.setText(qr);
    }
}
