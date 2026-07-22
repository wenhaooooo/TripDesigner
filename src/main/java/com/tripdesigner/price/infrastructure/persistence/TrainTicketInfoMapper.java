package com.tripdesigner.price.infrastructure.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TrainTicketInfoMapper extends BaseMapper<TrainTicketInfoPO> {

    @Select("SELECT * FROM train_ticket_info WHERE departure = #{departure} AND destination = #{destination} ORDER BY departure_time")
    List<TrainTicketInfoPO> selectByRoute(@Param("departure") String departure, @Param("destination") String destination);

    @Select("SELECT * FROM train_ticket_info WHERE departure = #{departure} AND destination = #{destination} AND ticket_class = #{ticketClass} ORDER BY departure_time")
    List<TrainTicketInfoPO> selectByRouteAndClass(@Param("departure") String departure, @Param("destination") String destination, @Param("ticketClass") String ticketClass);

    @Select("SELECT * FROM train_ticket_info WHERE train_number = #{trainNumber} ORDER BY ticket_class")
    List<TrainTicketInfoPO> selectByTrainNumber(@Param("trainNumber") String trainNumber);
}