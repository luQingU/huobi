package com.ruoyi.bussiness.service.impl;

import cn.hutool.json.JSONUtil;
import com.ruoyi.bussiness.domain.SendPhoneDto;
import com.ruoyi.bussiness.domain.setting.Setting;
import com.ruoyi.bussiness.domain.setting.SmsSetting;
import com.ruoyi.bussiness.service.SettingService;
import com.ruoyi.bussiness.service.SmsService;
import com.ruoyi.common.enums.SettingEnum;
import com.ruoyi.common.utils.UidUtils;
import com.ruoyi.common.utils.http.HttpUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
@Service
public class SmsServiceImpl implements SmsService {

    @Resource
    private SettingService settingService;

    @Override
    public String sendMobileCode(SendPhoneDto mobileNumber) {
        Setting setting = settingService.get(SettingEnum.SMS_SETTING.name());
        SmsSetting smsSetting = JSONUtil.toBean(setting.getSettingValue(), SmsSetting.class);
        mobileNumber.setAccount(smsSetting.getMobileAccount());
        mobileNumber.setPassword(smsSetting.getMobilePassword());
        Map<String,String> map = new HashMap<>();
        map.put("account",smsSetting.getMobileAccount());
        map.put("password",smsSetting.getMobilePassword());
        map.put("msg","Your verification code is {"+mobileNumber.getMsg()+"}. The validity period is 5 minutes. Please enter it in time.");
        map.put("mobile",mobileNumber.getMobile());
        map.put("uid", UidUtils.getUUID(true));
        return HttpUtils.doPostWithJson(smsSetting.getMobileUrl() ,map);
    }
}
