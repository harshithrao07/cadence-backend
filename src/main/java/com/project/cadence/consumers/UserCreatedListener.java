package com.project.cadence.listeners;

import com.project.cadence.events.UserCreatedEvent;
import com.project.cadence.model.Playlist;
import com.project.cadence.model.PlaylistVisibility;
import com.project.cadence.model.SystemPlaylistType;
import com.project.cadence.model.User;
import com.project.cadence.repository.PlaylistRepository;
import com.project.cadence.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserCreatedListener {

    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;

    @Async
    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserCreated(UserCreatedEvent event) {
        User user = userRepository.findById(event.userId())
                .orElseThrow();
        Playlist likedSongs = Playlist.builder()
                .name("Liked Songs")
                .owner(user)
                .isSystem(true)
                .visibility(PlaylistVisibility.PRIVATE)
                .systemType(SystemPlaylistType.LIKED_SONGS)
                .build();
        playlistRepository.save(likedSongs);
    }
}
