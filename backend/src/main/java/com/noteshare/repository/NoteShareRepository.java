package com.noteshare.repository;

import com.noteshare.model.NoteShare;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoteShareRepository extends JpaRepository<NoteShare, NoteShare.NoteShareId> {
    List<NoteShare> findByUserId(Long userId);
    List<NoteShare> findByNoteId(Long noteId);
    boolean existsByNoteIdAndUserId(Long noteId, Long userId);
    void deleteByNoteIdAndUserId(Long noteId, Long userId);
    void deleteByUserId(Long userId);
    void deleteByNoteIdIn(List<Long> noteIds);
}
