package com.yupi.springbootinit.utils;
import com.aliyun.teautil.models.RuntimeOptions;

/**
 * @ClassName CodeUtils
 * @Description TODO
 * @Author Mr.Wang
 * @Date 2023/11/10 23:15
 * @Version 1.0
 */

// This file is auto-generated, don't edit it. Thanks.


import com.aliyun.tea.*;

public class CodeUtils {

    /**
     * 使用AK&SK初始化账号Client
     * @param accessKeyId
     * @param accessKeySecret
     * @return Client
     * @throws Exception
     */
    public static com.aliyun.dysmsapi20170525.Client createClient(String accessKeyId, String accessKeySecret) throws Exception {
        com.aliyun.teaopenapi.models.Config config = new com.aliyun.teaopenapi.models.Config()
                // 必填，您的 AccessKey ID
                .setAccessKeyId(accessKeyId)
                // 必填，您的 AccessKey Secret
                .setAccessKeySecret(accessKeySecret);
        // Endpoint 请参考 https://api.aliyun.com/product/Dysmsapi
        config.endpoint = "dysmsapi.aliyuncs.com";
        return new com.aliyun.dysmsapi20170525.Client(config);
    }

    public static void main(String[] args_) throws Exception {
        java.util.List<String> args = java.util.Arrays.asList(args_);
        // 请确保代码运行环境设置了环境变量 ALIBABA_CLOUD_ACCESS_KEY_ID 和 ALIBABA_CLOUD_ACCESS_KEY_SECRET。
        // 工程代码泄露可能会导致 AccessKey 泄露，并威胁账号下所有资源的安全性。以下代码示例使用环境变量获取 AccessKey 的方式进行调用，仅供参考，建议使用更安全的 STS 方式，更多鉴权访问方式请参见：https://help.aliyun.com/document_detail/378657.html
        com.aliyun.dysmsapi20170525.Client client = CodeUtils.createClient(System.getenv("\n" +
                "LTAI5tRR6BDFGTuimJ8ph8hj"), System.getenv("sw0RJdbsoESlqkuInxqeJVIUM8dpw4"));
        com.aliyun.dysmsapi20170525.models.SendSmsRequest sendSmsRequest = new com.aliyun.dysmsapi20170525.models.SendSmsRequest()
                .setPhoneNumbers("15085706504")
                .setSignName("SmartBI")
                .setTemplateCode("SMS_290011963")//模板id
                .setTemplateParam("66666");
        try {
            // 复制代码运行请自行打印 API 的返回值
           client.sendSmsWithOptions(sendSmsRequest, new RuntimeOptions());
            System.out.println("发送成功");
        } catch (TeaException error) {
            // 如有需要，请打印 error
            com.aliyun.teautil.Common.assertAsString(error.message);
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            // 如有需要，请打印 error
            com.aliyun.teautil.Common.assertAsString(error.message);
        }
    }
}


