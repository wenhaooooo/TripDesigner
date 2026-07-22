package com.tripdesigner.knowledge.resolver;

import com.tripdesigner.knowledge.domain.City;
import com.tripdesigner.knowledge.domain.Country;
import com.tripdesigner.knowledge.domain.repository.CityRepository;
import com.tripdesigner.knowledge.domain.repository.CountryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 默认实体解析器实现。
 *
 * <p>将爬虫获取的文本名称解析为系统内部的实体 ID。
 * 使用 {@link CityRepository} 和 {@link CountryRepository} 进行查找。
 *
 * <p>解析规则：
 * <ul>
 *   <li>模糊匹配：大小写不敏感、自动 trim</li>
 *   <li>仅查找不创建——实体创建由 AppService 层负责</li>
 *   <li>未找到时返回 {@code null}</li>
 * </ul>
 *
 * <p>解析优先级：
 * <ol>
 *   <li>{@link #resolveCountry} — 按名称查找国家，取首个匹配结果</li>
 *   <li>{@link #resolveCity} — 先解析国家 ID，再按城市名+国家 ID 精确查找；失败则按城市名模糊查找</li>
 *   <li>{@link #resolvePoi} — 当前 POI 仓储不支持按名称查找，直接返回 null</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultEntityResolver implements EntityResolver {

    private final CityRepository cityRepository;
    private final CountryRepository countryRepository;

    @Override
    public Long resolveCity(String cityName, String countryName) {
        if (cityName == null || cityName.isBlank()) {
            return null;
        }

        String normalizedCity = cityName.trim();

        try {
            // 优先解析国家 ID，用于精确查找城市
            Long countryId = null;
            if (countryName != null && !countryName.isBlank()) {
                countryId = resolveCountry(countryName);
            }

            // 按城市名 + 国家 ID 精确查找
            if (countryId != null) {
                Optional<City> exact = cityRepository.findByNameAndCountryId(normalizedCity, countryId);
                if (exact.isPresent()) {
                    log.debug("[DefaultEntityResolver] Resolved city: {} ({}) -> {}",
                            cityName, countryName, exact.get().getId());
                    return exact.get().getId();
                }
            }

            // 回退到按城市名模糊查找（大小写不敏感）
            List<City> candidates = cityRepository.findByName(normalizedCity);
            if (!candidates.isEmpty()) {
                // 尝试大小写不敏感匹配
                Optional<City> caseInsensitive = candidates.stream()
                        .filter(c -> c.getName() != null
                                && c.getName().equalsIgnoreCase(normalizedCity))
                        .findFirst();
                City matched = caseInsensitive.orElse(candidates.get(0));
                log.debug("[DefaultEntityResolver] Resolved city (fuzzy): {} -> {}", cityName, matched.getId());
                return matched.getId();
            }

            log.debug("[DefaultEntityResolver] City not found: {} ({})", cityName, countryName);
            return null;
        } catch (Exception e) {
            log.warn("[DefaultEntityResolver] Failed to resolve city '{}': {}", cityName, e.getMessage());
            return null;
        }
    }

    @Override
    public Long resolveCountry(String countryName) {
        if (countryName == null || countryName.isBlank()) {
            return null;
        }

        String normalized = countryName.trim();

        try {
            List<Country> candidates = countryRepository.findByName(normalized);
            if (!candidates.isEmpty()) {
                // 尝试大小写不敏感匹配
                Optional<Country> caseInsensitive = candidates.stream()
                        .filter(c -> c.getName() != null
                                && c.getName().equalsIgnoreCase(normalized))
                        .findFirst();
                Country matched = caseInsensitive.orElse(candidates.get(0));
                log.debug("[DefaultEntityResolver] Resolved country: {} -> {}", countryName, matched.getId());
                return matched.getId();
            }

            log.debug("[DefaultEntityResolver] Country not found: {}", countryName);
            return null;
        } catch (Exception e) {
            log.warn("[DefaultEntityResolver] Failed to resolve country '{}': {}", countryName, e.getMessage());
            return null;
        }
    }

    @Override
    public Long resolvePoi(String poiName, Long cityId) {
        // 当前 PoiRepository 不支持按名称查找，POI 解析由 AppService 层负责
        log.debug("[DefaultEntityResolver] POI resolution not supported: name={}, cityId={}", poiName, cityId);
        return null;
    }
}
