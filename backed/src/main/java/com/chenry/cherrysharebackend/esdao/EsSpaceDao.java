package com.chenry.cherrysharebackend.esdao;

import com.chenry.cherrysharebackend.model.entity.es.EsSpace;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface EsSpaceDao extends ElasticsearchRepository<EsSpace, Long> {
}
