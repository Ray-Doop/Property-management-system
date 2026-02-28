package com.example.modules.system.stats.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface StatsMapper {
    Integer countUsers();
    Integer countAdmins();
    Integer countForumPosts();
    Integer countForumComments();
    Integer countTravelPassToday();
    List<Map<String, Object>> loginTrend(@Param("days") Integer days);
    List<Map<String, Object>> postTrend(@Param("days") Integer days);
    List<Map<String, Object>> commentTrend(@Param("days") Integer days);
    List<Map<String, Object>> travelTrend(@Param("days") Integer days);
}
