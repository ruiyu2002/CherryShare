package com.chenry.cherrysharebackend.model.dto.spaceuser;

import com.chenry.cherrysharebackend.common.PageRequest;
import lombok.Data;

import java.io.Serializable;

/**
 * 空间用户查询请求
 */
@Data
public class SpaceUserQueryRequest extends PageRequest implements Serializable {

    /**
     * ID
     */
    private Long id;

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    /**
     * 审核状态：0-待审核 1-已通过 2-已拒绝
     */
    private Integer status;

    private static final long serialVersionUID = 1L;
}
