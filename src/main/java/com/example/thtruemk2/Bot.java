package com.example.thtruemk2;

import com.example.thtruemk2.model.threquest;
import com.example.thtruemk2.model.thresponse1;
import com.example.thtruemk2.service.MyService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@Slf4j
public class Bot extends TelegramLongPollingBot {
     @Value("${telegram.bots.bot-token}")
     private String botToken;
     @Value("${telegram.bots.bot-username}")
     private String botUsername;
    @Autowired
    private MyService myService;
    @PostConstruct
    public void init() {
        log.info("Bot started!");
    }
    private String cccdtest; // Biến lưu CCCD sau khi người dùng nhập
    private boolean waitingForCCCD = false;
    private String inputSaved;
    private int tmp=0;
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String text = message.getText();
            if (waitingForCCCD) {
                cccdtest = text; // Lưu giá trị CCCD vào biến cccdtest
                sendTextMessage(message.getChatId(), "CCCD đã được lưu: " + cccdtest);
                waitingForCCCD = false; // Đặt lại waitingForCCCD để tiếp tục vòng lặp
                thresponse1 t1 = new thresponse1();
                try {
                    log.info(gettoken());
                    log.info(gettoken());
                    myService.updatewinner(gettoken(),t1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                naptien(message, inputSaved); // Gọi lại hàm nạp tiền để tiếp tục vòng lặp
            } else {
                try {
                    log.info(gettoken());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                inputSaved = text; // Lưu tin nhắn của người dùng để sử dụng lại nếu cần
                naptien(message, inputSaved);
            }
        }
    }
    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    @Override
    public String getBotUsername() {
        return "napthecaotu1_bot";
    }


    @Override
    public String getBotToken() {
        return "7824163715:AAFSWotEAHjge3rqRb1KnSSs8VZWdS561EY";
    }
    public void naptien(Message message, String input){
            String[] mangsdt = tachsdt(input);
            for (int i=tmp;i<mangsdt.length;i++) {
                String sdt = mangsdt[i];
                boolean check = true;
                int n=0;
                while (check) {
                    if (waitingForCCCD) {
                        sendTextMessage(message.getChatId(), "Vui lòng nhập CCCD để tiếp tục.");
                        return; // Dừng vòng lặp tạm thời và chờ người dùng nhập CCCD
                    }
                    String macode = getCodeFromFile();  // Lấy mã từ file luucode2
                    n++;
                    if (macode == null || macode.isEmpty()) {
                        sendTextMessage(message.getChatId(),"Không còn mã code nào trong file luucode2.");
                        check = false;
                        continue;
                    }

                    String name = generateRandomName();
                    threquest request = new threquest(name, macode, sdt);
                    try {
                        String code = myService.sendPostRequest(request);
                        switch (code) {
                            case "4":
                                sendTextMessage(message.getChatId(),"Đã nạp 10k vào số:"+sdt);
                                log.info(sdt);
                                check = false;
                                break;
                            case "3":
                                sendTextMessage(message.getChatId(),"Đã nạp 20k vào số:"+sdt);
                                log.info(sdt);
                                check = false;
                                break;
                            case "null":
                                sendTextMessage(message.getChatId(),sdt+"Khong trung:");
                                sendTextMessage(message.getChatId(),"nhap cccd:" );
                                tmp=i+1;
                                waitingForCCCD = true;
                                if(n>=5){
                                    check = false;
                                }
                                break;
                            case "1":
                                sendTextMessage(message.getChatId(),sdt+"Da trung Laptop voi ma:"+macode);
                                sendTextMessage(message.getChatId(),"nhap cccd:" );
                                saveCodeToFile(macode, "luucode3.txt");
                                tmp=i+1;
                                waitingForCCCD = true;
                                check = false;
                                break;
                            case "2":
                                sendTextMessage(message.getChatId(),sdt+"Da trung xe dap voi ma:"+macode);
                                saveCodeToFile(macode, "luucode3.txt");
                                sendTextMessage(message.getChatId(),"nhap cccd:" );
                                waitingForCCCD = true;
                                check = false;
                                break;
                            default:
                                sendTextMessage(message.getChatId(),"loi khong xac dinh");
                                check = false;
                                break;
                        }
                        deleteCodeFromFile("luucode2.txt", macode);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
    }
    // Hàm đọc mã từ file luucode2
    private String getCodeFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader("luucode2.txt"))) {
            return reader.readLine();  // Đọc dòng đầu tiên
        } catch (IOException e) {
            log.error("Lỗi khi đọc từ file luucode2: " + e.getMessage());
            return null;
        }
    }

    // Hàm xóa mã code khỏi file luucode2
    private void deleteCodeFromFile(String fileName, String codeToDelete) {
        Path inputFilePath = Path.of(fileName);
        Path tempFilePath = Path.of("tempfile.txt");

        try (BufferedReader reader = Files.newBufferedReader(inputFilePath);
             BufferedWriter writer = Files.newBufferedWriter(tempFilePath)) {

            String currentLine;
            while ((currentLine = reader.readLine()) != null) {
                if (!currentLine.equals(codeToDelete)) {
                    writer.write(currentLine);
                    writer.newLine();
                }
            }

            Files.deleteIfExists(inputFilePath); // Xóa file cũ
            Files.move(tempFilePath, inputFilePath, StandardCopyOption.REPLACE_EXISTING); // Đổi tên file tạm

        } catch (IOException e) {
            log.error("Lỗi khi xóa mã khỏi file: " + e.getMessage());
        }
    }

    // Hàm lưu mã code vào file luucode3
    private void saveCodeToFile(String macode, String fileName) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            writer.write(macode);
            writer.newLine();
            log.info("Mã đã được lưu vào file: " + fileName);
        } catch (IOException e) {
            log.error("Lỗi khi lưu vào file: " + e.getMessage());
        }
    }

    public static String[] tachsdt(String input) {
        return splitStringToArray(input);
    }

    public static String[] splitStringToArray(String input) {
        return input.split("\\s+"); // Tách bằng khoảng trắng
    }

    public static String generateRandomName() {
        String[] names = {"Hung", "Mai", "Lan", "Bich", "Nam", "Tuan", "Anh", "Hoa", "Minh", "Duy"};
        Random random = new Random();
        return names[random.nextInt(names.length)];
    }
    public String gettoken() throws IOException {
        Document doc = Jsoup.connect("https://quatangtopkid.thmilk.vn").get();
        Element inputElement = doc.selectFirst("input[name=__RequestVerificationToken]");
            String tokenValue = inputElement.attr("value");
        return tokenValue;
    }
    public thresponse1 generateRandomResponse() {
        List<thresponse1> thResponseList = new ArrayList<>();

        thResponseList.add(new thresponse1("Name1", "Code1", "Phone1", "CCCD1"));
        thResponseList.add(new thresponse1("Name2", "Code2", "Phone2", "CCCD2"));
        thResponseList.add(new thresponse1("Name3", "Code3", "Phone3", "CCCD3"));
        thResponseList.add(new thresponse1("Name4", "Code4", "Phone4", "CCCD4"));
        Random random = new Random();
        int randomIndex = random.nextInt(thResponseList.size());
        return thResponseList.get(randomIndex);
    }
}