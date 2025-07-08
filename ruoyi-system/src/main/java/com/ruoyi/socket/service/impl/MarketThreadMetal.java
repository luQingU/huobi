package com.ruoyi.socket.service.impl;

import cn.hutool.http.HttpRequest;
import com.ruoyi.bussiness.domain.TContractCoin;
import com.ruoyi.bussiness.domain.TCurrencySymbol;
import com.ruoyi.bussiness.domain.TSecondCoinConfig;
import com.ruoyi.bussiness.service.ITContractCoinService;
import com.ruoyi.bussiness.service.ITCurrencySymbolService;
import com.ruoyi.bussiness.service.ITSecondCoinConfigService;
import com.ruoyi.bussiness.service.ITSymbolManageService;
import com.ruoyi.socket.manager.WebSocketUserManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.URISyntaxException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;


@Slf4j
@Component
public class MarketThreadMetal {


    @Resource
    private ITSecondCoinConfigService secondCoinConfigService;
    @Resource
    private ITContractCoinService contractCoinService;
    @Resource
    private ITCurrencySymbolService tCurrencySymbolService;
    @Resource
    private WebSocketUserManager webSocketUserManager;
    @Async
    @Scheduled(cron = "*/15 * * * * ?")
    public void marketThreadRun() throws URISyntaxException {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        Set<String> strings = new HashSet<>();
        //秒合约
        TSecondCoinConfig tSecondCoinConfig = new TSecondCoinConfig();
        tSecondCoinConfig.setMarket("metal");
        tSecondCoinConfig.setStatus(1L);
        List<TSecondCoinConfig> tSecondCoinConfigs = secondCoinConfigService.selectTSecondCoinConfigList(tSecondCoinConfig);
        for (TSecondCoinConfig secondCoinConfig : tSecondCoinConfigs) {
            strings.add(secondCoinConfig.getCoin().toUpperCase());
        }
        //U本位
        TContractCoin tContractCoin =new TContractCoin();
        tContractCoin.setEnable(0L);
        tContractCoin.setMarket("metal");
        List<TContractCoin> tContractCoins = contractCoinService.selectTContractCoinList(tContractCoin);
        for (TContractCoin contractCoin : tContractCoins) {
            strings.add(contractCoin.getCoin().toUpperCase());
        }
        //币币
        TCurrencySymbol tCurrencySymbol = new TCurrencySymbol();
        tCurrencySymbol.setEnable("1");
        tCurrencySymbol.setMarket("metal");
        List<TCurrencySymbol> tCurrencySymbols = tCurrencySymbolService.selectTCurrencySymbolList(tCurrencySymbol);
        for (TCurrencySymbol currencySymbol : tCurrencySymbols) {
            strings.add(currencySymbol.getCoin().toUpperCase());
        }
        //兑换
        for (String string : strings) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                     Random random = new Random();
                     double v = random.nextDouble();
                     String url = "";
                     url = "https://api-q.fx678img.com/getQuote.php?exchName=WGJS&symbol="+string.toUpperCase()+"&st="+v;
                         String result = HttpRequest.get(url)
                                 .header("referer", "https://quote.fx678.com/")
                                 .header("Host","api-q.fx678img.com")
                                 .header("Origin","https://quote.fx678.com")
                                 .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                                 .timeout(20000)
                                 .execute().body();
                    webSocketUserManager.metalKlineSendMeg(result,string);
                    webSocketUserManager.metalDETAILSendMeg(result,string);
                }
            });
            thread.start();
        }
    }

}
