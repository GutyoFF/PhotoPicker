package com.example.photopicker;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.photopicker.databinding.SheetOptionsBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class BottomSheetOptions extends BottomSheetDialogFragment{
    private SheetOptionsBinding binding;
    private OnSheetOptionListener mListener;
    interface OnSheetOptionListener {
        void pickImageFromGallery();
        void takePhoto();
        void scanQR();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            mListener = (OnSheetOptionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    context.toString() + " must implement OnSheetOptionsListener"
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
        binding = SheetOptionsBinding.inflate(inflater, container, false);
        binding.btnPickImage.setOnClickListener((view)-> {
            mListener.pickImageFromGallery();
            dismiss();
        });
        binding.btnTakePic.setOnClickListener((view -> {
            mListener.takePhoto();
            dismiss();
        }));
        binding.btnQrScan.setOnClickListener(v -> {
            mListener.scanQR();
            dismiss();
        });
        return binding.getRoot();
    }
}