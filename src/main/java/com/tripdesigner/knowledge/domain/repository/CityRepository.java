package com.tripdesigner.knowledge.domain.repository;

import com.tripdesigner.knowledge.domain.City;

import java.util.List;
import java.util.Optional;

/**
 * 城市仓储接口。
 *
 * 定义城市知识实体的持久化操作。
 * 实现在 infrastructure 层（CityRepositoryImpl），使用 MyBatis Plus。
 */
public interface CityRepository {

    /** 按主键查询 */
    Optional<City> findById(Long id);

    /** 按国家 ID 查询城市列表 */
    List<City> findByCountryId(Long countryId);

    /** 按名称查询城市列表 */
    List<City> findByName(String name);

    /** 按名称与国家 ID 精确查询 */
    Optional<City> findByNameAndCountryId(String name, Long countryId);

    /** 保存（新增或更新） */
    City save(City city);

    /** 按数据来源与源 ID 查询 */
    Optional<City> findBySource(String source, String sourceId);
}
