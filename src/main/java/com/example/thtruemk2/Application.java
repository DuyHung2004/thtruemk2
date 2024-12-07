package com.example.thtruemk2;

import com.example.thtruemk2.model.threquest;
import com.example.thtruemk2.service.MyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@SpringBootApplication
@Slf4j
public class Application implements CommandLineRunner {
    private final List<String> proxyList = new ArrayList<>();
    @Autowired
    private MyService myService;  // Inject MyService từ Spring Context

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);  // Khởi chạy Spring Boot
    }

    @Override
    public void run(String... args) throws InterruptedException {
        // Tải proxy trước khi bắt đầu chạy các luồng

        while (true) { // Vòng lặp để chạy lại 10 luồng
            getproxy();
            AtomicBoolean firstThreadCompleted = new AtomicBoolean(false);
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            // Tạo một ExecutorService với 10 luồng
            ExecutorService executorService = Executors.newFixedThreadPool(20);
            CountDownLatch latch = new CountDownLatch(20); // Đảm bảo đợi tất cả luồng hoàn thành

            // Khởi chạy 10 luồng thực hiện tác vụ
            for (int i = 0; i < 20; i++) {
                int finalI = i;
                executorService.submit(() -> {
                    try {
                        int count = 0;
                        int dem = 0;
                        while (count <= 240) { // Thực hiện vòng lặp
                            try {
                                String randomPhone = generateRandomPhoneNumber();
                                String randomString = generateRandomString();
                                threquest request = new threquest(randomString, randomPhone);

                                myService.configureWebClient2(proxyList.get(20 * dem + finalI));
                                String code = myService.sendPostRequest(request);
                                if (code.isEmpty()) {
                                    code = "khong co gia tri";
                                }
                                log.info(randomString);
                                log.info(code);
                                // Kiểm tra code và lưu chuỗi vào file nếu điều kiện thỏa mãn
                                if (code.equals("success")) {
                                    saveStringToFile(randomString, "luucode2.txt");
                                }
                            } catch (WebClientResponseException e) {
                                log.error("Lỗi trong khi gửi request: {}, Status Code: {}", e.getMessage(), e.getStatusCode().value());
                                // Kiểm tra lỗi 403
                            } catch (Exception e) {
                                log.error("Lỗi trong khi gửi request: {}", e.getMessage());
                            }

                            count++;
                            dem++;
                            if (dem >= 12) {
                                dem = 0;
                            }
                            Thread.sleep(200); // Thời gian chờ giữa các lần gửi request
                        }
                        if (firstThreadCompleted.compareAndSet(false, true)) {
                            log.info("Luồng {} là luồng đầu tiên hoàn thành!", finalI);
                            scheduler.schedule(() -> {
                                log.info("10 giây đã trôi qua. Dừng tất cả luồng còn lại!");
                                executorService.shutdownNow(); // Dừng tất cả luồng
                            }, 5, TimeUnit.SECONDS);
                        }
                    } catch (InterruptedException e) {
                        log.error("Lỗi trong quá trình thực thi luồng: {}", e.getMessage());
                    } finally {
                        latch.countDown(); // Giảm latch khi luồng hoàn thành
                    }
                });
            }

            // Chờ tất cả 10 luồng hoàn thành
            try {
                latch.await(); // Chờ cho tới khi tất cả luồng hoàn thành
                log.info("Tất cả luồng đã hoàn thành hoặc bị dừng!");
            } catch (InterruptedException e) {
                log.error("Quá trình đợi luồng bị gián đoạn: {}", e.getMessage());
            } finally {
                executorService.shutdown(); // Đảm bảo tắt ExecutorService
                scheduler.shutdown(); // Đảm bảo tắt Scheduler
            }
        }
    }

    private static void clearFileContent(String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false))) {
            writer.write(""); // Ghi chuỗi rỗng để xóa nội dung
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static String extractProxyFromResponse(String response) {
        try {
            // Sử dụng Jackson hoặc thư viện tương tự để parse JSON
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode root = objectMapper.readTree(response);
            return root.path("data").path("proxy").asText(); // Truy cập trường "data.proxy"
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Trả về null nếu có lỗi
        }
    }

    private static List<String> readKeysFromFile(String fileName) {
        List<String> keys = new ArrayList<>(); // Danh sách lưu các key

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                keys.add(line.trim()); // Thêm key vào danh sách (bỏ khoảng trắng dư thừa)
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null; // Trả về null nếu xảy ra lỗi
        }

        return keys;
    }

    public static String generateRandomPhoneNumber() {
        String[] phonePrefixes = {"038","097","036","033","096"};
        Random random = new Random();
        int index = random.nextInt(phonePrefixes.length);
        String prefix = phonePrefixes[index];
        int numberPart = random.nextInt(1000000, 10000000);
        return prefix + numberPart;
    }

    public static String generateRandomString() {
        String chars = "ACDEFHKMNOPRTUWXY23479";
        Random random = new Random();
        StringBuilder result = new StringBuilder(9);
        result.append("YE4");
        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(chars.length());
            result.append(chars.charAt(index));
        }

        return result.toString();
    }

    public static void saveStringToFile(String randomString, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            writer.write(randomString);
            writer.newLine();
            System.out.println("Chuỗi đã được lưu vào file: " + fileName);
        } catch (IOException e) {
            System.err.println("Lỗi khi lưu vào file: " + e.getMessage());
        }
    }
    public void getproxy() throws InterruptedException {
        String fileName = "keyproxy.txt";
        List<String> keyList = readKeysFromFile(fileName);
        clearFileContent("fileproxy.txt");

        WebClient webClient2 = WebClient.builder().build();
        ExecutorService executor = Executors.newFixedThreadPool(40); // Giới hạn 40 luồng
        CountDownLatch latch = new CountDownLatch(40); // Đếm ngược 40 tác vụ
        for(int k=0;k<=5;k++){
            int h=k;
        for (int i = 0; i < 40; i++) {
            int j = i;
            executor.submit(() -> {
                try {
                    while (true) {
                        try {
                            log.info(keyList.get(j));
                            String apiUrl = "https://wwproxy.com/api/client/proxy/available?key=" + keyList.get(j+40*h) + "&provinceId=-1";

                            // Gửi yêu cầu GET
                            String proxy = webClient2.get()
                                    .uri(apiUrl)
                                    .retrieve()
                                    .bodyToMono(String.class)
                                    .timeout(Duration.ofSeconds(20)) // Đọc response dưới dạng String
                                    .map(Application::extractProxyFromResponse)  // Trích xuất proxy từ phản hồi
                                    .block(); // Lấy kết quả đồng bộ

                            // Nếu proxy hợp lệ thì lưu vào file và thoát vòng lặp
                            if (!proxy.equals("null") && !proxy.isEmpty()) {
                                saveStringToFile(proxy, "fileproxy.txt");
                                break;
                            }
                        } catch (Exception e) {
                            log.error("Lỗi khi lấy proxy cho key: " + keyList.get(j), e);
                            try {
                                Thread.sleep(1000); // Nghỉ 1 giây trước khi thử lại
                            } catch (InterruptedException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }
                } finally {
                    latch.countDown(); // Giảm latch khi hoàn thành
                }
            });
        } Thread.sleep(2000);
        }

        // Chờ tất cả các tác vụ hoàn thành
        try {
            latch.await(); // Đợi tất cả các luồng hoàn thành
            log.info("Hoàn tất tải proxy.");
        } catch (InterruptedException e) {
            log.error("Lỗi khi chờ tải proxy hoàn thành: {}", e.getMessage());
        }

        // Đảm bảo tắt ExecutorService
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("Lỗi khi tắt ExecutorService: {}", e.getMessage());
        }

        loadProxies("fileproxy.txt"); // Tải proxy vào proxyList
    }

    public void loadProxies(String filename) {
        proxyList.clear();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                proxyList.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
