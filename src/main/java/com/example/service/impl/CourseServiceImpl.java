package com.example.service.impl;

import com.example.entity.po.Course;
import com.example.mapper.CourseMapper;
import com.example.service.ICourseService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 学科表 服务实现类
 * </p>
 *
 * @author micostrich
 * @since 2026-10-08
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements ICourseService {

}
