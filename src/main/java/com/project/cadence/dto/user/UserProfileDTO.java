package com.project.cadence.dto.user;

import com.project.cadence.dto.artist.ArtistPreviewDTO;
import com.project.cadence.dto.playlist.PlaylistPreviewDTO;

import java.util.List;

public record UserProfileDTO(
        String id,
        String name,
        String email,
        String profileUrl,
        List<PlaylistPreviewDTO> createdPlaylistsPreview,
        List<PlaylistPreviewDTO> likedPlaylistsPreview,
        List<ArtistPreviewDTO> artistFollowing,
        boolean emailVerified,
        boolean isOwner
) {
}
