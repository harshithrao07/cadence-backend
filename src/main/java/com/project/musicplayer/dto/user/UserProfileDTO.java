package com.project.musicplayer.dto.user;

import com.project.musicplayer.dto.artist.ArtistPreviewDTO;
import com.project.musicplayer.dto.genre.GenrePreviewDTO;
import com.project.musicplayer.dto.playlist.PlaylistPreviewDTO;
import com.project.musicplayer.dto.song.TrackPreviewDTO;

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
