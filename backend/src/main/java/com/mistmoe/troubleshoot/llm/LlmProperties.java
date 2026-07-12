package com.mistmoe.troubleshoot.llm;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private boolean enabled = false;
    private String provider = "deepseek";
    private String apiUrl = "https://api.deepseek.com/chat/completions";
    private String apiKey = "";
    private String model = "deepseek-chat";
    private String qianfanAccessKey = "";
    private String qianfanSecretKey = "";
    private String qianfanTokenUrl = "https://aip.baidubce.com/oauth/2.0/token";
    private String qianfanChatUrl = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/{model}";
    private String qianfanModel = "ernie-3.5-8k";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getQianfanAccessKey() {
        return qianfanAccessKey;
    }

    public void setQianfanAccessKey(String qianfanAccessKey) {
        this.qianfanAccessKey = qianfanAccessKey;
    }

    public String getQianfanSecretKey() {
        return qianfanSecretKey;
    }

    public void setQianfanSecretKey(String qianfanSecretKey) {
        this.qianfanSecretKey = qianfanSecretKey;
    }

    public String getQianfanTokenUrl() {
        return qianfanTokenUrl;
    }

    public void setQianfanTokenUrl(String qianfanTokenUrl) {
        this.qianfanTokenUrl = qianfanTokenUrl;
    }

    public String getQianfanChatUrl() {
        return qianfanChatUrl;
    }

    public void setQianfanChatUrl(String qianfanChatUrl) {
        this.qianfanChatUrl = qianfanChatUrl;
    }

    public String getQianfanModel() {
        return qianfanModel;
    }

    public void setQianfanModel(String qianfanModel) {
        this.qianfanModel = qianfanModel;
    }
}
