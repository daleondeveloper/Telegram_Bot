import com.sun.org.apache.regexp.internal.RE;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class Sceond_bot extends TelegramLongPollingBot {
    public static final int BOT_START = 0;
    public static final int BOT_WAIT_FIO = 1;
    public static final int BOT_WAIT_FOR_NUMBER = 2;
    public static final int BOT_CHECK_FIO_AND_NUMBER = 3;

    String[] fioUser = new String[2];
    String numberUser ;

    int state  = 0;

    List<Update> idTogiveSite = new ArrayList<>();

    Thread administratorThread;
    Thread consoleThread;
    List<String> consoleCommandList = new ArrayList<>();

    DBConnection dbConnection;
    Statement statement;

    ResultSet resultSet;

    public Sceond_bot(){
        // Створюємо зєднання з базою даних
         dbConnection = new DBConnection();
         try{
             Statement statement = dbConnection.getConnection().createStatement();
             statement.execute("drop table Users");
             statement.execute("CREATE TABLE IF NOT EXISTS Users(idUsers INT(10) NOT NULL AUTO_INCREMENT COMMENT ,chatID VARCHAR(45) NOT NULL COMMENT ,firstname VARCHAR(45) NOT NULL COMMENT , lastname VARCHAR(45) NOT NULL COMMENT ,phonenumber VARCHAR(15) NOT NULL COMMENT ,checked VARCHAR(45) NOT NULL COMMENT ,sendLink INT(1) NOT NULL COMMENT , administrator INT(1) NOT NULL COMMENT ,PRIMARY KEY (idUsers)  COMMENT , UNIQUE INDEX idUsers_UNIQUE (idUsers ASC)  COMMENT )" );

         }catch (SQLException e){

         }
    }
    public void onUpdateReceived(final Update update) {
        ResultSet resultSet;
        if (update.hasMessage()) {
            // фіксуємо чат ід
            final String chatId = update.getMessage().getChatId().toString();

            try {
                //шукаємо користувача у базі даних по ід чату
                PreparedStatement preparedStatement = dbConnection.getConnection().prepareStatement("select * from users where chatID = ? ",ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
                preparedStatement.setString(1, chatId);

                resultSet = preparedStatement.executeQuery();
                //якщо ненайдено користувача в  базі даних запитуємо його дані для реєстрації
                if (!resultSet.next()) {
                    operationForNotRegisterUsers(update);
                }
                //повертаємося в початок списку найдених користувачів
                resultSet.beforeFirst();

                while (resultSet.next()) {
                        // якщо користувач не в режимі адміністратора виконуємо цю операцію
                        if (resultSet.getInt("administrator") == 0) {
                            operationForRegisterUser(update,resultSet);
                            continue;
                        }
                        //якщо користувач в режимі адміністратора виконуємо цю операцію
                        if (resultSet.getInt("administrator") == 1) {
                            operationForAdministrator(update,resultSet);
                            continue;
                        }
                    }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }
    //метод виконнаня дій для незареєстрованого користувача
    public void operationForNotRegisterUsers(Update update) throws SQLException{
        PreparedStatement preparedStatement = dbConnection.getConnection().prepareStatement("insert into users (chatID,firstname,lastname,phonenumber,checked,sendLink,administrator) values (?,?,?,?,'0','0','0')");
            if(update.hasMessage()) {
                if(update.getMessage().hasText()) {
                    String message = update.getMessage().getText();
            /*
            *
            */
                    //При запуску бота
                    if (state == BOT_START || message.equals("/start")) {
                        sendMsg(update.getMessage().getChatId().toString(), "Доброго дня");
                        state = BOT_WAIT_FIO;
                    }
            /*
            *
            */
                    //Бот очікує Імя і Фамілію
                    if (state == BOT_WAIT_FIO) {
                        String[] userMessageMasive = message.split(" ");
                        if (userMessageMasive.length == 2) {
                            for (int i = 0; i < 2; i++) {
                                fioUser[i] = userMessageMasive[i];
                            }
                            state = BOT_WAIT_FOR_NUMBER;
                        } else {
                            sendMsg(update.getMessage().getChatId().toString(), "Ведіть імя і фамілію");
                        }
                    }
            /*
            *
            */
                    //Бот очікує номер телефона
                    if (state == BOT_WAIT_FOR_NUMBER) {
                        boolean numberTrue = true;
                        String[] userMessageMasive = message.split("");
                        if (userMessageMasive.length == 13) {
                            if (!userMessageMasive[0].equals("+")) {
                                numberTrue = false;
                            }
                            for (int i = 1; i < userMessageMasive.length; i++) {
                                try {
                                    int j = Integer.parseInt(userMessageMasive[i]);
                                } catch (Exception e) {
                                    numberTrue = false;
                                }
                            }
                        } else {
                            numberTrue = false;
                        }
                        if (!numberTrue) {
                            sendMsg(update.getMessage().getChatId().toString(), "Ведіть номер: Наприклад \"+3806567891012\"");
                        } else {
                            sendMsg(update.getMessage().getChatId().toString(),"Очікуйте підтвердження ваших даних");
                            state = BOT_CHECK_FIO_AND_NUMBER;
                            numberUser = message;
                        }
                    }
                }
                //якщо користувач дав свої даані натискання кнопки
                if (update.getMessage().hasContact()) {
                    switch (state) {
                        case BOT_WAIT_FIO:
                            fioUser[0] = update.getMessage().getContact().getFirstName();
                            fioUser[1] = update.getMessage().getContact().getLastName();
                            state = BOT_WAIT_FOR_NUMBER;
                            sendMsg(update.getMessage().getChatId().toString(), "Ведіть номер: Наприклад \"+3806567891012\"");
                            break;
                        case BOT_WAIT_FOR_NUMBER:
                            sendMsg(update.getMessage().getChatId().toString(),"Очікуйте підтвердження ваших даних");
                            numberUser = update.getMessage().getContact().getPhoneNumber();
                            state = BOT_CHECK_FIO_AND_NUMBER;
                            break;
                    }
                }
            }
            //додаємо користувача в  бд
            if(fioUser[0] != null && fioUser[1]!=null && numberUser!=null) {
                preparedStatement.setString(1, update.getMessage().getChatId().toString());
                preparedStatement.setString(2, fioUser[0]);
                preparedStatement.setString(3, fioUser[1]);
                preparedStatement.setString(4, numberUser);
                preparedStatement.execute();
            }
    }
    //метод виконання дій для зареєстрованого користувача
    public void operationForRegisterUser(Update update, ResultSet resultSet) throws  SQLException{
        if(update.getMessage().hasText()){
            String text = update.getMessage().getText();
            //початкова команда вітається бот і говорить які команди доступні
            if(text.equals("/start")){
                sendMsg(update.getMessage().getChatId().toString(),"Доброго дня користувач \n" +
                        resultSet.getString("firstname") + "   " + resultSet.getString("lastname")+
                        "\nскористайтеся одною з команд /ad, /ch, /l");
            }
            //перетворює користувача у адмінстратора з правами перевірити дані або видалити користувача
            if(text.equals("/administrator") || text.equals("/ad")){
                    resultSet.updateInt(8, 1);
                    resultSet.updateRow();

            }
            //користувачу повідомляється чи його дані перевіренні
            if(text.equals("/checked")|| text.equals("/ch")){
                if(resultSet.getInt("checked") == 1){
                    sendMsg(update.getMessage().getChatId().toString(),"Дані провірені.\n Ви можете отримати силку.Пропишіть /link або /l");
                }else{
                    sendMsg(update.getMessage().getChatId().toString(),"Дані в черзі на провірку.");
                }
            }
            //користувачу надсилається силка якщо його дані перевіренні інакше користувача повідомляють що щось не так
            if(text.equals("/link")|| text.equals("/l")){
                if(resultSet.getInt("checked") == 1){
                    sendMsg(update.getMessage().getChatId().toString(),"https://golos.io/ru--programmirovanie/@kovatelj/telegramm-bot-na-java-chast-4");
                    resultSet.updateInt("sendLink",1);
                }else{
                    sendMsg(update.getMessage().getChatId().toString(), "Сталася помилка при надстлані силки\n" +
                            "Можливо акаунт ще у черзі на перевірку\n" +
                            "Щоб перевірити це пропишіть команду /checked або /ch ");
                }
            }
        }
    }
    //метол виконання дій для адміністратора
    public  void operationForAdministrator(Update update,ResultSet resultSet) throws SQLException{

        if(update.getMessage().hasText()){
            //фіксуєм повідомлеення і ід чату
            String text = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();
            String firstComman = text.split(" " )[0];

            //бот вітається і говорить список доступних команд
            if(text.equals("/start")){
                sendMsg(chatId,"Доброго дня адміністратор \n" +
                resultSet.getString("firstname") + "   " + resultSet.getString("lastname")+
                "\nскористайтеся одною з команд /us, /tNCU, /cU, /dU");
            }
            //перетворює адміністратора у звичайного користувача
            if(text.equals("/user") || text.equals("/us")){
                resultSet.updateInt("administrator",0);
                resultSet.updateRow();
            }
            // видає всіх не перевірених користувачів у вигляді ід у бд імя фамілія і номер
            if(text.equals("/takeNotChekedUsers") || text.equals("/tNCU")){
                PreparedStatement preparedStatement = dbConnection.getConnection().prepareStatement("select * from users where checked = 0",ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
                ResultSet usersResultSet = preparedStatement.executeQuery();
                while (usersResultSet.next()){
                    String name = usersResultSet.getString("firstname");
                    String family = usersResultSet.getString("lastname");
                    String phoneNumber = usersResultSet.getString("phoneNumber");
                    int idUsers = usersResultSet.getInt("idUsers" );

                    sendMsg(chatId,"id = " + idUsers + "\n"+ name + "  " + family +"\n " + phoneNumber);
                }
            }
            // ставить користувачів перевіреними за ід у бд
            if(firstComman.equals("/chekedUsers") || firstComman.equals("/cU")){
                String[] usersId = text.split(" ");
                PreparedStatement preparedStatement = dbConnection.getConnection().prepareStatement("select * from users where idUsers = ?",ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
                ResultSet usersResulrSet ;
                for(int i = 1; i < usersId.length;i++) {
                    try {
                        preparedStatement.setInt(1, Integer.parseInt(usersId[i]));
                        usersResulrSet = preparedStatement.executeQuery();
                        while (usersResulrSet.next()) {
                            usersResulrSet.updateInt("checked", 1);
                            usersResulrSet.updateRow();
                            sendMsg(usersResulrSet.getString("chatID"), "https://golos.io/ru--programmirovanie/@kovatelj/telegramm-bot-na-java-chast-4");
                            usersResulrSet.updateInt("sendLink", 1);
                            usersResulrSet.updateRow();
                        }
                    }catch (RuntimeException e){

                    }
                }
            }

            //видаляє користувача з бд по ід
            if(firstComman.equals("/deleteUsers") || firstComman.equals("/dU")){
                String[] usersId = text.split(" ");
                PreparedStatement preparedStatement = dbConnection.getConnection().prepareStatement("select * from users where idUsers = ?",ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
                ResultSet usersResulrSet ;
                for(int i = 1; i < usersId.length;i++) {
                    try {
                        preparedStatement.setInt(1, Integer.parseInt(usersId[i]));
                        usersResulrSet = preparedStatement.executeQuery();
                        while (usersResulrSet.next())
                            usersResulrSet.deleteRow();
                    }catch (RuntimeException e){

                    }
                }
            }
        }
    }


    public void sendMsg (String chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);
//        if (state == BOT_WAIT_FIO || state == BOT_WAIT_FOR_NUMBER) {
            setButtons(sendMessage);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
        }

    }

    public void setButtons(SendMessage sendMessage){

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardFirstRow = new KeyboardRow();

        if(state == BOT_WAIT_FOR_NUMBER || state == BOT_WAIT_FIO) {
            keyboardFirstRow.add(new KeyboardButton("Взяти дані цього акаунта").setRequestContact(true));
        }else
        keyboardFirstRow.add(new KeyboardButton("/start"));
        keyboard.add(keyboardFirstRow);


        replyKeyboardMarkup.setKeyboard(keyboard);


    }

    public String getBotUsername() {
        return "Daleon";
    }

    public String getBotToken() {
        return "594859384:AAEIBVKF-drHIq1y-CZsOSSiy7ZNeOtgVRU";
    }
}
