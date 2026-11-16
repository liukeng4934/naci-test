package com.naci.guestbook.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MessageQuery {

    public static final String SORT_NEWEST = "createdAt_desc";
    public static final String SORT_OLDEST = "createdAt_asc";

    @Pattern(regexp = "createdAt_(desc|asc)", message = "排序方式不合法")
    private String sort = SORT_NEWEST;

    @Min(value = 1, message = "页码必须大于 0")
    private int page = 1;

    @Min(value = 1, message = "每页条数必须大于 0")
    @Max(value = 50, message = "每页最多查询 50 条")
    private int pageSize = 8;

    @Size(max = 100, message = "搜索关键词不能超过 100 个字符")
    private String q = "";
}
