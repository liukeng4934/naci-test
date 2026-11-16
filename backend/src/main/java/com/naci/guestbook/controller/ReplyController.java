package com.naci.guestbook.controller;

import com.naci.guestbook.common.response.ApiResponse;
import com.naci.guestbook.common.security.AuthenticatedUser;
import com.naci.guestbook.model.dto.CreateReplyRequest;
import com.naci.guestbook.model.dto.ReplyQuery;
import com.naci.guestbook.model.vo.DeleteResultVO;
import com.naci.guestbook.model.vo.ReplyPageVO;
import com.naci.guestbook.model.vo.ReplyVO;
import com.naci.guestbook.service.ReplyService;
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
@RequestMapping("/api")
public class ReplyController {

    private final ReplyService replyService;

    /**
     * 创建回复控制器。
     *
     * @param replyService 回复业务服务
     */
    public ReplyController(ReplyService replyService) {
        this.replyService = replyService;
    }

    /**
     * 分页查询留言下指定层级的直接子回复。
     *
     * @param messageId 留言 ID
     * @param query 父回复 ID 和分页条件
     * @return 回复分页结果
     */
    @GetMapping("/messages/{messageId}/replies")
    public ApiResponse<ReplyPageVO> listReplies(
            @PathVariable Long messageId,
            @Valid @ModelAttribute ReplyQuery query
    ) {
        return ApiResponse.success(replyService.listReplies(messageId, query));
    }

    /**
     * 当前登录用户发布回复，支持通过父回复 ID 创建嵌套回复。
     *
     * @param messageId 留言 ID
     * @param currentUser 由 JWT 认证过滤器注入的当前用户
     * @param request 回复内容和父回复 ID
     * @return 新创建的回复
     */
    @PostMapping("/messages/{messageId}/replies")
    public ApiResponse<ReplyVO> createReply(
            @PathVariable Long messageId,
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody CreateReplyRequest request
    ) {
        return ApiResponse.success(replyService.createReply(
                messageId,
                currentUser.id(),
                currentUser.displayName(),
                request
        ));
    }

    /**
     * 当前登录用户软删除自己发布的回复及其完整子树。
     *
     * @param replyId 回复 ID
     * @param currentUser 由 JWT 认证过滤器注入的当前用户
     * @return 被删除的回复 ID 及本次实际标记的子树节点数量
     */
    @DeleteMapping("/replies/{replyId}")
    public ApiResponse<DeleteResultVO> deleteReply(
            @PathVariable Long replyId,
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ApiResponse.success(replyService.deleteReply(replyId, currentUser.id()));
    }
}
