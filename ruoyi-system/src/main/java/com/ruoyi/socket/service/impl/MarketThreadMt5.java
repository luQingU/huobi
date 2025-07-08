package com.ruoyi.socket.service.impl;

import cn.hutool.http.HttpRequest;
import com.ruoyi.bussiness.domain.TContractCoin;
import com.ruoyi.bussiness.domain.TCurrencySymbol;
import com.ruoyi.bussiness.domain.TSecondCoinConfig;
import com.ruoyi.bussiness.service.ITContractCoinService;
import com.ruoyi.bussiness.service.ITCurrencySymbolService;
import com.ruoyi.bussiness.service.ITSecondCoinConfigService;
import com.ruoyi.bussiness.service.ITSymbolManageService;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.socket.manager.WebSocketUserManager;
import com.ruoyi.system.service.ISysDictTypeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.net.URISyntaxException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;


@Slf4j
@Component
@EnableScheduling
public class MarketThreadMt5 {


    @Resource
    private ITSecondCoinConfigService secondCoinConfigService;
    @Resource
    private ITContractCoinService contractCoinService;
    @Resource
    private ITCurrencySymbolService tCurrencySymbolService;
    @Resource
    private ITSymbolManageService tSymbolManageService;

    @Resource
    private WebSocketUserManager webSocketUserManager;
    @Resource
    private ISysDictTypeService sysDictTypeService;
    @Async
    @Scheduled(cron = "*/15 * * * * ?")
    public void marketThreadRun() throws URISyntaxException {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();
        Set<String> strings = new HashSet<>();
        //秒合约
        TSecondCoinConfig tSecondCoinConfig = new TSecondCoinConfig();
        tSecondCoinConfig.setMarket("mt5");
        tSecondCoinConfig.setStatus(1L);
        List<TSecondCoinConfig> tSecondCoinConfigs = secondCoinConfigService.selectTSecondCoinConfigList(tSecondCoinConfig);
        for (TSecondCoinConfig secondCoinConfig : tSecondCoinConfigs) {
            strings.add(secondCoinConfig.getSymbol().toUpperCase());
        }
        //U本位
        TContractCoin tContractCoin =new TContractCoin();
        tContractCoin.setEnable(0L);
        tContractCoin.setMarket("mt5");
        List<TContractCoin> tContractCoins = contractCoinService.selectTContractCoinList(tContractCoin);
        for (TContractCoin contractCoin : tContractCoins) {
            strings.add(contractCoin.getSymbol().toUpperCase());
        }
        //币币
        TCurrencySymbol tCurrencySymbol = new TCurrencySymbol();
        tCurrencySymbol.setEnable("1");
        tCurrencySymbol.setMarket("mt5");
        List<TCurrencySymbol> tCurrencySymbols = tCurrencySymbolService.selectTCurrencySymbolList(tCurrencySymbol);
        for (TCurrencySymbol currencySymbol : tCurrencySymbols) {
            strings.add(currencySymbol.getSymbol().toUpperCase());
        }

        //字典银行卡绑定币种
        List<SysDictData> backCoinList = sysDictTypeService.selectDictDataByType("t_bank_coin");
        if (!CollectionUtils.isEmpty(backCoinList)){
            for (SysDictData sysDictData : backCoinList) {
                if ("USD".equalsIgnoreCase(sysDictData.getDictValue())) continue;
                strings.add(sysDictData.getDictValue().toUpperCase()+"USD");
                strings.add("USD"+sysDictData.getDictValue().toUpperCase());
            }
        }
        //兑换
        for (String string : strings) {

            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                     Random random = new Random();
                     double v = random.nextDouble();
                     String url = "";
                     url = "https://api-q.fx678img.com/getQuote.php?exchName=WH&symbol="+string+"&st="+v;
                         String result = HttpRequest.get(url)
                                 .header("referer", "https://quote.fx678.com/")
                                 .header("Host","api-q.fx678img.com")
                                 .header("Origin","https://quote.fx678.com")
                                 .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                                 .timeout(20000)
                                 .execute().body();

                    webSocketUserManager.mt5KlineSendMeg(result,string);
                    webSocketUserManager.mt5DETAILSendMeg(result,string);
                }
            });
            thread.start();
        }
    }
}
