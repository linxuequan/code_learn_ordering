package com.sky.service.impl;

import com.sky.dto.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 根据时间区间统计营业额
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnover(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);//日期计算，获得指定日期后1天的日期
            dateList.add(begin);
        }
        
       List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            Map map = new HashMap();
        	map.put("status", Orders.COMPLETED);
        	map.put("begin",beginTime);
        	map.put("end", endTime);
            Double turnover = orderMapper.sumByMap(map); 
            turnover = turnover == null ? 0.0 : turnover;
            turnoverList.add(turnover);
        }

        //数据封装
        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .turnoverList(StringUtils.join(turnoverList,","))
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        List<Integer> newUserList = new ArrayList<>(); //新增用户数
        List<Integer> totalUserList = new ArrayList<>(); //总用户数

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //新增用户数量 select count(id) from user where create_time > ? and create_time < ?
            Integer newUser = getUserCount(beginTime, endTime);
            //总用户数量 select count(id) from user where  create_time < ?
            Integer totalUser = getUserCount(null, endTime);

            newUserList.add(newUser);
            totalUserList.add(totalUser);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList,","))
                .newUserList(StringUtils.join(newUserList,","))
                .totalUserList(StringUtils.join(totalUserList,","))
                .build();
    }

    /**
     * 根据时间区间统计用户数量
     * @param beginTime
     * @param endTime
     * @return
     */
    private Integer getUserCount(LocalDateTime beginTime, LocalDateTime endTime) {
        Map map = new HashMap();
        map.put("begin",beginTime);
        map.put("end", endTime);
        return userMapper.countByMap(map);
    }

    /**
     * 根据时间区间统计订单数量
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end){
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);

        while (!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //每天订单总数集合
        List<Integer> orderCountList = new ArrayList<>();
        //每天有效订单数集合
        List<Integer> validOrderCountList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
            //查询每天的总订单数 select count(id) from orders where order_time > ? and order_time < ?
            Integer orderCount = getOrderCount(beginTime, endTime, null);

            //查询每天的有效订单数 select count(id) from orders where order_time > ? and order_time < ? and status = ?
            Integer validOrderCount = getOrderCount(beginTime, endTime, Orders.COMPLETED);

            orderCountList.add(orderCount);
            validOrderCountList.add(validOrderCount);
        }

        //时间区间内的总订单数
        Integer totalOrderCount = orderCountList.stream().reduce(Integer::sum).get();
        //时间区间内的总有效订单数
        Integer validOrderCount = validOrderCountList.stream().reduce(Integer::sum).get();
        //订单完成率
        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0){
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        }
        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(orderCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .build();

    }
    /**
     * 根据时间区间统计指定状态的订单数量
     * @param beginTime
     * @param endTime
     * @param status
     * @return
     */
    private Integer getOrderCount(LocalDateTime beginTime, LocalDateTime endTime, Integer status) {
        Map map = new HashMap();
        map.put("status", status);
        map.put("begin",beginTime);
        map.put("end", endTime);
        return orderMapper.countByMap(map);
    }

    /**
     * 查询指定时间区间内的销量排名top10
     * @param begin
     * @param end
     * @return
     * */
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end){
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(end, LocalTime.MAX);
        List<GoodsSalesDTO> goodsSalesDTOList = orderMapper.getSalesTop10(beginTime, endTime);

        String nameList = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getName).collect(Collectors.toList()),",");
        String numberList = StringUtils.join(goodsSalesDTOList.stream().map(GoodsSalesDTO::getNumber).collect(Collectors.toList()),",");

        return SalesTop10ReportVO.builder()
                .nameList(nameList)
                .numberList(numberList)
                .build();
    }

    /**
     * 导出近30天的运营数据报表
     * @param response
     **/
    @Override
    public void exportBusinessData(HttpServletResponse response) {
        //1.查询数据库，获取营业数据---查询最近30天的营业数据
        /* 最近30天：
         *     LocalDate.now()：获取当天时间日期
         *     LocalDate.now().minusDays(30)：获取相对今天来说往前倒30天的日期
         *     LocalDate.now().minusDays(1)：查到相对今天的前一天（昨天），为什么
         *          不查到今天呢？？？   因为今天有可能还没有结束，数据可能还会发生变动。
         * */
        LocalDate begin = LocalDate.now().minusDays(30);//从哪天开始查（30天之前）
        LocalDate end = LocalDate.now().minusDays(1);//查到那一天（昨天）

        //构建的开始结束时间是LocalDate（年月日类型），业务层需要的是LocalDateTime（年月日时分秒）类型，所以需要进行转化。
        LocalDateTime begins = LocalDateTime.of(begin, LocalTime.MIN);//起始日期的最开始时间：0时0分0秒
        LocalDateTime ends = LocalDateTime.of(end, LocalTime.MAX);//截止日期的最晚结束时间：11:59:59

        //查询“概览数据”：在工作台页面功能中已经查过概览数据了，所以需要之前查询概览数据的业务层对象WorkspaceService
        BusinessDataVO businessData = workspaceService.getBusinessData(begins, ends);

        //2.通过poi将数据写入到Excel文件中
        /*
         * this.getClass()：获得本类的类对象
         * this.getClass().getClassLoader()：获得类加载器
         * this.getClass().getClassLoader().getResourceAsStream()：获取类路径下的指定文件的输入流
         * */
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        XSSFWorkbook excel = null;
        ServletOutputStream out = null;

        try {
            //基于模版文件创建一个新的excel文件对象：需要传递一个输入流对象来读取项目中的Excel模版文件
            excel = new XSSFWorkbook(inputStream);
            //获得Excel文件中的一个Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");//根据标签页的名字

            //填充：时间区间数据
            //获取第二行的第二个单元格（通过查看excel表格可知）,并往单元格内设置时间数据
            // 注意：此时的行和单元格已存在了所以不需要创建用的是get方法获取。
            //      下标是从0开始
            sheet.getRow(1).getCell(1).setCellValue("时间："+begin+"至"+end);

            //填充：概览数据
            //获得第4行   位置查看excel表格可知
            XSSFRow row = sheet.getRow(3);
            row.getCell(2).setCellValue(businessData.getTurnover());//获取第4行的第3个单元格，并往单元格内设置数据（营业额）
            row.getCell(4).setCellValue(businessData.getOrderCompletionRate());//第5个单元格，设置（订单完成率）
            row.getCell(6).setCellValue(businessData.getNewUsers());//第7个单元格，设置（新增用户数）
            //获得第5行   位置查看excel表格可知
            row = sheet.getRow(4);
            row.getCell(2).setCellValue(businessData.getValidOrderCount());//第5行第3个单元格，设置（有效订单）
            row.getCell(4).setCellValue(businessData.getUnitPrice());//第5行第5个单元格，设置（平均客单价）

            //填充：明细数据
            /*思路分析：查看excel表格可知，概览数据和明细数据的各个数据项都是一样的，区别
                      概览数据查询的是近30天的，而这个明细数据查询的是每一天的数据项。上面
                      已经计算出了开始日期和结束日期，那么就可以往后推循环遍历30次，这个
                      次数是可以确定的。
            */
            for(int i=0;i<30;i++){
                //第一次循环i=0所以是自身的这一天，第二次循环i=1所以往后加一天，一直遍历30次，这样就可以把最近这30天都给遍历出来了
                LocalDate localDate = begin.plusDays(i);//起始时间每次往后加一天，一直遍历30次
                //获取当天的起始时间并转化为年月日时分秒类型
                LocalDateTime beginl = LocalDateTime.of(localDate, LocalTime.MIN);//每天的最开始时间：0时0分0秒
                LocalDateTime endl = LocalDateTime.of(localDate, LocalTime.MAX);//每天的最晚结束时间：11:59:59
                //查询某一天的营业数据
                BusinessDataVO businessData1 = workspaceService.getBusinessData(beginl, endl);

                //获得某一行：查看excel表格可知数据是从第8行（下标为7）开始填充的，由于i初始值为0所以从7开始每次加i就是从第8行开始往下填充数据
                row = sheet.getRow(7 + i);
                row.getCell(1).setCellValue(localDate.toString());//当前行的第2个单元格，填充日期，只能接收字符串类型所以需要转化
                row.getCell(2).setCellValue(businessData1.getTurnover());//当前行的第3个单元格，填充营业额
                row.getCell(3).setCellValue(businessData1.getValidOrderCount());//当前行的第4个单元格，填充有效订单
                row.getCell(4).setCellValue(businessData1.getOrderCompletionRate());//当前行的第5个单元格，填充订单完成率
                row.getCell(5).setCellValue(businessData1.getUnitPrice());//当前行的第6个单元格，填充平均客单价
                row.getCell(6).setCellValue(businessData1.getNewUsers());//当前行的第7个单元格，填充新增用户数
            }

            //3.通过输出流将Excel文件下载到客户端浏览器
            out = response.getOutputStream();
            excel.write(out);


        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            //关闭资源
            try {
                out.close();
                excel.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }



}
