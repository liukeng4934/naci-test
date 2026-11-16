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
public class Reply {

    private Long id;

    private Long messageId;

    private Long parentId;

    private Long userId;

    private String userName;

    private String content;

    private Long likeCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
