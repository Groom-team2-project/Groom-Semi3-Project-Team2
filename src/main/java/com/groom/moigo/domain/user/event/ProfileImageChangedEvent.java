package com.groom.moigo.domain.user.event;

public record ProfileImageChangedEvent(
        String newKey,
        String prevKey
) {
}
