package com.english.ai.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.english.ai.user.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户数据访问层 — 继承 MyBatis-Plus BaseMapper
 * 自动拥有: insert / update / delete / selectById / selectList 等方法
 */
@Mapper
public interface UserMapper extends BaseMapper<SysUser> {
    // 如有复杂查询，在此添加方法并在XML中实现
}
