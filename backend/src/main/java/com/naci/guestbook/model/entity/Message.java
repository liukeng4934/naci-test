package com.naci.guestbook.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    private Long id;

    private Long userId;

    private String userName;

    private String content;

    private Long likeCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
