package com.naci.guestbook.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateReplyRequest {

    private Long parentReplyId;

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 300, message = "回复内容不能超过 300 个字符")
    private String content;
}
