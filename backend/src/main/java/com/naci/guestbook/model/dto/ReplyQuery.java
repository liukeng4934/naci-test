package com.naci.guestbook.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReplyQuery {

    @Pattern(regexp = "(?i)null|\\d{1,19}", message = "parentReplyId 必须是 null 或数字 ID")
    private String parentReplyId;

    /**
     * 游标，格式为「上一页最后一条回复的创建时间毫秒数_回复 ID」。
     * 为空时查询第一页；不为空时查询严格早于该游标的回复。
     */
    @Pattern(regexp = "\\d{1,19}_\\d{1,19}", message = "cursor 格式不合法")
    private String cursor;

    @Min(value = 1, message = "每页条数必须大于 0")
    @Max(value = 50, message = "每页最多查询 50 条")
    private int pageSize = 8;

    /**
     * 将查询字符串解析为父回复 ID。
     *
     * @return 父回复 ID；参数为空或等于字符串 null 时返回 null
     * @throws NumberFormatException 参数不是合法长整数时抛出
     */
    public Long resolveParentReplyId() {
        if (parentReplyId == null || parentReplyId.isBlank() || "null".equalsIgnoreCase(parentReplyId)) {
            return null;
        }
        return Long.valueOf(parentReplyId);
    }
}
