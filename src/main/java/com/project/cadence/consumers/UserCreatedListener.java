package com.project.cadence.consumers;

import com.project.cadence.dto.Topics;
import com.project.cadence.events.UserCreatedEvent;
import com.project.cadence.model.Playlist;
import com.project.cadence.model.PlaylistVisibility;
import com.project.cadence.model.SystemPlaylistType;
import com.project.cadence.model.User;
import com.project.cadence.repository.PlaylistRepository;
import com.project.cadence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCreatedListener {

    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;

    @KafkaListener(topics = Topics.USER_CREATED_TOPIC, groupId = "cadence-group")
    public void handleUserCreated(UserCreatedEvent event) {
        User user = userRepository.findById(event.getUserId())
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
