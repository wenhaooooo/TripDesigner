package com.tripdesigner.trip.domain;
/**
 * 目的地仓储接口。
 */

import java.util.List;
import java.util.Optional;

public interface DestinationRepository {
    Destination save(Destination destination);
    Optional<Destination> findById(Long id);
    List<Destination> findByCountry(String country);
    List<Destination> findByCategory(String category);
    void deleteById(Long id);
}
