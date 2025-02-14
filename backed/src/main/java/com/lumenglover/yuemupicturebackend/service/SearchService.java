package com.lumenglover.yuemupicturebackend.service;

import com.lumenglover.yuemupicturebackend.model.dto.search.SearchRequest;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SearchService {
    List<String> getHotSearchKeywords(String type, Integer size);

    /**
     * 统一搜索接口
     * @param searchRequest
     * @return
     */
    Page<?> doSearch(SearchRequest searchRequest);
}
