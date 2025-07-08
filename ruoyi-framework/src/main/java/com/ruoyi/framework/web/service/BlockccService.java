package com.ruoyi.framework.web.service;

import cc.block.data.api.domain.enumeration.Interval;
import cc.block.data.api.domain.market.Kline;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.binance.connector.client.SpotClient;
import com.binance.connector.client.impl.SpotClientImpl;
import com.huobi.api.request.account.SwapMarketHistoryKlineRequest;
import com.huobi.api.response.market.SwapMarketHistoryKlineResponse;
import com.huobi.api.service.market.MarketAPIServiceImpl;
import com.ruoyi.bussiness.domain.KlineSymbol;
import com.ruoyi.bussiness.domain.TBotKlineModel;
import com.ruoyi.bussiness.service.IKlineSymbolService;
import com.ruoyi.bussiness.service.ITBotKlineModelInfoService;
import com.ruoyi.bussiness.service.ITBotKlineModelService;
import com.ruoyi.bussiness.service.ITOwnCoinService;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.enums.CachePrefix;
import com.ruoyi.common.enums.CandlestickIntervalEnum;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.domain.KlineParamVO;
import com.ruoyi.framework.web.domain.Ticker24hVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BlockccService {
    @Autowired
    private ITBotKlineModelInfoService botKlineModelInfoService;
    @Autowired
    private ITBotKlineModelService botKlineModelService;
    @Resource
    private ITOwnCoinService itOwnCoinService;
    @Resource
    private IKlineSymbolService klineSymbolService;
    @Resource
    private RedisCache redisCache;

    public List<Kline> getHistoryKline(KlineParamVO klineParam) {
        try {


        String market = StringUtils.isBlank(klineParam.getMarket()) ? "" : klineParam.getMarket();
        String timeCode = this.getMt5Time(klineParam.getInterval());
        String symbol = klineParam.getSymbol();
        switch (market) {
            case "binance": {
                //后续加上控线逻辑
                Map<String, Object> parameters = new LinkedHashMap<>();
                SpotClient client = new SpotClientImpl();
                parameters.put("symbol", symbol.toUpperCase() + "USDT");
                Interval interval = Interval.valueOf(klineParam.getInterval());
                parameters.put("interval", interval.toString());
                if (klineParam.getEnd() != null) {
                    parameters.put("endTime", klineParam.getEnd());
                }
                log.debug("参数" + JSONObject.toJSONString(parameters));
                String result = client.createMarket().klines(parameters);
                List<Kline> his = new ArrayList<>();
                JSONArray parse = JSONArray.parse(result);
                for (int i = 0; i < parse.size(); i++) {
                    JSONArray jsonObject = parse.getJSONArray(i);
                    Object[] array = jsonObject.toArray();
                    Kline kline = new Kline();
                    kline.setTimestamp((Long) array[0]);
                    kline.setOpen(Double.parseDouble((String) array[1]));
                    kline.setHigh(Double.parseDouble((String) array[2]));
                    kline.setLow(Double.parseDouble((String) array[3]));
                    kline.setClose(Double.parseDouble((String) array[4]));
                    kline.setVolume(Double.parseDouble((String) array[5]));
                    his.add(kline);
                }
                his = botKlineModelInfoService.selectBotLineList(symbol.toLowerCase() + "usdt", his, klineParam.getInterval());

                return his;
            }
            case "huobi": {
                MarketAPIServiceImpl huobiAPIService = new MarketAPIServiceImpl();
                SwapMarketHistoryKlineRequest result = SwapMarketHistoryKlineRequest.builder()
                        .contractCode(symbol.toUpperCase() + "-USDT")//合约代码	"BTC-USDT" ...
                        .period(CandlestickIntervalEnum.getValue(klineParam.getInterval())) //K线类型	1min, 5min, 15min, 30min, 60min,4hour,1day,1week,1mon
                        .size(1000) //获取数量，默认150	[1,2000]
                        //.from() //开始时间戳 10位 单位S
                        //.to();//结束时间戳 10位 单位S
                        .build();
                List<Kline> his = new ArrayList<>();
                SwapMarketHistoryKlineResponse response = huobiAPIService.getSwapMarketHistoryKline(result);
                if ("ok".equalsIgnoreCase(response.getStatus())) {
                    List<SwapMarketHistoryKlineResponse.DataBean> list = response.getData();
                    for (SwapMarketHistoryKlineResponse.DataBean data : list) {
                        Kline kline = new Kline();
                        kline.setTimestamp(data.getId());
                        kline.setOpen(data.getOpen().doubleValue());
                        kline.setHigh(data.getHigh().doubleValue());
                        kline.setLow(data.getLow().doubleValue());
                        kline.setClose(data.getClose().doubleValue());
                        kline.setVolume(data.getVol().doubleValue());
                        his.add(kline);
                    }
                }
                his = botKlineModelInfoService.selectBotLineList(symbol.toLowerCase() + "usdt", his, klineParam.getInterval());
                return his;
            }
            case "echo": {
                //后续加上控线逻辑
                Map<String, Object> parameters = new LinkedHashMap<>();
                SpotClient client = new SpotClientImpl();
                KlineSymbol one = klineSymbolService.getOne(new LambdaQueryWrapper<KlineSymbol>().eq(KlineSymbol::getSymbol, symbol.toLowerCase()));
                if (null == one) {
                    return null;
                }
                parameters.put("symbol", one.getReferCoin().toUpperCase() + "USDT");
                Interval interval = Interval.valueOf(klineParam.getInterval());
                parameters.put("interval", interval.toString());
                if (klineParam.getEnd() != null) {
                    parameters.put("endTime", klineParam.getEnd());
                }
                log.debug("参数" + JSONObject.toJSONString(parameters));
                String result = client.createMarket().klines(parameters);
                List<Kline> his = new ArrayList<>();
                JSONArray parse = JSONArray.parse(result);
                for (int i = 0; i < parse.size(); i++) {
                    JSONArray jsonObject = parse.getJSONArray(i);
                    Object[] array = jsonObject.toArray();
                    Kline kline = new Kline();
                    kline.setTimestamp((Long) array[0]);
                    kline.setOpen(Double.parseDouble((String) array[1]));
                    kline.setHigh(Double.parseDouble((String) array[2]));
                    kline.setLow(Double.parseDouble((String) array[3]));
                    kline.setClose(Double.parseDouble((String) array[4]));
                    kline.setVolume(Double.parseDouble((String) array[5]));
                    his.add(kline);
                }
                his = itOwnCoinService.selectLineList(one, his);
                his = botKlineModelInfoService.selectBotLineList(symbol.toLowerCase() + "usdt", his, klineParam.getInterval());
                return his;
            }
            case "energy": {
                Random random = new Random();
                double v = random.nextDouble();
                String url = "https://api-q.fx678img.com/histories.php?symbol="
                        + symbol.toUpperCase() + "&limit=" + 1000 + "&resolution=" + timeCode + "&codeType=5700&st=" + v;
                String result = HttpRequest.get(url)
                        .header("Referer", "https://quote.fx678.com/")
                        .header("Host", "api-q.fx678img.com")
                        .header("Origin", "https://quote.fx678.com")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .timeout(20000)
                        .execute().body();
                JSONObject ret = JSONObject.parseObject(result);
                List<Kline> klines = buildHisKline(ret);
                return botKlineModelInfoService.selectBotLineList(symbol.toLowerCase(), klines, klineParam.getInterval());
            }
            default: {
                Random random = new Random();
                double v = random.nextDouble();
                String url = "https://api-q.fx678img.com/histories.php?symbol=" + symbol.toUpperCase() + "&limit=" + 1000 + "&resolution=" + timeCode + "&codeType=8100&st=" + v;
                String result = HttpRequest.get(url)
                        .header("referer", "https://quote.fx678.com/")
                        .header("Origin", "https://quote.fx678.com")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .timeout(10000)
                        .execute().body();
                JSONObject ret = JSONObject.parseObject(result);
                List<Kline> klines = buildHisKline(ret);
                if (klines.isEmpty()) {
                    url = "https://api-q.fx678img.com/histories.php?symbol=" + symbol.toUpperCase() + "&limit=" + 1000 + "&resolution=" + timeCode + "&codeType=8200&st=" + v;
                    result = HttpRequest.get(url)
                            .header("referer", "https://quote.fx678.com/")
                            .header("Origin", "https://quote.fx678.com")
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                            .timeout(10000)
                            .execute().body();
                }
                ret = JSONObject.parseObject(result);
                klines = buildHisKline(ret);
                if (klines.isEmpty()) {
                    url = "https://api-q.fx678img.com/histories.php?symbol=" + symbol.toUpperCase() + "&limit=" + 1000 + "&resolution=" + timeCode + "&codeType=5c00&st=" + v;
                    result = HttpRequest.get(url)
                            .header("referer", "https://quote.fx678.com/")
                            .header("Origin", "https://quote.fx678.com")
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                            .timeout(10000)
                            .execute().body();
                }
                ret = JSONObject.parseObject(result);
                klines = buildHisKline(ret);
                return botKlineModelInfoService.selectBotLineList(symbol.toLowerCase(), klines, klineParam.getInterval());
            }
        }
        }catch (Exception e){
            log.info(e.toString());
        }
        return null;
    }

    public List<Kline> getHistoryKline2(KlineParamVO klineParam) {
        String market = StringUtils.isBlank(klineParam.getMarket()) ? "" : klineParam.getMarket();
        String timeCode = this.getMt5Time(klineParam.getInterval());
        String symbol = klineParam.getSymbol();
        switch (market) {
            case "binance": {
                //后续加上控线逻辑
                Map<String, Object> parameters = new LinkedHashMap<>();
                SpotClient client = new SpotClientImpl();
                parameters.put("symbol", symbol.toUpperCase() + "USDT");
                Interval interval = Interval.valueOf(klineParam.getInterval());
                parameters.put("interval", interval.toString());
                parameters.put("limit",50);
                if (klineParam.getEnd() != null) {
                    parameters.put("endTime", klineParam.getEnd());
                }
                log.debug("参数" + JSONObject.toJSONString(parameters));
                String result = client.createMarket().klines(parameters);
                List<Kline> his = new ArrayList<>();
                JSONArray parse = JSONArray.parse(result);
                for (int i = 0; i < parse.size(); i++) {
                    JSONArray jsonObject = parse.getJSONArray(i);
                    Object[] array = jsonObject.toArray();
                    Kline kline = new Kline();
                    kline.setTimestamp((Long) array[0]);
                    kline.setOpen(Double.parseDouble((String) array[1]));
                    kline.setHigh(Double.parseDouble((String) array[2]));
                    kline.setLow(Double.parseDouble((String) array[3]));
                    kline.setClose(Double.parseDouble((String) array[4]));
                    kline.setVolume(Double.parseDouble((String) array[5]));
                    his.add(kline);
                }
                his = botKlineModelInfoService.selectBotLineList(symbol.toLowerCase() + "usdt", his, klineParam.getInterval());

                return his;
            }
            case "huobi": {
                MarketAPIServiceImpl huobiAPIService = new MarketAPIServiceImpl();
                SwapMarketHistoryKlineRequest result = SwapMarketHistoryKlineRequest.builder()
                        .contractCode(symbol.toUpperCase() + "-USDT")//合约代码	"BTC-USDT" ...
                        .period(CandlestickIntervalEnum.getValue(klineParam.getInterval())) //K线类型	1min, 5min, 15min, 30min, 60min,4hour,1day,1week,1mon
                        .size(1000) //获取数量，默认150	[1,2000]
                        //.from() //开始时间戳 10位 单位S
                        //.to();//结束时间戳 10位 单位S
                        .build();
                List<Kline> his = new ArrayList<>();
                SwapMarketHistoryKlineResponse response = huobiAPIService.getSwapMarketHistoryKline(result);
                if ("ok".equalsIgnoreCase(response.getStatus())) {
                    List<SwapMarketHistoryKlineResponse.DataBean> list = response.getData();
                    for (SwapMarketHistoryKlineResponse.DataBean data : list) {
                        Kline kline = new Kline();
                        kline.setTimestamp(data.getId());
                        kline.setOpen(data.getOpen().doubleValue());
                        kline.setHigh(data.getHigh().doubleValue());
                        kline.setLow(data.getLow().doubleValue());
                        kline.setClose(data.getClose().doubleValue());
                        kline.setVolume(data.getVol().doubleValue());
                        his.add(kline);
                    }
                }
                his = botKlineModelInfoService.selectBotLineList(symbol.toLowerCase() + "usdt", his, klineParam.getInterval());
                return his;
            }
            case "echo": {
                //后续加上控线逻辑
                Map<String, Object> parameters = new LinkedHashMap<>();
                SpotClient client = new SpotClientImpl();
                KlineSymbol one = klineSymbolService.getOne(new LambdaQueryWrapper<KlineSymbol>().eq(KlineSymbol::getSymbol, symbol.toLowerCase()));
                if (null == one) {
                    return null;
                }
                parameters.put("symbol", one.getReferCoin().toUpperCase() + "USDT");
                Interval interval = Interval.valueOf(klineParam.getInterval());
                parameters.put("interval", interval.toString());
                if (klineParam.getEnd() != null) {
                    parameters.put("endTime", klineParam.getEnd());
                }
                log.debug("参数" + JSONObject.toJSONString(parameters));
                String result = client.createMarket().klines(parameters);
                List<Kline> his = new ArrayList<>();
                JSONArray parse = JSONArray.parse(result);
                for (int i = 0; i < parse.size(); i++) {
                    JSONArray jsonObject = parse.getJSONArray(i);
                    Object[] array = jsonObject.toArray();
                    Kline kline = new Kline();
                    kline.setTimestamp((Long) array[0]);
                    kline.setOpen(Double.parseDouble((String) array[1]));
                    kline.setHigh(Double.parseDouble((String) array[2]));
                    kline.setLow(Double.parseDouble((String) array[3]));
                    kline.setClose(Double.parseDouble((String) array[4]));
                    kline.setVolume(Double.parseDouble((String) array[5]));
                    his.add(kline);
                }
                his = itOwnCoinService.selectLineList(one, his);
                his = botKlineModelInfoService.selectBotLineList(symbol.toLowerCase() + "usdt", his, klineParam.getInterval());
                return his;
            }
            case "energy": {
                Random random = new Random();
                double v = random.nextDouble();
                String url = "https://api-q.fx678img.com/histories.php?symbol="
                        + symbol.toUpperCase() + "&limit=" + 1000 + "&resolution=" + timeCode + "&codeType=5700&st=" + v;
                String result = HttpRequest.get(url)
                        .header("Referer", "https://quote.fx678.com/")
                        .header("Host", "api-q.fx678img.com")
                        .header("Origin", "https://quote.fx678.com")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .timeout(20000)
                        .execute().body();
                JSONObject ret = JSONObject.parseObject(result);
                List<Kline> klines = buildHisKline(ret);
                return botKlineModelInfoService.selectBotLineList(symbol.toLowerCase(), klines, klineParam.getInterval());
            }
            default: {
                Random random = new Random();
                double v = random.nextDouble();
                String url = "https://api-q.fx678img.com/histories.php?symbol=" + symbol.toUpperCase() + "&limit=" + 1000 + "&resolution=" + timeCode + "&codeType=8100&st=" + v;
                String result = HttpRequest.get(url)
                        .header("referer", "https://quote.fx678.com/")
                        .header("Origin", "https://quote.fx678.com")
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                        .timeout(10000)
                        .execute().body();
                JSONObject ret = JSONObject.parseObject(result);
                List<Kline> klines = buildHisKline(ret);
                if (klines.isEmpty()) {
                    url = "https://api-q.fx678img.com/histories.php?symbol=" + symbol.toUpperCase() + "&limit=" + 1000 + "&resolution=" + timeCode + "&codeType=8200&st=" + v;
                    result = HttpRequest.get(url)
                            .header("referer", "https://quote.fx678.com/")
                            .header("Origin", "https://quote.fx678.com")
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                            .timeout(10000)
                            .execute().body();
                }
                ret = JSONObject.parseObject(result);
                klines = buildHisKline(ret);
                if (klines.isEmpty()) {
                    url = "https://api-q.fx678img.com/histories.php?symbol=" + symbol.toUpperCase() + "&limit=" + 1000 + "&resolution=" + timeCode + "&codeType=5c00&st=" + v;
                    result = HttpRequest.get(url)
                            .header("referer", "https://quote.fx678.com/")
                            .header("Origin", "https://quote.fx678.com")
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                            .timeout(10000)
                            .execute().body();
                }
                ret = JSONObject.parseObject(result);
                klines = buildHisKline(ret);
                return botKlineModelInfoService.selectBotLineList(symbol.toLowerCase(), klines, klineParam.getInterval());
            }
        }
    }

    private String getMt5Time(String time) {
        //["ONE_MIN","FIVE_MIN","FIFTEEN_MIN","THIRTY_MIN","ONE_HOUR","TWO_HOUR","SIX_HOUR","ONE_DAY","TWO_DAY","SEVEN_DAY"]
        switch (time) {
            case "ONE_MIN":
                return "1";
            case "FIVE_MIN":
                return "5";
            case "FIFTEEN_MIN":
                return "15";
            case "THIRTY_MIN":
                return "30";
            case "ONE_HOUR":
                return "60";
            case "ONE_DAY":
                return "D";
            case "SEVEN_DAY":
                return "W";
            default:
                return "";
        }
    }

    private List<Kline> buildHisKline(JSONObject ret) {
        List<Kline> klines = new ArrayList<>();
        String s = ret.getString("s");
        if ("ok".equals(s)) {
            JSONArray close = ret.getJSONArray("c");
            JSONArray high = ret.getJSONArray("h");
            JSONArray low = ret.getJSONArray("l");
            JSONArray open = ret.getJSONArray("o");
            JSONArray volume = ret.getJSONArray("v");
            JSONArray timestamp = ret.getJSONArray("t");
            Kline kline;
            for (int i = 0; i < close.size(); i++) {
                kline = new Kline();
                kline.setClose(close.getDouble(i));
                kline.setOpen(open.getDouble(i));
                kline.setHigh(high.getDouble(i));
                kline.setLow(low.getDouble(i));
                kline.setVolume(volume.getDouble(i));
                kline.setTimestamp((timestamp.getLong(i)) * 1000L);
                klines.add(kline);
            }
        }
        return klines;
    }

    public Ticker24hVO getHistoryKline24hrTicker(KlineParamVO klineParamVO) {
        String market = klineParamVO.getMarket();
        if("metal|mt5|energy".contains(market)){
            Ticker24hVO ticker24hVO = new Ticker24hVO();
            ticker24hVO.setSymbol(klineParamVO.getSymbol());
            BigDecimal cacheObject = redisCache.getCacheObject(CachePrefix.CURRENCY_PRICE.getPrefix() + klineParamVO.getSymbol());
            ticker24hVO.setHighPrice(cacheObject);
            ticker24hVO.setLowPrice(cacheObject);
            Random random = new Random();

            double randomValue = 0 + (1000 - 0) * random.nextDouble();
            ticker24hVO.setVolume(new BigDecimal(randomValue));
            return ticker24hVO;
        }
        Map<String, Object> parameters = new LinkedHashMap<>();
        SpotClient client = new SpotClientImpl();
        parameters.put("symbol", klineParamVO.getSymbol().toUpperCase() + "USDT");
        if("echo".equals(market)){
            KlineSymbol one = klineSymbolService.getOne(new LambdaQueryWrapper<KlineSymbol>().eq(KlineSymbol::getSymbol, klineParamVO.getSymbol().toLowerCase()));
            if(null == one){
                return null;
            }
            parameters.put("symbol", one.getReferCoin().toUpperCase() + "USDT");
        }
        String result = client.createMarket().ticker24H(parameters);
        Ticker24hVO ticker24hVO = JSON.parseObject(result, Ticker24hVO.class);
        return ticker24hVO;
    }

    public Ticker24hVO getHistoryKline24hrTicker2(KlineParamVO klineParamVO) {
        //遍历传来的market参数数组
        Object marketValue = klineParamVO.getMarkets();
        // 如果 marketValue 是字符串数组，遍历数组元素执行条件判断
        String[] marketArray = (String[]) marketValue;
        List<Kline> dataList = new ArrayList<>();
        Map<String, Object> parameters = new LinkedHashMap<>();
        SpotClient client = new SpotClientImpl();
        //循环遍历所有数据来执行以下操作
        for (String market : marketArray) {
            Object symbolValue = klineParamVO.getSymbols();
            // 如果 symbolValue 是字符串数组，遍历数组元素执行条件判断
            String[] symbolArray = (String[]) symbolValue;
            for (String symbol : symbolArray) {
                if (market.equals("metal") || market.equals("mt5")) {
                    Ticker24hVO ticker24hVO = new Ticker24hVO();

                    ticker24hVO.setSymbol(symbol);
                    BigDecimal cacheObject = redisCache.getCacheObject(CachePrefix.CURRENCY_PRICE.getPrefix() + symbol);
                    ticker24hVO.setHighPrice(cacheObject);
                    ticker24hVO.setLowPrice(cacheObject);
                    Random random = new Random();
                    double randomValue = 0 + (1000 - 0) * random.nextDouble();
                    ticker24hVO.setVolume(new BigDecimal(randomValue));
                    return ticker24hVO;
                }
                parameters.put("symbol", symbol.toUpperCase() + "USDT");
                if (market.equals("echo")) {
                    KlineSymbol one = klineSymbolService.getOne(new LambdaQueryWrapper<KlineSymbol>().eq(KlineSymbol::getSymbol, symbol.toLowerCase()));
                    if (null == one) {
                        return null;
                    }
                    parameters.put("symbol", one.getReferCoin().toUpperCase() + "USDT");
                }
            }
        }
        String result = client.createMarket().ticker24H(parameters);
        Ticker24hVO ticker24hVO = JSON.parseObject(result, Ticker24hVO.class);
        return ticker24hVO;
    }

    public List<Kline>  getConPriceMap(KlineParamVO klineParamVO,  List<Kline> historyKline){
        TBotKlineModel tBotKlineModel = new TBotKlineModel();
        tBotKlineModel.setSymbol(klineParamVO.getSymbol()+"usdt");
        tBotKlineModel.setModel(0L);
        List<TBotKlineModel> tBotKlineModels = botKlineModelService.selectTBotKlineModelList(tBotKlineModel);
        BigDecimal cc = new BigDecimal(0);
        int num = 0;
        for (TBotKlineModel tBotKlineModel1: tBotKlineModels ) {
            cc=cc.add( tBotKlineModel1.getConPrice());
            boolean isF = true;
            int a = 0;
            long time = tBotKlineModel1.getBeginTime().getTime();
            for (Kline kline: historyKline) {

                    if(kline.getTimestamp()>=time){
                        if(isF){
                            double c = kline.getClose() + tBotKlineModel1.getConPrice().doubleValue();
                            double h = kline.getHigh() + tBotKlineModel1.getConPrice().doubleValue();
                            double l = kline.getLow() + tBotKlineModel1.getConPrice().doubleValue();
                            kline.setClose(c);
                            if(h>kline.getHigh()){
                                kline.setHigh(h);
                            }
                            if(l<kline.getLow()){
                                kline.setLow(l);
                            }
                            isF=false;
                        }else {
                            double c = kline.getClose() + tBotKlineModel1.getConPrice().doubleValue();
                            double h = kline.getHigh() + tBotKlineModel1.getConPrice().doubleValue();
                            double l = kline.getLow() + tBotKlineModel1.getConPrice().doubleValue();
                            double o = kline.getOpen() + tBotKlineModel1.getConPrice().doubleValue();
                            kline.setClose(c);
                            kline.setHigh(h);
                            kline.setLow(l);
                            kline.setOpen(o);
                        }
                    }

                a++;
                if(a==historyKline.size()){
                    BigDecimal cacheObject = redisCache.getCacheObject(CachePrefix.CURRENCY_PRICE.getPrefix() + klineParamVO.getSymbol().replace("usdt", "").toLowerCase());
                     kline.setClose(cacheObject.doubleValue());
                }

            }

            num++;
        }
        return historyKline;
    }

    public List<Kline>  getConPriceMap2(KlineParamVO klineParamVO,  List<Kline> historyKline){
        TBotKlineModel tBotKlineModel = new TBotKlineModel();
        //遍历币种数组
        Object symbolValue = klineParamVO.getSymbols();
        // 如果 symbolValue 是字符串数组，遍历数组元素执行条件判断
        String[] symbolArray = (String[]) symbolValue;
        for (String symbol : symbolArray) {
            tBotKlineModel.setSymbol(symbol + "usdt");
            tBotKlineModel.setModel(0L);
            List<TBotKlineModel> tBotKlineModels = botKlineModelService.selectTBotKlineModelList(tBotKlineModel);
            BigDecimal cc = new BigDecimal(0);
            int num = 0;
            for (TBotKlineModel tBotKlineModel1 : tBotKlineModels) {
                cc = cc.add(tBotKlineModel1.getConPrice());
                boolean isF = true;
                int a = 0;
                long time = tBotKlineModel1.getBeginTime().getTime();
                for (Kline kline : historyKline) {

                    if (kline.getTimestamp() >= time) {
                        if (isF) {
                            double c = kline.getClose() + tBotKlineModel1.getConPrice().doubleValue();
                            double h = kline.getHigh() + tBotKlineModel1.getConPrice().doubleValue();
                            double l = kline.getLow() + tBotKlineModel1.getConPrice().doubleValue();
                            kline.setClose(c);
                            if (h > kline.getHigh()) {
                                kline.setHigh(h);
                            }
                            if (l < kline.getLow()) {
                                kline.setLow(l);
                            }
                            isF = false;
                        } else {
                            double c = kline.getClose() + tBotKlineModel1.getConPrice().doubleValue();
                            double h = kline.getHigh() + tBotKlineModel1.getConPrice().doubleValue();
                            double l = kline.getLow() + tBotKlineModel1.getConPrice().doubleValue();
                            double o = kline.getOpen() + tBotKlineModel1.getConPrice().doubleValue();
                            kline.setClose(c);
                            kline.setHigh(h);
                            kline.setLow(l);
                            kline.setOpen(o);
                        }
                    }

                    a++;
                    if (a == historyKline.size()) {
                        BigDecimal cacheObject = redisCache.getCacheObject(CachePrefix.CURRENCY_PRICE.getPrefix() + symbol.replace("usdt", "").toLowerCase());
                        kline.setClose(cacheObject.doubleValue());
                    }

                }

                num++;
            }
        }
        return historyKline;
    }
}
