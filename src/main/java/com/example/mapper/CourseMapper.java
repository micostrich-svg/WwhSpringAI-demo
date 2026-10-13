package com.example.mapper;

import com.example.entity.po.Course;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 学科表 Mapper 接口
 * </p>
 *
 * @author micostrich
 * @since 2026-10-08
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {

}
