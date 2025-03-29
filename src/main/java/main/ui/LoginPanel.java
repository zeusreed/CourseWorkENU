package main.ui;

import com.transportcompany.db.UserDao; // Для аутентификации
import com.transportcompany.exception.TrainValidationException; // Для обработки ошибок входа/регистрации
import org.apache.log4j.Logger; // Логгер

import javax.swing.*;
import java.awt.*;
import java.util.Arrays; // Для очистки массива пароля

/**
 * <p>Панель пользовательского интерфейса (Swing JPanel) для входа и регистрации пользователей.</p>
 * <p>Предоставляет поля для ввода имени пользователя и пароля, а также кнопки "Войти" и "Зарегистрироваться".</p>
 *
 * <p><b>Функционал:</b></p>
 * <ul>
 *     <li>Сбор учетных данных пользователя.</li>
 *     <li>Вызов методов {@link UserDao} для проверки учетных данных ({@code loginUser})
 *         или регистрации нового пользователя ({@code registerUser}).</li>
 *     <li>Отображение сообщений об успехе или ошибке в метке {@code messageLabel}.</li>
 *     <li>Взаимодействие с {@link MainGUI} для переключения на соответствующую панель
 *         ({@link AdminPanel} или {@link UserPanel}) после успешного входа.</li>
 *     <li>Базовая валидация ввода (проверка на пустые поля, минимальная длина пароля при регистрации).</li>
 *     <li>Очистка поля пароля после попытки входа/регистрации из соображений безопасности.</li>
 * </ul>
 *
 * <p><b>Структура:</b></p>
 * <ul>
 *     <li>Использует {@link GridBagLayout} для гибкого размещения компонентов.</li>
 *     <li>Содержит {@link JLabel}, {@link JTextField} (для имени), {@link JPasswordField} (для пароля),
 *         {@link JButton} (Войти, Зарегистрироваться) и {@link JLabel} (для сообщений).</li>
 * </ul>
 *
 * @see UserDao Используется для взаимодействия с данными пользователей.
 * @see MainGUI Родительское окно, управляющее переключением панелей.
 * @see AdminPanel Панель, отображаемая для администраторов.
 * @see UserPanel Панель, отображаемая для обычных пользователей.
 * @see JPanel Базовый класс Swing.
 * @see GridBagLayout Менеджер компоновки.
 */
public class LoginPanel extends JPanel {
    // Логгер для событий панели входа
    private static final Logger logger = Logger.getLogger(LoginPanel.class);

    // Компоненты UI
    private final JTextField usernameField;    // Поле для ввода имени пользователя
    private final JPasswordField passwordField;  // Поле для ввода пароля
    private final JButton loginButton;        // Кнопка "Войти"
    private final JButton registerButton;     // Кнопка "Зарегистрироваться"
    private final JLabel messageLabel;       // Метка для сообщений пользователю

    // Зависимости
    private final MainGUI mainGUI; // Ссылка на главное окно для переключения панелей
    private final UserDao userDao; // Ссылка на DAO для работы с пользователями

    /**
     * Конструктор панели входа/регистрации.
     *
     * @param mainGUI Ссылка на главное окно {@link MainGUI}.
     * @param userDao Экземпляр {@link UserDao} для взаимодействия с БД пользователей.
     */
    public LoginPanel(MainGUI mainGUI, UserDao userDao) {
        // Проверка зависимостей
        if (mainGUI == null || userDao == null) {
            throw new IllegalArgumentException("MainGUI and UserDao cannot be null for LoginPanel.");
        }
        this.mainGUI = mainGUI;
        this.userDao = userDao;

        logger.debug("Initializing LoginPanel...");

        // --- Настройка менеджера компоновки GridBagLayout ---
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        // Общие настройки для GridBagConstraints
        gbc.insets = new Insets(5, 10, 5, 10); // Внешние отступы для ячеек (top, left, bottom, right)
        gbc.anchor = GridBagConstraints.WEST; // Выравнивание компонентов по левому краю ячейки

        // --- Создание и добавление компонентов ---

        // 1. Метка и поле для имени пользователя
        gbc.gridx = 0; // Колонка 0
        gbc.gridy = 0; // Строка 0
        gbc.gridwidth = 1; // Занимает 1 колонку
        gbc.fill = GridBagConstraints.NONE; // Не растягивать метку
        gbc.weightx = 0.0; // Не давать доп. пространство метке
        add(new JLabel("Имя пользователя:"), gbc);

        gbc.gridx = 1; // Колонка 1
        gbc.gridy = 0; // Строка 0
        gbc.fill = GridBagConstraints.HORIZONTAL; // Растягивать поле по горизонтали
        gbc.weightx = 1.0; // Давать полю все доступное доп. пространство по горизонтали
        usernameField = new JTextField(20); // Начальная ширина ~20 символов
        add(usernameField, gbc);

        // 2. Метка и поле для пароля
        gbc.gridx = 0; // Колонка 0
        gbc.gridy = 1; // Строка 1
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        add(new JLabel("Пароль:"), gbc);

        gbc.gridx = 1; // Колонка 1
        gbc.gridy = 1; // Строка 1
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        passwordField = new JPasswordField(20);
        add(passwordField, gbc);

        // 3. Панель с кнопками "Войти" и "Зарегистрироваться"
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0)); // Кнопки по центру, отступ между ними
        loginButton = new JButton("Войти");
        loginButton.setToolTipText("Войти в систему с указанными данными");
        registerButton = new JButton("Зарегистрироваться");
        registerButton.setToolTipText("Создать новую учетную запись пользователя");
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);

        gbc.gridx = 0; // Колонка 0
        gbc.gridy = 2; // Строка 2
        gbc.gridwidth = 2; // Занимает обе колонки
        gbc.fill = GridBagConstraints.NONE; // Не растягивать панель кнопок
        gbc.anchor = GridBagConstraints.CENTER; // Центрировать панель кнопок
        gbc.weightx = 0.0;
        add(buttonPanel, gbc);

        // 4. Метка для сообщений пользователю
        messageLabel = new JLabel(" ", SwingConstants.CENTER); // Пробел для резерва места, выравнивание по центру
        messageLabel.setForeground(Color.RED); // По умолчанию цвет для ошибок
        messageLabel.setPreferredSize(new Dimension(300, 20)); // Задаем предпочтительную высоту
        gbc.gridx = 0; // Колонка 0
        gbc.gridy = 3; // Строка 3
        gbc.gridwidth = 2; // Занимает обе колонки
        gbc.fill = GridBagConstraints.HORIZONTAL; // Растягивать по горизонтали
        gbc.anchor = GridBagConstraints.CENTER;
        add(messageLabel, gbc);

        // --- Назначение обработчиков событий ---
        loginButton.addActionListener(e -> login());
        registerButton.addActionListener(e -> register());
        // Можно добавить обработчик на Enter в полях ввода
        passwordField.addActionListener(e -> login()); // По Enter в поле пароля - пытаемся войти
        usernameField.addActionListener(e -> passwordField.requestFocusInWindow()); // По Enter в имени - переходим к паролю

        // --- Установка фокуса при открытии ---
        // Делаем это позже, после того как панель станет видимой (в MainGUI.showLoginPanel)
        // SwingUtilities.invokeLater(usernameField::requestFocusInWindow);

        logger.debug("LoginPanel components created and listeners assigned.");
    }

    /**
     * <p>Обработчик нажатия кнопки "Войти".</p>
     * <p>Получает имя пользователя и пароль, выполняет базовую проверку на пустоту.
     * Вызывает {@code userDao.loginUser()} для аутентификации.
     * В случае успеха вызывает {@code mainGUI.showAdminPanel()} или {@code mainGUI.showUserPanel()}
     * в зависимости от роли пользователя.
     * В случае неудачи или ошибки отображает сообщение в {@code messageLabel}.</p>
     * <p>Всегда очищает поле пароля после выполнения.</p>
     */
    private void login() {
        String username = usernameField.getText().trim();
        char[] passwordChars = passwordField.getPassword(); // Получаем пароль как массив символов
        String password = new String(passwordChars); // Преобразуем в строку для передачи в DAO
        // Немедленно очищаем массив символов из памяти для безопасности
        Arrays.fill(passwordChars, ' ');

        logger.debug("Login button action performed. Attempting login for username: '" + username + "'");

        // 1. Базовая валидация на клиенте
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setForeground(Color.RED);
            messageLabel.setText("Пожалуйста, заполните имя пользователя и пароль.");
            logger.warn("Login attempt failed: username or password field is empty.");
            clearPasswordOnly(); // Пароль в любом случае уже стерт из массива, но очистим и поле
            return;
        }

        // 2. Попытка аутентификации через DAO
        try {
            logger.debug("Calling userDao.loginUser...");
            UserDao.User user = userDao.loginUser(username, password); // DAO вернет User или null

            // 3. Обработка результата
            if (user != null) {
                // Успешный вход
                messageLabel.setText(" "); // Очищаем сообщение об ошибке
                logger.info("Login successful for user: '" + user.getUsername() + "' with role: '" + user.getRole() + "'");

                // Переключаем на соответствующую панель
                if ("admin".equals(user.getRole())) {
                    logger.debug("User is admin. Showing Admin Panel...");
                    mainGUI.showAdminPanel(user);
                } else if ("user".equals(user.getRole())) {
                    logger.debug("User is regular user. Showing User Panel...");
                    mainGUI.showUserPanel(user);
                } else {
                    // Неизвестная роль - ошибка в данных или логике
                    logger.error("Unknown user role encountered after successful login for user '"
                            + user.getUsername() + "': '" + user.getRole() + "'");
                    messageLabel.setForeground(Color.RED);
                    messageLabel.setText("Ошибка: Неизвестная роль пользователя. Обратитесь к администратору.");
                }
                // Очищаем оба поля после успешного входа и перехода на другую панель
                clearInputFields();

            } else {
                // Неудачный вход (пользователь не найден или пароль неверный)
                logger.warn("Login failed for user: '" + username + "' (invalid username or password).");
                messageLabel.setForeground(Color.RED);
                messageLabel.setText("Неверное имя пользователя или пароль.");
                clearPasswordOnly(); // Очищаем только пароль, имя оставляем
            }
        } catch (TrainValidationException e) {
            // Ошибка при взаимодействии с БД (перехваченная SQLException)
            logger.error("TrainValidationException during login for user: '" + username + "'", e);
            messageLabel.setForeground(Color.RED);
            // Показываем сообщение из исключения
            messageLabel.setText("Ошибка входа: " + e.getMessage());
            clearPasswordOnly();
        } catch (Exception e) {
            // Другие неожиданные ошибки
            logger.fatal("Unexpected exception during login processing for user: '" + username + "'", e);
            messageLabel.setForeground(Color.RED);
            messageLabel.setText("Критическая ошибка при входе. См. лог.");
            clearPasswordOnly();
        }
    }

    /**
     * <p>Обработчик нажатия кнопки "Зарегистрироваться".</p>
     * <p>Получает имя пользователя и пароль, выполняет базовую валидацию (пустота, длина пароля).
     * Вызывает {@code userDao.registerUser()} для создания нового пользователя с ролью "user".
     * Отображает сообщение об успехе или ошибке в {@code messageLabel}.</p>
     * <p>Всегда очищает поле пароля после выполнения.</p>
     */
    private void register() {
        String username = usernameField.getText().trim();
        char[] passwordChars = passwordField.getPassword();
        String password = new String(passwordChars);
        Arrays.fill(passwordChars, ' '); // Очищаем массив

        logger.debug("Register button action performed. Attempting registration for username: '" + username + "'");

        // 1. Базовая валидация на клиенте
        if (username.isEmpty() || password.isEmpty()) {
            messageLabel.setForeground(Color.RED);
            messageLabel.setText("Пожалуйста, заполните имя и пароль для регистрации.");
            logger.warn("Registration attempt failed: username or password field is empty.");
            clearPasswordOnly();
            return;
        }
        // Пример проверки минимальной длины пароля
        final int MIN_PASSWORD_LENGTH = 6;
        if (password.length() < MIN_PASSWORD_LENGTH) {
            messageLabel.setForeground(Color.RED);
            messageLabel.setText("Пароль должен содержать не менее " + MIN_PASSWORD_LENGTH + " символов.");
            logger.warn("Registration attempt failed for user '" + username + "': password too short.");
            clearPasswordOnly();
            return;
        }
        // TODO: Добавить более строгую проверку сложности пароля, если требуется

        // 2. Попытка регистрации через DAO (с ролью "user" по умолчанию)
        try {
            userDao.registerUser(username, password, "user");
            // Успешная регистрация
            messageLabel.setForeground(Color.BLUE); // Используем синий цвет для успеха
            messageLabel.setText("Регистрация прошла успешно. Теперь вы можете войти.");
            logger.info("Registration successful for new user: '" + username + "'");
            clearInputFields(); // Очищаем оба поля после успешной регистрации

        } catch (TrainValidationException e) {
            // Ошибка регистрации (чаще всего - имя занято, или ошибка БД)
            messageLabel.setForeground(Color.RED);
            messageLabel.setText("Ошибка регистрации: " + e.getMessage());
            logger.warn("TrainValidationException during registration for user '" + username + "': " + e.getMessage());
            clearPasswordOnly();
        } catch (Exception e) {
            // Другие неожиданные ошибки
            messageLabel.setForeground(Color.RED);
            logger.error("Unexpected exception during registration for user: '" + username + "'", e);
            messageLabel.setText("Неожиданная ошибка при регистрации. См. лог.");
            clearPasswordOnly();
        }
    }

    /**
     * Очищает поля ввода имени пользователя и пароля, а также метку сообщений.
     * Вызывается после успешного входа или регистрации.
     * Должен выполняться в потоке EDT.
     */
    private void clearInputFields() {
        // Проверяем, выполняется ли в EDT, или используем invokeLater
        if (SwingUtilities.isEventDispatchThread()) {
            usernameField.setText("");
            passwordField.setText("");
            messageLabel.setText(" "); // Сбрасываем сообщение
            logger.trace("Username and password fields cleared.");
        } else {
            SwingUtilities.invokeLater(() -> {
                usernameField.setText("");
                passwordField.setText("");
                messageLabel.setText(" ");
                logger.trace("Username and password fields cleared via invokeLater.");
            });
        }
    }

    /**
     * Очищает только поле ввода пароля.
     * Вызывается после неудачной попытки входа или регистрации.
     * Должен выполняться в потоке EDT.
     */
    private void clearPasswordOnly() {
        if (SwingUtilities.isEventDispatchThread()) {
            passwordField.setText("");
            logger.trace("Password field cleared.");
        } else {
            SwingUtilities.invokeLater(() -> {
                passwordField.setText("");
                logger.trace("Password field cleared via invokeLater.");
            });
        }
    }

    /**
     * Метод для установки фокуса на поле имени пользователя.
     * Вызывается извне (MainGUI), когда панель становится видимой.
     */
    public void requestFocusOnUsername() {
        SwingUtilities.invokeLater(usernameField::requestFocusInWindow);
        logger.trace("Requested focus on username field.");
    }

} // Конец класса LoginPanel