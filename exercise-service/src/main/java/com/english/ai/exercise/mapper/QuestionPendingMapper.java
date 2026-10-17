package com.english.ai.exercise.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.english.ai.exercise.entity.QuestionPending;
import org.apache.ibatis.annotations.Mapper;

/**
 * 待审核题目 Mapper
 * 继承 MyBatis-Plus BaseMapper，自动拥有 CRUD 能力
 */
@Mapper
public interface QuestionPendingMapper extends BaseMapper<QuestionPending> {
}