package com.yupi.springbootinit.manager;

import com.yupi.springbootinit.common.ErrorCode;
import com.yupi.springbootinit.exception.BusinessException;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author Mr.Wang
 * @create 2023-09-03-18:34
 */
@Service
public class AiManager {

    @Resource
    private YuCongMingClient yuCongMingClient;

    /**
     * ai对话
     * @param massage
     * @return
     */
    public String doChart(Long modelId,String massage){
        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setModelId(modelId);//1698271403607916545L
        devChatRequest.setMessage(massage);
        BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);
        if(response==null||response.getData()==null){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"ai 相应错误");
        }
        return response.getData().toString();
    }

}
