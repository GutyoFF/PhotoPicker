package com.example.photopicker;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.camera.core.impl.utils.Exif;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photopicker.databinding.ItemImageResourceBinding;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {
    private final String TAG = this.getClass().getSimpleName();
    private final ArrayList<Uri> mData;
    private final OnResourceClickListener mListener;

    interface OnResourceClickListener {
        default void onDeleteResource() {}
        void onResourceClick(Uri resourceUri);
    }

    // data is passed into the constructor
    ImageAdapter(ArrayList<Uri> data, OnResourceClickListener listener) {
        this.mData = data;
        this.mListener = listener;
    }

    // inflates the row layout from xml when needed
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemImageResourceBinding binding = ItemImageResourceBinding
                .inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    // binds the data to the TextView in each row
    @SuppressLint("RestrictedApi")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Uri uri = mData.get(position);
        Log.i(TAG, "onBindViewHolder: " + uri);
        int rotation = 0;
        try {
            InputStream x = holder.itemView.getContext().getContentResolver().openInputStream(uri);
            Exif exif = Exif.createFromInputStream(x);
            x.close();
            rotation = exif.getRotation();
            Log.i(TAG, "onBindViewHolder: "+rotation);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Picasso.get().load(uri)
                .into(holder.imageResource, new Callback() {
                    @Override
                    public void onSuccess() {
                        holder.imageResource.setOnClickListener((v) -> {
                            mListener.onResourceClick(uri);
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        holder.btnDelete.setOnClickListener((v)-> {
                            removeUri(holder.getAdapterPosition());
                            mListener.onDeleteResource();
                        });
                    }
                });
    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // stores and recycles views as they are scrolled off screen
    public static class ViewHolder extends RecyclerView.ViewHolder  {
        ImageView imageResource;
        ImageView btnDelete;

        ViewHolder(ItemImageResourceBinding binding) {
            super(binding.getRoot());
            imageResource = binding.ivImage;
            btnDelete = binding.btnDelete;
        }
    }

    private void removeUri(int position) {
        mData.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mData.size()-1);
        Log.i(TAG, "removeUri: "+mData.size());
        Log.i(TAG, "removeUri: "+mData);
    }

    public void addUri(Uri uri) {
        mData.add(uri);
        notifyItemInserted(mData.size() - 1);
        Log.i(TAG, "addUri: "+ mData);
    }
}
