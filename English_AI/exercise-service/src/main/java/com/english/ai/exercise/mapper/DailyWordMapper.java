package com.english.ai.exercise.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.english.ai.exercise.entity.DailyWord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DailyWordMapper extends BaseMapper<DailyWord> {
}
