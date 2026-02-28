package com.example.modules.business.repair.mapper;

import com.example.entity.Employee;
import com.example.entity.repair.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface RepairMapper {
    List<RepairCategory> selectCategories();

    void addRepairOrder(RepairOrder repairOrder);

    void addRepairFiles(RepairFile repairFile);

    List<RepairOrder> selectMyRepair(Integer  userID);


    // 查询报修单及类别名称
    RepairOrder getRepairDetailByOrderId(@Param("orderId") Integer orderId);

    // 查询附件列表
    List<RepairFile> getRepairFilesByOrderId(@Param("orderId") Integer orderId);

    void cancelRepair(Integer orderId);

    List<RepairOrder> allRepair();

    List<RepairOrder> status(String status);
    List<RepairWorker> findAvailableWorkers(@Param("categoryId") Long categoryId,
                                            @Param("appointmentTime") Date appointmentTime);

    void dispatchOrder(RepairAssignment repairAssignment);

    void modificationTime(RepairAssignment repairAssignment);

    String selectStatusByOrderId(@Param("orderId") Long orderId);

    void updateAssignmentOnDispatch(@Param("orderId") Long orderId, @Param("workerId") Long workerId, @Param("status") String status);

    List<Employee> findWorkers(Long categoryId);

    void updateStatus(@Param("orderId") Long orderId, @Param("status") String status, @Param("finishedTime") String finishedTime);

    // 用于员工接单（可选，如果接单即变为维修中）
    void updateWorkerStatus(@Param("orderId") Long orderId, @Param("status") String status);
    
    void acceptByWorker(@Param("orderId") Long orderId, @Param("workerId") Long workerId);
    RepairAssignment selectAssignmentByOrderId(@Param("orderId") Long orderId);
    void updateAssignmentOnAccept(@Param("orderId") Long orderId, @Param("workerId") Long workerId);
    void insertAssignmentOnAccept(@Param("orderId") Long orderId, @Param("workerId") Long workerId);

    void evaluateRepair(@Param("orderId") Long orderId, @Param("evaluation") String evaluation, @Param("rating") Integer rating);
    
    List<RepairOrder> selectByWorker(@Param("workerId") Long workerId, @Param("status") String status);
    
    RepairEvaluation getEvaluationByOrderId(@Param("orderId") Long orderId);
    
    void replyEvaluation(@Param("orderId") Long orderId, @Param("replyContent") String replyContent);

    // 统计各状态数量
    Long countByStatus(@Param("status") String status);
}
