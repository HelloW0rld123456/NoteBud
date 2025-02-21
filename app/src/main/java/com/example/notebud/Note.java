package com.example.notebud;

public class Note {
    private long id;
    private String title;
    private String content;
    private String timestamp;
    private String attachmentPath;
    private String imagePath;
    private boolean isLocked;
    private String notePassword;

    public Note(long id, String title, String content, String timestamp, String attachmentPath) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.timestamp = timestamp;
        this.attachmentPath = attachmentPath;
        this.isLocked = false;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getAttachmentPath() {
        return attachmentPath;
    }

    public void setAttachmentPath(String attachmentPath) {
        this.attachmentPath = attachmentPath;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean hasAttachment() {
        return attachmentPath != null && !attachmentPath.isEmpty();
    }

    public boolean hasImage() {
        return imagePath != null && !imagePath.isEmpty();
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public String getNotePassword() {
        return notePassword;
    }

    public void setNotePassword(String password) {
        this.notePassword = password;
    }
}