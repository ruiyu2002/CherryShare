package com.chenry.cherrysharebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenry.cherrysharebackend.model.entity.PostAttachment;
import com.chenry.cherrysharebackend.service.PostAttachmentService;
import com.chenry.cherrysharebackend.mapper.PostAttachmentMapper;
import org.springframework.stereotype.Service;

/**
* @author 鹿梦
* @description 针对表【post_attachment(帖子附件表)】的数据库操作Service实现
* @createDate 2025-02-05 11:08:53
*/
@Service
public class PostAttachmentServiceImpl extends ServiceImpl<PostAttachmentMapper, PostAttachment>
    implements PostAttachmentService{

}




