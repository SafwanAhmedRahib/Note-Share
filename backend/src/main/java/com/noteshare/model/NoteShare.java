package com.noteshare.model;

import jakarta.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "note_shares")
@IdClass(NoteShare.NoteShareId.class)
public class NoteShare {
    @Id
    private Long noteId;

    @Id
    private Long userId;

    public NoteShare() {}
    public NoteShare(Long noteId, Long userId) {
        this.noteId = noteId;
        this.userId = userId;
    }

    public Long getNoteId() { return noteId; }
    public void setNoteId(Long noteId) { this.noteId = noteId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public static class NoteShareId implements Serializable {
        private Long noteId;
        private Long userId;
        public NoteShareId() {}
        public NoteShareId(Long noteId, Long userId) {
            this.noteId = noteId;
            this.userId = userId;
        }
        public Long getNoteId() { return noteId; }
        public void setNoteId(Long noteId) { this.noteId = noteId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }
}
