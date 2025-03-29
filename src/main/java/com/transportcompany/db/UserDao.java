package com.transportcompany.db;

import com.transportcompany.exception.TrainValidationException;
import org.apache.log4j.Logger;
import org.mindrot.jbcrypt.BCrypt; // Библиотека для хэширования паролей

import java.sql.*;

/**
 * <p>Data Access Object (DAO) для работы с данными пользователей (`Users`) в базе данных H2.</p>
 * <p>Отвечает за операции, связанные с пользователями: регистрация нового пользователя
 * и проверка учетных данных при входе (аутентификация).</p>
 * <p>Использует ту же базу данных (`train_db`), что и {@link TrainDao}.
 * Таблица `Users` создается при инициализации {@link TrainDao}.</p>
 *
 * <p><b>Ключевые обязанности:</b></p>
 * <ul>
 *     <li>Регистрация пользователя: проверка уникальности имени, хэширование пароля с помощью BCrypt, вставка записи в таблицу `Users`.</li>
 *     <li>Аутентификация пользователя: поиск пользователя по имени, сравнение предоставленного пароля с хэшированным паролем из БД с помощью BCrypt.</li>
 *     <li>Использование JDBC для взаимодействия с таблицей `Users`.</li>
 *     <li>Обработка {@link SQLException} и их преобразование в {@link TrainValidationException}.</li>
 *     <li>Предоставление внутреннего статического класса {@link User} для представления данных пользователя.</li>
 * </ul>
 *
 * <p><b>Безопасность:</b></p>
 * <ul>
 *     <li>Пароли хранятся в БД не в открытом виде, а как хэши, сгенерированные BCrypt.</li>
 *     <li>BCrypt автоматически включает соль в генерируемый хэш, что защищает от радужных таблиц.</li>
 *     <li>Для проверки пароля используется метод {@code BCrypt.checkpw()}, который извлекает соль из хэша и выполняет сравнение.</li>
 * </ul>
 *
 * @see TrainDao DAO для поездов, инициализирует БД и таблицу Users.
 * @see User Внутренний класс для представления пользователя.
 * @see TrainValidationException Исключение для ошибок регистрации/входа.
 * @see BCrypt Утилита для хэширования паролей.
 */
public class UserDao {
    // Логгер для событий, связанных с операциями над пользователями
    private static final Logger logger = Logger.getLogger(UserDao.class);

    // Параметры подключения к той же БД, что и TrainDao
    private static final String DB_URL = "jdbc:h2:tcp://localhost:9092/./train_db";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";

    // --- SQL Запросы ---
    /** SQL для вставки нового пользователя (имя, хэш пароля, роль). */
    private static final String INSERT_USER = "INSERT INTO Users (username, password, role) VALUES (?, ?, ?)";
    /** SQL для поиска пользователя по его уникальному имени. */
    private static final String SELECT_USER_BY_USERNAME = "SELECT id, username, password, role FROM Users WHERE username = ?";

    /**
     * <p>Конструктор UserDao.</p>
     * <p>В текущей реализации конструктор выполняет только опциональную проверку соединения с БД
     * (для отладки) и логгирует факт создания экземпляра.</p>
     * <p>Предполагается, что база данных и таблицы уже инициализированы экземпляром {@link TrainDao}.</p>
     */
    public UserDao() {
        logger.debug("UserDao instance created.");
        // Опциональная проверка соединения при создании DAO (может мешать при первом запуске, когда сервер еще не стартовал)
         /*
         try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
             logger.debug("Successfully connected to DB in UserDao constructor check.");
         } catch (SQLException e) {
             logger.warn("Failed to connect to DB in UserDao constructor check! Server might not be running yet or DB issue.", e);
         }
         */
    }

    /**
     * <p>Регистрирует нового пользователя в системе.</p>
     * <p>Проверяет, не занято ли имя пользователя. Если свободно, хэширует пароль
     * с помощью BCrypt и сохраняет данные в таблицу `Users`.</p>
     *
     * @param username Имя нового пользователя (должно быть уникальным).
     * @param password Пароль нового пользователя (будет захэширован).
     * @param role Роль нового пользователя (например, "user", "admin").
     * @throws TrainValidationException Если имя пользователя уже занято, или произошла ошибка при доступе к БД,
     *                                  или переданные аргументы некорректны (хотя базовые проверки лучше делать до вызова DAO).
     * @throws IllegalArgumentException Если username, password или role равны null или пустые (лучше проверять до вызова).
     */
    public void registerUser(String username, String password, String role) throws TrainValidationException {
        // Базовые проверки входных данных
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty for registration.");
        }
        if (password == null || password.isEmpty()) { // Пароль не тримим
            throw new IllegalArgumentException("Password cannot be null or empty for registration.");
        }
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be null or empty for registration.");
        }
        String trimmedUsername = username.trim();
        String trimmedRole = role.trim();

        logger.debug("Attempting to register user: '" + trimmedUsername + "' with role: '" + trimmedRole + "'");

        // 1. Проверка, не занято ли имя
        if (userExists(trimmedUsername)) {
            logger.warn("Registration failed - username already exists: '" + trimmedUsername + "'");
            // Используем TrainValidationException для ошибки бизнес-логики
            throw new TrainValidationException("Пользователь с именем '" + trimmedUsername + "' уже существует.");
        }

        // 2. Хэширование пароля
        String salt = BCrypt.gensalt(); // Генерируем соль (BCrypt рекомендует использовать дефолтные параметры)
        String hashedPassword = BCrypt.hashpw(password, salt); // Хэшируем пароль с солью
        logger.trace("Password hashed for user: '" + trimmedUsername + "'. Hash length: " + hashedPassword.length());

        // 3. Вставка в БД
        // Используем try-with-resources для Connection и PreparedStatement
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(INSERT_USER)) {

            stmt.setString(1, trimmedUsername); // Устанавливаем имя пользователя
            stmt.setString(2, hashedPassword);  // Устанавливаем хэшированный пароль
            stmt.setString(3, trimmedRole);      // Устанавливаем роль

            stmt.executeUpdate(); // Выполняем вставку

            logger.info("User registered successfully: '" + trimmedUsername + "'");

        } catch (SQLException e) {
            // Ошибка при работе с БД
            logger.error("Database error during registration for user: '" + trimmedUsername + "'", e);
            // Оборачиваем SQLException в TrainValidationException
            throw new TrainValidationException("Ошибка базы данных при регистрации пользователя.", e);
        }
    }

    /**
     * <p>Выполняет аутентификацию пользователя по имени и паролю.</p>
     * <p>Находит пользователя по имени в БД. Если найден, сравнивает предоставленный пароль
     * с хэшированным паролем из БД с помощью {@code BCrypt.checkpw()}.</p>
     *
     * @param username Имя пользователя для входа.
     * @param password Предоставленный пароль для проверки.
     * @return Объект {@link User}, если аутентификация прошла успешно (имя найдено и пароль совпал).
     *         Возвращает {@code null}, если пользователь не найден или пароль неверный.
     * @throws TrainValidationException Если произошла ошибка при доступе к БД.
     * @throws IllegalArgumentException Если username или password равны null/пустые (лучше проверять до вызова).
     */
    public User loginUser(String username, String password) throws TrainValidationException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty for login.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty for login.");
        }
        String trimmedUsername = username.trim();

        logger.debug("Attempting login for user: '" + trimmedUsername + "'");
        // Используем try-with-resources для Connection, PreparedStatement, ResultSet
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(SELECT_USER_BY_USERNAME)) {

            stmt.setString(1, trimmedUsername); // Ищем по имени
            ResultSet rs = stmt.executeQuery();

            // Проверяем, найден ли пользователь
            if (rs.next()) { // Если пользователь с таким именем существует
                int id = rs.getInt("id");
                String dbUsername = rs.getString("username"); // Получаем имя из БД (для лога)
                String hashedPassword = rs.getString("password"); // Получаем хэш пароля из БД
                String role = rs.getString("role"); // Получаем роль

                logger.debug("User found in DB: id=" + id + ", username='" + dbUsername + "', role='" + role + "'");

                // Проверка пароля с помощью BCrypt
                boolean passwordMatch = false;
                if (hashedPassword != null && !hashedPassword.isEmpty()) {
                    try {
                        passwordMatch = BCrypt.checkpw(password, hashedPassword);
                        logger.debug("BCrypt password check result for user '" + dbUsername + "': " + passwordMatch);
                    } catch (IllegalArgumentException bcryptEx) {
                        // Эта ошибка возникает, если хэш в БД имеет неверный формат (не начинается с $2a$, $2b$, $2y$)
                        logger.error("Invalid password hash format in DB for user '" + dbUsername + "'! Hash: " + hashedPassword, bcryptEx);
                        // Считаем, что пароль не совпал, если хэш некорректен
                        passwordMatch = false;
                    }
                } else {
                    // Если хэш пустой или null в БД - это ошибка данных
                    logger.error("Password hash is null or empty in DB for user: '" + dbUsername + "'! Login failed.");
                    passwordMatch = false;
                }


                // Возвращаем объект User только если пароль совпал
                if (passwordMatch) {
                    logger.info("Login successful for user: '" + dbUsername + "'");
                    return new User(id, dbUsername, role); // Успешный вход
                } else {
                    logger.warn("Incorrect password provided for user: '" + dbUsername + "'");
                    return null; // Неверный пароль
                }

            } else {
                // Пользователь с таким именем не найден
                logger.warn("User not found in DB: '" + trimmedUsername + "'");
                return null; // Пользователь не найден
            }

        } catch (SQLException e) {
            // Ошибка при работе с БД
            logger.error("Database error during login attempt for user: '" + trimmedUsername + "'", e);
            throw new TrainValidationException("Ошибка базы данных при попытке входа.", e);
        }
        // Ресурсы Connection, PreparedStatement, ResultSet будут автоматически закрыты
    }

    /**
     * <p>Вспомогательный приватный метод для проверки существования пользователя по имени.</p>
     * <p>Используется в {@link #registerUser(String, String, String)} для предотвращения дублирования имен.</p>
     *
     * @param username Имя пользователя для проверки.
     * @return {@code true}, если пользователь с таким именем уже существует в таблице `Users`, {@code false} в противном случае.
     * @throws TrainValidationException Если произошла ошибка при доступе к БД.
     */
    private boolean userExists(String username) throws TrainValidationException {
        // Проверка аргумента (хотя метод приватный, для надежности)
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username cannot be null or empty for existence check.");
        }
        logger.trace("Checking existence for user: '" + username + "'");
        // Используем try-with-resources
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(SELECT_USER_BY_USERNAME)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            boolean exists = rs.next(); // true, если найдена хотя бы одна строка
            logger.trace("User '" + username + "' exists check result: " + exists);
            return exists;

        } catch (SQLException e) {
            logger.error("Database error while checking user existence for: '" + username + "'", e);
            // Оборачиваем в TrainValidationException, т.к. это ошибка операции, связанной с пользователем
            throw new TrainValidationException("Ошибка базы данных при проверке существования пользователя.", e);
        }
    }

    /**
     * <p>Внутренний статический класс для представления данных пользователя.</p>
     * <p>Содержит ID, имя пользователя и его роль. Используется как возвращаемое значение
     * методом {@link #loginUser(String, String)}.</p>
     * <p>Поля сделаны {@code final} для обеспечения неизменяемости (immutability) объекта User
     * после его создания.</p>
     */
    public static class User {
        private final int id;       // Уникальный ID из БД
        private final String username; // Имя пользователя
        private final String role;     // Роль пользователя (e.g., "admin", "user")

        /**
         * Конструктор объекта User.
         * @param id ID пользователя из БД.
         * @param username Имя пользователя (не null, не пустое).
         * @param role Роль пользователя (не null, не пустая).
         * @throws IllegalArgumentException если username или role некорректны.
         */
        public User(int id, String username, String role) {
            // Валидация входных данных в конструкторе
            if (username == null || username.trim().isEmpty()) {
                throw new IllegalArgumentException("Username cannot be null or empty for User object.");
            }
            if (role == null || role.trim().isEmpty()) {
                throw new IllegalArgumentException("Role cannot be null or empty for User object.");
            }
            this.id = id;
            // Сохраняем trim-версии для чистоты данных
            this.username = username.trim();
            this.role = role.trim();
            // Логгировать создание User объекта не обязательно, т.к. это происходит после успешного логина,
            // который уже залоггирован.
        }

        // --- Геттеры ---
        public int getId() { return id; }
        public String getUsername() { return username; }
        public String getRole() { return role; }

        /**
         * Возвращает строковое представление объекта User (для логгирования и отладки).
         * Не включает пароль!
         * @return Строка вида "User{id=..., username='...', role='...'}".
         */
        @Override
        public String toString() {
            return "User{" +
                    "id=" + id +
                    ", username='" + username + '\'' +
                    ", role='" + role + '\'' +
                    '}';
        }

        // Методы equals() и hashCode() здесь не реализованы, т.к. обычно объекты User
        // сравнивают по ID или по имени, а не по полному совпадению всех полей.
        // Если потребуется сравнение объектов User, эти методы нужно будет добавить.
    }
} // Конец класса UserDao