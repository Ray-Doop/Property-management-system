package com.example.modules.auth.mapper;

import com.example.entity.Admin;
import com.example.entity.Employee;
import com.example.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 认证与账号数据访问
 */
@Mapper
public interface LoginRegisterMapper {


    

    @Select("select * from user where username = #{username}")
    User selectByUsername(String username);

    @Select("select * from user where remark = #{remark}")
    User selectByRemark(String remark);

    @Select("select * from user where username = #{username}")
    User selectByUserid(String username);

    @Select("select * from user where phone = #{phone}")
    User selectByPhone(String phone);

    int insert(User  user);


    User selectUserData(Long userId);


    void uplastlogintime(Long userId);

    void UpdatePassword(User user);

    void updateAvatar(Long userId, String avatarUrl);

    void UpdateUserData(User user);

    @Select("select * from admin where username = #{username}")
    Admin selectByAdminname(String username);

    // Get all users
    List<User> selectAllUser(User user);

    @Select("select * from admin where username = #{username}")
    Admin getAdminByUsername(String username);

    List<User> selectUserApproval(User user);

    void passUser(String username);

    void refuseUser(Long userId,String remark);

    void muteUser(Long userId, String remark);

    void banUser(Long userId, String remark);
    
    Admin selectAdminByUsername(String username);
    
    void deleteUser(Long userId);
    
    void unmuteUser(Long userId);

    Employee selectEmployeeByUsername(String username);

    @Select("select * from employee where employee_id = #{id}")
    Employee selectEmployeeById(Long id);

    @Select("select * from user where status = 1")
    List<User> selectAllValidUsers();

    @Select("select * from admin where admin_id = #{adminId}")
    Admin selectAdminById(Long adminId);
    
    // 管理员密码相关方法
    void updateAdminPassword(Admin admin);
    
    // 管理员资料更新方法
    void updateAdminData(Admin admin);
    
    // 管理员头像更新方法
    void updateAdminAvatar(Long adminId, String avatarUrl);
}
