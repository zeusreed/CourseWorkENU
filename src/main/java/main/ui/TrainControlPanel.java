package main.ui;

import main.TrainController; // Класс, методы которого вызываются по кнопкам
import javax.swing.*;
import java.awt.*;
import com.transportcompany.vehicle.Vehicle; // Используется в лямбдах
import org.apache.log4j.Logger; // Логгер

/**
 * <p>Панель пользовательского интерфейса (Swing JPanel), содержащая кнопки
 * для управления вагонами текущего выбранного поезда (в контексте администратора).</p>
 * <p>Предоставляет кнопки "Добавить вагон", "Удалить вагон", "Редактировать вагон".</p>
 *
 * <p><b>Взаимодействие:</b></p>
 * <ul>
 *     <li>Получает ссылку на {@link TrainController} для вызова соответствующих методов
 *         при нажатии кнопок (например, {@code controller.showAddWagonDialog(...)}).</li>
 *     <li>Получает ссылку на {@link WagonsTablePanel} для определения выбранного вагона (индекса строки)
 *         при удалении или редактировании.</li>
 *     <li>Получает ссылку на родительскую панель {@link AdminPanel} для:
 *         <ul>
 *             <li>Передачи в качестве родительского компонента для диалоговых окон ({@code JOptionPane}).</li>
 *             <li>Вызова метода {@code adminPanel.updateUIAfterWagonChange()} для обновления всего UI
 *                 админ-панели после успешного добавления/удаления/редактирования вагона.</li>
 *         </ul>
 *     </li>
 * </ul>
 * <p>Эта панель обычно размещается внизу {@link AdminPanel}.</p>
 *
 * @see TrainController Обрабатывает логику добавления/удаления/редактирования.
 * @see WagonsTablePanel Таблица, из которой берутся данные о выбранном вагоне.
 * @see AdminPanel Родительская панель, которая обновляется после действий.
 * @see JPanel Базовый класс Swing.
 */
public class TrainControlPanel extends JPanel {
    // Логгер для событий панели управления
    private static final Logger logger = Logger.getLogger(TrainControlPanel.class);

    // Кнопки управления (final, т.к. создаются в конструкторе)
    private final JButton addWagonButton;
    private final JButton removeWagonButton;
    private final JButton editWagonButton;

    // Ссылки на другие компоненты и контроллер (final, передаются в конструкторе)
    private final WagonsTablePanel wagonsTablePanel; // Нужна для получения индекса выбранной строки
    private final AdminPanel adminPanel; // Нужна как родитель диалогов и для обновления UI
    private final TrainController controller; // Нужен для вызова бизнес-логики

    /**
     * Конструктор панели управления вагонами.
     *
     * @param controller Экземпляр {@link TrainController} для выполнения операций над поездом. Не null.
     * @param wagonsTablePanel Экземпляр {@link WagonsTablePanel}, отображающей вагоны. Не null.
     * @param adminPanel Экземпляр родительской {@link AdminPanel}. Не null.
     * @throws IllegalArgumentException если какой-либо из параметров равен null.
     */
    public TrainControlPanel(TrainController controller, WagonsTablePanel wagonsTablePanel, AdminPanel adminPanel) {
        // Проверка зависимостей на null - критично для работы
        if (controller == null || wagonsTablePanel == null || adminPanel == null) {
            String errorMsg = "Controller, WagonsTablePanel, and AdminPanel cannot be null for TrainControlPanel.";
            logger.fatal(errorMsg); // Логгируем как фатальную ошибку конфигурации
            throw new IllegalArgumentException(errorMsg);
        }
        this.controller = controller;
        this.wagonsTablePanel = wagonsTablePanel;
        this.adminPanel = adminPanel;

        logger.debug("Initializing TrainControlPanel...");
        // Используем FlowLayout для расположения кнопок в ряд по центру
        setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10)); // 10px горизонтальный и вертикальный отступ

        // --- Инициализация кнопки "Добавить вагон" ---
        addWagonButton = new JButton("Добавить вагон");
        addWagonButton.setIcon(UIManager.getIcon("FileChooser.newFolderIcon")); // Пример иконки
        addWagonButton.setToolTipText("Добавить новый вагон к текущему выбранному поезду");
        addWagonButton.addActionListener(e -> handleAddWagon()); // Выносим логику в отдельный метод

        // --- Инициализация кнопки "Удалить вагон" ---
        removeWagonButton = new JButton("Удалить вагон");
        removeWagonButton.setIcon(UIManager.getIcon("FileChooser.deleteIcon")); // Пример иконки
        removeWagonButton.setToolTipText("Удалить вагон, выбранный в таблице выше");
        removeWagonButton.addActionListener(e -> handleRemoveWagon()); // Отдельный метод

        // --- Инициализация кнопки "Редактировать вагон" ---
        editWagonButton = new JButton("Редактировать вагон");
        editWagonButton.setIcon(UIManager.getIcon("FileChooser.detailsViewIcon")); // Пример иконки
        editWagonButton.setToolTipText("Изменить данные вагона, выбранного в таблице выше");
        editWagonButton.addActionListener(e -> handleEditWagon()); // Отдельный метод

        // Добавляем созданные кнопки на панель
        add(addWagonButton);
        add(removeWagonButton);
        add(editWagonButton);
        logger.debug("TrainControlPanel initialized with buttons and listeners.");
    }

    /**
     * Обработчик нажатия кнопки "Добавить вагон".
     * Проверяет, выбран ли поезд, и вызывает диалог добавления через контроллер.
     * Обновляет UI родительской панели в случае успеха.
     */
    private void handleAddWagon() {
        logger.debug("Add wagon button action started.");
        // Проверка, выбран ли поезд
        if (controller.getTrain() == null) {
            logger.warn("Add wagon attempt failed: No train selected in controller.");
            // Сообщаем пользователю через родительскую панель
            JOptionPane.showMessageDialog(this.adminPanel,
                    "Пожалуйста, сначала выберите или создайте поезд.",
                    "Поезд не выбран", JOptionPane.WARNING_MESSAGE);
            return; // Прерываем действие
        }
        // Вызов диалога через контроллер
        Vehicle newWagon = controller.showAddWagonDialog(this.adminPanel);
        if (newWagon != null) {
            // Успех - обновляем UI
            logger.info("New wagon added via dialog. Requesting AdminPanel UI update.");
            this.adminPanel.updateUIAfterWagonChange();
        } else {
            // Отмена или ошибка (сообщение показал контроллер)
            logger.debug("Add wagon dialog was cancelled or resulted in an error.");
        }
    }

    /**
     * Обработчик нажатия кнопки "Удалить вагон".
     * Проверяет, выбран ли поезд и вагон в таблице, запрашивает подтверждение,
     * вызывает удаление через контроллер и обновляет UI родительской панели.
     */
    private void handleRemoveWagon() {
        logger.debug("Remove wagon button action started.");
        // Проверка наличия поезда
        if (controller.getTrain() == null) {
            logger.warn("Remove wagon attempt failed: No train selected.");
            JOptionPane.showMessageDialog(this.adminPanel, "Пожалуйста, сначала выберите поезд.", "Поезд не выбран", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Получение индекса выбранной строки (view)
        int selectedViewRow = wagonsTablePanel.getSelectedRow();
        if (selectedViewRow < 0) { // Если строка не выбрана
            logger.warn("Remove wagon attempt failed: No wagon selected in the table.");
            JOptionPane.showMessageDialog(this.adminPanel, NO_WAGON_SELECTED_MESSAGE, "Вагон не выбран", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Конвертация индекса view в индекс модели
        int modelRow = wagonsTablePanel.getWagonsTable().convertRowIndexToModel(selectedViewRow);
        logger.debug("Attempting removal for view row " + selectedViewRow + " (model row " + modelRow + ")");

        // Подтверждение удаления
        int confirm = JOptionPane.showConfirmDialog(this.adminPanel,
                "Вы уверены, что хотите удалить этот вагон?",
                "Подтверждение удаления", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            logger.debug("User confirmed wagon removal.");
            try {
                // Вызов метода контроллера для удаления
                controller.removeWagon(modelRow);
                logger.info("Wagon at model index " + modelRow + " removed successfully. Requesting UI update.");
                // Обновление UI после успешного удаления и сохранения
                this.adminPanel.updateUIAfterWagonChange();
                // } catch (IndexOutOfBoundsException | IllegalStateException | RuntimeException ex) { // <<<--- НЕВЕРНЫЙ MULTI-CATCH
            } catch (RuntimeException ex) { // <<<--- ИСПРАВЛЕНИЕ: Ловим RuntimeException, он покрывает остальные
                // Перехватываем ошибки, которые мог бросить контроллер
                logger.error("Error occurred during wagon removal triggered by UI.", ex);
                JOptionPane.showMessageDialog(this.adminPanel,
                        "Не удалось удалить вагон:\n" + ex.getMessage(),
                        "Ошибка удаления", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            logger.debug("User cancelled wagon removal.");
        }
    }

    /**
     * Обработчик нажатия кнопки "Редактировать вагон".
     * Проверяет, выбран ли поезд и вагон, вызывает диалог редактирования через контроллер
     * и обновляет UI родительской панели в случае успеха.
     */
    private void handleEditWagon() {
        logger.debug("Edit wagon button action started.");
        // Проверка наличия поезда
        if (controller.getTrain() == null) {
            logger.warn("Edit wagon attempt failed: No train selected.");
            JOptionPane.showMessageDialog(this.adminPanel, "Пожалуйста, сначала выберите поезд.", "Поезд не выбран", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // Получение индекса выбранной строки (view)
        int selectedViewRow = wagonsTablePanel.getSelectedRow();
        if (selectedViewRow < 0) { // Если строка не выбрана
            logger.warn("Edit wagon attempt failed: No wagon selected in the table.");
            JOptionPane.showMessageDialog(this.adminPanel, NO_WAGON_SELECTED_MESSAGE, "Вагон не выбран", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Конвертация индекса view в индекс модели
        int modelRow = wagonsTablePanel.getWagonsTable().convertRowIndexToModel(selectedViewRow);
        logger.debug("Attempting edit for view row " + selectedViewRow + " (model row " + modelRow + ")");

        // Вызов диалога редактирования через контроллер
        boolean updated = controller.showEditWagonDialog(this.adminPanel, modelRow);
        if (updated) {
            // Если контроллер вернул true (успешное редактирование и сохранение)
            logger.info("Wagon at model index " + modelRow + " edited successfully. Requesting UI update.");
            // Обновляем UI
            this.adminPanel.updateUIAfterWagonChange();
        } else {
            // Диалог был отменен или произошла ошибка (сообщение показал контроллер)
            logger.debug("Edit wagon dialog was cancelled or resulted in an error.");
        }
    }

    // Константа для общего сообщения об ошибке выбора вагона
    private static final String NO_WAGON_SELECTED_MESSAGE = "Пожалуйста, сначала выберите вагон в таблице.";

} // Конец класса TrainControlPanel