package com.yupi.springbootinit.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.gson.Gson;
import com.yupi.springbootinit.annotation.AuthCheck;
import com.yupi.springbootinit.bizmq.BiMessageProducer;
import com.yupi.springbootinit.common.BaseResponse;
import com.yupi.springbootinit.common.DeleteRequest;
import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.common.ResultUtils;
import com.yupi.springbootinit.constant.CommonConstant;
import com.yupi.springbootinit.constant.FileConstant;
import com.yupi.springbootinit.constant.RedisConstant;
import com.yupi.springbootinit.constant.UserConstant;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.springbootinit.exception.ThrowUtils;
import com.yupi.springbootinit.manager.AiManager;
import com.yupi.springbootinit.manager.RedisLimitManager;
import com.yupi.springbootinit.model.dto.chart.*;
import com.yupi.springbootinit.model.dto.file.UploadFileRequest;
import com.yupi.springbootinit.model.entity.Chart;
import com.yupi.springbootinit.model.entity.User;
import com.yupi.springbootinit.model.enums.FileUploadBizEnum;
import com.yupi.springbootinit.model.vo.BiResponse;
import com.yupi.springbootinit.service.ChartService;
import com.yupi.springbootinit.service.UserService;
import com.yupi.springbootinit.utils.ExcelUtils;
import com.yupi.springbootinit.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 图标接口
 *
 * @author <a href="https://github.com/liyupi">程序员鱼皮</a>
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;
    @Resource
    private RedisLimitManager redisLimitManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    private final static Gson GSON = new Gson();


    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（封装类）
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                     HttpServletRequest request) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest,
                                                       HttpServletRequest request) {

        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        //查询第几页
        long current = chartQueryRequest.getCurrent();
        //redis查询
        //设计key=业务逻辑+id+第几页,实现缓存每页的数据
        String key=RedisConstant.CHAR_LIST_KEY+loginUser.getId();
        //使用current作为hash中的表项key
        String s = (String) stringRedisTemplate.opsForHash().get(key,String.valueOf(current));
        if (StrUtil.isNotBlank(s) ){
            Page bean = JSONUtil.toBean(s, Page.class);
            return ResultUtils.success(bean);
        }
        chartQueryRequest.setUserId(loginUser.getId());

        long size = chartQueryRequest.getPageSize();


        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size),
                getQueryWrapper(chartQueryRequest));

        //将查询的数据存放在redis中
        //将page对象转化为json储存在redis中
        String jsonStr = JSONUtil.toJsonStr(chartPage);
        stringRedisTemplate.opsForHash().put(key,String.valueOf(current),jsonStr);
        stringRedisTemplate.expire(key,30, TimeUnit.SECONDS);
        return ResultUtils.success(chartPage);
    }

    // endregion

    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 获取查询包装类
     *
     * @param chartQueryRequest
     * @return
     */
    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String name = chartQueryRequest.getName();
        String goal = chartQueryRequest.getGoal();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotEmpty(name), "name", name);
        queryWrapper.eq(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    /**
     * 智能分析(同步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/")
    public BaseResponse<BiResponse> genChartByAI(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {

        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //校验,如果目标为空则抛出异常
        ThrowUtils.throwIf(StringUtils.isEmpty(goal),ErrorCode.PARAMS_ERROR,"目标为空");
        ThrowUtils.throwIf(StringUtils.isEmpty(name)&&name.length()>100,ErrorCode.PARAMS_ERROR,"名称过长");
        //校验文件
        long fileSize = multipartFile.getSize();//文件大小
        String originalFilename = multipartFile.getOriginalFilename();
        //检验文件大小
        final Integer ONE_MB=1*1024*1024;//默认单位为字节
        ThrowUtils.throwIf(fileSize>ONE_MB,ErrorCode.PARAMS_ERROR,"文件超过1MB");

        //校验文件后缀名
        //直接调用hootu别人做好的工具类,
        String suffix = FileUtil.getSuffix(originalFilename);
        //定义可通过的文件名后缀
        final List<String> list = Arrays.asList("xls", "xlsx");

        ThrowUtils.throwIf(!list.contains(suffix),ErrorCode.PARAMS_ERROR,"文件后缀非法");

        //必须登录才能访问
        User loginUser = userService.getLoginUser(request);

        //设置限流器，限流判断每个用户都有对应的限流器
        redisLimitManager.doRateLimit("genChartByAi_"+loginUser.getId());


        //如果在平台已经设置了模型prompt，就不需要设置了
//        //系统预设
//        final String prompt="你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容： \n" +
//                "分析需求： \n" +
//                "{数据分析的需求或目标}\n" +
//                "原始数据： \n" +
//                "{csv格式的原始数据,用,作为分隔符}\n" +
//                " 请根据根据这两部分内容，按照以下指定格式格式生成内容（此外不要输出任何多余的开头、结尾、注释等内容）\n" +
//                "【【【【【\n" +
//                "{前端 Echarts V5 的 option 配置对象 js 代码，合理的将数据可视化，不要生成多余的内容比如注释}\n" +
//                "【【【【【\n" +
//                "{明确数据分析结论，越详细越好，不要生成多余的注释}";
////
        long modelId=1698271403607916545L;
        //需求分析：
        //分析网站用户的增长情况
        //原始数据如下：
        //日期,用户数
        //1,10
        //2,20
        //3,30


        //用户输入：
        StringBuffer userInput = new StringBuffer();
        userInput.append("需求分析：").append("\n");
        //拼接分析目标
        String userGoal=goal;
        if(chartType!=null){
            userGoal="请使用:"+chartType+userGoal;
        }

        userInput.append(userGoal).append("\n");
        userInput.append("原始数据如下：：").append("\n");

        //压缩后的数据
        String csvData= ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");
        String res= aiManager.doChart(modelId, userInput.toString());
        //使用次数-1
        loginUser.setNum(loginUser.getNum()-1);
        userService.updateById(loginUser);


        //将回答进行拆分
        String[] split = res.split("【【【【【");
        if(split.length<3){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"AI生成错误");
        }
        String genChart=split[1];
        String genResult=split[2];
        //插入到数据库中进行保存
        Chart chart = new Chart();

        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        chart.setName(loginUser.getUserName());
        chart.setStatus("succeed");
        //保存
        boolean saveResult = chartService.save(chart);
        if (!saveResult){
            throw  new BusinessException(ErrorCode.SYSTEM_ERROR,"图标保存失败");
        }

        //数据库更新完成后，删除redis中的缓存
        String key=RedisConstant.CHAR_LIST_KEY+loginUser.getId();
        stringRedisTemplate.delete(key);


        BiResponse biResponse = new BiResponse();
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        biResponse.setChartId(chart.getId());

        return ResultUtils.success(biResponse);
    }

    /**
     * 智能分析（异步）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile,
                                                 GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {


        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //校验,如果目标为空则抛出异常
        ThrowUtils.throwIf(StringUtils.isEmpty(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isEmpty(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        //校验文件
        long fileSize = multipartFile.getSize();//文件大小
        String originalFilename = multipartFile.getOriginalFilename();
        //检验文件大小
        final Integer ONE_MB = 1 * 1024 * 1024;//默认单位为字节
        ThrowUtils.throwIf(fileSize > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1MB");

        //校验文件后缀名
        //直接调用hootu别人做好的工具类,
        String suffix = FileUtil.getSuffix(originalFilename);
        //定义可通过的文件名后缀
        final List<String> list = Arrays.asList("xls", "xlsx");

        ThrowUtils.throwIf(!list.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        //必须登录才能访问
        User loginUser = userService.getLoginUser(request);

        //设置限流器，限流判断每个用户都有对应的限流器
        redisLimitManager.doRateLimit("genChartByAi_" + loginUser.getId());


        //如果在平台已经设置了模型prompt，就不需要设置了
//        //系统预设
//        final String prompt="你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容： \n" +
//                "分析需求： \n" +
//                "{数据分析的需求或目标}\n" +
//                "原始数据： \n" +
//                "{csv格式的原始数据,用,作为分隔符}\n" +
//                " 请根据根据这两部分内容，按照以下指定格式格式生成内容（此外不要输出任何多余的开头、结尾、注释等内容）\n" +
//                "【【【【【\n" +
//                "{前端 Echarts V5 的 option 配置对象 js 代码，合理的将数据可视化，不要生成多余的内容比如注释}\n" +
//                "【【【【【\n" +
//                "{明确数据分析结论，越详细越好，不要生成多余的注释}";
////
        long modelId = 1698271403607916545L;
        //需求分析：
        //分析网站用户的增长情况
        //原始数据如下：
        //日期,用户数
        //1,10
        //2,20
        //3,30


        //用户输入：
        StringBuffer userInput = new StringBuffer();
        userInput.append("需求分析：").append("\n");
        //拼接分析目标
        String userGoal = goal;
        if (chartType != null) {
            userGoal = "请使用:" + chartType + userGoal;
        }

        userInput.append(userGoal).append("\n");
        userInput.append("原始数据如下：：").append("\n");

        //压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
        userInput.append(csvData).append("\n");

        //插入到数据库中进行保存
        Chart chart = new Chart();

        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        chart.setName(loginUser.getUserName());
        //保存
        boolean saveResult = chartService.save(chart);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图表保存失败");
        }
        //数据库更新完成后，删除redis中的缓存
        String key=RedisConstant.CHAR_LIST_KEY+loginUser.getId();
        stringRedisTemplate.delete(key);


        //调用任务
        // todo 建议处理任务队列满之后，抛异常的情况
        CompletableFuture.runAsync(() -> {
                    Chart updateChart = new Chart();
                    updateChart.setId(chart.getId());
                    updateChart.setStatus("running");
                    boolean b = chartService.updateById(updateChart);
                    if (!b) {
                        handlerUpdateChartError(chart.getId(), "更新图表执行中状态失败");
//                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新图表中的状态失败");
                        return;
                    }

                    //调用ai
                    String res = aiManager.doChart(modelId, userInput.toString());
                    //将回答进行拆分
                    String[] split = res.split("【【【【【");
                    if (split.length < 3) {
                        handlerUpdateChartError(chart.getId(), "AI生成错误");
//                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成错误");
                        return;
                    }

                    String genChart = split[1].trim();
                    String genResult = split[2].trim();

                    //ai处理结束后再次更新数据库的信息
                    Chart updateResultChart = new Chart();
                    updateResultChart.setId(chart.getId());
                    updateResultChart.setGenChart(genChart);
                    updateResultChart.setGenResult(genResult);
                    //建议定义状态为枚举类型
                    updateResultChart.setStatus("succeed");
                    boolean updateResult = chartService.updateById(updateResultChart);
                    if (!updateResult) {
                        handlerUpdateChartError(chart.getId(), "更新图表成功状态失败");
//                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新图表中的状态失败");
                        return;
                    }

                },threadPoolExecutor);


        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());

        return ResultUtils.success(biResponse);
    }



    /**
     * 智能分析（异步消息队列）
     *
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                      GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {


        String name = genChartByAiRequest.getName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        //校验,如果目标为空则抛出异常
        ThrowUtils.throwIf(StringUtils.isEmpty(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isEmpty(name) && name.length() > 100, ErrorCode.PARAMS_ERROR, "名称过长");
        //校验文件
        long fileSize = multipartFile.getSize();//文件大小
        String originalFilename = multipartFile.getOriginalFilename();
        //检验文件大小
        final Integer ONE_MB = 1 * 1024 * 1024;//默认单位为字节
        ThrowUtils.throwIf(fileSize > ONE_MB, ErrorCode.PARAMS_ERROR, "文件超过1MB");

        //校验文件后缀名
        //直接调用hootu别人做好的工具类,
        String suffix = FileUtil.getSuffix(originalFilename);
        //定义可通过的文件名后缀
        final List<String> list = Arrays.asList("xls", "xlsx");

        ThrowUtils.throwIf(!list.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");

        //必须登录才能访问
        User loginUser = userService.getLoginUser(request);

        //判断当前用户调用ai的次数是否大于0
        Integer num = loginUser.getNum();
        ThrowUtils.throwIf(num<=0,ErrorCode.OPERATION_ERROR,"今天的使用次数已用完");


        //设置限流器，限流判断每个用户都有对应的限流器
        redisLimitManager.doRateLimit("genChartByAi_" + loginUser.getId());


        //如果在平台已经设置了模型prompt，就不需要设置了
//        //系统预设
//        final String prompt="你是一个数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容： \n" +
//                "分析需求： \n" +
//                "{数据分析的需求或目标}\n" +
//                "原始数据： \n" +
//                "{csv格式的原始数据,用,作为分隔符}\n" +
//                " 请根据根据这两部分内容，按照以下指定格式格式生成内容（此外不要输出任何多余的开头、结尾、注释等内容）\n" +
//                "【【【【【\n" +
//                "{前端 Echarts V5 的 option 配置对象 js 代码，合理的将数据可视化，不要生成多余的内容比如注释}\n" +
//                "【【【【【\n" +
//                "{明确数据分析结论，越详细越好，不要生成多余的注释}";
////
        long modelId = CommonConstant.BI_MODEL_ID;
        //需求分析：
        //分析网站用户的增长情况
        //原始数据如下：
        //日期,用户数
        //1,10
        //2,20
        //3,30


//        //用户输入：
//        StringBuffer userInput = new StringBuffer();
//        userInput.append("需求分析：").append("\n");
//        //拼接分析目标
//        String userGoal = goal;
//        if (chartType != null) {
//            userGoal = "请使用:" + chartType + userGoal;
//        }
//
//        userInput.append(userGoal).append("\n");
//        userInput.append("原始数据如下：：").append("\n");
//
//        //压缩后的数据
        String csvData = ExcelUtils.excelToCsv(multipartFile);
//        userInput.append(csvData).append("\n");

        //插入到数据库中进行保存
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setChartData(csvData);
        chart.setChartType(chartType);
        chart.setStatus("wait");
        chart.setUserId(loginUser.getId());
        chart.setName(loginUser.getUserName());
        //保存
        boolean saveResult = chartService.save(chart);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图表保存失败");
        }
        //使用次数-1
        loginUser.setNum(loginUser.getNum()-1);
        userService.updateById(loginUser);

        //数据库更新完成后，删除redis中的缓存
        String key=RedisConstant.CHAR_LIST_KEY+loginUser.getId();
        stringRedisTemplate.delete(key);
        Long newChartId = chart.getId();

        //调用任务
        // todo 建议处理任务队列满之后，抛异常的情况
        //将用线程池来处理ai服务器，换成用消息队列来处理
//        CompletableFuture.runAsync(() -> {
//        },threadPoolExecutor);

        biMessageProducer.sendMessage(new String[]{String.valueOf(newChartId),
                String.valueOf(loginUser.getId())});

        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(newChartId);

        return ResultUtils.success(biResponse);
    }

    public void handlerUpdateChartError(Long chartId,String massage){
        Chart updateResultChart = new Chart();
        updateResultChart.setId(chartId);
        updateResultChart.setStatus("failed");
        updateResultChart.setExecMassage("execMassage");
        boolean b = chartService.updateById(updateResultChart);
        if(!b){
            log.error("更新图表状态失败",+chartId+":"+massage);
        }

    }



}
