package com.naci.guestbook.model.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReplyWithChildCount extends Reply {

    private Long childReplyCount;
}
