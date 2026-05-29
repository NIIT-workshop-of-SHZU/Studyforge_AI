package com.studyforge.content.mapper.skill;

import com.studyforge.content.entity.skill.RoadmapNode;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface RoadmapNodeMapper {
    RoadmapNode selectById(@Param("nodeId") Long nodeId);

    List<RoadmapNode> selectByRoadmapId(@Param("roadmapId") Long roadmapId);

    int insert(RoadmapNode roadmapNode);

    int updateById(RoadmapNode roadmapNode);

    int deleteById(@Param("nodeId") Long nodeId);

    int deleteByRoadmapId(@Param("roadmapId") Long roadmapId);

    int countByRoadmapId(@Param("roadmapId") Long roadmapId);
}
