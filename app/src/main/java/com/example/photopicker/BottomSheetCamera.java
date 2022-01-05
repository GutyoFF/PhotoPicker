package com.example.photopicker;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.interop.Camera2CameraInfo;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.core.ViewPort;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.photopicker.databinding.SheetCameraBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class BottomSheetCamera extends BottomSheetDialogFragment {
    private static final String FOLDER_IMAGES = "EsmeImpulsa";
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private SheetCameraBinding binding;
    private BottomSheetCamera.OnSheetCaptureListener mListener;
    private ImageCapture imageCapture;
    private final String TAG = this.getClass().getSimpleName();
    private int lensFacing = CameraSelector.LENS_FACING_BACK;

    /*
     La resolucion por defecto de la camara, creo que está respetable pero deberías
     revisar las resoluciones que la tableta traiga, debe traer más porque está bien
     cariñosa, aún así se supone debe guardarlas un poquito más ligeras por el MINIMUM_LATENCY
     de por ahi abajo. Con un log del metodo initCameraX puedes ver el array de resoluciones
     disponibles de la camara. Trae un problema con la responsividad ésta tontería en el preview
     de la cámara si lo quieres arreglar ahi pues ta bn, sino pues igual no se ve tan mal.
     */
    private Size currentResolution = new Size(640, 480);
    private PopupMenu popupMenu;
    private Size[] resolutions;

    interface OnSheetCaptureListener {
        void captureImage(Uri photoUri);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (BottomSheetCamera.OnSheetCaptureListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    context.toString() + " must implement OnSheetCaptureListener"
            );
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final BottomSheetDialog sheet = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        sheet.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
        return sheet;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SheetCameraBinding.inflate(inflater, container, false);
        startCameraX();
        return binding.getRoot();
    }

    private void capturePhoto() {
        /* Se hizo un desastre pero aqui lo único que haces es ponerle nombre a las imagenes
        en base al momento en el que fueron tomadas, y despues con el callback de la camara
        regresarle la Uri de la foto tomada a la actividad principal
        */
        String timestamp = String.valueOf(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        if (Build.VERSION.SDK_INT >= 29) {
            contentValues.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/" + FOLDER_IMAGES);
        } else {
            createFolderIfNotExist();
            String path = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES) + "/" + FOLDER_IMAGES + "/" + System.currentTimeMillis();
            contentValues.put(MediaStore.Images.Media.DATA, path);
        }

        imageCapture.takePicture(
                new ImageCapture.OutputFileOptions.Builder(
                        requireContext().getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                ).build(),
                getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(getContext(), "Photo's been saved", Toast.LENGTH_SHORT).show();
                        mListener.captureImage(outputFileResults.getSavedUri());
                        dismiss();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        exception.printStackTrace();
                    }
                });
    }

    private Executor getExecutor() {
        // Metodo que copié y pegué, hazlo así xdd
        return ContextCompat.getMainExecutor(requireContext());
    }

    private void switchCameraX() {
        // Para revisar la camara en uso y alternarla
        if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            lensFacing = CameraSelector.LENS_FACING_FRONT;
        } else {
            lensFacing = CameraSelector.LENS_FACING_BACK;
        }
        startCameraX();
    }

    private void startCameraX(){
        // Para iniciar la camara
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(()-> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                initCameraX(cameraProvider);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, getExecutor());
    }

    @SuppressLint("RestrictedApi")
    private void initCameraX(ProcessCameraProvider cameraProvider) {
        /* Aqui le pongo las configuraciones a la camara dependiendo de la resolución solicitada
        por defecto la tengo como la que está en las variables de arriba pero despues de inicializar
        la camara puedo recuperar las especificaciones y dar opción de utilizar las que traiga,
        desde la más alta hasta la más baja
        */
        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        Preview previewView = new Preview.Builder()
                .setTargetResolution(currentResolution)
                .build();

        Rational rational;
        int rotation;
        if (Math.max(currentResolution.getWidth(), currentResolution.getHeight())
                == currentResolution.getWidth()) {
            rotation = Surface.ROTATION_90;
            rational = new Rational(4, 3);
        } else {
            rotation = Surface.ROTATION_0;
            rational = new Rational(3, 4);
        }

        ViewPort viewPort = new ViewPort.Builder(
                rational,
                rotation).build();

        previewView.setSurfaceProvider(binding.pvCamera.getSurfaceProvider());
        imageCapture = new ImageCapture.Builder()
                .setTargetName("Capture")
                .setTargetResolution(currentResolution)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                .addUseCase(previewView)
                .addUseCase(imageCapture)
                .setViewPort(viewPort)
                .build();

        Camera camera = cameraProvider
                .bindToLifecycle((LifecycleOwner) this, cameraSelector, useCaseGroup);

        @SuppressLint({"RestrictedApi", "UnsafeOptInUsageError"})
        CameraCharacteristics characteristics =
                Camera2CameraInfo.extractCameraCharacteristics(camera.getCameraInfo());
        StreamConfigurationMap map =
                characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        resolutions = map.getOutputSizes(ImageFormat.JPEG);
        setListeners();
        Log.d(TAG, "initCameraX: " + Arrays.toString(map.getOutputSizes(ImageFormat.JPEG)));
    }

    private void setListeners() {
        // Aqui nomas para poner los listeners e inflar el menú de seleccion de resoluciones

        popupMenu = new PopupMenu(new ContextThemeWrapper(
                getContext(),
                R.style.Base_Widget_MaterialComponents_PopupMenu
        ), binding.btnSettings);
        popupMenu.setOnMenuItemClickListener(itemClickListener);
        for (int i = 0; i < resolutions.length; i++) {
            popupMenu.getMenu().add(1, i, i, resolutions[i].toString());
        }
        binding.btnCapture.setOnClickListener((view)-> {
            capturePhoto();
        });
        binding.btnSwitch.setOnClickListener((v) -> {
            switchCameraX();
        });
        binding.btnSettings.setOnClickListener((view -> {
            popupMenu.show();
        }));
    }

    private final PopupMenu.OnMenuItemClickListener itemClickListener =
            new PopupMenu.OnMenuItemClickListener() {
        /*
        * Les excita girar pantallas y por eso aqui se hace el switch para saber la
        * orientacion de la pantalla y en base a ello poder ponerla bien porque
        * android tontito piensa que al girar la pantalla, la camara es diferente,
        * o al menos el viewport, no sé exactamente pero tiene que ser así o si no
        * el tontito de android elige la resolucion que se le pega la gana*/
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            int id = menuItem.getItemId();
            for (int i = 0; i < resolutions.length; i++) {
                Log.i(TAG, "onMenuItemClick: " + id + " - " + i);
                if (i == id) {
                    switch (getResources().getConfiguration().orientation) {
                        case Configuration.ORIENTATION_LANDSCAPE:
                            currentResolution =
                                    new Size(resolutions[i].getWidth(), resolutions[i].getHeight());
                            break;
                        case Configuration.ORIENTATION_PORTRAIT:
                            currentResolution =
                                    new Size(resolutions[i].getHeight(), resolutions[i].getWidth());
                            break;
                    }
                    Log.i(TAG, "onMenuItemClick: " + currentResolution.toString());
                    startCameraX();
                    break;
                }
            }
            return false;
        }
    };

    private void createFolderIfNotExist() {
        // Tambien copié y pegue xd
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES) + "/" + FOLDER_IMAGES);
        if (!file.exists()) {
            if (!file.mkdir()) {
                Log.d(TAG, "Folder Create -> Failure");
            } else {
                Log.d(TAG, "Folder Create -> Success");
            }
        }
    }


}
