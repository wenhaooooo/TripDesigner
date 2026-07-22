package com.tripdesigner.knowledge.domain.repository;

import com.tripdesigner.knowledge.domain.Country;

import java.util.List;
import java.util.Optional;

/**
 * 国家仓储接口。
 *
 * 定义国家知识实体的持久化操作。
 * 实现在 infrastructure 层（CountryRepositoryImpl），使用 MyBatis Plus。
 */
public interface CountryRepository {

    /** 按主键查询 */
    Optional<Country> findById(Long id);

    /** 按 ISO alpha-2 代码查询 */
    Optional<Country> findByIsoCode2(String code);

    /** 按名称模糊查询 */
    List<Country> findByName(String name);

    /** 查询全部 */
    List<Country> findAll();

    /** 保存（新增或更新） */
    Country save(Country country);

    /** 按数据来源与源 ID 查询，用于增量同步去重 */
    Optional<Country> findBySource(String source, String sourceId);
}
