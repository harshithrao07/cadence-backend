package com.project.cadence.consumers;

import com.project.cadence.dto.Topics;
import com.project.cadence.events.StreamSongEvent;
import com.project.cadence.model.*;
import com.project.cadence.repository.PlayHistoryRepository;
import com.project.cadence.repository.SongRepository;
import com.project.cadence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class StreamSongConsumer {

    private final PlayHistoryRepository playHistoryRepository;
    private final SongRepository songRepository;
    private final UserRepository userRepository;

    @KafkaListener(topics = Topics.STREAM_SONG_TOPIC, groupId = "cadence-group")
    public void handlePlayHistory(StreamSongEvent event) {
        String userId = event.getUserId();
        String songId = event.getSongId();

        Song song = songRepository.findById(songId).orElseThrow(() -> new RuntimeException("Song not found"));

        Optional<PlayHistory> existing = playHistoryRepository.findByUserIdAndSongId(userId, song.getId());

        PlayHistory playHistory;
        if (existing.isPresent()) {
            playHistory = existing.get();
            playHistory.setPlayCount(playHistory.getPlayCount() + 1);
        } else {
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));

            playHistory = PlayHistory.builder()
                    .id(new PlayHistoryId(userId, song.getId()))
                    .user(user)
                    .song(song)
                    .playCount(1)
                    .build();
        }
        playHistoryRepository.save(playHistory);
    }
}
