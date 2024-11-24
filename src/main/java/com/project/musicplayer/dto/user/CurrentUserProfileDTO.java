package com.project.musicplayer.dto.user;

import com.project.musicplayer.dto.artist.ArtistPreviewDTO;
import com.project.musicplayer.dto.playlist.PlaylistPreviewDTO;
import com.project.musicplayer.dto.song.TrackPreviewDTO;

import java.util.Set;

public record CurrentUserProfileDTO(
        String id,
        String name,
        String email,
        String profileUrl,
        Set<PlaylistPreviewDTO> createdPlaylistsPreview,
        Set<PlaylistPreviewDTO> likedPlaylistsPreview,
        Set<TrackPreviewDTO> likedSongs,
        Set<FriendsPreviewDTO> userFollowers,
        Set<FriendsPreviewDTO> userFollowing,
        Set<ArtistPreviewDTO> artistFollowing
) {
}
