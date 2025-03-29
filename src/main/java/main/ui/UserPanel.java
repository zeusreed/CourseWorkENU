package main.ui;

import com.transportcompany.db.TrainDao; // DAO для получения данных
import com.transportcompany.db.UserDao; // Для данных пользователя
import com.transportcompany.exception.TrainValidationException; // Обработка ошибок DAO
import com.transportcompany.train.Train; // Для отображения данных
import org.apache.log4j.Logger; // Логгер

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener; // Для работы со слушателем ComboBox
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>Панель пользовательского интерфейса (Swing JPanel) для обычного пользователя ("user").</p>
 * <p>Предоставляет функциональность просмотра информации о поездах и их вагонах, но без возможности редактирования.</p>
 *
 * <p><b>Функционал:</b></p>
 * <ul>
 *     <li>Отображение имени текущего пользователя.</li>
 *     <li>Выпадающий список ({@link JComboBox}) для выбора поезда из доступных в БД.</li>
 *     <li>Отображение основной информации о выбранном поезде (через {@link TrainInfoPanel}).</li>
 *     <li>Отображение списка вагонов выбранного поезда в таблице (через {@link WagonsTablePanel}).</li>
 *     <li>Возможность фильтрации и сортировки вагонов в таблице (функционал {@link WagonsTablePanel}).</li>
 *     <li>Кнопка/меню для выхода из системы.</li>
 * </ul>
 *
 * <p><b>Структура:</b></p>
 * <ul>
 *     <li>Использует {@link BorderLayout}.</li>
 *     <li>Вверху ({@code NORTH}) находится составная панель {@code topPanel}, включающая:
 *         <ul>
 *             <li>Меню {@link JMenuBar} с опцией "Выйти из системы".</li>
 *             <li>Панель {@code topSelectionPanel} с меткой пользователя и {@link JComboBox} для выбора поезда.</li>
 *             <li>Панель {@link TrainInfoPanel} для отображения деталей поезда.</li>
 *         </ul>
 *     </li>
 *     <li>В центре ({@code CENTER}) расположена панель {@link WagonsTablePanel} с таблицей вагонов.</li>
 * </ul>
 *
 * <p><b>Взаимодействие:</b></p>
 * <ul>
 *     <li>Получает ссылку на {@link TrainDao} для загрузки списка поездов и данных выбранного поезда.</li>
 *     <li>Получает ссылку на {@link MainGUI} для выполнения выхода из системы (переключения на {@link LoginPanel}).</li>
 *     <li>При выборе поезда в {@code JComboBox} вызывает метод {@link #loadSelectedTrain()}, который загружает
 *         данные через DAO и обновляет {@code TrainInfoPanel} и {@code WagonsTablePanel}.</li>
 *     <li>Метод {@link #refreshTrainList()} вызывается извне ({@code MainGUI}) при показе этой панели,
 *         чтобы обновить список поездов в {@code JComboBox} на случай, если админ добавил/удалил поезда.</li>
 * </ul>
 *
 * @see TrainDao Используется для чтения данных о поездах.
 * @see UserDao.User Используется для отображения имени пользователя.
 * @see MainGUI Родительское окно, управляет переключением панелей.
 * @see TrainInfoPanel Дочерняя панель для информации о поезде.
 * @see WagonsTablePanel Дочерняя панель для отображения вагонов.
 * @see JPanel Базовый класс Swing.
 */
public class UserPanel extends JPanel {
    // Логгер для событий панели пользователя
    private static final Logger logger = Logger.getLogger(UserPanel.class);

    // Компоненты UI (final убран у инициализируемых в createTopPanel)
    private TrainInfoPanel trainInfoPanel;   // <<<--- final УБРАН
    private final WagonsTablePanel wagonsTablePanel; // Эта панель создается в конструкторе
    private JComboBox<String> trainComboBox; // <<<--- final УБРАН
    private JLabel userInfoLabel; // <<<--- final УБРАН

    // Зависимости
    private final TrainDao trainDao;     // DAO для поездов
    private final MainGUI mainGUI;       // Главное окно
    private final UserDao.User currentUser; // Текущий пользователь

    /**
     * Конструктор панели пользователя.
     *
     * @param mainGUI Ссылка на главное окно приложения (для выхода).
     * @param trainDao DAO для доступа к данным поездов.
     * @param user Объект текущего пользователя.
     */
    public UserPanel(MainGUI mainGUI, TrainDao trainDao, UserDao.User user) {
        if (mainGUI == null || trainDao == null || user == null) {
            throw new IllegalArgumentException("MainGUI, TrainDao, and User cannot be null for UserPanel.");
        }
        this.mainGUI = mainGUI;
        this.trainDao = trainDao;
        this.currentUser = user;
        logger.info("Initializing UserPanel for user: '" + user.getUsername() + "'");

        // Настройка основной панели
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // --- Создание верхней части (Меню + Выбор + Инфо) ---
        // Этот метод инициализирует trainInfoPanel, trainComboBox, userInfoLabel
        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        // --- Создание центральной части (Таблица вагонов) ---
        // Передаем null вместо контроллера, т.к. UserPanel не использует его функции
        wagonsTablePanel = new WagonsTablePanel(null);
        add(wagonsTablePanel, BorderLayout.CENTER);

        // Первичная загрузка списка поездов в ComboBox
        loadTrainsIntoComboBox();
        logger.debug("UserPanel initialization complete.");
    }

    /**
     * Вспомогательный метод для создания верхней части UI (Меню, Выбор, Инфо).
     * Инициализирует поля {@code trainInfoPanel}, {@code trainComboBox}, {@code userInfoLabel}.
     * @return Готовая панель {@link JPanel}.
     */
    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());

        // 1. Меню (только с выходом)
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Файл");
        JMenuItem logoutMenuItem = new JMenuItem("Выйти из системы");
        logoutMenuItem.setToolTipText("Завершить сеанс и вернуться к экрану входа");
        logoutMenuItem.addActionListener(e -> {
            logger.info("Logout action triggered by user: '" + currentUser.getUsername() + "'");
            mainGUI.showLoginPanel(); // Вызываем метод главного окна
        });
        fileMenu.add(logoutMenuItem);
        menuBar.add(fileMenu);
        topPanel.add(menuBar, BorderLayout.NORTH); // Меню сверху

        // 2. Панель выбора поезда
        JPanel topSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        // Инициализация метки и комбо-бокса ЗДЕСЬ
        userInfoLabel = new JLabel("Пользователь: " + currentUser.getUsername() + " | Выберите поезд:");
        trainComboBox = new JComboBox<>();
        trainComboBox.setPreferredSize(new Dimension(250, trainComboBox.getPreferredSize().height));
        trainComboBox.setToolTipText("Выберите поезд для просмотра информации");
        // Слушатель для обработки выбора поезда
        trainComboBox.addActionListener(e -> {
            // Реагируем только на явное изменение выбора пользователем
            if ("comboBoxChanged".equals(e.getActionCommand()) && trainComboBox.getSelectedItem() != null) {
                loadSelectedTrain();
            }
        });
        topSelectionPanel.add(userInfoLabel);
        topSelectionPanel.add(trainComboBox);
        topPanel.add(topSelectionPanel, BorderLayout.CENTER); // Панель выбора под меню

        // 3. Панель информации о поезде
        // Инициализация ЗДЕСЬ
        trainInfoPanel = new TrainInfoPanel();
        topPanel.add(trainInfoPanel, BorderLayout.SOUTH); // Панель инфо внизу верхней части

        return topPanel;
    }


    /**
     * <p>Обновляет список поездов в выпадающем списке {@code trainComboBox}.</p>
     * <p>Вызывается из {@link MainGUI} при каждом отображении {@code UserPanel},
     * чтобы пользователь видел актуальный список поездов.</p>
     */
    public void refreshTrainList() {
        logger.debug("Refreshing train list in UserPanel ComboBox requested.");
        // Просто вызываем метод загрузки, он обработает обновление
        loadTrainsIntoComboBox();
    }


    /**
     * Загружает/перезагружает список номеров поездов из DAO в JComboBox.
     * Пытается сохранить текущий выбор, если это возможно.
     */
    private void loadTrainsIntoComboBox() {
        logger.debug("Loading/Refreshing trains into UserPanel ComboBox...");
        String previouslySelectedItem = (String) trainComboBox.getSelectedItem(); // Запоминаем
        logger.trace("Previously selected item in UserPanel ComboBox: " + previouslySelectedItem);

        try {
            // Получаем актуальный список поездов из DAO
            List<Train> trains = trainDao.getAllTrains();
            List<String> trainNumbers = trains.stream()
                    .map(Train::getTrainNumber)
                    .sorted() // Сортируем
                    .collect(Collectors.toList());
            logger.debug("Found " + trainNumbers.size() + " train numbers from DAO for UserPanel.");

            // Обновляем модель ComboBox в потоке EDT
            SwingUtilities.invokeLater(() -> {
                // Временно отключаем ActionListener
                ActionListener[] listeners = trainComboBox.getActionListeners();
                for(ActionListener l : listeners) trainComboBox.removeActionListener(l);
                logger.trace("Temporarily removed listeners from UserPanel ComboBox.");

                // Обновляем содержимое ComboBox
                trainComboBox.removeAllItems();
                trainNumbers.forEach(trainComboBox::addItem);
                logger.debug("UserPanel ComboBox model updated with " + trainNumbers.size() + " items.");

                // Пытаемся восстановить выбор или выбрать первый
                String itemToSelect = null;
                if (previouslySelectedItem != null && trainNumbers.contains(previouslySelectedItem)) {
                    itemToSelect = previouslySelectedItem;
                } else if (!trainNumbers.isEmpty()) {
                    itemToSelect = trainNumbers.get(0);
                }

                // Устанавливаем выбранный элемент
                if (itemToSelect != null) {
                    trainComboBox.setSelectedItem(itemToSelect);
                    logger.debug("Set selected item in UserPanel ComboBox to: '" + trainComboBox.getSelectedItem() + "'");
                } else {
                    // Если список поездов стал пустым
                    logger.debug("UserPanel ComboBox is empty after refresh. Clearing info/table.");
                    trainInfoPanel.setTrainInfo(null);
                    wagonsTablePanel.setWagons(new ArrayList<>());
                }

                // Возвращаем ActionListener'ы
                for(ActionListener l : listeners) trainComboBox.addActionListener(l);
                logger.trace("Re-added listeners to UserPanel ComboBox.");

                // Явно вызываем загрузку данных для выбранного элемента ПОСЛЕ обновления списка
                if (trainComboBox.getSelectedItem() != null) {
                    logger.trace("Triggering loadSelectedTrain explicitly after UserPanel ComboBox update.");
                    loadSelectedTrain();
                } else {
                    logger.trace("No item selected in UserPanel ComboBox after update, loadSelectedTrain not triggered.");
                }
            });

        } catch (TrainValidationException e) {
            logger.error("Error loading train list for UserPanel ComboBox", e);
            // Показываем ошибку пользователю в потоке EDT
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Ошибка при загрузке списка поездов:\n" + e.getMessage(), "Ошибка загрузки", JOptionPane.ERROR_MESSAGE)
            );
        }
    }

    /**
     * Загружает полные данные для поезда, выбранного в JComboBox, и обновляет
     * панели {@code trainInfoPanel} и {@code wagonsTablePanel}.
     * Вызывается слушателем JComboBox.
     */
    private void loadSelectedTrain() {
        String selectedTrainNumber = (String) trainComboBox.getSelectedItem();
        // Проверка на null (может быть при программном изменении или ошибках)
        if (selectedTrainNumber == null) {
            logger.warn("loadSelectedTrain (UserPanel) called, but getSelectedItem() returned null. Clearing UI.");
            trainInfoPanel.setTrainInfo(null);
            wagonsTablePanel.setWagons(new ArrayList<>());
            return;
        }

        logger.info("UserPanel loading data for selected train: '" + selectedTrainNumber + "'");
        try {
            // Загружаем данные поезда через DAO
            Train selectedTrain = trainDao.loadTrain(selectedTrainNumber);
            if (selectedTrain != null) {
                // Если успешно загружено, обновляем дочерние панели
                trainInfoPanel.setTrainInfo(selectedTrain);
                wagonsTablePanel.setWagons(selectedTrain.getVehicles());
                logger.debug("UserPanel successfully updated with data for train: '" + selectedTrainNumber + "'");
            } else {
                // Поезд был в списке, но не загрузился - ошибка данных или гонка состояний
                logger.error("CRITICAL INCONSISTENCY (UserPanel): Selected train '" + selectedTrainNumber + "' exists in ComboBox but loadTrain returned null!");
                JOptionPane.showMessageDialog(this, "Ошибка: Выбранный поезд не найден в базе данных, хотя он был в списке!", "Ошибка данных", JOptionPane.ERROR_MESSAGE);
                // Очищаем панели
                trainInfoPanel.setTrainInfo(null);
                wagonsTablePanel.setWagons(new ArrayList<>());
                // Можно попробовать перезагрузить список на случай удаления поезда админом
                // refreshTrainList();
            }
        } catch (TrainValidationException e) {
            // Ошибка при загрузке из DAO
            logger.error("Error loading selected train data in UserPanel: '" + selectedTrainNumber + "'", e);
            JOptionPane.showMessageDialog(this, "Ошибка при загрузке данных поезда:\n" + e.getMessage(), "Ошибка загрузки", JOptionPane.ERROR_MESSAGE);
            // Очищаем панели при ошибке
            trainInfoPanel.setTrainInfo(null);
            wagonsTablePanel.setWagons(new ArrayList<>());
        } catch (Exception e) {
            // Ловим другие неожиданные ошибки при загрузке
            logger.error("Unexpected error loading train data in UserPanel for '" + selectedTrainNumber + "'", e);
            JOptionPane.showMessageDialog(this, "Неожиданная ошибка при загрузке поезда.", "Критическая ошибка", JOptionPane.ERROR_MESSAGE);
            trainInfoPanel.setTrainInfo(null);
            wagonsTablePanel.setWagons(new ArrayList<>());
        }
    }

} // Конец класса UserPanel