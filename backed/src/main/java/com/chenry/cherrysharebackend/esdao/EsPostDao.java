package com.chenry.cherrysharebackend.esdao;

import com.chenry.cherrysharebackend.model.entity.es.EsPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EsPostDao extends ElasticsearchRepository<EsPost, Long> {
}
