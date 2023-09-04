package com.yupi.springbootinit.model.vo;

import lombok.Data;

/**
 * BI的返回结果
 * @author Mr.Wang
 * @create 2023-09-03-21:40
 */
@Data
public class BiResponse {
    private String genChart;
    private String genResult;

    private Long chartId;
}
