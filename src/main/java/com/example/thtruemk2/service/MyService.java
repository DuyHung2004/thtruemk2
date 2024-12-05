package com.example.thtruemk2.service;

import com.example.thtruemk2.model.threquest;
import com.fasterxml.jackson.databind.JsonNode;
import io.netty.handler.timeout.TimeoutException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class MyService {
    private static final int REQUEST_LIMIT = 10;
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private List<String[]> proxyList = new ArrayList<>();
    private AtomicInteger proxyIndex = new AtomicInteger(0);
    public List<String> cookies;
    public String requestVerificationToken;
    private WebClient webClient;
    @Autowired
    public MyService(WebClient.Builder webClientBuilder) {
        webClient = webClientBuilder.baseUrl("https://quatangtopkid.thmilk.vn").build();

        // Gửi yêu cầu HTTP và lấy phản hồi
        ResponseEntity<String> response = webClient.get()
                .uri("/")
                .retrieve()
                .toEntity(String.class)
                .block();

        // Lấy cookie và token từ phản hồi
        cookies = response.getHeaders().get("Set-Cookie");
        String htmlContent = response.getBody();
        Document document = Jsoup.parse(htmlContent);
        Element tokenInput = document.selectFirst("input[name=__RequestVerificationToken]");
        requestVerificationToken = tokenInput.attr("value");

    }


//    @PostConstruct
//    private void init() {
//        loadProxies("fileproxy.txt");
//        configureWebClient();
//    }

    public void loadProxies(String filename) {
        proxyList.clear();
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
                        );

        webClient = WebClient.builder()
                .baseUrl("https://quatangtopkid.thmilk.vn")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
    public void configureWebClient2(String proxy) {
        try {
            // Tách proxy thành các phần: host, port, username, password
            String[] parts = proxy.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            // Cấu hình HttpClient với proxy
            HttpClient httpClient = HttpClient.create()
                    .proxy(proxyOptions -> proxyOptions.type(ProxyProvider.Proxy.HTTP)
                            .address(new InetSocketAddress(host, port))
                            );

            // Tạo WebClient với HttpClient đã cấu hình
            webClient = WebClient.builder()
                    .baseUrl("https://quatangtopkid.thmilk.vn")
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();

            log.info("WebClient đã được cấu hình với proxy: {}", proxy);
        } catch (Exception e) {
            log.error("Lỗi khi cấu hình proxy: {}", e.getMessage(), e);
        }
    }

    public void switchProxyIfNeeded(int i) {
        // Đảm bảo rằng chỉ số i nằm trong giới hạn hợp lệ
        if (i >= 0 && i < proxyList.size()) {
            proxyIndex.set(i); // Cập nhật proxyIndex thành giá trị i
            configureWebClient(); // Cấu hình lại WebClient với proxy mới
            log.info("Đã chuyển sang proxy thứ {}: {}:{}", i, proxyList.get(i)[0], proxyList.get(i)[1]);
        } else {
            log.warn("Chỉ số proxy không hợp lệ: {}. Không thay đổi proxy.", i);
        }
    }


    public String sendPostRequest(threquest requestObject) throws InterruptedException {
        String value = "Code=" + requestObject.getCode() + "&" + "Phone=" + requestObject.getPhone();
        String firstCookie = cookies.get(0).split(";")[0];
        try {
            String response = webClient.post()
                    .uri("/Home/CheckCode")
                    .header("Host", "quatangtopkid.thmilk.vn")
                    .header("content-length","31")
                    .header("accept", "*/*")
                    .header("requestverificationtoken",requestVerificationToken)
                    .header("user-agent", "Mozilla/5.0 (Linux; Android 9; SM-G975N Build/PQ3B.190801.03251549; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/91.0.4472.114 Mobile Safari/537.36")
                    .header("origin", "https://quatangtopkid.thmilk.vn")
                    .header("sec-fetch-site", "same-origin")
                    .header("sec-ch-ua-platform","\"Android\"")
                    .header("sec-ch-ua","\"Chromium\";v=\"130\", \"Google Chrome\";v=\"130\", \"Not?A_Brand\";v=\"99\"")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-ch-ua-mobile","?1")
                    .header("sec-fetch-dest", "empty")
                    .header("Cookie", firstCookie)
                    .header("referer", "https://quatangtopkid.thmilk.vn/")
                    .header("accept-language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("content-type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .bodyValue(value)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .timeout(Duration.ofSeconds(5))
                    .map(jsonNode -> {
                        JsonNode Type = jsonNode.get("Type");
                        return (Type != null) ? Type.asText() : "";
                    })
                    .block();
            return response;
        }catch (TimeoutException e) {
            log.error("Quá thời gian chờ phản hồi từ server sau 10 giây.");
            return "";
        }
        catch (WebClientResponseException e) {
            // Kiểm tra lỗi 429
            if (e.getStatusCode().value() == 429) {
                String[] currentProxy = proxyList.get(proxyIndex.get());
                log.error("Lỗi 429 từ proxy {}:{} - Tạm dừng trước khi chuyển tiếp...", currentProxy[0], currentProxy[1]);
                return "";
            }
        }
        return "";
    }


}
