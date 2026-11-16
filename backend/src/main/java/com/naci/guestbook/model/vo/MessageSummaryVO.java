package com.naci.guestbook.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageSummaryVO {

    private long messageCount;

    private long replyCount;

    private long todayCount;
}
