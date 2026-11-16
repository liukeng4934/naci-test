package com.naci.guestbook.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessagePageVO {

    private List<MessageVO> items = List.of();

    private int page;

    private int pageSize;

    private long total;

    private boolean hasMore;

    private MessageSummaryVO summary;
}
