package com.ruoyi.framework.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;


@Configuration
public class WebSocketConfigOne {

    /**
     * 这个bean会自动注册使用了@ServerEndpoint注解声明的对象
     * 没有的话会报404
     *
     * @return
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

}