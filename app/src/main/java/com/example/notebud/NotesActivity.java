package com.example.notebud;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesActivity extends AppCompatActivity implements NotesAdapter.OnNoteClickListener {
    private static final String TAG = "NotesActivity";
    private static final int REQUEST_CODE_STORAGE_PERMISSION = 1;
    private static final int REQUEST_CODE_SELECT_FILE = 2;
    private static final int REQUEST_IMAGE_CAPTURE = 3;
    private static final int REQUEST_PICK_IMAGE = 4;

    private RecyclerView recyclerView;
    private NotesAdapter adapter;
    private DatabaseHelper dbHelper;
    private String currentAttachmentPath;
    private String currentPhotoPath;
    private EditText currentTitleEditText;
    private EditText currentContentEditText;
    private ImageView currentImagePreview;
    private AlertDialog currentDialog;
    private Note currentNote;
    private ImageView lockButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes);

        setupToolbar();
        dbHelper = new DatabaseHelper(this);
        setupRecyclerView();
        setupAddNoteFab();
        loadNotes();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setTitle(R.string.your_notes);
            }
        }
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotesAdapter(dbHelper.getAllNotes(), this);
        recyclerView.setAdapter(adapter);
    }

    private void setupAddNoteFab() {
        FloatingActionButton fabAddNote = findViewById(R.id.fabAddNote);
        fabAddNote.setOnClickListener(v -> showNoteDialog(null));
    }

    private void loadNotes() {
        List<Note> notes = dbHelper.getAllNotes();
        adapter.updateNotes(notes);
    }

    private void showNoteDialog(final Note note) {
        boolean isEdit = note != null;
        currentNote = note;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_note, null);

        currentTitleEditText = dialogView.findViewById(R.id.titleEditText);
        currentContentEditText = dialogView.findViewById(R.id.contentEditText);
        currentImagePreview = dialogView.findViewById(R.id.imagePreview);
        ImageView attachmentButton = dialogView.findViewById(R.id.attachmentButton);
        ImageView imageButton = dialogView.findViewById(R.id.imageButton);
        ImageView cameraButton = dialogView.findViewById(R.id.cameraButton);
        lockButton = dialogView.findViewById(R.id.lockButton);

        if (isEdit) {
            currentTitleEditText.setText(note.getTitle());
            currentContentEditText.setText(note.getContent());
            currentAttachmentPath = note.getAttachmentPath();
            currentPhotoPath = note.getImagePath();
            lockButton.setSelected(note.isLocked());
            updateLockButtonState(note.isLocked());

            if (note.hasImage()) {
                loadImageIntoPreview(note.getImagePath());
            }
        }

        attachmentButton.setOnClickListener(v -> checkPermissionAndSelectFile());
        imageButton.setOnClickListener(v -> checkPermissionAndPickImage());
        cameraButton.setOnClickListener(v -> checkPermissionAndTakePhoto());
        lockButton.setOnClickListener(v -> showPasswordDialog());

        builder.setView(dialogView)
                .setTitle(isEdit ? R.string.edit_note : R.string.add_note)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    currentAttachmentPath = null;
                    currentPhotoPath = null;
                    currentNote = null;
                });

        if (isEdit) {
            builder.setNeutralButton(R.string.delete, (dialog, which) -> showDeleteConfirmationDialog(note));
        }

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                if (saveNote()) {
                    dialog.dismiss();
                }
            });
        });

        currentDialog = dialog;
        dialog.show();
    }

    private void showPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(R.string.enter_password);

        builder.setTitle(currentNote != null && currentNote.isLocked() ?
                        R.string.unlock_note : R.string.lock_note)
                .setView(input)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String password = input.getText().toString();
                    if (!password.isEmpty()) {
                        if (currentNote != null) {
                            boolean wasLocked = currentNote.isLocked();
                            currentNote.setLocked(!wasLocked);
                            currentNote.setNotePassword(password);
                            lockButton.setSelected(currentNote.isLocked());

                            // Update UI to reflect lock state
                            updateLockButtonState(currentNote.isLocked());

                            // Update the database
                            if (dbHelper.updateNote(currentNote) > 0) {
                                String message = wasLocked ?
                                        getString(R.string.note_unlocked) :
                                        getString(R.string.note_locked);
                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                                loadNotes(); // Refresh the list to show updated lock status
                            } else {
                                Toast.makeText(this, R.string.error_saving_note, Toast.LENGTH_SHORT).show();
                                // Revert the lock state if update failed
                                currentNote.setLocked(wasLocked);
                                updateLockButtonState(wasLocked);
                            }
                        }
                    } else {
                        Toast.makeText(this, R.string.error_empty_password, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void updateLockButtonState(boolean isLocked) {
        if (lockButton != null) {
            lockButton.setSelected(isLocked);
            lockButton.setImageResource(isLocked ? R.drawable.lock : R.drawable.lock_icon);
        }
    }

    private void checkPermissionAndSelectFile() {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_CODE_STORAGE_PERMISSION);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] {
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_AUDIO
                        },
                        REQUEST_CODE_STORAGE_PERMISSION);
                return;
            }
        }
        selectFile();
    }

    private void checkPermissionAndPickImage() {
        if (checkImagePermissions()) {
            pickImage();
        }
    }

    private void checkPermissionAndTakePhoto() {
        if (checkImagePermissions()) {
            takePhoto();
        }
    }

    private boolean checkImagePermissions() {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA
                        },
                        REQUEST_CODE_STORAGE_PERMISSION);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.CAMERA
                        },
                        REQUEST_CODE_STORAGE_PERMISSION);
                return false;
            }
        }
        return true;
    }

    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Select File"), REQUEST_CODE_SELECT_FILE);
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".provider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                selectFile();
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadImageIntoPreview(String imagePath) {
        if (imagePath != null && !imagePath.isEmpty() && currentImagePreview != null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            if (bitmap != null) {
                currentImagePreview.setImageBitmap(bitmap);
                currentImagePreview.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_SELECT_FILE:
                    handleFileSelection(data);
                    break;

                case REQUEST_PICK_IMAGE:
                    handleImagePick(data);
                    break;

                case REQUEST_IMAGE_CAPTURE:
                    handleImageCapture();
                    break;
            }
        }
    }

    private void handleFileSelection(Intent data) {
        if (data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                String fileName = "attachment_" + System.currentTimeMillis();
                File file = new File(getFilesDir(), fileName);
                copyFileFromUri(uri, file);
                currentAttachmentPath = file.getAbsolutePath();
                Toast.makeText(this, R.string.file_attached, Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Log.e(TAG, "Error copying file: " + e.getMessage());
                Toast.makeText(this, R.string.error_attaching_file, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleImagePick(Intent data) {
        if (data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                String fileName = "image_" + System.currentTimeMillis() + ".jpg";
                File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);
                copyFileFromUri(imageUri, file);
                currentPhotoPath = file.getAbsolutePath();
                loadImageIntoPreview(currentPhotoPath);
            } catch (IOException e) {
                Log.e(TAG, "Error copying image: " + e.getMessage());
                Toast.makeText(this, "Error saving image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handleImageCapture() {
        if (currentPhotoPath != null) {
            loadImageIntoPreview(currentPhotoPath);
        }
    }

    private void copyFileFromUri(Uri uri, File destination) throws IOException {
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(destination)) {
            if (inputStream == null) {
                throw new IOException("Failed to open input stream");
            }
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
        }
    }

    private boolean saveNote() {
        String title = currentTitleEditText.getText().toString().trim();
        String content = currentContentEditText.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_title, Toast.LENGTH_SHORT).show();
            return false;
        }

        if (currentNote != null) {
            // Update existing note
            currentNote.setTitle(title);
            currentNote.setContent(content);
            currentNote.setAttachmentPath(currentAttachmentPath);
            currentNote.setImagePath(currentPhotoPath);
            // Lock state is preserved as it's handled separately

            if (dbHelper.updateNote(currentNote) > 0) {
                Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.error_saving_note, Toast.LENGTH_SHORT).show();
                return false;
            }
        } else {
            // Create new note
            long noteId = dbHelper.addNote(title, content, currentAttachmentPath, currentPhotoPath);
            if (noteId == -1) {
                Toast.makeText(this, R.string.error_saving_note, Toast.LENGTH_SHORT).show();
                return false;
            }
            Toast.makeText(this, R.string.note_saved, Toast.LENGTH_SHORT).show();
        }

        currentAttachmentPath = null;
        currentPhotoPath = null;
        currentNote = null;
        loadNotes();
        return true;
    }

    private void showUnlockDialog(final Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setHint(R.string.enter_password);

        builder.setTitle(R.string.unlock_note)
                .setView(input)
                .setPositiveButton(R.string.confirm, (dialog, which) -> {
                    String password = input.getText().toString();
                    if (dbHelper.checkNotePassword(note.getId(), password)) {
                        showNoteDetails(note);
                    } else {
                        Toast.makeText(this, R.string.incorrect_password, Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showNoteDetails(Note note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_show_note, null);

        TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
        TextView contentTextView = dialogView.findViewById(R.id.contentTextView);
        TextView dateTextView = dialogView.findViewById(R.id.dateTextView);
        ImageView attachmentIndicator = dialogView.findViewById(R.id.attachmentIndicator);
        ImageView noteImageView = dialogView.findViewById(R.id.noteImageView);
        ImageView editButton = dialogView.findViewById(R.id.editButton);
        ImageView lockStatusIcon = dialogView.findViewById(R.id.lockStatusIcon);

        titleTextView.setText(note.getTitle());
        contentTextView.setText(note.getContent());
        dateTextView.setText(note.getTimestamp());

        // Update lock status icon
        if (note.isLocked()) {
            lockStatusIcon.setVisibility(View.VISIBLE);
            lockStatusIcon.setImageResource(R.drawable.lock);
        } else {
            lockStatusIcon.setVisibility(View.GONE);
        }

        if (note.hasAttachment()) {
            attachmentIndicator.setVisibility(View.VISIBLE);
            attachmentIndicator.setOnClickListener(v -> openAttachment(note.getAttachmentPath()));
        } else {
            attachmentIndicator.setVisibility(View.GONE);
        }

        if (note.hasImage()) {
            Bitmap bitmap = BitmapFactory.decodeFile(note.getImagePath());
            if (bitmap != null) {
                noteImageView.setImageBitmap(bitmap);
                noteImageView.setVisibility(View.VISIBLE);
            }
        } else {
            noteImageView.setVisibility(View.GONE);
        }

        editButton.setOnClickListener(v -> {
            if (currentDialog != null) {
                currentDialog.dismiss();
            }
            if (note.isLocked()) {
                showUnlockDialog(note);
            } else {
                showNoteDialog(note);
            }
        });

        builder.setView(dialogView)
                .setPositiveButton(R.string.close, null);

        currentDialog = builder.create();
        currentDialog.show();
    }

    @Override
    public void onNoteClick(Note note) {
        if (note.isLocked()) {
            showUnlockDialog(note);
        } else {
            showNoteDetails(note);
        }
    }

    @Override
    public void onNoteLongClick(Note note) {
        if (note.isLocked()) {
            showUnlockDialog(note);
        } else {
            showDeleteConfirmationDialog(note);
        }
    }

    private void showDeleteConfirmationDialog(Note note) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_note)
                .setMessage(R.string.delete_confirmation)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    if (note.isLocked()) {
                        showUnlockDialog(note);
                    } else {
                        deleteNote(note);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteNote(Note note) {
        if (note.hasAttachment()) {
            File attachmentFile = new File(note.getAttachmentPath());
            if (attachmentFile.exists()) {
                attachmentFile.delete();
            }
        }

        if (note.hasImage()) {
            File imageFile = new File(note.getImagePath());
            if (imageFile.exists()) {
                imageFile.delete();
            }
        }

        dbHelper.deleteNote(note.getId());
        loadNotes();
        Toast.makeText(this, R.string.note_deleted, Toast.LENGTH_SHORT).show();
    }

    private void openAttachment(String attachmentPath) {
        File file = new File(attachmentPath);
        if (!file.exists()) {
            Toast.makeText(this, R.string.error_file_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(this,
                getApplicationContext().getPackageName() + ".provider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.error_no_app, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_notes, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_logout) {
            logout();
            return true;
        } else if (itemId == R.id.action_sort) {
            // Handle sort menu
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void sortNotesByDate() {
        List<Note> notes = dbHelper.getAllNotes(); // Already sorted by date in DB query
        adapter.updateNotes(notes);
    }

    private void sortNotesByTitle() {
        List<Note> notes = dbHelper.getAllNotes();
        Collections.sort(notes, (n1, n2) -> n1.getTitle().compareToIgnoreCase(n2.getTitle()));
        adapter.updateNotes(notes);
    }

    private void logout() {
        getSharedPreferences("LoginPrefs", MODE_PRIVATE)
                .edit()
                .putBoolean("isLoggedIn", false)
                .apply();

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
    }
}