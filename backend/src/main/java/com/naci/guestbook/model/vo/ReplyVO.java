package com.naci.guestbook.model.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReplyVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long messageId;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentReplyId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String userName;

    private String content;

    private Instant createdAt;

    private long childReplyCount;
}
