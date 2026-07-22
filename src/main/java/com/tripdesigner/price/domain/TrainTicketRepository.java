package com.tripdesigner.price.domain;

import java.util.List;

public interface TrainTicketRepository {

    List<TrainTicketInfo> findByRoute(String departure, String destination);

    List<TrainTicketInfo> findByRouteAndClass(String departure, String destination, TicketClass ticketClass);

    List<TrainTicketInfo> findByTrainNumber(String trainNumber);

    TrainTicketInfo findById(Long id);

    TrainTicketInfo save(TrainTicketInfo info);

    void deleteById(Long id);
}