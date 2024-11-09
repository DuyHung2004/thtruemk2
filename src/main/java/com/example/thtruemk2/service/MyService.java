package com.example.thtruemk2.service;

import com.example.thtruemk2.model.threquest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
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
    private static final int REQUEST_LIMIT = 15;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final List<String[]> proxyList = new ArrayList<>();
    private AtomicInteger proxyIndex = new AtomicInteger(0);

    private WebClient webClient;

    @PostConstruct
    private void init() {
        loadProxies("fileproxy.txt");
        configureWebClient();
    }

    private void loadProxies(String filename) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                proxyList.add(line.split(":"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configureWebClient() {
        String[] currentProxy = proxyList.get(proxyIndex.get());
        HttpClient httpClient = HttpClient.create()
                .proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
                        .address(new InetSocketAddress(currentProxy[0], Integer.parseInt(currentProxy[1])))
                        .username(currentProxy[2])
                        .password(pass -> currentProxy[3]));

        webClient = WebClient.builder()
                .baseUrl("https://quatangtopkid.thmilk.vn")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private void switchProxyIfNeeded() {
        if (requestCount.incrementAndGet() >= REQUEST_LIMIT) {
            proxyIndex.updateAndGet(i -> (i + 1) % proxyList.size());
            requestCount.set(0);
            configureWebClient();
        }
    }

    public String sendPostRequest(threquest requestObject) throws InterruptedException {
        switchProxyIfNeeded();
        String value = "Code=" + requestObject.getCode() + "&" + "Phone=" + requestObject.getPhone();
        try {
            Thread.sleep(50);
            String response = webClient.post()
                    .uri("/Home/CheckCode")
                    .header("Host", "quatangtopkid.thmilk.vn")
                    .header("accept", "*/*")
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("content-length","31")
                    .header("user-agent", "Mozilla/5.0 (Linux; Android 9; SM-G975N Build/PQ3B.190801.03251549; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/91.0.4472.114 Mobile Safari/537.36")
                    .header("origin", "https://quatangtopkid.thmilk.vn")
                    .header("sec-fetch-site", "same-origin")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-fetch-dest", "empty")
                    .header("referer", "https://quatangtopkid.thmilk.vn/")
                    .header("accept-language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .bodyValue(value)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .map(jsonNode -> {
                        JsonNode Type = jsonNode.get("Type");
                        return (Type != null) ? Type.asText() : "";
                    })
                    .block();

            return response;
        } catch (WebClientResponseException e) {
            // Kiểm tra lỗi 429
            if (e.getStatusCode().value() == 429) {
                String[] currentProxy = proxyList.get(proxyIndex.get());
                log.error("Lỗi 429 từ proxy {}:{} - Tạm dừng trước khi chuyển tiếp...", currentProxy[0], currentProxy[1]);
                proxyIndex.updateAndGet(i -> (i + 1) % proxyList.size());  // Chuyển sang proxy tiếp theo
                configureWebClient();
            } else {
                log.error("Lỗi trong sendPostRequest: " + e.getMessage());
            }
        }

        return "";
    }


}
