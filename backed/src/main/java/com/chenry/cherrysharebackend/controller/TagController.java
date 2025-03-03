package com.chenry.cherrysharebackend.controller;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chenry.cherrysharebackend.annotation.AuthCheck;
import com.chenry.cherrysharebackend.common.BaseResponse;
import com.chenry.cherrysharebackend.common.PageRequest;
import com.chenry.cherrysharebackend.common.ResultUtils;
import com.chenry.cherrysharebackend.constant.UserConstant;
import com.chenry.cherrysharebackend.exception.ErrorCode;
import com.chenry.cherrysharebackend.exception.ThrowUtils;
import com.chenry.cherrysharebackend.model.entity.Tag;
import com.chenry.cherrysharebackend.model.vo.TagVO;
import com.chenry.cherrysharebackend.service.TagService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/tag")
public class TagController {
    @Resource
    private TagService tagService;
    /**
     * 获取所有标签
     */
    @PostMapping("list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<TagVO>> listTagVOByPage(@RequestBody PageRequest pageRequest){
        long current = pageRequest.getCurrent();
        long pageSize = pageRequest.getPageSize();
        Page<Tag> tagPage = tagService.page(new Page<>(current, pageSize));
        Page<TagVO> tagVOPage = new Page<>(current, pageSize,tagPage.getTotal());
        List<TagVO> tagVOList = tagService.listTagVOByPage(tagPage.getRecords());
        tagVOPage.setRecords(tagVOList);
        return ResultUtils.success(tagVOPage);
    }

    /**
     * 添加标签
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> addTag(String tagName){
        ThrowUtils.throwIf(tagName == null || tagName.length() == 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(tagService.addTag(tagName));
    }

    /**
     * 删除标签
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteTag(Long id){
        ThrowUtils.throwIf(id == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(tagService.deleteTag(id));
    }

    /**
     * 查找标签
     */
    @PostMapping("/search")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<TagVO>> searchTag(String tagName){
        ThrowUtils.throwIf(tagName == null || tagName.length() == 0, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(tagService.searchTag(tagName));
    }

}
