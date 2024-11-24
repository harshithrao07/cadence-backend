package com.project.musicplayer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "InvalidatedToken")
public class InvalidatedToken {

    @Id
    @Indexed
    private String email;

    private long invalidatedTokenTime;
}
