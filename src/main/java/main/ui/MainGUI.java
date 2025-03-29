package main.ui;

import com.transportcompany.db.TrainDao; // DAO для поездов и управления сервером H2
import com.transportcompany.db.UserDao; // DAO для пользователей
import org.apache.log4j.Logger; // Логгер
import org.apache.log4j.PropertyConfigurator; // Для конфигурации Log4j из файла
import org.apache.log4j.BasicConfigurator; // Для базовой конфигурации Log4j

import javax.swing.*;
import java.awt.*;
import java.net.URL; // Для поиска ресурса log4j.properties

/**
 * <p>Главное окно приложения (Swing JFrame), использующее {@link CardLayout}
 * для переключения между различными панелями (Вход, Панель администратора, Панель пользователя).</p>
 * <p>Отвечает за:</p>
 * <ul>
 *     <li>Инициализацию приложения: настройку Log4j, создание экземпляров DAO (что запускает сервер H2),
 *         создание основного контейнера с CardLayout.</li>
 *     <li>Создание и отображение начальной панели входа ({@link LoginPanel}).</li>
 *     <li>Предоставление методов ({@code showAdminPanel}, {@code showUserPanel}, {@code showLoginPanel})
 *         для переключения между основными панелями приложения.</li>
 *     <li>Обработку закрытия окна: корректную остановку сервера H2 (через {@link TrainDao#stopServer()})
 *         и завершение работы приложения.</li>
 *     <li>Отображение критических ошибок при инициализации и завершение работы при необходимости.</li>
 * </ul>
 *
 * <p><b>Жизненный цикл:</b></p>
 * <ol>
 *     <li>Конструктор: Настраивает Log4j, создает DAO, создает GUI, добавляет обработчик закрытия, делает окно видимым.</li>
 *     <li>Приложение показывает {@link LoginPanel}.</li>
 *     <li>{@link LoginPanel} после успешного входа вызывает {@code showAdminPanel} или {@code showUserPanel}.</li>
 *     <li>{@code showAdminPanel}/{@code showUserPanel} создают (если нужно) и показывают соответствующую панель.</li>
 *     <li>Панели {@code AdminPanel} и {@code UserPanel} при выходе вызывают {@code showLoginPanel}.</li>
 *     <li>{@code showLoginPanel} удаляет панели админа/пользователя и показывает панель входа.</li>
 *     <li>При закрытии окна вызывается {@code shutdownApplication}, который останавливает сервер H2 и завершает JVM.</li>
 * </ol>
 *
 * @see JFrame Базовый класс окна приложения.
 * @see CardLayout Менеджер компоновки для переключения панелей.
 * @see LoginPanel Панель входа и регистрации.
 * @see AdminPanel Панель для администратора.
 * @see UserPanel Панель для обычного пользователя.
 * @see TrainDao DAO для поездов, управляет сервером H2.
 * @see UserDao DAO для пользователей.
 */
public class MainGUI extends JFrame {
    // Логгер для главного окна
    private static final Logger logger = Logger.getLogger(MainGUI.class);

    // Компоненты UI
    private final CardLayout cardLayout; // Менеджер переключения панелей
    private final JPanel mainPanel;    // Основная панель, содержащая другие панели

    // Дочерние панели (могут быть null до первого показа)
    private LoginPanel loginPanel;
    private AdminPanel adminPanel;
    private UserPanel userPanel;

    // DAO - создаются один раз при старте
    private TrainDao trainDao;
    private UserDao userDao;

    /**
     * Конструктор главного окна приложения.
     * Выполняет всю необходимую инициализацию.
     */
    public MainGUI() {
        super("Система Управления Поездами"); // Заголовок окна

        // --- 1. Конфигурация Log4j (ВАЖНО: сделать до первого логгирования) ---
        configureLogging();

        logger.info("========================================================");
        logger.info("Application Starting...");
        logger.info("========================================================");

        // --- 2. Настройка Основного Окна ---
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Закрытие управляется вручную через listener
        setSize(850, 650); // Увеличим размер по умолчанию
        setMinimumSize(new Dimension(700, 500)); // Установим минимальный размер

        // --- 3. Инициализация DAO и Сервера БД ---
        // Эти операции могут выбросить RuntimeException при критических ошибках
        try {
            logger.info("Initializing TrainDao (this will start H2 server if not running)...");
            trainDao = new TrainDao(); // Создание TrainDao запускает сервер H2 и инициализирует таблицы
            logger.info("TrainDao initialized successfully.");

            logger.info("Initializing UserDao...");
            userDao = new UserDao(); // UserDao использует тот же сервер и БД
            logger.info("UserDao initialized successfully.");
        } catch (RuntimeException e) {
            // Критическая ошибка при инициализации DAO (например, порт занят, ошибка файла БД)
            logger.fatal("CRITICAL FAILURE during DAO initialization!", e);
            // Показываем сообщение и немедленно завершаем работу
            showErrorAndExit("Критическая ошибка инициализации базы данных:\n" + e.getMessage() + "\nПриложение будет закрыто.");
            // Важно: конструктор прервется здесь, окно не будет создано до конца.
            // Инициализируем поля, чтобы избежать NPE в редких случаях.
            cardLayout = null;
            mainPanel = null;
            return;
        }

        // --- 4. Настройка GUI Панелей ---
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Создаем и добавляем панель входа (она нужна всегда)
        logger.debug("Creating LoginPanel...");
        loginPanel = new LoginPanel(this, userDao); // Передаем ссылку на MainGUI и UserDao
        mainPanel.add(loginPanel, "login"); // Добавляем с именем "login"
        logger.debug("LoginPanel created and added to main panel.");

        // Панели AdminPanel и UserPanel будут создаваться по мере необходимости в методах show...

        // Добавляем основную панель в окно
        add(mainPanel);

        // --- 5. Обработчик Закрытия Окна ---
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                logger.info("Window closing event detected by user.");
                // Спрашиваем подтверждение перед выходом
                int choice = JOptionPane.showConfirmDialog(MainGUI.this,
                        "Вы действительно хотите выйти из приложения?",
                        "Подтверждение выхода",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                if (choice == JOptionPane.YES_OPTION) {
                    logger.debug("User confirmed exit.");
                    // Выполняем корректное завершение работы
                    shutdownApplication();
                } else {
                    logger.debug("User cancelled exit.");
                    // Ничего не делаем, окно остается открытым
                }
            }
        });

        // --- 6. Финальные штрихи и Отображение Окна ---
        setLocationRelativeTo(null); // Центрируем окно на экране
        setVisible(true);            // Делаем окно видимым
        cardLayout.show(mainPanel, "login"); // Показываем панель входа при старте
        loginPanel.requestFocusOnUsername(); // Устанавливаем фокус на поле имени пользователя

        logger.info("MainGUI initialization complete. Application is running and LoginPanel is visible.");
    }

    /**
     * Настраивает систему логгирования Log4j.
     * Пытается загрузить конфигурацию из файла `log4j.properties` в classpath.
     * Если файл не найден, использует базовую конфигурацию (вывод INFO и выше в консоль).
     */
    private void configureLogging() {
        // Ищем файл log4j.properties в корне classpath
        URL log4jResource = getClass().getClassLoader().getResource("log4j.properties");
        if (log4jResource != null) {
            try {
                PropertyConfigurator.configure(log4jResource);
                System.out.println("Log4j configured using: " + log4jResource.getPath()); // Используем System.out до инициализации логгера
                logger.info("Log4j configuration loaded successfully from: " + log4jResource.getPath());
            } catch (Exception e) {
                // Если ошибка при чтении файла конфигурации
                System.err.println("Error configuring Log4j from " + log4jResource.getPath() + ": " + e.getMessage());
                BasicConfigurator.configure(); // Используем базовую конфигурацию
                logger.error("Failed to configure Log4j from properties file. Using BasicConfigurator.", e);
            }
        } else {
            // Файл log4j.properties не найден в classpath
            System.out.println("log4j.properties not found in classpath. Using BasicConfigurator (console output).");
            BasicConfigurator.configure(); // Базовая конфигурация (вывод INFO в консоль)
            logger.warn("log4j.properties not found in classpath. Using default BasicConfigurator.");
        }
    }

    /**
     * Отображает диалоговое окно с сообщением о критической ошибке и
     * немедленно завершает работу приложения с кодом выхода 1.
     * @param message Сообщение об ошибке для пользователя.
     */
    private void showErrorAndExit(String message) {
        // Показываем сообщение в потоке EDT
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null, // null родитель, т.к. окно может быть еще не полностью создано
                        message,
                        "Критическая ошибка",
                        JOptionPane.ERROR_MESSAGE)
        );
        // Даем немного времени на отображение сообщения перед выходом
        try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        logger.fatal("Exiting application due to critical error: " + message);
        System.exit(1); // Немедленный выход
    }

    /**
     * Выполняет последовательность действий для корректного завершения работы приложения:
     * останавливает сервер H2, освобождает ресурсы окна и завершает JVM с кодом 0.
     */
    private void shutdownApplication() {
        logger.info("Initiating application shutdown sequence...");
        // 1. Останавливаем сервер H2
        logger.debug("Stopping H2 database server...");
        TrainDao.stopServer(); // Вызываем статический метод DAO
        logger.info("H2 server stopped.");

        // 2. Освобождаем ресурсы графического окна
        logger.debug("Disposing MainGUI window...");
        dispose(); // Закрывает окно и освобождает его ресурсы
        logger.debug("MainGUI window disposed.");

        // 3. Завершаем JVM
        logger.info("Exiting application with status 0.");
        System.exit(0); // Нормальное завершение
    }

    // --- Методы для Переключения Панелей ---

    /**
     * Создает (если необходимо) и отображает панель администратора ({@link AdminPanel}).
     * Переключает {@code CardLayout} на отображение панели "admin".
     * @param user Объект {@link UserDao.User} вошедшего администратора. Не должен быть null.
     */
    public void showAdminPanel(UserDao.User user) {
        if (user == null) {
            logger.error("Attempted to show AdminPanel with a null user! Aborting.");
            return; // Нельзя показать панель без пользователя
        }
        if (!"admin".equals(user.getRole())) {
            logger.error("Attempted to show AdminPanel for a non-admin user: " + user + "! Aborting.");
            // Можно показать сообщение об ошибке или перенаправить на панель пользователя
            showLoginPanel(); // Возвращаем на логин в случае ошибки роли
            return;
        }

        // Создаем AdminPanel, если она еще не создана (ленивая инициализация)
        if (adminPanel == null) {
            logger.debug("AdminPanel instance is null. Creating a new one...");
            // Передаем все необходимые зависимости: self, DAO, user
            adminPanel = new AdminPanel(this, trainDao, user);
            // Добавляем созданную панель в mainPanel с именем "admin"
            mainPanel.add(adminPanel, "admin");
            logger.debug("New AdminPanel created and added to the main panel.");
        } else {
            logger.debug("AdminPanel instance already exists. Reusing.");
            // TODO: Если несколько админов могут входить по очереди, здесь нужно обновить
            // информацию о текущем админе в существующей панели 'adminPanel'.
            // adminPanel.setCurrentUser(user); // Пример (метод нужно добавить в AdminPanel)
        }
        // Переключаем CardLayout на отображение панели "admin"
        cardLayout.show(mainPanel, "admin");
        logger.info("Switched view to AdminPanel for user: '" + user.getUsername() + "'");
    }

    /**
     * Создает (если необходимо) и отображает панель обычного пользователя ({@link UserPanel}).
     * Переключает {@code CardLayout} на отображение панели "user".
     * При каждом показе обновляет список поездов в {@code UserPanel}.
     * @param user Объект {@link UserDao.User} вошедшего пользователя. Не должен быть null.
     */
    public void showUserPanel(UserDao.User user) {
        if (user == null) {
            logger.error("Attempted to show UserPanel with a null user! Aborting.");
            return;
        }
        // Создаем UserPanel, если она еще не создана
        if (userPanel == null) {
            logger.debug("UserPanel instance is null. Creating a new one...");
            // Передаем зависимости: self, DAO, user
            userPanel = new UserPanel(this, trainDao, user);
            // Добавляем в mainPanel с именем "user"
            mainPanel.add(userPanel, "user");
            logger.debug("New UserPanel created and added to the main panel.");
        } else {
            logger.debug("UserPanel instance already exists. Reusing.");
            // TODO: Обновить информацию о пользователе, если нужно
            // userPanel.setCurrentUser(user); // Пример
            // При каждом показе существующей панели обновляем список поездов
            logger.debug("Refreshing train list in existing UserPanel.");
            userPanel.refreshTrainList();
        }
        // Переключаем CardLayout на отображение панели "user"
        cardLayout.show(mainPanel, "user");
        logger.info("Switched view to UserPanel for user: '" + user.getUsername() + "'");
    }

    /**
     * Отображает панель входа ({@link LoginPanel}).
     * Удаляет экземпляры панелей администратора и пользователя (если они были созданы),
     * чтобы при следующем входе они были созданы заново (простой способ сброса состояния).
     * Устанавливает фокус на поле имени пользователя.
     */
    public void showLoginPanel() {
        logger.debug("Switching view to LoginPanel.");
        // Удаляем панели админа и пользователя, чтобы они пересоздались при следующем входе
        if (adminPanel != null) {
            logger.trace("Removing existing AdminPanel instance.");
            mainPanel.remove(adminPanel);
            adminPanel = null; // Сбрасываем ссылку
        }
        if (userPanel != null) {
            logger.trace("Removing existing UserPanel instance.");
            mainPanel.remove(userPanel);
            userPanel = null; // Сбрасываем ссылку
        }

        // Панель логина (loginPanel) обычно не удаляется, просто показывается снова
        if (loginPanel == null) {
            // Эта ситуация не должна возникать, т.к. loginPanel создается в конструкторе
            logger.error("LoginPanel is null when trying to show it! Recreating...");
            loginPanel = new LoginPanel(this, userDao);
            mainPanel.add(loginPanel, "login");
        }

        // Переключаемся на панель "login"
        cardLayout.show(mainPanel, "login");
        logger.info("Switched view to LoginPanel.");

        // Устанавливаем фокус на поле имени пользователя
        loginPanel.requestFocusOnUsername();
        // Очищаем поля на панели логина (опционально)
        // loginPanel.clearInputFields();
    }

    // Точка входа main() находится в классе Main
} // Конец класса MainGUI