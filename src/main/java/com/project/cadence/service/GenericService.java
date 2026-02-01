package com.project.cadence.service;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.artist.ArtistPreviewDTO;
import com.project.cadence.dto.generic.DiscoverDTO;
import com.project.cadence.dto.generic.GlobalSearchDTO;
import com.project.cadence.dto.genre.GenrePreviewDTO;
import com.project.cadence.dto.record.RecordPreviewDTO;
import com.project.cadence.dto.record.RecordPreviewWithCoverImageDTO;
import com.project.cadence.dto.song.EachSongDTO;
import com.project.cadence.dto.song.SongBaseDTO;
import com.project.cadence.model.Artist;
import com.project.cadence.model.User;
import com.project.cadence.repository.ArtistRepository;
import com.project.cadence.repository.RecordRepository;
import com.project.cadence.repository.SongRepository;
import com.project.cadence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenericService {
    private final ArtistService artistService;
    private final RecordService recordService;
    private final SongService songService;
    private final PlaylistService playlistService;
    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final RecordRepository recordRepository;
    private final UserRepository userRepository;

    public ResponseEntity<ApiResponseDTO<GlobalSearchDTO>> getSearchResponse(int page, int size, String key) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            GlobalSearchDTO globalSearchDTO = new GlobalSearchDTO(
                    artistService.getArtistsForSearch(pageable, key),
                    recordService.getRecordsForSearch(pageable, key),
                    songService.getRecordsForSearch(pageable, key),
                    playlistService.getPlaylistsForSearch(pageable, key)
            );
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Successfully retrieved data", globalSearchDTO));
        } catch (Exception e) {
            log.error("An exception has occurred", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<DiscoverDTO>> getDiscoveryFeed(String email) {
        try {
            User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User cannot be found"));
            Instant lastWeek = Instant.now().minus(7, ChronoUnit.DAYS);

            // Trending songs from last 7 days
            List<SongBaseDTO> trendingBase = songRepository.findTrendingSongs(lastWeek, PageRequest.of(0, 20));
            List<EachSongDTO> trendingSongs = enrichSongs(trendingBase);

            // Popular Artists
            List<ArtistPreviewDTO> popularArtists = artistRepository.findPopularArtists(PageRequest.of(0, 10));

            // New Releases from all Artists
            List<RecordPreviewDTO> newReleases = recordRepository.findNewReleases(PageRequest.of(0, 12))
                    .stream()
                    .map(record -> new RecordPreviewDTO(
                            record.getId(),
                            record.getTitle(),
                            record.getReleaseTimestamp(),
                            record.getCoverUrl(),
                            record.getRecordType(),
                            record.getArtists().stream()
                                    .map(artist -> new ArtistPreviewDTO(
                                            artist.getId(),
                                            artist.getName(),
                                            artist.getProfileUrl()
                                    ))
                                    .toList()
                    ))
                    .toList();

            // New Releases from Followed Artists
            List<String> followedArtistIds = user.getArtistFollowing().stream().map(Artist::getId).toList();
            List<RecordPreviewDTO> newReleasesOfFollowingArtists = List.of();
            if (!followedArtistIds.isEmpty()) {
                newReleasesOfFollowingArtists =
                        recordRepository.findNewReleasesFromFollowedArtists(
                                        followedArtistIds,
                                        PageRequest.of(0, 12)
                                ).stream()
                                .map(record -> new RecordPreviewDTO(
                                        record.getId(),
                                        record.getTitle(),
                                        record.getReleaseTimestamp(),
                                        record.getCoverUrl(),
                                        record.getRecordType(),
                                        record.getArtists().stream()
                                                .map(artist -> new ArtistPreviewDTO(
                                                        artist.getId(),
                                                        artist.getName(),
                                                        artist.getProfileUrl()
                                                ))
                                                .toList()
                                ))
                                .toList();
            }

            // Recommended Songs and Suggested Artists based on genre likeness of user
            List<EachSongDTO> recommendedSongs;
            List<ArtistPreviewDTO> suggestedArtists;

            List<String> topGenres = songRepository.findTopGenresForUser(
                    user.getId(),
                    PageRequest.of(0, 3)
            );

            if (topGenres.isEmpty()) {
                recommendedSongs = List.of();
                suggestedArtists = List.of();
            } else {
                List<SongBaseDTO> recommendedBase =
                        songRepository.findRecommendedFromGenres(
                                user.getId(),
                                topGenres,
                                PageRequest.of(0, 20)
                        );

                recommendedSongs = enrichSongs(recommendedBase);
                suggestedArtists = artistRepository.findSuggestedArtists(
                        topGenres,
                        followedArtistIds.isEmpty() ? List.of("-1") : followedArtistIds,
                        PageRequest.of(0, 10));
            }

            // Recently played songs of the user
            List<SongBaseDTO> recentBase = songRepository.findRecentlyPlayedSongs(
                    user.getId(),
                    PageRequest.of(0, 15)
            );
            List<EachSongDTO> recentlyPlayed = enrichSongs(recentBase);


            DiscoverDTO discoverDTO = new DiscoverDTO(
                    trendingSongs,
                    popularArtists,
                    recommendedSongs,
                    newReleases,
                    newReleasesOfFollowingArtists,
                    recentlyPlayed,
                    suggestedArtists
            );

            return ResponseEntity.ok(
                    new ApiResponseDTO<>(true, "Successfully retrieved data", discoverDTO)
            );
        } catch (Exception e) {
            log.error("An exception has occurred", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    private List<EachSongDTO> enrichSongs(List<SongBaseDTO> baseSongs) {

        if (baseSongs.isEmpty()) return List.of();

        List<String> songIds = baseSongs.stream()
                .map(SongBaseDTO::id)
                .toList();

        List<Object[]> artistRows =
                songRepository.findArtistsForSongs(songIds);

        Map<String, List<ArtistPreviewDTO>> artistMap = new HashMap<>();

        for (Object[] row : artistRows) {
            String songId = (String) row[0];
            ArtistPreviewDTO artist = (ArtistPreviewDTO) row[1];

            artistMap
                    .computeIfAbsent(songId, k -> new ArrayList<>())
                    .add(artist);
        }

        List<Object[]> genreRows =
                songRepository.findGenresForSongs(songIds);

        Map<String, List<GenrePreviewDTO>> genreMap = new HashMap<>();

        for (Object[] row : genreRows) {
            String songId = (String) row[0];
            GenrePreviewDTO genre = (GenrePreviewDTO) row[1];

            genreMap
                    .computeIfAbsent(songId, k -> new ArrayList<>())
                    .add(genre);
        }

        return baseSongs.stream()
                .map(base -> new EachSongDTO(
                        base.id(),
                        base.title(),
                        base.totalDuration(),
                        artistMap.getOrDefault(base.id(), List.of()),
                        genreMap.getOrDefault(base.id(), List.of()),
                        new RecordPreviewWithCoverImageDTO(
                                base.recordId(),
                                base.recordTitle(),
                                base.coverUrl(),
                                0
                        )
                ))
                .toList();
    }
}

