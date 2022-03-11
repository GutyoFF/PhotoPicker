package com.example.photopicker;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.core.UseCaseGroup;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.example.photopicker.classes.QRCodeAnalyzer;
import com.example.photopicker.databinding.SheetQrBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class BottomSheetQr extends BottomSheetDialogFragment {
    private SheetQrBinding binding;
    private OnQRScanned listener;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private final String TAG = this.getClass().getSimpleName();
    private String qrCode;

    public interface OnQRScanned {
        void onQRScanned(String qr);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (OnQRScanned) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    context.toString() + " must implement OnQRScanned"
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SheetQrBinding.inflate(inflater, container, false);
        startCameraX();
        return binding.getRoot();
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

    private Executor getExecutor() {
        // Metodo que copié y pegué, hazlo así xdd
        return ContextCompat.getMainExecutor(requireContext());
    }

    private ImageAnalysis qrAnalysis() {
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
        binding.btnCapture.setOnClickListener(v -> {
            Toast.makeText(getContext(), qrCode, Toast.LENGTH_SHORT).show();
            listener.onQRScanned(qrCode);
        });
        imageAnalysis.setAnalyzer(getExecutor(), new QRCodeAnalyzer(new QRCodeAnalyzer.QRCodeFoundListener() {
            @Override
            public void onQRCodeFound(String _qrCode) {
                qrCode = _qrCode;
                binding.btnCapture.setVisibility(View.VISIBLE);
            }

            @Override
            public void qrCodeNotFound() {
                binding.btnCapture.setVisibility(View.INVISIBLE);
            }
        }));
        return imageAnalysis;
    }

    private void initCameraX(ProcessCameraProvider cameraProvider) {
        /* Aqui le pongo las configuraciones a la camara dependiendo de la resolución solicitada
        por defecto la tengo como la que está en las variables de arriba pero despues de inicializar
        la camara puedo recuperar las especificaciones y dar opción de utilizar las que traiga,
        desde la más alta hasta la más baja
        */
        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .build();

        Preview previewView = new Preview.Builder()
                .build();

        previewView.setSurfaceProvider(binding.pvCamera.getSurfaceProvider());

        UseCaseGroup useCaseGroup = new UseCaseGroup.Builder()
                .addUseCase(previewView)
                .addUseCase(qrAnalysis())
                .build();

        cameraProvider
                .bindToLifecycle((LifecycleOwner) this, cameraSelector, useCaseGroup);
    }

    private void setListeners() {
    }
}
