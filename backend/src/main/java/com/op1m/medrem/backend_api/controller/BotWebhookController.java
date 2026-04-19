package com.op1m.medrem.backend_api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.op1m.medrem.backend_api.entity.MedicineHistory;
import com.op1m.medrem.backend_api.entity.User;
import com.op1m.medrem.backend_api.service.MedicineHistoryService;
import com.op1m.medrem.backend_api.service.SseEmitterManager;
import com.op1m.medrem.backend_api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/bot")
public class BotWebhookController {

    @Autowired
    private UserService userService;

    @Autowired
    private MedicineHistoryService medicineHistoryService;

    @Autowired
    private SseEmitterManager emitterManager;

    @Value("${telegram.bot.token}")
    private String botToken;

    private final ObjectMapper mapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(@RequestBody String update) {
        try {
            JsonNode root = mapper.readTree(update);
            System.out.println("📩 Получен update от Telegram: " + update);

            if (root.has("message")) {
                handleMessage(root);
            }

            if (root.has("callback_query")) {
                handleCallbackQuery(root);
            }

            return ResponseEntity.ok().body("{\"ok\":true}");

        } catch (Exception e) {
            System.err.println("❌ Ошибка обработки вебхука: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok().body("{\"ok\":false, \"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleMessage(JsonNode root) {
        JsonNode message = root.get("message");
        JsonNode chat = message.get("chat");
        JsonNode from = message.get("from");

        Long realChatId = chat.get("id").asLong();
        Long telegramId = from.get("id").asLong();
        String username = from.has("username") ? from.get("username").asText() : null;
        String firstName = from.has("first_name") ? from.get("first_name").asText() : null;
        String lastName = from.has("last_name") ? from.get("last_name").asText() : null;

        System.out.println("✅ Реальный chat_id: " + realChatId);
        System.out.println("✅ Telegram ID: " + telegramId);

        User user = userService.findByTelegramId(telegramId);

        if (user == null) {
            user = new User();
            user.setTelegramId(telegramId);
            user.setUsername(username != null ? username : "tg_" + telegramId);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user = userService.save(user);
            System.out.println("✅ Создан новый пользователь из вебхука");
        }

        if (!realChatId.equals(user.getTelegramChatId())) {
            user.setTelegramChatId(realChatId);
            userService.update(user);
            System.out.println("✅ Обновлён chat_id для пользователя " + username);
            sendWelcomeMessage(realChatId, firstName);
        }
    }

    private void handleCallbackQuery(JsonNode root) {
        JsonNode callback = root.get("callback_query");
        String callbackData = callback.get("data").asText();
        String callbackId = callback.get("id").asText();
        JsonNode message = callback.get("message");
        Integer messageId = message.get("message_id").asInt();
        Long chatId = message.get("chat").get("id").asLong();

        System.out.println("🔄 Callback data: " + callbackData + " from chat " + chatId);

        answerCallbackQuery(callbackId, "⏳ Обрабатываю...");

        CompletableFuture.runAsync(() -> {
            try {
                if (callbackData.startsWith("take_")) {
                    handleTake(chatId, callbackData, messageId);
                } else if (callbackData.startsWith("postpone_")) {
                    handlePostpone(chatId, callbackData, messageId);
                } else if (callbackData.startsWith("skip_")) {
                    handleSkip(chatId, callbackData, messageId);
                }
            } catch (Exception e) {
                System.err.println("❌ Ошибка асинхронной обработки: " + e.getMessage());
                e.printStackTrace();
                sendErrorMessage(chatId);
            }
        });
    }

    private void handleTake(Long chatId, String callbackData, Integer messageId) {
        Long reminderId = Long.parseLong(callbackData.substring(5));

        System.out.println("✅ handleTake: reminderId=" + reminderId + ", chatId=" + chatId);

        medicineHistoryService.markReminderAsTakenByBot(reminderId, chatId);

        emitterManager.sendUpdate(chatId, "history-updated",
                Map.of("reminderId", reminderId, "action", "taken"));

        editMessageText(chatId, messageId, "✅ Вы приняли лекарство. Молодец!");
    }

    private void handlePostpone(Long chatId, String callbackData, Integer messageId) {
        String[] parts = callbackData.split("_");
        Long reminderId = Long.parseLong(parts[1]);
        int minutes = parts.length > 2 ? Integer.parseInt(parts[2]) : 10;

        System.out.println("⏰ handlePostpone: reminderId=" + reminderId +
            ", minutes=" + minutes + ", chatId=" + chatId);

        try {
            MedicineHistory postponed = medicineHistoryService.postponeReminder(reminderId, chatId, minutes);

            System.out.println("✅ Напоминание отложено: historyId=" + postponed.getId());

            emitterManager.sendUpdate(chatId, "history-updated",
                    Map.of("reminderId", reminderId, "action", "postponed", "newTime", postponed.getScheduledTime()));

            editMessagePostponed(chatId, messageId, reminderId, minutes);

        } catch (Exception e) {
            System.err.println("❌ Ошибка откладывания: " + e.getMessage());
            e.printStackTrace();
            sendErrorMessage(chatId);
        }
    }

    private void handleSkip(Long chatId, String callbackData, Integer messageId) {
        Long reminderId = Long.parseLong(callbackData.substring(5));

        System.out.println("❌ handleSkip: reminderId=" + reminderId + ", chatId=" + chatId);

        medicineHistoryService.markReminderAsSkippedByBot(reminderId, chatId);

        emitterManager.sendUpdate(chatId, "history-updated",
                Map.of("reminderId", reminderId, "action", "skipped"));

        editMessageText(chatId, messageId, "❌ Вы пропустили приём.");
    }

    private void answerCallbackQuery(String callbackId, String text) {
        String url = "https://api.telegram.org/bot" + botToken + "/answerCallbackQuery";
        Map<String, Object> body = new HashMap<>();
        body.put("callback_query_id", callbackId);
        body.put("text", text);
        body.put("show_alert", false);
        try {
            restTemplate.postForEntity(url, body, String.class);
        } catch (Exception e) {
            System.err.println("❌ Ошибка answerCallbackQuery: " + e.getMessage());
        }
    }

    private void editMessageText(Long chatId, Integer messageId, String newText) {
        String url = "https://api.telegram.org/bot" + botToken + "/editMessageText";
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("message_id", messageId);
        body.put("text", newText);
        try {
            restTemplate.postForEntity(url, body, String.class);
        } catch (Exception e) {
            System.err.println("❌ Ошибка editMessageText: " + e.getMessage());
        }
    }

    private void editMessagePostponed(Long chatId, Integer messageId, Long reminderId, int minutes) {
        String url = "https://api.telegram.org/bot" + botToken + "/editMessageText";

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("message_id", messageId);
        body.put("text", String.format(
                "⏰ Напоминание отложено на %d минут. Я напомню снова через %d минут.\n\n" +
                "А пока можешь отметить сейчас:",
                minutes, minutes
        ));

        Map<String, Object> replyMarkup = new HashMap<>();
        replyMarkup.put("inline_keyboard", new Object[][]{
                {
                    Map.of("text", "✅ Принять сейчас", "callback_data", "take_" + reminderId),
                    Map.of("text", "❌ Пропустить", "callback_data", "skip_" + reminderId)
                }
        });
        body.put("reply_markup", replyMarkup);

        try {
            restTemplate.postForEntity(url, body, String.class);
            System.out.println("✅ editMessagePostponed успешно");

            scheduleMessageExpiration(chatId, messageId, minutes);

        } catch (Exception e) {
            System.err.println("❌ Ошибка editMessagePostponed: " + e.getMessage());
        }
    }

    private void sendSuccessNotification(Long chatId, String message) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", message);
        body.put("parse_mode", "HTML");
        try {
            restTemplate.postForEntity(url, body, String.class);
        } catch (Exception e) {
            System.err.println("❌ Ошибка sendSuccessNotification: " + e.getMessage());
        }
    }

    private void sendErrorMessage(Long chatId) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", "❌ Произошла ошибка при обработке запроса. Пожалуйста, попробуйте ещё раз.");
        try {
            restTemplate.postForEntity(url, body, String.class);
        } catch (Exception e) {
            System.err.println("❌ Ошибка sendErrorMessage: " + e.getMessage());
        }
    }

    private void sendWelcomeMessage(Long chatId, String firstName) {
        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", String.format(
                "👋 Привет, %s!\n\n✅ Теперь я могу отправлять тебе уведомления о приёме лекарств.\n💊 Ты будешь получать напоминания вовремя!",
                firstName != null ? firstName : "друг"
        ));
        try {
            restTemplate.postForEntity(url, body, String.class);
        } catch (Exception e) {
            System.err.println("❌ Ошибка sendWelcomeMessage: " + e.getMessage());
        }
    }

    private void scheduleMessageExpiration(Long chatId, Integer messageId, int minutes) {
        new java.util.Timer().schedule(new java.util.TimerTask() {
        @Override
            public void run() {
                try {
                    String url = "https://api.telegram.org/bot" + botToken + "/editMessageText";
                    Map<String, Object> body = new HashMap<>();
                    body.put("chat_id", chatId);
                    body.put("message_id", messageId);
                    body.put("text", "❌ Вы пропустили приём. Следующее напоминание придёт по расписанию.");
                    body.put("reply_markup", null);

                    restTemplate.postForEntity(url, body, String.class);
                    System.out.println("✅ Сообщение заменено на пропуск через " + minutes + " минут");
                } catch (Exception e) {
                    System.err.println("❌ Ошибка замены сообщения: " + e.getMessage());
                }
            }
        }, minutes * 60 * 1000);
    }
}