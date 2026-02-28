package com.example.modules.business.repair.mapper;

import com.example.entity.repair.RepairEvaluation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RepairEvaluationMapper {
    List<RepairEvaluation> selectPage(@Param("score") Integer score, @Param("assignmentId") Long assignmentId);
    void updateReply(@Param("evalId") Long evalId, @Param("replyContent") String replyContent);
}
