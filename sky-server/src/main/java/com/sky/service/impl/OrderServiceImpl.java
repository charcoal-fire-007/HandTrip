package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {
        Long userId = BaseContext.getCurrentId();

        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null ) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        ShoppingCart shoppingCart = ShoppingCart.builder().userId(userId).build();
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if(shoppingCartList == null) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        String fullAddress = String.join(" ",
                addressBook.getProvinceName(),
                addressBook.getCityName(),
                addressBook.getDistrictName(),
                addressBook.getDetail());
        Orders orders =  Orders.builder()
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

        List<OrderDetail> orderDetailList = shoppingCartList.stream().map(cart ->{
            OrderDetail orderDetail = OrderDetail.builder().orderId(orders.getId()).build();
            BeanUtils.copyProperties(cart, orderDetail);
            return orderDetail;
        }).toList();

        shoppingCartMapper.delete(userId);

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
    @Override
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
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
        public void userCancelById(Long id) throws Exception{
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
    }

