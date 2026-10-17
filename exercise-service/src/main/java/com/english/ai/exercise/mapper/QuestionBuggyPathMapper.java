package com.english.ai.exercise.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.english.ai.exercise.entity.QuestionBuggyPath;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QuestionBuggyPathMapper extends BaseMapper<QuestionBuggyPath> {

    @Select("SELECT * FROM question_buggy_path WHERE question_id = #{questionId} " +
            "AND wrong_answer = #{wrongAnswer} LIMIT 1")
    QuestionBuggyPath findByQuestionAndAnswer(@Param("questionId") Long questionId, 
                                               @Param("wrongAnswer") String wrongAnswer);
}