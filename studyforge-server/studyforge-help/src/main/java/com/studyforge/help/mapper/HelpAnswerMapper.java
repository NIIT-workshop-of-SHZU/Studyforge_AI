package com.studyforge.help.mapper;

import com.studyforge.help.entity.HelpAnswer;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HelpAnswerMapper {
    HelpAnswer selectById(@Param("answerId") Long answerId);

    List<HelpAnswer> selectByHelpId(@Param("helpId") Long helpId);

    int insert(HelpAnswer helpAnswer);

    int clearAccepted(@Param("helpId") Long helpId);

    int accept(@Param("answerId") Long answerId);
}
