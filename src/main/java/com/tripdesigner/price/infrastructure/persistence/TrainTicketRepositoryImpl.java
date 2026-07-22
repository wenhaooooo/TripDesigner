package com.tripdesigner.price.infrastructure.persistence;

import com.tripdesigner.price.domain.TicketClass;
import com.tripdesigner.price.domain.TrainTicketInfo;
import com.tripdesigner.price.domain.TrainTicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TrainTicketRepositoryImpl implements TrainTicketRepository {

    private final TrainTicketInfoMapper mapper;

    @Override
    public List<TrainTicketInfo> findByRoute(String departure, String destination) {
        return mapper.selectByRoute(departure, destination)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TrainTicketInfo> findByRouteAndClass(String departure, String destination, TicketClass ticketClass) {
        return mapper.selectByRouteAndClass(departure, destination, ticketClass.name())
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<TrainTicketInfo> findByTrainNumber(String trainNumber) {
        return mapper.selectByTrainNumber(trainNumber)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public TrainTicketInfo findById(Long id) {
        TrainTicketInfoPO po = mapper.selectById(id);
        return po != null ? toDomain(po) : null;
    }

    @Override
    public TrainTicketInfo save(TrainTicketInfo info) {
        TrainTicketInfoPO po = toPO(info);
        if (info.getId() == null) {
            mapper.insert(po);
        } else {
            mapper.updateById(po);
        }
        return toDomain(po);
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private TrainTicketInfo toDomain(TrainTicketInfoPO po) {
        return new TrainTicketInfo(
                po.getId(),
                po.getDeparture(),
                po.getDestination(),
                po.getTrainNumber(),
                TicketClass.fromValue(po.getTicketClass()),
                po.getPrice(),
                po.getDepartureTime(),
                po.getArrivalTime(),
                po.getDurationMinutes(),
                po.getTrainType()
        );
    }

    private TrainTicketInfoPO toPO(TrainTicketInfo info) {
        TrainTicketInfoPO po = new TrainTicketInfoPO();
        po.setId(info.getId());
        po.setDeparture(info.getDeparture());
        po.setDestination(info.getDestination());
        po.setTrainNumber(info.getTrainNumber());
        po.setTicketClass(info.getTicketClass().name());
        po.setPrice(info.getPrice());
        po.setDepartureTime(info.getDepartureTime());
        po.setArrivalTime(info.getArrivalTime());
        po.setDurationMinutes(info.getDurationMinutes());
        po.setTrainType(info.getTrainType());
        return po;
    }
}