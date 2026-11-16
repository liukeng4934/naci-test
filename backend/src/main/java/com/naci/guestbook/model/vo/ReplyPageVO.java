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
public class ReplyPageVO {

    private List<ReplyVO> items = List.of();

    private int pageSize;

    private long total;

    private boolean hasMore;

    /** 下一页游标；hasMore 为 false 时为 null。 */
    private String nextCursor;
}
