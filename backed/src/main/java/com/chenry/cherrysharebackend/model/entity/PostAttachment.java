package com.chenry.cherrysharebackend.model.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("post_attachment")
public class PostAttachment implements Serializable {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long postId;

    private Integer type; // 1-图片 2-文件

    private String url;

    private String name;

    private Long size;

    // 在文章中的位置，可以是字符位置或者段落位置
    private Integer position;

    // 在文章中的标识符，例如 {img-1}, {img-2}
    private String marker;

    private Integer sort;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;
}
