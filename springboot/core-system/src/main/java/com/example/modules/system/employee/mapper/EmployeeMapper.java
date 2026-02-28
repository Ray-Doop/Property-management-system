package com.example.modules.system.employee.mapper;

import com.example.entity.Employee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
@Mapper
public interface EmployeeMapper {
    List<Employee> selectAll(Employee employee);
    List<Employee> selectPage(Employee employee);

//    @Select("select * from employee where id=#{id}")
    Employee selectById(Integer id);

    void insert(Employee employee);

    void update(Employee employee);

    void del(Integer id);

    @Update("update employee set avatar_url = #{avatarUrl} where employee_id = #{employeeId}")
    void updateAvatar(Long employeeId, String avatarUrl);

    @Select("select avatar_url from employee where employee_id = #{employeeId}")
    String getAvatarUrl(Long employeeId);

    @Update("update employee set password = #{newPassword} where employee_id = #{employeeId}")
    void updatePassword(Long employeeId, String newPassword);
}
