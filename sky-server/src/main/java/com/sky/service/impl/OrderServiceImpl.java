package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.config.TakeOutConfig;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.AmapUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private TakeOutConfig takeOutConfig;

    private void checkOutOfRange(String userAddress) {
        try {
            String shopAddress = takeOutConfig.getShop().getAddress();
            String amapKey     = takeOutConfig.getShop().getAmap().getKey();

            String shopLocation = AmapUtil.geocode(shopAddress,  amapKey);
            String userLocation = AmapUtil.geocode(userAddress,  amapKey);
            long distance = AmapUtil.drivingDistance(shopLocation, userLocation, amapKey);

            if (distance > 10000) {
                throw new OrderBusinessException("超出配送范围");
            }
        } catch (IOException e) {
            throw new OrderBusinessException("高德 API 调用异常：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        Long userId = BaseContext.getCurrentId();

        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        String fullAddress = String.join(" ",
                addressBook.getProvinceName(),
                addressBook.getCityName(),
                addressBook.getDistrictName(),
                addressBook.getDetail());


        checkOutOfRange(fullAddress);


        Orders orders = Orders.builder()
                .userId(userId)
                .orderTime(LocalDateTime.now())
                .payStatus(Orders.UN_PAID)
                .status(Orders.PENDING_PAYMENT)
                .number(String.valueOf(System.currentTimeMillis()))
                .phone(addressBook.getPhone())
                .consignee(addressBook.getConsignee())
                .addressBookId(addressBook.getId())
                .address(fullAddress)
                .build();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orderMapper.insert(orders);

        List<OrderDetail> orderDetailList = shoppingCartList.stream().map(cart -> {
            OrderDetail orderDetail = OrderDetail.builder().orderId(orders.getId()).build();
            BeanUtils.copyProperties(cart, orderDetail);
            return orderDetail;
        }).toList();

        shoppingCartMapper.delete(userId);
        log.info("[下单] userId={}, address={}, cartSize={}",
                userId, fullAddress, shoppingCartList.size());
        orderDetailMapper.insert(orderDetailList);
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
    }


    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    @Override
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "美团闪购外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );


//------------------测试数据，跳过支付接口------------------------------------------------------------------------
        JSONObject jsonObject = new JSONObject();
//------------------测试数据，跳过支付接口------------------------------------------------------------------------


        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void paySuccess(String outTradeNo) {
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);
        if (ordersDB == null) {
            log.warn("支付回调：订单号不存在 {}", outTradeNo);
            return;
        }
        if (Orders.PAID.equals(ordersDB.getPayStatus())) {
            log.info("支付回调：订单已支付 {}", outTradeNo);
            return;
        }

        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        int row = orderMapper.updateWithStatusCheck(orders);
        if (row == 0) {
            throw new RuntimeException("订单状态已变更，支付失败");
        }

        PaySuccessMsg msg = PaySuccessMsg.builder()
                .orderId(orders.getId())
                .content("订单号"+outTradeNo)
                .build();

        try {
            webSocketServer.sendToAllClient(JSON.toJSONString(msg));
        } catch (Exception e) {
            log.error("WebSocket推送失败, orderId={}", ordersDB.getId(), e);
        }

        log.info("订单支付成功：{}", outTradeNo);
    }

    @Override
    public PageResult pageQueryUser(int page, int pageSize, Integer status) {
        PageHelper.startPage(page, pageSize);
        Page<Orders> pageList = orderMapper.selectPageQuery(
                OrdersPageQueryDTO.builder()
                        .userId(BaseContext.getCurrentId())
                        .page(page)
                        .pageSize(pageSize)
                        .status(status)
                        .build()
        );

        // 1. 一次性查出当前页所有订单的明细
        List<Long> orderIds = pageList.stream()
                .map(Orders::getId)
                .toList();
        List<OrderDetail> allDetails = orderDetailMapper.selectByIds(orderIds);

        // 2. 按订单 id 分组
        Map<Long, List<OrderDetail>> detailMap = new HashMap<Long, List<OrderDetail>>();
        for (OrderDetail d : allDetails) {
            Long key = d.getOrderId();
            List<OrderDetail> list = detailMap.computeIfAbsent(key, k -> new ArrayList<>());
            list.add(d);
        }

        // 3. 组装
        List<OrderVO> orderVOS = pageList.stream()
                .map(o -> {
                    OrderVO vo = new OrderVO();
                    BeanUtils.copyProperties(o, vo);
                    vo.setOrderDetailList(detailMap.getOrDefault(o.getId(), List.of()));
                    return vo;
                })
                .toList();

        return new PageResult(pageList.getTotal(), orderVOS);
    }

    /**
     * 查询订单详情
     *
     * @param id
     * @return
     */
    public OrderVO details(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);

        // 查询该订单对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.selectById(orders.getId());


        // 将该订单及其详情封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

    /**
     * 用户取消订单
     *
     * @param id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userCancelById(Long id) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        // 订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            //调用微信支付退款接口
//                weChatPayUtil.refund(
//                        ordersDB.getNumber(), //商户订单号
//                        ordersDB.getNumber(), //商户退款单号
//                        new BigDecimal(0.01),//退款金额，单位 元
//                        new BigDecimal(0.01));//原订单金额

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    /**
     * 再来一单
     *
     * @param id
     */
    public void repetition(Long id) {
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.selectById(id);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).toList();

        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> pageList = orderMapper.selectPageQuery(ordersPageQueryDTO);

        List<Long> orderIds = pageList.stream()
                .map(Orders::getId)
                .toList();

        List<OrderDetail> allDetails = CollectionUtils.isEmpty(orderIds)
                ? List.of()
                : orderDetailMapper.selectByIds(orderIds);

        Map<Long, String> dishStrMap = allDetails.stream()
                .collect(Collectors.groupingBy(
                        OrderDetail::getOrderId,
                        Collectors.mapping(
                                d -> d.getName() + "*" + d.getNumber() + ";",
                                Collectors.joining(""))));

        List<OrderVO> orderVOS = pageList.stream()
                .map(o -> {
                    OrderVO vo = new OrderVO();
                    BeanUtils.copyProperties(o, vo);
                    vo.setOrderDishes(dishStrMap.getOrDefault(o.getId(), ""));
                    return vo;
                })
                .toList();

        return new PageResult(pageList.getTotal(), orderVOS);
    }

    /**
     * 各个状态的订单数量统计
     *
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        // 根据状态，分别查询出待接单、待派送、派送中的订单数量
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        // 将查询出的数据封装到orderStatisticsVO中响应
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);
    }

    /**
     * 拒单
     *
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());

        // 订单只有存在且状态为2（待接单）才可以拒单
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == Orders.PAID) {
            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
            log.info("申请退款");
        }

        // 拒单需要退款，根据订单id更新订单状态、拒单原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 取消订单
     *
     * @param ordersCancelDTO
     */
    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());

        //支付状态
        Integer payStatus = ordersDB.getPayStatus();
        if (payStatus == 1) {
//            //用户已支付，需要退款
//            String refund = weChatPayUtil.refund(
//                    ordersDB.getNumber(),
//                    ordersDB.getNumber(),
//                    new BigDecimal(0.01),
//                    new BigDecimal(0.01));
            log.info("申请退款");
        }

        // 管理端取消订单需要退款，根据订单id更新订单状态、取消原因、取消时间
        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 派送订单
     *
     * @param id
     */
    @Override
    public void delivery(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为3
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.CONFIRMED)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为派送中
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);

        orderMapper.update(orders);
    }

    /**
     * 完成订单
     *
     * @param id
     */
    @Override
    public void complete(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在，并且状态为4
        if (ordersDB == null || !ordersDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        // 更新订单状态,状态转为完成
        orders.setStatus(Orders.COMPLETED);
        orders.setDeliveryTime(LocalDateTime.now());

        orderMapper.update(orders);
    }

    /**
     * 客户催单
     *
     * @param id
     */
    @Override
    public void reminder(Long id) {
        // 根据id查询订单
        Orders ordersDB = orderMapper.getById(id);

        // 校验订单是否存在
        if (ordersDB == null ) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        ReminderMsg msg = ReminderMsg.builder()
                .orderId(id)
                .content("订单号"+ordersDB.getNumber())
                .build();

        try {
            webSocketServer.sendToAllClient(JSON.toJSONString(msg));
        } catch (Exception e) {
            log.error("WebSocket推送失败, orderId={}", ordersDB.getId(), e);
        }

    }
}
