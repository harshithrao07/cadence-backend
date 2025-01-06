package com.project.cadence.dto.user;

import com.project.cadence.dto.artist.ArtistPreviewDTO;
import com.project.cadence.dto.genre.GenrePreviewDTO;
import com.project.cadence.dto.playlist.PlaylistPreviewDTO;
import com.project.cadence.dto.song.TrackPreviewDTO;

import java.util.Set;

public record UserProfileDTO(
        String id,
        String name,
        String email,
        String profileUrl,
        Set<GenrePreviewDTO> genrePreferences,
        Set<PlaylistPreviewDTO> createdPlaylistsPreview,
        Set<PlaylistPreviewDTO> likedPlaylistsPreview,
        Set<TrackPreviewDTO> likedSongs,
        Set<UserPreviewDTO> userFollowers,
        Set<UserPreviewDTO> userFollowing,
        Set<ArtistPreviewDTO> artistFollowing
) {
}
