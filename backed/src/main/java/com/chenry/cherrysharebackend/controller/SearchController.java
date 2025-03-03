package com.chenry.cherrysharebackend.controller;

import com.chenry.cherrysharebackend.common.BaseResponse;
import com.chenry.cherrysharebackend.common.ResultUtils;
import com.chenry.cherrysharebackend.exception.BusinessException;
import com.chenry.cherrysharebackend.exception.ErrorCode;
import com.chenry.cherrysharebackend.model.dto.search.SearchRequest;
import com.chenry.cherrysharebackend.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping("/search")
@Slf4j
public class SearchController {

    @Resource
    private SearchService searchService;

    @PostMapping("/all")
    public BaseResponse<Page<?>> searchAll(@RequestBody SearchRequest searchRequest) {
        return ResultUtils.success(searchService.doSearch(searchRequest));
    }

    /**
     * 获取热门搜索关键词
     * @param type 搜索类型 (picture/user/post/space)
     * @param size 返回数量，默认9个
     * @return 热门搜索关键词列表
     */
    @GetMapping("/hot")
    public BaseResponse<List<String>> getHotSearchKeywords(
            @RequestParam(required = true) String type,
            @RequestParam(required = false, defaultValue = "9") Integer size) {
        // 参数校验
        if (StringUtils.isBlank(type)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "搜索类型不能为空");
        }
        if (size <= 0 || size > 100) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "size必须在1-100之间");
        }

        // 校验搜索类型
        if (!type.matches("^(picture|user|post|space)$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的搜索类型");
        }

        return ResultUtils.success(searchService.getHotSearchKeywords(type, size));
    }
}
