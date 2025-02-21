package com.example.notebud;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {
    private List<Note> notes;
    private OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
        void onNoteLongClick(Note note);
    }

    public NotesAdapter(List<Note> notes, OnNoteClickListener listener) {
        this.notes = notes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = notes.get(position);
        holder.titleTextView.setText(note.getTitle());
        holder.dateTextView.setText(note.getTimestamp());

        // Show/hide attachment icon
        holder.attachmentIcon.setVisibility(note.hasAttachment() ? View.VISIBLE : View.GONE);

        // Show/hide lock icon and handle content display
        if (note.isLocked()) {
            holder.lockIcon.setVisibility(View.VISIBLE);
            holder.contentTextView.setText("🔒 Content is locked");
            holder.lockIcon.setImageResource(R.drawable.lock); // Use the locked icon
        } else {
            holder.lockIcon.setVisibility(View.GONE);
            holder.contentTextView.setText(note.getContent());
        }

        // Handle image thumbnail
        if (note.hasImage()) {
            loadThumbnail(holder.thumbnailImage, note.getImagePath());
            holder.thumbnailImage.setVisibility(View.VISIBLE);
        } else {
            holder.thumbnailImage.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNoteClick(note);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onNoteLongClick(note);
                return true;
            }
            return false;
        });
    }

    private void loadThumbnail(ImageView imageView, String imagePath) {
        // Load and resize the image for thumbnail
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath, options);

        // Calculate sample size for thumbnail
        options.inSampleSize = calculateInSampleSize(options, 100, 100);
        options.inJustDecodeBounds = false;

        // Load the sampled down bitmap
        Bitmap thumbnail = BitmapFactory.decodeFile(imagePath, options);
        if (thumbnail != null) {
            imageView.setImageBitmap(thumbnail);
        }
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    @Override
    public int getItemCount() {
        return notes != null ? notes.size() : 0;
    }

    public void updateNotes(List<Note> newNotes) {
        this.notes = newNotes;
        notifyDataSetChanged();
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        TextView contentTextView;
        TextView dateTextView;
        ImageView attachmentIcon;
        ImageView lockIcon;
        ImageView thumbnailImage;

        NoteViewHolder(View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            attachmentIcon = itemView.findViewById(R.id.attachmentIcon);
            lockIcon = itemView.findViewById(R.id.lockIcon);
            thumbnailImage = itemView.findViewById(R.id.thumbnailImage);
        }
    }
}