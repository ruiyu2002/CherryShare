package com.lumenglover.yuemupicturebackend.model.dto.comments;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.lumenglover.yuemupicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 评论查询请求
 */
@Data
public class CommentsQueryRequest extends PageRequest implements Serializable {

    /**
     * 评论目标ID
     */
    private Long targetId;

    /**
     * 评论目标类型：1-图片 2-帖子，默认为1(图片)
     */
    private Integer targetType ;

    private static final long serialVersionUID = 1L;
}
