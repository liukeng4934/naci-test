package com.naci.guestbook.controller;

import com.naci.guestbook.common.response.ApiResponse;
import com.naci.guestbook.common.security.AuthenticatedUser;
import com.naci.guestbook.model.dto.CreateMessageRequest;
import com.naci.guestbook.model.dto.MessageQuery;
import com.naci.guestbook.model.vo.DeleteResultVO;
import com.naci.guestbook.model.vo.MessagePageVO;
import com.naci.guestbook.model.vo.MessageVO;
import com.naci.guestbook.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    /**
     * 创建留言控制器。
     *
     * @param messageService 留言业务服务
     */
    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 分页查询留言，支持按创建时间排序和关键字搜索。
     *
     * @param query 分页、排序和搜索条件
     * @return 留言分页结果及留言板汇总统计
     */
    @GetMapping
    public ApiResponse<MessagePageVO> listMessages(@Valid @ModelAttribute MessageQuery query) {
        return ApiResponse.success(messageService.listMessages(query));
    }

    /**
     * 当前登录用户发布留言。
     *
     * @param currentUser 由 JWT 认证过滤器注入的当前用户
     * @param request 留言内容
     * @return 新创建的留言
     */
    @PostMapping
    public ApiResponse<MessageVO> createMessage(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CreateMessageRequest request
    ) {
        return ApiResponse.success(messageService.createMessage(
                currentUser.id(),
                currentUser.displayName(),
                request
        ));
    }

    /**
     * 当前登录用户删除自己发布的留言及其全部回复。
     *
     * @param messageId 留言 ID
     * @param currentUser 由 JWT 认证过滤器注入的当前用户
     * @return 被删除的留言 ID 及回复数量
     */
    @DeleteMapping("/{messageId}")
    public ApiResponse<DeleteResultVO> deleteMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(messageService.deleteMessage(messageId, currentUser.id()));
    }
}
