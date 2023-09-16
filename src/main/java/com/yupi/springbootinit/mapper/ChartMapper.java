package com.yupi.springbootinit.mapper;

import com.yupi.springbootinit.model.entity.Chart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

/**
* @author champion
* @description 针对表【chart(图标信息表)】的数据库操作Mapper
* @createDate 2023-08-30 17:57:54
* @Entity com.yupi.springbootinit.model.entity.Chart
*/
public interface ChartMapper extends BaseMapper<Chart> {

    List<Map<String,Object>> queryChartData(String querySql);

}




