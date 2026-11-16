package com.naci.guestbook.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;

    private String username;

    @JsonIgnore
    private String passwordHash;

    private String displayName;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
