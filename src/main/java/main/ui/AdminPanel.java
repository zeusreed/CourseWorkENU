package main.ui;

import com.transportcompany.db.TrainDao;
import com.transportcompany.db.UserDao;
import com.transportcompany.exception.TrainValidationException;
import com.transportcompany.train.Train;
import main.TrainController;
import org.apache.log4j.Logger; // Импорт логгера

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(AdminPanel.class); // Логгер

    private TrainInfoPanel trainInfoPanel;
    private WagonsTablePanel wagonsTablePanel;
    private TrainControlPanel trainControlPanel;
    private TrainController controller;
    private JComboBox<String> trainComboBox;
    private JMenuBar menuBar;
    private MainGUI mainGUI; // Добавим ссылку на главное окно для выхода

    public AdminPanel(MainGUI mainGUI, TrainDao trainDao, UserDao.User user) { // Добавлен параметр MainGUI
        this.mainGUI = mainGUI; // Сохраняем ссылку
        logger.info("Initializing AdminPanel for user: " + user.getUsername());
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Передаем DAO в контроллер. Текущий поезд (null) установится при выборе из ComboBox
        this.controller = new TrainController(null, trainDao);

        // --- Создание меню ---
        menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Файл");
        JMenuItem logoutMenuItem = new JMenuItem("Выйти из системы");
        // Используем ссылку на mainGUI для переключения на панель логина
        logoutMenuItem.addActionListener(e -> {
            logger.info("Logout action triggered by user: " + user.getUsername());
            controller.setTrain(null); // Сбрасываем текущий поезд
            mainGUI.showLoginPanel();
        });
        fileMenu.add(logoutMenuItem);

        JMenu trainMenu = new JMenu("Поезд");
        JMenuItem newTrainMenuItem = new JMenuItem("Новый поезд");
        JMenuItem saveTrainMenuItem = new JMenuItem("Сохранить текущий");
        JMenuItem deleteTrainMenuItem = new JMenuItem("Удалить текущий");

        newTrainMenuItem.addActionListener(e -> createNewTrain());
        saveTrainMenuItem.addActionListener(e -> saveCurrentTrain());
        deleteTrainMenuItem.addActionListener(e -> deleteSelectedTrain());

        trainMenu.add(newTrainMenuItem);
        trainMenu.add(saveTrainMenuItem);
        trainMenu.add(deleteTrainMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(trainMenu);
        // --------------------

        // --- Создание панелей ---
        trainInfoPanel = new TrainInfoPanel();
        wagonsTablePanel = new WagonsTablePanel(controller); // Передаем контроллер (хотя он там больше не используется)
        trainControlPanel = new TrainControlPanel(controller, wagonsTablePanel, this);

        // --- Верхняя панель с выбором поезда ---
        JPanel topSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topSelectionPanel.add(new JLabel("Админ: " + user.getUsername() + " | Выберите поезд:"));
        trainComboBox = new JComboBox<>();
        trainComboBox.setPreferredSize(new Dimension(200, trainComboBox.getPreferredSize().height));
        // Добавляем защиту от NPE при первом запуске, когда список еще пуст
        trainComboBox.addActionListener(e -> {
            // Выполняем действие, только если событие не вызвано программным изменением
            // и есть выбранный элемент
            if (e.getActionCommand().equals("comboBoxChanged") && trainComboBox.getSelectedItem() != null) {
                loadSelectedTrain();
            }
        });
        topSelectionPanel.add(trainComboBox);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(menuBar, BorderLayout.NORTH);
        topPanel.add(topSelectionPanel, BorderLayout.CENTER);
        topPanel.add(trainInfoPanel, BorderLayout.SOUTH);

        // --- Добавление компонентов на AdminPanel ---
        add(topPanel, BorderLayout.NORTH);
        add(wagonsTablePanel, BorderLayout.CENTER);
        add(trainControlPanel, BorderLayout.SOUTH);

        // Первоначальная загрузка списка поездов
        loadTrainsIntoComboBox();
        logger.debug("AdminPanel initialization complete.");
    }

    private void saveCurrentTrain() {
        Train currentTrain = controller.getTrain();
        if (currentTrain != null) {
            logger.debug("Save current train action triggered for train: " + currentTrain.getTrainNumber());
            try {
                controller.saveTrain(); // Сохранение текущего
                JOptionPane.showMessageDialog(this, "Поезд '" + currentTrain.getTrainNumber() + "' успешно сохранен.", "Сохранено", JOptionPane.INFORMATION_MESSAGE);
                logger.info("Train '" + currentTrain.getTrainNumber() + "' saved by user action.");
            } catch (RuntimeException ex) { // Ловим проброшенное исключение из saveTrain
                logger.error("Failed to save train '" + currentTrain.getTrainNumber() + "' from menu action.", ex);
                JOptionPane.showMessageDialog(this, "Ошибка при сохранении поезда: " + ex.getMessage(), "Ошибка сохранения", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            logger.warn("Save current train action triggered, but no train is selected.");
            JOptionPane.showMessageDialog(this, "Нет выбранного поезда для сохранения.", "Ошибка", JOptionPane.WARNING_MESSAGE);
        }
    }


    private void createNewTrain() {
        logger.debug("'New Train' action triggered.");
        String trainNumber = JOptionPane.showInputDialog(this, "Введите номер нового поезда:", "Новый поезд", JOptionPane.PLAIN_MESSAGE);
        if (trainNumber != null && !trainNumber.trim().isEmpty()) {
            trainNumber = trainNumber.trim();
            logger.info("Attempting to create train: " + trainNumber);
            try {
                // Проверяем существование через DAO
                if (controller.getTrainDao().loadTrain(trainNumber) != null) {
                    logger.warn("Train number already exists: " + trainNumber);
                    JOptionPane.showMessageDialog(this, "Поезд с номером '" + trainNumber + "' уже существует!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                // Создаем объект Train (может бросить IllegalArgumentException)
                Train newTrain = new Train(trainNumber);
                controller.setTrain(newTrain); // Устанавливаем как текущий в контроллере
                controller.saveTrain(); // Сохраняем новый (пустой) поезд в БД
                logger.info("New train created and saved: " + trainNumber);

                // Обновляем UI
                trainInfoPanel.setTrainInfo(newTrain);
                wagonsTablePanel.setWagons(new ArrayList<>()); // Новый поезд пока без вагонов
                loadTrainsIntoComboBox(); // Перезагружаем список поездов в ComboBox
                // Выбираем только что созданный поезд в ComboBox
                final String finalTrainNumber = trainNumber; // Нужна final переменная для лямбды
                SwingUtilities.invokeLater(() -> {
                    // Убедимся, что элемент действительно добавлен перед выбором
                    for (int i = 0; i < trainComboBox.getItemCount(); i++) {
                        if (finalTrainNumber.equals(trainComboBox.getItemAt(i))) {
                            trainComboBox.setSelectedItem(finalTrainNumber);
                            logger.debug("Programmatically selected newly created train: " + finalTrainNumber);
                            break;
                        }
                    }
                });
                JOptionPane.showMessageDialog(this, "Новый поезд '" + trainNumber + "' создан.", "Успех", JOptionPane.INFORMATION_MESSAGE);

            } catch (IllegalArgumentException ex) { // Ошибка конструктора Train
                logger.error("Invalid train number format entered: " + trainNumber, ex);
                JOptionPane.showMessageDialog(this, "Ошибка: " + ex.getMessage(), "Неверный номер поезда", JOptionPane.ERROR_MESSAGE);
            } catch (TrainValidationException | RuntimeException ex) { // Ошибка DAO или сохранения
                logger.error("Error during new train creation/saving for number: " + trainNumber, ex);
                JOptionPane.showMessageDialog(this, "Ошибка при создании/сохранении поезда: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) { // Другие неожиданные ошибки
                logger.error("Unexpected error during new train creation for number: " + trainNumber, ex);
                JOptionPane.showMessageDialog(this, "Неожиданная ошибка при создании поезда.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            logger.debug("New train creation cancelled or empty number entered.");
        }
    }

    private void deleteSelectedTrain() {
        Train currentTrain = controller.getTrain();
        if (currentTrain != null) {
            String trainNumber = currentTrain.getTrainNumber();
            logger.debug("'Delete Train' action triggered for: " + trainNumber);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Вы уверены, что хотите удалить поезд '" + trainNumber + "'?\nВсе вагоны этого поезда также будут удалены.",
                    "Подтверждение удаления", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                logger.debug("User confirmed deletion for train: " + trainNumber);
                try {
                    controller.getTrainDao().deleteTrain(trainNumber);
                    logger.info("Train deleted successfully from DB: " + trainNumber);
                    controller.setTrain(null); // Сбрасываем текущий поезд в контроллере
                    loadTrainsIntoComboBox(); // Обновляем список и выбираем первый/ничего
                    JOptionPane.showMessageDialog(this, "Поезд '" + trainNumber + "' удален.", "Удалено", JOptionPane.INFORMATION_MESSAGE);
                } catch (TrainValidationException ex) {
                    logger.error("Error during train deletion: " + trainNumber, ex);
                    JOptionPane.showMessageDialog(this, "Ошибка при удалении поезда: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                logger.debug("Train deletion cancelled for: " + trainNumber);
            }
        } else {
            logger.warn("'Delete Train' action triggered, but no train is selected.");
            JOptionPane.showMessageDialog(this, "Нет выбранного поезда для удаления.", "Ошибка", JOptionPane.WARNING_MESSAGE);
        }
    }


    // Обновление UI после изменения вагона (добавления/удаления/редактирования)
    public void updateUIAfterWagonChange() {
        logger.debug("Updating AdminPanel UI after wagon change...");
        Train currentTrain = controller.getTrain();
        if (currentTrain != null) {
            // Данные в controller.getTrain() должны быть актуальны после saveTrain()
            trainInfoPanel.setTrainInfo(currentTrain);
            wagonsTablePanel.setWagons(currentTrain.getVehicles());
            logger.debug("AdminPanel UI updated for train: " + currentTrain.getTrainNumber());
        } else {
            trainInfoPanel.setTrainInfo(null);
            wagonsTablePanel.setWagons(new ArrayList<>());
            logger.debug("AdminPanel UI cleared as current train is null after wagon change.");
        }
    }

    // Загрузка списка поездов в ComboBox
    private void loadTrainsIntoComboBox() {
        logger.debug("Loading trains into ComboBox...");
        String previouslySelectedItem = (String) trainComboBox.getSelectedItem(); // Запоминаем текущий выбор

        try {
            List<Train> trains = controller.getTrainDao().getAllTrains();
            List<String> trainNumbers = trains.stream()
                    .map(Train::getTrainNumber)
                    .sorted() // Сортируем номера поездов
                    .collect(Collectors.toList());
            logger.debug("Found " + trainNumbers.size() + " train numbers.");

            // Обновляем модель ComboBox в потоке EDT
            SwingUtilities.invokeLater(() -> {
                // Отключаем ActionListener на время обновления
                ActionListener[] listeners = trainComboBox.getActionListeners();
                for(ActionListener l : listeners) trainComboBox.removeActionListener(l);

                trainComboBox.removeAllItems();
                trainNumbers.forEach(trainComboBox::addItem);

                // Пытаемся восстановить выбор или выбрать первый
                if (previouslySelectedItem != null && trainNumbers.contains(previouslySelectedItem)) {
                    trainComboBox.setSelectedItem(previouslySelectedItem);
                    logger.trace("Restored selection in ComboBox: " + previouslySelectedItem);
                } else if (!trainNumbers.isEmpty()) {
                    trainComboBox.setSelectedIndex(0); // Выбираем первый элемент
                    logger.trace("Selected first item in ComboBox: " + trainComboBox.getSelectedItem());
                } else {
                    // Если список поездов пуст после обновления
                    controller.setTrain(null);
                    trainInfoPanel.setTrainInfo(null);
                    wagonsTablePanel.setWagons(new ArrayList<>());
                    logger.debug("ComboBox is empty after refresh, cleared UI.");
                }

                // Возвращаем ActionListener'ы обратно
                for(ActionListener l : listeners) trainComboBox.addActionListener(l);

                // Явно вызываем загрузку данных для выбранного элемента ПОСЛЕ обновления списка
                // и возвращения слушателей, если элемент выбран
                if (trainComboBox.getSelectedItem() != null) {
                    loadSelectedTrain();
                }

                logger.debug("ComboBox model updated.");
            });

        } catch (TrainValidationException e) {
            logger.error("Error loading train list for ComboBox", e);
            // Показываем ошибку пользователю
            SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Ошибка при загрузке списка поездов: " + e.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE)
            );
        }
    }

    // Загрузка данных выбранного поезда
    private void loadSelectedTrain() {
        String selectedTrainNumber = (String) trainComboBox.getSelectedItem();
        logger.debug("Attempting to load selected train: " + selectedTrainNumber);

        // Проверка, выбран ли действительно элемент
        if (selectedTrainNumber == null || selectedTrainNumber.trim().isEmpty()) {
            logger.debug("No train selected in ComboBox (item is null or empty). Clearing UI.");
            controller.setTrain(null);
            trainInfoPanel.setTrainInfo(null);
            wagonsTablePanel.setWagons(new ArrayList<>());
            return;
        }

        // Проверяем, не загружен ли уже этот поезд
        Train currentTrainInController = controller.getTrain();
        if (currentTrainInController != null && selectedTrainNumber.equals(currentTrainInController.getTrainNumber())) {
            logger.debug("Train " + selectedTrainNumber + " is already the current train. Skipping reload.");
            // На всякий случай обновим панели, вдруг данные изменились извне
            trainInfoPanel.setTrainInfo(currentTrainInController);
            wagonsTablePanel.setWagons(currentTrainInController.getVehicles());
            return;
        }

        logger.info("Loading data for selected train: " + selectedTrainNumber);
        try {
            Train selectedTrain = controller.getTrainDao().loadTrain(selectedTrainNumber);
            if (selectedTrain != null) {
                controller.setTrain(selectedTrain); // Устанавливаем как текущий
                trainInfoPanel.setTrainInfo(selectedTrain);
                wagonsTablePanel.setWagons(selectedTrain.getVehicles());
                logger.debug("Successfully loaded data for train: " + selectedTrainNumber);
            } else {
                // Ситуация, когда поезд есть в списке, но не загрузился (ошибка?)
                logger.error("Selected train number '" + selectedTrainNumber + "' exists in ComboBox but loadTrain returned null!");
                JOptionPane.showMessageDialog(this, "Ошибка: Выбранный поезд не найден в базе данных!", "Ошибка данных", JOptionPane.ERROR_MESSAGE);
                controller.setTrain(null);
                trainInfoPanel.setTrainInfo(null);
                wagonsTablePanel.setWagons(new ArrayList<>());
            }
        } catch (TrainValidationException e) {
            logger.error("Error loading selected train data: " + selectedTrainNumber, e);
            JOptionPane.showMessageDialog(this, "Ошибка при загрузке данных поезда: " + e.getMessage(), "Ошибка загрузки", JOptionPane.ERROR_MESSAGE);
            // Очищаем UI в случае ошибки
            controller.setTrain(null);
            trainInfoPanel.setTrainInfo(null);
            wagonsTablePanel.setWagons(new ArrayList<>());
        }
    }
}