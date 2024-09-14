package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Component
@Slf4j
public class MyTask {
    @Autowired
    private OrderMapper orderMapper;
//    @Scheduled(cron = "0/5 * * * * ?")
    public void execute(){
        log.info("{}",new Date());
    }


    @Scheduled(cron = "0 * * * * ?")
    public void OverTimeLT(){
        log.info("超时未支付");
        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(-15);
        //查询超时15分钟的订单
        List<Orders> orders = orderMapper.getOverTimeOrderByStausAndTime(Orders.PENDING_PAYMENT, localDateTime);

        //将超时待支付状态的订单状态改为已取消
        if(orders != null && orders.size() > 0){
            for (Orders order: orders){
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void processDeliveryOrder(){
        log.info("定时处理派送中的订单");
        LocalDateTime localDateTime = LocalDateTime.now().plusMinutes(-60);
        List<Orders> orders = orderMapper.getOverTimeOrderByStausAndTime(Orders.PENDING_PAYMENT, localDateTime);

        //将超时待支付状态的订单状态改为已取消
        if(orders != null && orders.size() > 0){
            for (Orders order: orders){
                order.setStatus(Orders.COMPLETED);
                orderMapper.update(order);
            }
        }
    }
}
