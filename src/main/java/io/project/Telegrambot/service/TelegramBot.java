package io.project.Telegrambot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.vdurmont.emoji.EmojiParser;
import io.project.Telegrambot.config.BotConfig;
import io.project.Telegrambot.model.Joke;
import io.project.Telegrambot.model.JokeRepository;
import io.project.Telegrambot.model.User;
import io.project.Telegrambot.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.sql.Timestamp;
import java.util.*;

@Slf4j
@Component // аннотация автоматически создает экземпляр спрингу
public class TelegramBot extends TelegramLongPollingBot { //расширение обязательно для общения с телеграм бэком

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JokeRepository jokeRepository;
    final BotConfig config;

    static final String HELP_TEXT = """
            This bot is created to send a random joke from the database each time you request it.

            You can execute commands from the main menu on the left or by typing commands manually:

            Type /start to see a welcome message
            Type /joke to get a random joke
            Type /help to see this message again""";

    static final int MAX_JOKE_ID_MINUS_ONE = 3772;
    static final String NEXT_JOKE = " NEXT_JOKE";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/joke", "get a random joke"));
        listOfCommands.add(new BotCommand("/help", "info how to use this bot"));
//        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        try{
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        }
        catch (TelegramApiException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if(messageText.contains("/send") && config.getOwnerId() == chatId) {
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                var users = userRepository.findAll();
                for (User user: users) {
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }
            }
            else {

                switch (messageText) {
                    case "/start" -> {
                        registerUser(update.getMessage());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        try {
                            ObjectMapper objectMapper = new ObjectMapper();
                            TypeFactory typeFactory = objectMapper.getTypeFactory();
                            List<Joke> jokeList = objectMapper.readValue(new File("db/stupidstuff.json"),
                                    typeFactory.constructCollectionType(List.class, Joke.class));
                            jokeRepository.saveAll(jokeList);
                        } catch (Exception e) {
                            log.error(Arrays.toString(e.getStackTrace()));
                        }
                    }
                    case "/joke" -> {
                        var joke = getRandomJoke();
                        joke.ifPresent(randomJoke -> addButtonAndSendMessage(randomJoke.getBody(), chatId));
                    }
                    case "/help" ->
                        prepareAndSendMessage(chatId, HELP_TEXT);
                    default ->commandNotFound(chatId);
                }
            }
        }
        else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.equals(NEXT_JOKE)) {
                var joke = getRandomJoke();
                joke.ifPresent(randomJoke -> addButtonAndSendMessage(randomJoke.getBody(), chatId));
//                joke.ifPresent(randomJoke -> addButtonAndEditText(randomJoke.getBody(), chatId,
//                        update.getCallbackQuery().getMessage().getMessageId()));
            }
        }
    }

    private Optional<Joke> getRandomJoke() {
        var r = new Random();
        var randomId = r.nextInt(MAX_JOKE_ID_MINUS_ONE) + 1;
        return jokeRepository.findById(randomId);
    }

    private void addButtonAndSendMessage(String joke, Long chatId) {
        SendMessage message = new SendMessage();
        message.setText(joke);
        message.setChatId(chatId);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        var inlinekeyboardButton = new InlineKeyboardButton();
        inlinekeyboardButton.setCallbackData(NEXT_JOKE);
        inlinekeyboardButton.setText(EmojiParser.parseToUnicode("Next joke " + ":rolling_on_the_floor_laughing:"));
        rowInline.add(inlinekeyboardButton);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        executeMessage(message);
    }

    private void addButtonAndEditText(String joke, Long chatId, Integer msgId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(chatId);
        message.setText(joke);
        message.setMessageId(msgId);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        var inlinekeyboardButton = new InlineKeyboardButton();
        inlinekeyboardButton.setCallbackData(NEXT_JOKE);
        inlinekeyboardButton.setText(EmojiParser.parseToUnicode("next joke " + ":rolling_on_the_floor_laughing:"));
        rowInline.add(inlinekeyboardButton);
        rowsInline.add(rowInline);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        sendEditMessageText(message);
    }

    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setBio(chat.getBio());
            user.setDescription(chat.getDescription());
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Hi, " + name + "!" + ":blush:");
        sendMessage(chatId, answer);
    }

    private void commandNotFound(long chatId) {
        String answer = EmojiParser.parseToUnicode(
                "Command not recognized, please verify and try again :stuck_out_tongue_winking_eye: ");
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message); // Sending our message object to user
        }
        catch (TelegramApiException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }
    }

    private void sendEditMessageText(EditMessageText message) {
        try {
            execute(message);
        }
        catch (TelegramApiException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }
}