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
public class CreateMessageRequest {

    @NotBlank(message = "留言内容不能为空")
    @Size(max = 500, message = "留言内容不能超过 500 个字符")
    private String content;
}
