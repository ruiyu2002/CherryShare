package com.chenry.cherrysharebackend.manager.websocket;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.chenry.cherrysharebackend.model.entity.User;
import com.chenry.cherrysharebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * WebSocket 拦截器，建立连接前要先校验
 */
@Slf4j
@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Resource
    private UserService userService;

    /**
     * 建立连接前要先校验
     *
     * @param request
     * @param response
     * @param wsHandler
     * @param attributes 给 WebSocketSession 会话设置属性
     * @return
     * @throws Exception
     */
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (!(request instanceof ServletServerHttpRequest)) {
            return false;
        }

        HttpServletRequest httpRequest = ((ServletServerHttpRequest) request).getServletRequest();

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(httpRequest);
        if (ObjUtil.isEmpty(loginUser)) {
            log.error("用户未登录，拒绝握手");
            return false;
        }

        // 获取参数
        String pictureIdStr = httpRequest.getParameter("pictureId");
        String spaceIdStr = httpRequest.getParameter("spaceId");
        String privateChatId=httpRequest.getParameter("privateChatId");

        // 设置用户信息
        attributes.put("user", loginUser);

        // 设置pictureId（如果有）
        if (StrUtil.isNotBlank(pictureIdStr)) {
            try {
                Long pictureId = Long.parseLong(pictureIdStr);
                attributes.put("pictureId", pictureId);
            } catch (NumberFormatException e) {
                log.error("pictureId格式错误，拒绝握手");
                return false;
            }
        }

        // 设置spaceId（如果有）
        if (StrUtil.isNotBlank(spaceIdStr)) {
            try {
                Long spaceId = Long.parseLong(spaceIdStr);
                attributes.put("spaceId", spaceId);
            } catch (NumberFormatException e) {
                log.error("spaceId格式错误，拒绝握手");
                return false;
            }
        }

        // 设置privateChatId（如果有）
        if (StrUtil.isNotBlank(privateChatId)) {
            try {
                Long privateChatIdLong = Long.parseLong(privateChatId);
                attributes.put("privateChatId", privateChatIdLong);
            }
            catch (NumberFormatException e) {
                log.error("privateChatId格式错误，拒绝握手");
                return false;
            }
        }

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
