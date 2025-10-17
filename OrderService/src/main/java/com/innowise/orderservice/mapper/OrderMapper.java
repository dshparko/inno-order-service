package com.innowise.orderservice.mapper;

import com.innowise.orderservice.model.dto.CreateOrderItemDto;
import com.innowise.orderservice.model.dto.OrderDto;
import com.innowise.orderservice.model.entity.Order;
import com.innowise.orderservice.model.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderDto map(Order order);

    @Mapping(source = "itemId", target = "item.id")
    OrderItem map(CreateOrderItemDto dto);

    @Mapping(source = "item.id", target = "itemId")
    CreateOrderItemDto mapToCreateDto(OrderItem orderItem);

    Order map(OrderDto dto);

}
