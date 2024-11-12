package com.example.thtruemk2.service;

import com.example.thtruemk2.model.threquest;
import com.example.thtruemk2.model.thresponse1;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class MyService {

    private WebClient webClient;

    @Autowired
    public MyService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("https://quatangtopkid.thmilk.vn").build();
    }



    public String sendPostRequest(threquest requestObject) throws InterruptedException {

        String value = "Name="+requestObject.getName()+ "&" + "Phone=0" + requestObject.getPhone()+"&ProvinceCode=01"+"&Code=" + requestObject.getCode() ;
        try {

            String response = webClient.post()
                    .uri("Home/IndexAjax")
                    .header("Host", "quatangtopkid.thmilk.vn")
                    .header("accept", "*/*")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("user-agent", "Mozilla/5.0 (Linux; Android 9; SM-G977N Build/PQ3A.190605.003; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/91.0.4472.114 Mobile Safari/537.36")
                    .header("origin", "https://quatangtopkid.thmilk.vn")
                    .header("sec-fetch-site", "same-origin")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-fetch-dest", "empty")
                    .header("accept-encoding","gzip, deflate")
                    .header("accept-language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .bodyValue(value)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(jsonNode -> {
                        JsonNode Type = jsonNode.get("Prize");
                        return (Type != null) ? Type.asText() : "";
                    })
                    .block();

            return response;
        } catch (WebClientResponseException e) {

        }

        return "";
    }
    public String updatewinner(String token, thresponse1 requestObject) throws InterruptedException {
        String value="WinnerInfo.Name="+requestObject.getName()+"&WinnerInfo.Code="+requestObject.getCode()+"&WinnerInfo.Phone="+requestObject.getPhone()
                +"&WinnerInfo.NationalId="+requestObject.getCccd()
                +"&WinnerInfo.Province=01&WinnerInfo.District=005&WinnerInfo.Store=STORE145&__RequestVerificationToken="+token;
        try {

            String response = webClient.post()
                    .uri("/Home/UpdateWinnerInfo")
                    .header("Host", "quatangtopkid.thmilk.vn")
                    .header("cache-control", "max-age=0")
                    .header("upgrade-insecure-requests","1")
                    .header("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                    .header("x-requested-with", "com.android.browser")
                    .header("user-agent", "Mozilla/5.0 (Linux; Android 9; SM-G977N Build/PQ3A.190605.003; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/91.0.4472.114 Mobile Safari/537.36")
                    .header("origin", "https://quatangtopkid.thmilk.vn")
                    .header("sec-fetch-site", "same-origin")
                    .header("sec-fetch-mode", "navigate")
                    .header("sec-fetch-dest", "document")
                    .header("sec-fetch-user","?1")
                    .header("referer","https://quatangtopkid.thmilk.vn")
                    .header("accept-encoding","gzip, deflate")
                    .header("accept-language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("content-type", "application/x-www-form-urlencoded")
                    .bodyValue(value)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return "thanhcong";
        } catch (WebClientResponseException e) {

        }
        return "";
    }

}
