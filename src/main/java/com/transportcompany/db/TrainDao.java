package com.transportcompany.db;

import com.transportcompany.exception.TrainValidationException;
import com.transportcompany.train.Train;
import com.transportcompany.vehicle.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.h2.tools.Server; // Сервер H2 для работы в режиме TCP
import org.mindrot.jbcrypt.BCrypt; // Для хэширования паролей пользователей

/**
 * <p>Data Access Object (DAO) для работы с данными поездов ({@link Train}) и их вагонов ({@link Vehicle})
 * в базе данных H2.</p>
 * <p>Отвечает за все операции CRUD (Create, Read, Update, Delete) для поездов и вагонов,
 * а также за инициализацию структуры БД (таблиц) и начальных данных (типы вагонов, пользователи).</p>
 *
 * <p><b>Ключевые обязанности:</b></p>
 * <ul>
 *     <li>Управление жизненным циклом TCP-сервера H2 (запуск при создании первого экземпляра DAO, остановка при завершении приложения).</li>
 *     <li>Создание необходимых таблиц (`Trains`, `TrainCarTypes`, `TrainCars`, `Users`) при первом запуске, если они не существуют.</li>
 *     <li>Добавление записей по умолчанию (типы вагонов, пользователи admin/user) при первом запуске.</li>
 *     <li>Сохранение объекта {@link Train} со всеми его вагонами в БД (операция `INSERT` или `UPDATE`).</li>
 *     <li>Загрузка объекта {@link Train} по его номеру из БД (операция `SELECT`).</li>
 *     <li>Получение списка всех существующих поездов (только номеров или полных объектов).</li>
 *     <li>Удаление поезда и его вагонов из БД (операция `DELETE`).</li>
 *     <li>Преобразование данных между объектами Java ({@code Train}, {@code Vehicle}) и реляционными данными в таблицах БД.</li>
 *     <li>Использование JDBC для взаимодействия с H2.</li>
 *     <li>Обработка {@link SQLException} и их преобразование в {@link TrainValidationException} или {@code RuntimeException} для вышележащих слоев.</li>
 *     <li>Управление транзакциями БД (commit, rollback) для атомарности операций сохранения и удаления.</li>
 * </ul>
 *
 * <p><b>Важно:</b> Этот класс управляет сервером H2. Предполагается, что в приложении будет создан только один экземпляр этого DAO
 * (или будет использоваться единый механизм инициализации сервера). Остановка сервера происходит через статический метод {@link #stopServer()},
 * который должен вызываться при завершении работы приложения (см. {@link main.ui.MainGUI}).</p>
 *
 * @see Train Объектная модель поезда.
 * @see Vehicle Интерфейс вагона.
 * @see UserDao DAO для работы с пользователями (использует ту же БД и сервер).
 * @see TrainValidationException Исключение, выбрасываемое при ошибках операций.
 * @see Server Класс H2 для управления TCP сервером.
 * @see Connection JDBC соединение с БД.
 * @see PreparedStatement Используется для выполнения параметризованных SQL-запросов.
 * @see ResultSet Используется для обработки результатов SELECT-запросов.
 */
public class TrainDao {
    // Логгер для записи всех событий DAO
    private static final Logger logger = Logger.getLogger(TrainDao.class);

    // --- Параметры подключения к БД H2 ---
    /** URL для подключения к БД H2 в режиме TCP сервера. Файл БД будет train_db.mv.db в текущей директории запуска. */
    private static final String DB_URL = "jdbc:h2:tcp://localhost:9092/./train_db"; // Режим TCP/IP
    /** Имя пользователя БД H2 (по умолчанию для H2). */
    private static final String DB_USER = "sa";
    /** Пароль пользователя БД H2 (по умолчанию пустой). */
    private static final String DB_PASSWORD = "";

    // --- SQL Запросы (Константы) ---
    // Эти константы повышают читаемость и упрощают изменение SQL-запросов.

    // Запросы на создание таблиц (DDL - Data Definition Language)
    // <<<--- ИСПРАВЛЕНИЕ: Полные определения таблиц ---<<<
    private static final String CREATE_TRAIN_CAR_TYPES_TABLE =
            "CREATE TABLE IF NOT EXISTS TrainCarTypes (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +         // ID типа вагона (PK)
                    "name VARCHAR(255) UNIQUE NOT NULL" +          // Название типа (e.g., "PassengerCar")
                    ")";
    private static final String CREATE_TRAINS_TABLE =
            "CREATE TABLE IF NOT EXISTS Trains (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +         // ID поезда (PK)
                    "train_number VARCHAR(255) UNIQUE NOT NULL" +  // Номер поезда (уникальный)
                    ")";
    private static final String CREATE_TRAIN_CARS_TABLE =
            "CREATE TABLE IF NOT EXISTS TrainCars (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +         // ID вагона (PK)
                    "train_id INT," +                              // ID поезда (FK к Trains)
                    "type_id INT NOT NULL," +                      // ID типа вагона (FK к TrainCarTypes)
                    "capacity INT NOT NULL," +                     // Вместимость пассажиров
                    "baggage_capacity DOUBLE NOT NULL," +          // Вместимость багажа
                    "comfort_level INT NOT NULL," +                // Уровень комфорта
                    "additional_info VARCHAR(255)," +             // Доп. информация (тип пасс. вагона, кол-во столов и т.д.)
                    "FOREIGN KEY (type_id) REFERENCES TrainCarTypes(id)," + // Внешний ключ на типы
                    "FOREIGN KEY (train_id) REFERENCES Trains(id) ON DELETE CASCADE" + // Внешний ключ на поезда с каскадным удалением
                    ")";
    private static final String CREATE_USERS_TABLE =
            "CREATE TABLE IF NOT EXISTS Users (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +         // ID пользователя (PK)
                    "username VARCHAR(255) UNIQUE NOT NULL," +     // Имя пользователя (уникальное)
                    "password VARCHAR(255) NOT NULL," +            // Хэш пароля BCrypt (может быть длинным)
                    "role VARCHAR(50) NOT NULL" +                  // Роль (admin, user)
                    ")";
    // >>>--------------------------------------------------->>>

    // Запросы на модификацию данных (DML - Data Manipulation Language)
    private static final String INSERT_USER = "INSERT INTO Users (username, password, role) VALUES (?, ?, ?)";
    private static final String COUNT_USERS = "SELECT COUNT(*) FROM Users";
    private static final String INSERT_TRAIN_CAR_TYPE = "INSERT INTO TrainCarTypes (name) VALUES (?)";
    private static final String SELECT_TRAIN_CAR_TYPE_ID = "SELECT id FROM TrainCarTypes WHERE name = ?";
    private static final String INSERT_TRAIN = "INSERT INTO Trains (train_number) VALUES (?)";
    private static final String SELECT_TRAIN_ID = "SELECT id FROM Trains WHERE train_number = ?";
    private static final String INSERT_TRAIN_CAR = "INSERT INTO TrainCars (train_id, type_id, capacity, baggage_capacity, comfort_level, additional_info) VALUES (?, ?, ?, ?, ?, ?)";
    // Запрос для загрузки вагонов с именем типа
    private static final String SELECT_TRAIN_CARS = "SELECT c.*, ct.name as type_name FROM TrainCars c JOIN TrainCarTypes ct ON c.type_id = ct.id WHERE c.train_id = ?";
    // Запросы для удаления
    private static final String DELETE_TRAIN = "DELETE FROM Trains WHERE id = ?";
    private static final String DELETE_TRAIN_CARS_BY_TRAIN_ID = "DELETE FROM TrainCars WHERE train_id = ?";
    // Запрос для обновления (вероятно, не используется в текущей логике saveTrain)
    private static final String UPDATE_TRAIN_CAR = "UPDATE TrainCars SET capacity = ?, baggage_capacity = ?, comfort_level = ?, additional_info = ? WHERE id = ?";
    // Запрос для получения списка всех поездов
    private static final String SELECT_ALL_TRAINS = "SELECT id, train_number FROM Trains"; // Достаточно ID и номера

    /** Ссылка на запущенный сервер H2. Статическая, т.к. сервер один на приложение. */
    private static Server server;

    /**
     * <p>Конструктор TrainDao.</p>
     * <p><b>Важно:</b> При создании ПЕРВОГО экземпляра этого класса происходит запуск TCP-сервера H2,
     * создание таблиц БД (если их нет) и добавление записей по умолчанию.</p>
     * <p>Последующие вызовы конструктора просто проверяют, что сервер уже запущен.</p>
     * <p>Если запуск сервера или инициализация БД не удаются, выбрасывается {@link RuntimeException},
     * так как приложение не сможет работать без базы данных.</p>
     *
     * @throws RuntimeException Если произошла критическая ошибка при запуске сервера H2 или инициализации БД.
     */
    public TrainDao() {
        // Синхронизация на классе, чтобы избежать гонки потоков при одновременном создании DAO
        synchronized (TrainDao.class) {
            if (server == null || !server.isRunning(false)) {
                try {
                    logger.info("H2 TCP server not running. Attempting to start...");
                    logger.debug("Creating H2 TCP server instance...");
                    // Создаем и сразу запускаем сервер
                    Server tcpServer = Server.createTcpServer("-tcpPort", "9092", "-tcpAllowOthers", "-ifNotExists");
                    logger.debug("H2 TCP server instance created. Starting...");
                    server = tcpServer.start();
                    logger.info("H2 TCP server started successfully on port " + server.getPort() +
                            ". Web console (if enabled) available, DB file: train_db.mv.db");

                    // Инициализация БД после успешного старта сервера
                    logger.debug("Attempting to connect to DB for initial setup...");
                    try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                        logger.debug("Connected to database for initial setup (creating tables, default data)...");
                        logger.debug("Creating tables...");
                        createTables(connection); // Вызов метода создания таблиц
                        logger.debug("Tables created.");
                        logger.debug("Inserting default car types...");
                        insertDefaultTrainCarTypes(connection); // Вызов метода вставки типов
                        logger.debug("Default car types inserted.");
                        logger.debug("Adding default users...");
                        addDefaultUsers(connection); // Вызов метода добавления пользователей
                        logger.debug("Default users added.");
                        logger.info("Initial database setup verified/completed.");
                    } catch (SQLException setupEx) {
                        logger.error("CRITICAL: SQLException during initial DB setup after server start!", setupEx);
                        stopServer(); // Пытаемся остановить сервер, если настройка не удалась
                        // Оборачиваем в RuntimeException, т.к. это критическая ошибка старта
                        throw new RuntimeException("Error during initial DB setup", setupEx);
                    }

                } catch (SQLException e) {
                    logger.error("CRITICAL: SQLException during H2 server start!", e);
                    stopServer(); // Убеждаемся, что сервер остановлен, если старт не удался
                    throw new RuntimeException("Error starting H2 server", e);
                } catch (Exception e) { // Ловим другие возможные ошибки при старте сервера H2
                    logger.error("CRITICAL: Unexpected exception during H2 server start!", e);
                    stopServer();
                    throw new RuntimeException("Unexpected error starting H2 server", e);
                }
            } else {
                logger.info("H2 TCP server is already running.");
            }
        } // end synchronized
    }

    /**
     * <p>Останавливает запущенный TCP-сервер H2.</p>
     * <p>Этот статический метод ДОЛЖЕН вызываться при завершении работы приложения
     * (например, в обработчике закрытия главного окна), чтобы корректно освободить ресурсы
     * и закрыть файл базы данных.</p>
     * <p>Если сервер не был запущен или уже остановлен, метод ничего не делает.</p>
     */
    public static void stopServer() {
        // Синхронизация, чтобы избежать проблем при одновременном вызове
        synchronized (TrainDao.class) {
            if (server != null && server.isRunning(true)) { // isRunning(true) - активная проверка
                logger.info("Stopping H2 TCP server...");
                server.stop(); // Метод остановки сервера
                server = null; // Сбрасываем ссылку после остановки
                logger.info("H2 TCP server stopped successfully.");
            } else {
                logger.info("H2 TCP server was not running or already stopped. No action taken.");
            }
        }
    }

    /**
     * Создает все необходимые таблицы в базе данных, если они еще не существуют.
     * Выполняет SQL-запросы DDL (CREATE TABLE IF NOT EXISTS).
     *
     * @param connection Активное JDBC соединение с базой данных.
     * @throws SQLException Если произошла ошибка при выполнении SQL-запросов.
     */
    private void createTables(Connection connection) throws SQLException {
        // Используем try-with-resources для автоматического закрытия Statement
        try (Statement stmt = connection.createStatement()) {
            logger.debug("Executing CREATE TABLE IF NOT EXISTS for TrainCarTypes...");
            stmt.executeUpdate(CREATE_TRAIN_CAR_TYPES_TABLE); // <<<--- ИСПОЛЬЗУЕМ ПОЛНЫЙ SQL
            logger.debug("Executing CREATE TABLE IF NOT EXISTS for Trains...");
            stmt.executeUpdate(CREATE_TRAINS_TABLE); // <<<--- ИСПОЛЬЗУЕМ ПОЛНЫЙ SQL
            logger.debug("Executing CREATE TABLE IF NOT EXISTS for TrainCars...");
            stmt.executeUpdate(CREATE_TRAIN_CARS_TABLE); // <<<--- ИСПОЛЬЗУЕМ ПОЛНЫЙ SQL
            logger.debug("Executing CREATE TABLE IF NOT EXISTS for Users...");
            stmt.executeUpdate(CREATE_USERS_TABLE); // <<<--- ИСПОЛЬЗУЕМ ПОЛНЫЙ SQL
            logger.info("Schema verification/creation complete. All required tables exist.");
        } catch (SQLException e) {
            // Логгируем ошибку и пробрасываем исключение дальше, т.к. без таблиц работа невозможна
            logger.error("SQLException during table creation!", e);
            throw e; // Пробрасываем для обработки в конструкторе DAO
        }
    }

    /**
     * Добавляет стандартные типы вагонов (PassengerCar, RestaurantCar и т.д.)
     * в таблицу `TrainCarTypes`, если они там отсутствуют.
     * Гарантирует наличие базовых типов для связей в таблице `TrainCars`.
     *
     * @param connection Активное JDBC соединение.
     * @throws SQLException Если произошла ошибка при доступе к БД или вставке данных.
     */
    private void insertDefaultTrainCarTypes(Connection connection) throws SQLException {
        logger.debug("Checking/Inserting default TrainCarTypes...");
        // Вызываем вспомогательный метод для каждого стандартного типа
        insertTrainCarTypeIfNotExists(connection, "PassengerCar");
        insertTrainCarTypeIfNotExists(connection, "RestaurantCar");
        insertTrainCarTypeIfNotExists(connection, "BaggageCar");
        insertTrainCarTypeIfNotExists(connection, "Locomotive");
        logger.debug("Default TrainCarTypes check/insertion complete.");
    }

    /**
     * Вспомогательный метод: проверяет наличие типа вагона по имени в `TrainCarTypes`
     * и вставляет его, если он отсутствует.
     *
     * @param connection Активное JDBC соединение.
     * @param typeName Имя типа вагона для проверки/вставки.
     * @throws SQLException Если произошла ошибка при доступе к БД.
     */
    private void insertTrainCarTypeIfNotExists(Connection connection, String typeName) throws SQLException {
        logger.trace("Checking existence of TrainCarType: '" + typeName + "'");
        // Используем try-with-resources для PreparedStatement и ResultSet
        try (PreparedStatement checkStmt = connection.prepareStatement(SELECT_TRAIN_CAR_TYPE_ID)) {
            checkStmt.setString(1, typeName);
            try (ResultSet rs = checkStmt.executeQuery()) { // Вложенный try-with-resources для ResultSet
                if (!rs.next()) { // Если не найден
                    logger.info("TrainCarType '" + typeName + "' not found. Inserting...");
                    // Вставляем новый тип
                    try (PreparedStatement insertStmt = connection.prepareStatement(INSERT_TRAIN_CAR_TYPE)) {
                        insertStmt.setString(1, typeName);
                        insertStmt.executeUpdate();
                        logger.info("Successfully inserted TrainCarType: '" + typeName + "'");
                    } // insertStmt закроется здесь
                } else {
                    // Тип уже существует
                    logger.trace("TrainCarType '" + typeName + "' already exists.");
                }
            } // rs закроется здесь
        } // checkStmt закроется здесь
    }

    /**
     * Добавляет пользователей по умолчанию ('admin'/'admin123', 'user'/'user123')
     * в таблицу `Users`, если в таблице еще нет ни одного пользователя.
     * Использует BCrypt для хэширования паролей перед сохранением.
     *
     * @param connection Активное JDBC соединение.
     * @throws SQLException Если произошла ошибка при доступе к БД или вставке данных.
     */
    private void addDefaultUsers(Connection connection) throws SQLException {
        logger.debug("Checking if default users need to be added...");
        // Проверяем, есть ли вообще пользователи в таблице
        if (!usersExist(connection)) {
            logger.info("Users table is empty. Adding default users (admin, user)...");
            // Генерируем соли и хэшируем пароли
            String adminSalt = BCrypt.gensalt();
            String adminHashedPassword = BCrypt.hashpw("admin123", adminSalt);
            String userSalt = BCrypt.gensalt();
            String userHashedPassword = BCrypt.hashpw("user123", userSalt);
            logger.trace("Passwords hashed for default users.");

            // Вставляем пользователей
            try (PreparedStatement stmt = connection.prepareStatement(INSERT_USER)) {
                // Админ
                stmt.setString(1, "admin");
                stmt.setString(2, adminHashedPassword);
                stmt.setString(3, "admin"); // Роль
                stmt.executeUpdate();
                logger.debug("Added default 'admin' user.");

                // Пользователь
                stmt.setString(1, "user");
                stmt.setString(2, userHashedPassword);
                stmt.setString(3, "user"); // Роль
                stmt.executeUpdate();
                logger.debug("Added default 'user' user.");

                logger.info("Default users (admin, user) added successfully.");
            } catch (SQLException e) {
                // Логгируем ошибку и пробрасываем, т.к. это проблема инициализации
                logger.error("SQLException during adding default users!", e);
                throw e;
            }
        } else {
            logger.debug("Default users already exist or table is not empty. Skipping insertion.");
        }
    }

    /**
     * Проверяет, существует ли хотя бы один пользователь в таблице `Users`.
     *
     * @param connection Активное JDBC соединение.
     * @return {@code true}, если таблица пользователей не пуста, {@code false} в противном случае.
     * @throws SQLException Если произошла ошибка при выполнении запроса COUNT(*).
     */
    private boolean usersExist(Connection connection) throws SQLException {
        logger.trace("Checking if any users exist in the Users table...");
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(COUNT_USERS)) {
            if (rs.next()) {
                int count = rs.getInt(1);
                logger.trace("User count query returned: " + count);
                return count > 0; // true, если есть хотя бы один пользователь
            }
            // Эта ветка не должна достигаться для COUNT(*), но для полноты:
            logger.warn("Could not get user count from ResultSet (query might have failed unexpectedly?).");
            return false;
        } catch (SQLException e) {
            logger.error("SQLException while checking user existence (COUNT query)!", e);
            throw e; // Пробрасываем ошибку
        }
    }

    /**
     * <p>Загружает поезд со всеми его вагонами из базы данных по номеру поезда.</p>
     *
     * @param trainNumber Номер искомого поезда.
     * @return Объект {@link Train}, заполненный данными из БД, или {@code null}, если поезд с таким номером не найден.
     * @throws TrainValidationException Если произошла ошибка при доступе к БД или обработке данных.
     */
    public Train loadTrain(String trainNumber) throws TrainValidationException {
        if (trainNumber == null || trainNumber.trim().isEmpty()) {
            logger.warn("loadTrain called with null or empty trainNumber.");
            // Можно бросить исключение или вернуть null, в зависимости от требований
            throw new TrainValidationException("Номер поезда для загрузки не может быть пустым.");
            // return null;
        }
        String trimmedTrainNumber = trainNumber.trim();
        logger.debug("Attempting to load train: '" + trimmedTrainNumber + "'");
        Train train = null;
        // Используем try-with-resources для Connection
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            // 1. Найти ID поезда по номеру
            int trainId = getTrainId(connection, trimmedTrainNumber);
            if (trainId == -1) {
                logger.info("Train not found in database: '" + trimmedTrainNumber + "'");
                return null; // Поезд не найден
            }
            logger.debug("Found train ID: " + trainId + " for number: '" + trimmedTrainNumber + "'");

            // 2. Создать объект Train
            try {
                train = new Train(trimmedTrainNumber);
                logger.trace("Train object created for '" + trimmedTrainNumber + "'");
            } catch (IllegalArgumentException e) {
                // Эта ошибка не должна возникать, т.к. номер уже есть в БД, но для полноты
                logger.error("Inconsistency: Train number '" + trimmedTrainNumber + "' found in DB but invalid for Train constructor!", e);
                throw new TrainValidationException("Некорректный формат номера поезда ('" + trimmedTrainNumber + "') найден в БД", e);
            }


            // 3. Загрузить вагоны этого поезда
            logger.trace("Executing query to load vehicles for train ID: " + trainId);
            try (PreparedStatement stmt = connection.prepareStatement(SELECT_TRAIN_CARS)) {
                stmt.setInt(1, trainId); // Устанавливаем параметр train_id
                try (ResultSet rs = stmt.executeQuery()) { // Вложенный try-with-resources для ResultSet
                    logger.trace("Vehicle query executed. Processing results...");
                    int vehicleCount = 0;
                    while (rs.next()) { // Итерация по всем найденным вагонам
                        try {
                            // Создаем объект Vehicle из текущей строки ResultSet
                            Vehicle vehicle = createVehicleFromResultSet(rs);
                            // Добавляем вагон в объект Train
                            train.addVehicle(vehicle);
                            vehicleCount++;
                        } catch (SQLException | IllegalArgumentException | TrainValidationException e) {
                            // Логгируем ошибку создания/добавления КОНКРЕТНОГО вагона,
                            // но продолжаем загрузку остальных вагонов поезда.
                            int carId = -1; try { carId = rs.getInt("id"); } catch(SQLException ignored) {} // Попытка получить ID для лога
                            logger.error("Failed to create or add vehicle (ID: " + carId + ") for train '" + trimmedTrainNumber + "'. Skipping this vehicle.", e);
                            // Можно накапливать ошибки, но пропуск поврежденных - простой вариант.
                        }
                    } // конец while(rs.next())
                    logger.debug("Successfully loaded and added " + vehicleCount + " vehicles for train: '" + trimmedTrainNumber + "'");
                } // ResultSet закроется здесь
            } // PreparedStatement закроется здесь

        } catch (SQLException e) {
            // Ошибка подключения к БД или выполнения запроса ID поезда
            logger.error("Database error loading train data for train: '" + trimmedTrainNumber + "'", e);
            throw new TrainValidationException("Ошибка базы данных при загрузке поезда '" + trimmedTrainNumber + "'", e);
        }
        // Возвращаем собранный объект Train (или null, если не найден)
        return train;
    }

    /**
     * <p>Возвращает список всех поездов, существующих в базе данных.</p>
     * <p>Возвращает список объектов {@link Train}, содержащих только номер поезда.</p>
     *
     * @return Список объектов {@link Train} (только с номерами) или пустой список.
     * @throws TrainValidationException Если произошла ошибка при доступе к БД.
     */
    public List<Train> getAllTrains() throws TrainValidationException {
        List<Train> trains = new ArrayList<>();
        logger.debug("Attempting to get all train numbers from database...");
        // Используем try-with-resources для Connection, Statement, ResultSet
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_ALL_TRAINS)) { // Используем константу

            int count = 0;
            while (rs.next()) {
                String trainNumber = rs.getString("train_number");
                int trainId = rs.getInt("id"); // Получаем ID для лога
                try {
                    // Создаем объект Train только с номером
                    trains.add(new Train(trainNumber));
                    count++;
                    logger.trace("Found train: ID=" + trainId + ", Number='" + trainNumber + "'");
                } catch (IllegalArgumentException e) {
                    // Логируем, если номер в БД невалиден, и пропускаем
                    logger.error("Invalid train number format ('" + trainNumber + "', ID=" + trainId + ") found in database during getAllTrains. Skipping.", e);
                }
            }
            logger.info("Retrieved " + count + " valid train records from database.");
        } catch (SQLException e) {
            logger.error("Database error retrieving list of all trains.", e);
            throw new TrainValidationException("Ошибка базы данных при получении списка поездов", e);
        }
        return trains;
    }

    /**
     * <p>Сохраняет объект поезда со всеми его вагонами в базу данных.</p>
     * <p>Выполняет операцию в транзакции.</p>
     * (Остальной Javadoc опущен для краткости)
     * @param train Объект {@link Train} для сохранения. Не должен быть null.
     * @throws TrainValidationException Если {@code train} равен null, или ошибка БД.
     * @throws IllegalArgumentException Если номер поезда некорректен.
     */
    public void saveTrain(Train train) throws TrainValidationException {
        if (train == null) {
            logger.error("Attempted to save a null train object.");
            throw new TrainValidationException("Cannot save a null train.");
        }
        String trainNumber = train.getTrainNumber();
        int vehicleCount = train.getVehicles().size();
        logger.debug("Attempting to save train: '" + trainNumber + "' with " + vehicleCount + " vehicles.");

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            connection.setAutoCommit(false); // Начинаем транзакцию
            logger.trace("Transaction started for saving train '" + trainNumber + "'.");

            int trainId = getTrainId(connection, trainNumber);
            boolean isNewTrain = (trainId == -1);

            if (isNewTrain) {
                trainId = insertTrain(connection, trainNumber);
                logger.info("Inserted new train '" + trainNumber + "' with ID: " + trainId);
            } else {
                logger.debug("Train '" + trainNumber + "' (ID: " + trainId + ") already exists. Updating vehicles...");
                deleteTrainCarsExplicitly(connection, trainId); // Удаляем старые вагоны
                logger.debug("Deleted old vehicles for existing train ID: " + trainId);
            }

            int carsInsertedCount = 0;
            for (Vehicle vehicle : train.getVehicles()) {
                if (vehicle != null) {
                    insertTrainCar(connection, trainId, vehicle); // Вставляем вагон
                    carsInsertedCount++;
                } else {
                    logger.warn("Null vehicle found in the list for train '" + trainNumber + "' during save. Skipping.");
                }
            }
            logger.debug("Inserted " + carsInsertedCount + " vehicles for train ID: " + trainId);

            connection.commit(); // Коммитим транзакцию
            logger.info("Train '" + trainNumber + "' saved successfully (transaction committed).");

        } catch (SQLException e) {
            logger.error("SQLException occurred during save transaction for train '" + trainNumber + "'. Rolling back transaction.", e);
            if (connection != null) {
                try {
                    connection.rollback();
                    logger.warn("Transaction successfully rolled back for train '" + trainNumber + "'.");
                } catch (SQLException rbEx) {
                    logger.error("CRITICAL: Failed to rollback transaction for train '" + trainNumber + "'!", rbEx);
                    throw new TrainValidationException("Критическая ошибка: не удалось откатить транзакцию сохранения поезда.", rbEx);
                }
            }
            throw new TrainValidationException("Ошибка базы данных при сохранении поезда: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) { // Ошибки валидации номера поезда
            logger.error("Invalid argument during save operation for train '" + trainNumber + "'. Rolling back.", e);
            if (connection != null) { try { connection.rollback(); } catch (SQLException rbEx) { logger.error("Failed to rollback.", rbEx);}}
            throw new TrainValidationException("Ошибка данных при сохранении поезда: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                try {
                    if (!connection.getAutoCommit()) {
                        connection.setAutoCommit(true);
                    }
                    connection.close();
                    logger.trace("Database connection closed for save operation of train '" + trainNumber + "'.");
                } catch (SQLException finallyEx) {
                    logger.error("Error closing connection or resetting autoCommit after saving train '" + trainNumber + "'.", finallyEx);
                }
            }
        }
    }

    /**
     * Вспомогательный метод для явного удаления всех вагонов поезда.
     * (Javadoc был добавлен ранее)
     * @param connection Активное JDBC соединение.
     * @param trainId ID поезда.
     * @throws SQLException Если ошибка при удалении.
     */
    private void deleteTrainCarsExplicitly(Connection connection, int trainId) throws SQLException {
        logger.trace("Attempting to explicitly delete vehicles for train ID: " + trainId);
        try (PreparedStatement stmt = connection.prepareStatement(DELETE_TRAIN_CARS_BY_TRAIN_ID)) {
            stmt.setInt(1, trainId);
            int deletedRows = stmt.executeUpdate();
            logger.debug("Explicitly deleted " + deletedRows + " vehicles for train_id: " + trainId);
        }
    }

    /**
     * Удаляет поезд и все связанные вагоны по номеру поезда.
     * (Javadoc был добавлен ранее)
     * @param trainNumber Номер поезда для удаления.
     * @throws TrainValidationException Если поезд не найден или ошибка БД.
     */
    public void deleteTrain(String trainNumber) throws TrainValidationException {
        if (trainNumber == null || trainNumber.trim().isEmpty()) {
            logger.warn("deleteTrain called with null or empty trainNumber.");
            throw new TrainValidationException("Номер поезда для удаления не может быть пустым.");
        }
        String trimmedTrainNumber = trainNumber.trim();
        logger.debug("Attempting to delete train: '" + trimmedTrainNumber + "'");
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            connection.setAutoCommit(false);
            logger.trace("Transaction started for deleting train '" + trimmedTrainNumber + "'.");

            int trainId = getTrainId(connection, trimmedTrainNumber);

            if (trainId != -1) {
                logger.debug("Found train ID: " + trainId + " for deletion.");
                deleteTrainById(connection, trainId); // Удаляем поезд (вагоны удалятся каскадно)
                logger.info("Deleted train record with ID: " + trainId + " (Number: '" + trimmedTrainNumber + "'). Associated cars deleted via CASCADE.");
                connection.commit();
                logger.info("Train deletion committed for: '" + trimmedTrainNumber + "'");
            } else {
                logger.warn("Train not found in database for deletion: '" + trimmedTrainNumber + "'. No changes made.");
                // Не бросаем ошибку, если поезд не найден - это не ошибка удаления
            }
        } catch (SQLException e) {
            logger.error("SQLException during delete transaction for train '" + trimmedTrainNumber + "'. Rolling back.", e);
            if (connection != null) {
                try { connection.rollback(); logger.warn("Transaction rolled back for train deletion: '" + trimmedTrainNumber + "'."); }
                catch (SQLException rbEx) { logger.error("CRITICAL: Failed to rollback transaction for train deletion: '" + trimmedTrainNumber + "'!", rbEx); }
            }
            throw new TrainValidationException("Ошибка базы данных при удалении поезда: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                try {
                    if (!connection.getAutoCommit()) { connection.setAutoCommit(true); }
                    connection.close();
                    logger.trace("Database connection closed for delete operation of train '" + trimmedTrainNumber + "'.");
                } catch (SQLException finallyEx) {
                    logger.error("Error closing connection or resetting autoCommit after deleting train '" + trimmedTrainNumber + "'.", finallyEx);
                }
            }
        }
    }

    /**
     * Вспомогательный метод для удаления записи поезда по его ID.
     * (Javadoc был добавлен ранее)
     * @param connection Активное JDBC соединение.
     * @param trainId ID поезда для удаления.
     * @throws SQLException Если ошибка при удалении.
     */
    private void deleteTrainById(Connection connection, int trainId) throws SQLException {
        logger.trace("Executing DELETE query for train ID: " + trainId);
        try (PreparedStatement stmt = connection.prepareStatement(DELETE_TRAIN)) {
            stmt.setInt(1, trainId);
            int rowsAffected = stmt.executeUpdate();
            logger.debug("Executed DELETE_TRAIN for ID: " + trainId + ". Rows affected: " + rowsAffected);
            if (rowsAffected == 0) {
                logger.warn("DELETE_TRAIN query for ID " + trainId + " affected 0 rows (expected 1).");
            }
        }
    }

    /**
     * Вспомогательный метод для получения ID поезда по его номеру.
     * (Javadoc был добавлен ранее)
     * @param connection Активное JDBC соединение.
     * @param trainNumber Номер искомого поезда.
     * @return ID поезда, если найден, иначе -1.
     * @throws SQLException Если ошибка при SELECT.
     * @throws IllegalArgumentException Если trainNumber null или пуст.
     */
    private int getTrainId(Connection connection, String trainNumber) throws SQLException {
        if (trainNumber == null || trainNumber.isEmpty()) {
            throw new IllegalArgumentException("Train number cannot be null or empty when querying for ID.");
        }
        logger.trace("Querying for train ID by number: '" + trainNumber + "'");
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_TRAIN_ID)) {
            stmt.setString(1, trainNumber);
            try (ResultSet rs = stmt.executeQuery()) { // try-with-resources для ResultSet
                int id = -1;
                if (rs.next()) {
                    id = rs.getInt("id");
                    logger.trace("Found train ID: " + id + " for number '" + trainNumber + "'");
                } else {
                    logger.trace("Train ID not found for number '" + trainNumber + "'");
                }
                return id;
            } // rs закроется здесь
        } // stmt закроется здесь
    }

    /**
     * Вспомогательный метод для вставки новой записи поезда.
     * (Javadoc был добавлен ранее)
     * @param connection Активное JDBC соединение.
     * @param trainNumber Номер нового поезда.
     * @return Сгенерированный ID нового поезда.
     * @throws SQLException Если ошибка при INSERT или получении ID.
     * @throws IllegalArgumentException Если trainNumber null или пуст.
     */
    private int insertTrain(Connection connection, String trainNumber) throws SQLException {
        if (trainNumber == null || trainNumber.isEmpty()) {
            throw new IllegalArgumentException("Train number cannot be null or empty for insertion.");
        }
        logger.trace("Inserting new train record with number: '" + trainNumber + "'");
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_TRAIN, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, trainNumber);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows == 0) {
                logger.error("Creating train record failed for '" + trainNumber + "', no rows affected.");
                throw new SQLException("Creating train failed, no rows affected.");
            }
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int newId = generatedKeys.getInt(1);
                    logger.debug("Successfully inserted train '" + trainNumber + "' and obtained generated ID: " + newId);
                    return newId;
                } else {
                    logger.error("Creating train record failed for '" + trainNumber + "', no ID obtained.");
                    throw new SQLException("Creating train failed, no ID obtained.");
                }
            }
        }
    }

    /**
     * Обновляет данные КОНКРЕТНОГО вагона в таблице `TrainCars`.
     * (Javadoc был добавлен ранее)
     * @param carId ID вагона в БД.
     * @param updatedVehicle Объект {@link Vehicle} с новыми данными.
     * @throws TrainValidationException Если ошибка при обновлении.
     */
    public void updateTrainCar(int carId, Vehicle updatedVehicle) throws TrainValidationException {
        if (updatedVehicle == null) {
            throw new TrainValidationException("Cannot update car with null vehicle data.");
        }
        logger.warn("Direct updateTrainCar called for car ID: " + carId + ". Consider saving the whole train instead. Vehicle data: " + updatedVehicle);
        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = connection.prepareStatement(UPDATE_TRAIN_CAR)) {
            stmt.setInt(1, updatedVehicle.getCapacity());
            stmt.setDouble(2, updatedVehicle.getBaggageCapacity());
            stmt.setInt(3, updatedVehicle.getComfortLevel());
            stmt.setString(4, getAdditionalInfo(updatedVehicle)); // Получаем доп. инфо
            stmt.setInt(5, carId); // WHERE id = ?

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                logger.debug("Successfully updated car record for ID " + carId + ". Rows affected: " + rowsAffected);
            } else {
                logger.warn("No car record found with ID " + carId + " for update. Rows affected: " + rowsAffected);
            }
        } catch (SQLException e) {
            logger.error("Error updating train car record in database for ID: " + carId, e);
            throw new TrainValidationException("Ошибка базы данных при обновлении вагона", e);
        }
    }

    /**
     * Вспомогательный метод для вставки записи об одном вагоне.
     * (Javadoc был добавлен ранее)
     * @param connection Активное JDBC соединение.
     * @param trainId ID поезда.
     * @param vehicle Вагон для сохранения.
     * @throws SQLException Если ошибка при INSERT или получении ID типа.
     * @throws IllegalArgumentException Если vehicle null.
     */
    private void insertTrainCar(Connection connection, int trainId, Vehicle vehicle) throws SQLException {
        if (vehicle == null) {
            throw new IllegalArgumentException("Cannot insert null vehicle into TrainCars table.");
        }
        logger.trace("Preparing to insert vehicle: " + vehicle + " for train ID: " + trainId);
        try (PreparedStatement stmt = connection.prepareStatement(INSERT_TRAIN_CAR)) {
            String baseTypeName = vehicle.getVehicleType().split(" - ")[0];
            int typeId = getTrainCarTypeId(connection, baseTypeName); // Может бросить SQLException

            stmt.setInt(1, trainId);
            stmt.setInt(2, typeId);
            stmt.setInt(3, vehicle.getCapacity());
            stmt.setDouble(4, vehicle.getBaggageCapacity());
            stmt.setInt(5, vehicle.getComfortLevel());
            stmt.setString(6, getAdditionalInfo(vehicle)); // Получаем доп. инфо

            stmt.executeUpdate();
            logger.trace("Successfully inserted vehicle record of type '" + baseTypeName + "' for train ID " + trainId);
        } catch (SQLException e) {
            logger.error("Failed to insert vehicle record for type '" + vehicle.getVehicleType() + "' (train ID: " + trainId + ")", e);
            throw e; // Пробрасываем для отката транзакции
        }
    }

    /**
     * Вспомогательный метод для получения специфической информации вагона для сохранения.
     * (Javadoc был добавлен ранее)
     * @param vehicle Вагон.
     * @return Строка с доп. информацией или пустая строка.
     */
    private String getAdditionalInfo(Vehicle vehicle) {
        String additionalInfo = "";
        if (vehicle instanceof PassengerCar) {
            String[] parts = ((PassengerCar) vehicle).getVehicleType().split(" - ", 2);
            additionalInfo = (parts.length > 1) ? parts[1].trim() : "";
            if (additionalInfo.isEmpty()){ logger.warn("PassengerCar (ID: " + vehicle.getId() + ") has empty specific type string."); }
        } else if (vehicle instanceof RestaurantCar) {
            additionalInfo = String.valueOf(((RestaurantCar) vehicle).getNumberOfTables());
        } else if (vehicle instanceof BaggageCar) {
            additionalInfo = String.valueOf(((BaggageCar) vehicle).getMaxWeightCapacity());
        } else if (vehicle instanceof Locomotive) {
            additionalInfo = String.valueOf(((Locomotive) vehicle).getTractionForce());
        } else {
            logger.warn("Unknown vehicle type encountered in getAdditionalInfo: " + vehicle.getClass().getSimpleName() + " (ID: " + vehicle.getId() + ").");
        }
        logger.trace("getAdditionalInfo for vehicle type " + vehicle.getClass().getSimpleName() + " (ID: " + vehicle.getId() + ") returned: '" + additionalInfo + "'");
        return additionalInfo;
    }

    /**
     * Вспомогательный метод для получения ID типа вагона по его базовому имени.
     * (Javadoc был добавлен ранее)
     * @param connection Активное JDBC соединение.
     * @param baseTypeName Базовое имя типа вагона.
     * @return ID типа вагона.
     * @throws SQLException Если тип не найден или ошибка БД.
     */
    private int getTrainCarTypeId(Connection connection, String baseTypeName) throws SQLException {
        if (baseTypeName == null || baseTypeName.trim().isEmpty()) {
            throw new IllegalArgumentException("Base type name cannot be null or empty when querying for Type ID.");
        }
        String trimmedTypeName = baseTypeName.trim();
        logger.trace("Querying for TrainCarType ID by name: '" + trimmedTypeName + "'");
        try (PreparedStatement stmt = connection.prepareStatement(SELECT_TRAIN_CAR_TYPE_ID)) {
            stmt.setString(1, trimmedTypeName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    logger.trace("Found TrainCarType ID: " + id + " for name '" + trimmedTypeName + "'");
                    return id;
                } else {
                    logger.error("CRITICAL CONFIGURATION ERROR: TrainCarType not found in DB for name: '" + trimmedTypeName + "'!");
                    throw new SQLException("Required TrainCarType not found in database: " + trimmedTypeName);
                }
            } // rs закроется
        } // stmt закроется
    }

    /**
     * Вспомогательный метод для создания объекта Vehicle из строки ResultSet.
     * (Javadoc был добавлен ранее)
     * @param rs ResultSet, установленный на текущую строку.
     * @return Созданный объект {@link Vehicle}.
     * @throws SQLException Если ошибка чтения/парсинга или неизвестный тип.
     * @throws IllegalArgumentException Если данные некорректны для конструктора.
     */
    private Vehicle createVehicleFromResultSet(ResultSet rs) throws SQLException {
        int id = 0; // Инициализируем на случай ошибки до чтения ID
        String typeName = null;
        String additionalInfo = null; // Объявляем здесь для блока catch
        try {
            // Читаем общие данные вагона
            id = rs.getInt("id");
            int capacity = rs.getInt("capacity");
            double baggageCapacity = rs.getDouble("baggage_capacity");
            int comfortLevel = rs.getInt("comfort_level");
            // Читаем специфичную информацию и базовый тип
            additionalInfo = rs.getString("additional_info"); // Может быть null
            typeName = rs.getString("type_name"); // Имя базового типа из TrainCarTypes (JOIN)

            // Проверка наличия базового типа - критично
            if (typeName == null) {
                logger.error("CRITICAL: TrainCarType name ('type_name') is null in ResultSet for car ID: " + id + ". Check SELECT_TRAIN_CARS query JOIN.");
                throw new SQLException("TrainCarType name is null in query result for car ID: " + id);
            }
            logger.trace("Creating vehicle from ResultSet: ID=" + id + ", TypeName='" + typeName + "', AddInfo='" + additionalInfo + "'");

            Vehicle vehicle;
            // Создаем объект конкретного класса вагона в зависимости от typeName
            // <<<--- ИСПРАВЛЕНИЕ: Используем строковые литералы вместо констант ---<<<
            switch (typeName) {
                case "PassengerCar": // Используем строку "PassengerCar"
                    vehicle = new PassengerCar(capacity, baggageCapacity, comfortLevel, additionalInfo != null ? additionalInfo : "");
                    break;
                case "RestaurantCar": // Используем строку "RestaurantCar"
                    int tables = (additionalInfo != null && !additionalInfo.isEmpty()) ? Integer.parseInt(additionalInfo) : 0;
                    vehicle = new RestaurantCar(capacity, baggageCapacity, comfortLevel, tables);
                    break;
                case "BaggageCar": // Используем строку "BaggageCar"
                    double maxWeight = (additionalInfo != null && !additionalInfo.isEmpty()) ? Double.parseDouble(additionalInfo) : 0.0;
                    vehicle = new BaggageCar(capacity, baggageCapacity, comfortLevel, maxWeight);
                    break;
                case "Locomotive": // Используем строку "Locomotive"
                    int traction = (additionalInfo != null && !additionalInfo.isEmpty()) ? Integer.parseInt(additionalInfo) : 0;
                    vehicle = new Locomotive(capacity, baggageCapacity, comfortLevel, traction);
                    break;
                default:
                    // Если в БД есть тип, который мы не знаем - ошибка
                    logger.error("Unknown TrainCarType ('" + typeName + "') encountered during vehicle creation from DB for car ID: " + id);
                    throw new SQLException("Unknown TrainCarType from DB: " + typeName);
            }
            // >>>------------------------------------------------------------------->>>

            vehicle.setId(id); // Устанавливаем ID, прочитанный из БД
            logger.trace("Successfully created Vehicle object: " + vehicle);
            return vehicle;

        } catch (NumberFormatException nfe) {
            // Ошибка при парсинге additionalInfo в число
            logger.error("Error parsing numeric additional_info ('" + additionalInfo + "') from database for car ID: " + id + ", TypeName: '" + typeName + "'", nfe);
            throw new SQLException("Error parsing numeric data for vehicle ID " + id + " from database", nfe);
        } catch (IllegalArgumentException iae) {
            // Ошибки валидации в конструкторах Vehicle
            logger.error("Error validating data during vehicle creation from DB for car ID: " + id + ", TypeName: '" + typeName + "'", iae);
            throw new SQLException("Invalid data constraints for vehicle ID " + id + " found in database", iae);
        } catch (SQLException sqlEx) {
            // Другие ошибки чтения из ResultSet
            logger.error("SQL error creating vehicle from ResultSet for potential car ID: " + id + ", TypeName: '" + typeName + "'", sqlEx);
            throw sqlEx; // Пробрасываем дальше
        }
    }
} // Конец класса TrainDao