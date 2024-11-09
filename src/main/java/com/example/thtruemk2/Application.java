package com.example.thtruemk2;

import com.example.thtruemk2.model.threquest;
import com.example.thtruemk2.service.MyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@Slf4j
public class Application implements CommandLineRunner {

    @Autowired
    private MyService myService;  // Inject MyService từ Spring Context

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);  // Khởi chạy Spring Boot
    }

    @Override
    public void run(String... args) {
        // Tạo một ExecutorService với 10 luồng
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        // Khởi chạy 10 luồng thực hiện tác vụ
        for (int i = 0; i < 5; i++) {
            executorService.submit(() -> {
                while (true) { // Thực hiện vòng lặp
                    try {
                        String randomPhone = generateRandomPhoneNumber();
                        String randomString = generateRandomString();
                        threquest request = new threquest(randomString, randomPhone);
                        String code = myService.sendPostRequest(request);
                        log.info(randomString);
                        log.info(code);
                        // Kiểm tra code và lưu chuỗi vào file nếu điều kiện thỏa mãn
                        if (code.equals("success")) {
                            saveStringToFile(randomString, "luucode2.txt");
                        }
                    } catch (WebClientResponseException e) {
                        // Kiểm tra lỗi 403
                        if (e.getStatusCode().value() == 429) {
                            System.err.println("Lỗi 429: Tạm dừng 3 giây trước khi tiếp tục...");
                            try {
                                Thread.sleep(3000); // Dừng 3 giây nếu gặp lỗi 403
                            } catch (InterruptedException ex) {
                                Thread.currentThread().interrupt(); // Khôi phục trạng thái interrupt
                                throw new RuntimeException(ex);
                            }
                        } else {
                            System.err.println("Lỗi trong vòng lặp: " + e.getMessage());
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi trong vòng lặp: " + e.getMessage());
                    }
                }
            });
        }

        // Tùy vào nhu cầu của bạn có thể gọi executorService.shutdown() khi không cần chạy nữa
    }


    public static String generateRandomPhoneNumber() {
        String[] phonePrefixes = {"090", "091", "092", "093", "094", "095", "096", "097", "098", "099"};
        Random random = new Random();
        int index = random.nextInt(phonePrefixes.length);
        String prefix = phonePrefixes[index];
        int numberPart = random.nextInt(1000000, 10000000);
        return prefix + numberPart;
    }

    public static String generateRandomString() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder result = new StringBuilder(9);
        String prefix = random.nextBoolean() ? "TY4" : "YE4";
        result.append(prefix);
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
}
