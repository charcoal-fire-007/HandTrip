package com.sky.service.impl;

import com.fasterxml.jackson.databind.ser.Serializers;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password =DigestUtils.md5DigestAsHex(employeeLoginDTO.getPassword().getBytes(StandardCharsets.UTF_8));

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    @Override
    public void save(EmployeeDTO employeeDTO) {
        Employee employee = Employee.builder()
                .status(StatusConstant.ENABLE)
                .password(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes(StandardCharsets.UTF_8)))
                .build();
        
        BeanUtils.copyProperties(employeeDTO, employee);
        employeeMapper.insert(employee);
    }

    @Override
    public PageResult page(EmployeePageQueryDTO employeePageQueryDTO){
            PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
            Page<Employee> page = employeeMapper.select(employeePageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    @Override
    public void updateStatus(Integer status, Long id) {
        employeeMapper.update(Employee.builder().id(id).status(status).build());
    }

    @Override
    public Employee getById(Integer id) {
        Employee employee =  employeeMapper.selectById(id);
        employee.setPassword("******");
        return employee;
    }

    @Override
    public void updateEmp(EmployeeDTO employeeDTO) {
        Employee employee = Employee.builder().build();
        BeanUtils.copyProperties(employeeDTO, employee);
        employeeMapper.update(employee);
    }
}
