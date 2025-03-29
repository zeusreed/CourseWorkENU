package main.ui;

// Импорты для классов вагонов
import com.transportcompany.vehicle.BaggageCar;
import com.transportcompany.vehicle.Locomotive;
import com.transportcompany.vehicle.PassengerCar;
import com.transportcompany.vehicle.RestaurantCar;
import com.transportcompany.vehicle.Vehicle;
// Импорт для TrainController (хотя он больше не используется напрямую)
import main.TrainController;
// Логгер
import org.apache.log4j.Logger;

// Импорты для Swing UI
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer; // Для настройки отображения ячеек
import javax.swing.table.DefaultTableModel;    // Модель данных таблицы
import javax.swing.table.TableRowSorter;       // Для сортировки и фильтрации
import javax.swing.table.TableColumnModel; // <<<--- ИМПОРТ ДЛЯ МОДЕЛИ КОЛОНОК
// Импорты для AWT Layout и компонентов
import java.awt.*;
// Импорты для коллекций
import java.util.ArrayList;
import java.util.List;
// Импорт для регулярных выражений при фильтрации
import java.util.regex.Pattern;

/**
 * <p>Панель пользовательского интерфейса (Swing JPanel), отображающая список вагонов
 * поезда в виде таблицы ({@link JTable}).</p>
 * <p>Предоставляет функциональность для:</p>
 * <ul>
 *     <li>Отображения данных вагонов (тип, вместимость, багаж, комфорт, доп. инфо).</li>
 *     <li>Сортировки таблицы по любому столбцу (кликом по заголовку).</li>
 *     <li>Фильтрации отображаемых вагонов по типу и диапазону вместимости.</li>
 * </ul>
 *
 * <p><b>Структура:</b></p>
 * <ul>
 *     <li>Использует {@link BorderLayout}.</li>
 *     <li>Вверху ({@code NORTH}) расположена панель фильтров ({@code filterPanel}) с {@link JComboBox} для типа
 *         и {@link JTextField} для диапазона вместимости, а также кнопка "Фильтр".</li>
 *     <li>В центре ({@code CENTER}) расположена таблица {@link JTable}, обернутая в {@link JScrollPane}.</li>
 * </ul>
 *
 * <p><b>Модель данных и отображение:</b></p>
 * <ul>
 *     <li>Использует {@link DefaultTableModel} для хранения данных таблицы. Модель настроена как нередактируемая.</li>
 *     <li>Указаны типы данных для столбцов ({@code Integer}, {@code Double}, {@code String}) для корректной сортировки.</li>
 *     <li>Используются {@link DefaultTableCellRenderer} для настройки выравнивания текста в ячейках (числа вправо, остальное влево).</li>
 *     <li>Ширина столбцов настраивается вручную.</li>
 * </ul>
 *
 * <p><b>Сортировка и фильтрация:</b></p>
 * <ul>
 *     <li>{@link TableRowSorter} привязан к модели таблицы, обеспечивая сортировку по клику на заголовок.</li>
 *     <li>Метод {@link #applyTableFilter()} создает и применяет {@link RowFilter} к сортеру на основе
 *         значений, выбранных в полях фильтрации. Фильтрация происходит на стороне клиента (в UI),
 *         не затрагивая исходный список вагонов.</li>
 * </ul>
 *
 * <p><b>Обновление данных:</b></p>
 * <ul>
 *     <li>Метод {@link #setWagons(List)} принимает список вагонов и полностью перезаписывает
 *         содержимое таблицы. Этот метод вызывается из родительских панелей ({@link AdminPanel}, {@link UserPanel})
 *         при загрузке поезда или после изменения состава вагонов.</li>
 * </ul>
 *
 * @see JTable Основной компонент для отображения данных.
 * @see DefaultTableModel Модель данных таблицы.
 * @see TableRowSorter Обеспечивает сортировку и фильтрацию представления таблицы.
 * @see RowFilter Используется для определения, какие строки показывать.
 * @see Vehicle Данные этих объектов отображаются в таблице.
 */
public class WagonsTablePanel extends JPanel {
    // Логгер для событий панели таблицы
    private static final Logger logger = Logger.getLogger(WagonsTablePanel.class);

    // Компоненты UI (final убран у тех, что инициализируются в createFilterPanel)
    private final JTable wagonsTable;             // Таблица для отображения вагонов
    private final DefaultTableModel tableModel;   // Модель данных таблицы
    private final TableRowSorter<DefaultTableModel> sorter; // Сортировщик/фильтровщик
    // Компоненты фильтрации (ИНИЦИАЛИЗИРУЮТСЯ В createFilterPanel)
    private JComboBox<String> wagonTypeFilter; // <<<--- final УБРАН
    private JTextField minCapacityField;    // <<<--- final УБРАН
    private JTextField maxCapacityField;    // <<<--- final УБРАН

    /**
     * Конструктор панели с таблицей вагонов.
     *
     * @param controller Экземпляр {@link TrainController}. В текущей версии панели не используется,
     *                   но оставлен для возможной будущей совместимости или передачи других зависимостей.
     *                   Может быть {@code null}.
     */
    public WagonsTablePanel(TrainController controller) { // controller может быть null
        logger.debug("Initializing WagonsTablePanel...");
        setLayout(new BorderLayout(5, 5)); // BorderLayout с отступами между компонентами
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Внешние отступы панели

        // --- Создание панели фильтров (Северная часть) ---
        // Этот метод инициализирует поля wagonTypeFilter, minCapacityField, maxCapacityField
        JPanel filterPanel = createFilterPanel();
        add(filterPanel, BorderLayout.NORTH);

        // --- Создание таблицы и модели (Центральная часть) ---
        // 1. Определение заголовков столбцов
        String[] columnNames = {"№", "Тип", "Вместимость", "Багаж (кг)", "Комфорт", "Доп. инфо"}; // Уточнил багаж

        // 2. Создание модели таблицы
        tableModel = new DefaultTableModel(columnNames, 0) { // 0 начальных строк
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Запрещаем редактирование ячеек напрямую в таблице
            }

            // 3. Определение типов данных столбцов (важно для сортировки!)
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                    case 0: // № (Номер строки)
                    case 2: // Вместимость
                    case 4: // Комфорт
                        return Integer.class;
                    case 3: // Багаж (кг)
                        return Double.class;
                    case 1: // Тип вагона
                    case 5: // Доп. инфо
                    default:
                        return String.class; // По умолчанию - строка
                }
            }
        };

        // 4. Создание самой таблицы на основе модели
        wagonsTable = new JTable(tableModel);

        // 5. Настройка внешнего вида таблицы
        configureTableAppearance(); // Вызов хелпер-метода для настройки

        // 6. Добавление таблицы на панель прокрутки
        JScrollPane tableScrollPane = new JScrollPane(wagonsTable);
        add(tableScrollPane, BorderLayout.CENTER); // Добавляем в центр панели

        // 7. Настройка сортировки
        sorter = new TableRowSorter<>(tableModel);
        wagonsTable.setRowSorter(sorter); // Привязываем сортировщик к таблице

        logger.debug("WagonsTablePanel initialized successfully.");
    }

    /**
     * Вспомогательный метод для создания и настройки панели фильтров.
     * Инициализирует поля {@code wagonTypeFilter}, {@code minCapacityField}, {@code maxCapacityField}.
     * @return Готовая панель {@link JPanel} с компонентами фильтрации.
     */
    private JPanel createFilterPanel() {
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        filterPanel.setBorder(BorderFactory.createTitledBorder("Фильтры")); // Рамка с заголовком

        // Инициализация компонентов фильтрации (ЗДЕСЬ)
        wagonTypeFilter = new JComboBox<>(new String[]{"Все", "PassengerCar", "RestaurantCar", "BaggageCar", "Locomotive"});
        wagonTypeFilter.setToolTipText("Фильтровать вагоны по типу");

        minCapacityField = new JTextField(5); // Ширина поля ~5 символов
        minCapacityField.setToolTipText("Минимальная пассажирская вместимость (включительно)");
        maxCapacityField = new JTextField(5);
        maxCapacityField.setToolTipText("Максимальная пассажирская вместимость (включительно)");

        JButton filterButton = new JButton("Применить фильтр");
        filterButton.setToolTipText("Отфильтровать таблицу по заданным критериям");
        filterButton.addActionListener(e -> applyTableFilter()); // Действие по нажатию

        // Добавление компонентов на панель фильтров
        filterPanel.add(new JLabel("Тип:"));
        filterPanel.add(wagonTypeFilter);
        filterPanel.add(Box.createHorizontalStrut(10)); // Горизонтальный отступ
        filterPanel.add(new JLabel("Вместимость от:"));
        filterPanel.add(minCapacityField);
        filterPanel.add(new JLabel("до:"));
        filterPanel.add(maxCapacityField);
        filterPanel.add(Box.createHorizontalStrut(10));
        filterPanel.add(filterButton);

        return filterPanel;
    }

    /**
     * Вспомогательный метод для настройки внешнего вида таблицы {@code wagonsTable}.
     * Устанавливает рендереры, ширину столбцов и другие параметры отображения.
     */
    private void configureTableAppearance() {
        // Увеличиваем высоту строк для лучшей читаемости
        wagonsTable.setRowHeight(wagonsTable.getRowHeight() + 4);
        // Устанавливаем небольшие отступы между ячейками
        wagonsTable.setIntercellSpacing(new Dimension(4, 2));

        // --- Рендереры для выравнивания ---
        // Выравнивание по левому краю с отступом (для строк)
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);
        leftRenderer.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5)); // top, left, bottom, right padding
        wagonsTable.setDefaultRenderer(String.class, leftRenderer);

        // Выравнивание по правому краю с отступом (для чисел)
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        rightRenderer.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        wagonsTable.setDefaultRenderer(Integer.class, rightRenderer);
        wagonsTable.setDefaultRenderer(Double.class, rightRenderer);

        // Ручная установка предпочтительной ширины столбцов
        TableColumnModel columnModel = wagonsTable.getColumnModel(); // <<<--- ИСПОЛЬЗУЕМ ИМПОРТ
        columnModel.getColumn(0).setPreferredWidth(40);  // №
        columnModel.getColumn(1).setPreferredWidth(130); // Тип
        columnModel.getColumn(2).setPreferredWidth(90);  // Вместимость
        columnModel.getColumn(3).setPreferredWidth(90);  // Багаж (кг)
        columnModel.getColumn(4).setPreferredWidth(80);  // Комфорт
        columnModel.getColumn(5).setPreferredWidth(200); // Доп. инфо
        wagonsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); // Обязательно для ручной ширины

        // --- Дополнительные настройки таблицы ---
        wagonsTable.getTableHeader().setReorderingAllowed(false); // Запрет перемещения столбцов
        wagonsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Выбор только одной строки
    }

    /**
     * <p>Заполняет таблицу данными из переданного списка вагонов.</p>
     * <p>Полностью очищает текущее содержимое таблицы перед заполнением.</p>
     * <p>Выполняет обновление в потоке обработки событий Swing (EDT).</p>
     * <p>Сбрасывает текущий фильтр таблицы после обновления данных.</p>
     *
     * @param wagons Список объектов {@link Vehicle} для отображения. Может быть {@code null} или пустым.
     */
    public void setWagons(List<Vehicle> wagons) {
        logger.debug("Setting wagons in table. Received list size: " + (wagons != null ? wagons.size() : "null"));
        // Обновление таблицы должно происходить в Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            // 1. Очищаем модель таблицы от старых данных
            tableModel.setRowCount(0);

            // 2. Проверяем, есть ли что отображать
            if (wagons == null || wagons.isEmpty()) {
                logger.debug("Input wagon list is null or empty. Table view is now empty.");
                sorter.setRowFilter(null); // Сброс фильтра
                return; // Выходим
            }

            // 3. Заполняем модель новыми данными
            int rowNum = 1;
            for (Vehicle vehicle : wagons) {
                if (vehicle != null) {
                    Object[] rowData = getRowDataForTable(vehicle, rowNum++);
                    tableModel.addRow(rowData);
                } else {
                    logger.warn("Null vehicle object encountered in the list during setWagons. Skipping.");
                }
            }
            logger.info("WagonsTablePanel updated with " + tableModel.getRowCount() + " rows.");

            // 4. Сбрасываем фильтр
            sorter.setRowFilter(null);
            logger.trace("Table filter reset after updating data.");

            // 5. Опционально: прокрутить таблицу к началу
            if (tableModel.getRowCount() > 0) {
                wagonsTable.scrollRectToVisible(wagonsTable.getCellRect(0, 0, true));
            }
        });
    }


    /**
     * <p>Преобразует объект {@link Vehicle} в массив {@code Object[]} для отображения
     * в одной строке таблицы.</p>
     *
     * @param vehicle Вагон, данные которого нужно представить. Не должен быть null.
     * @param rowNum Номер строки для отображения в первом столбце (начиная с 1).
     * @return Массив объектов {@code [Integer, String, Integer, Double, Integer, String]},
     *         соответствующий столбцам таблицы.
     */
    private Object[] getRowDataForTable(Vehicle vehicle, int rowNum) {
        if (vehicle == null) {
            logger.error("CRITICAL: getRowDataForTable called with null vehicle!");
            return new Object[]{rowNum, "ERROR", 0, 0.0, 0, "Invalid Vehicle Data"};
        }

        int capacity = vehicle.getCapacity();
        double baggageCapacity = vehicle.getBaggageCapacity();
        int comfortLevel = vehicle.getComfortLevel();
        String type;
        String additionalInfo;

        // Определяем тип и доп. инфо, используя instanceof
        if (vehicle instanceof PassengerCar) { // <<<--- ИСПОЛЬЗУЕМ ИМПОРТ
            String fullType = ((PassengerCar) vehicle).getVehicleType();
            String[] parts = fullType.split(" - ", 2);
            type = parts[0];
            additionalInfo = (parts.length > 1 && !parts[1].trim().isEmpty()) ? parts[1].trim() : "N/A";
        } else if (vehicle instanceof RestaurantCar) { // <<<--- ИСПОЛЬЗУЕМ ИМПОРТ
            type = "RestaurantCar";
            additionalInfo = "Столов: " + ((RestaurantCar) vehicle).getNumberOfTables();
        } else if (vehicle instanceof BaggageCar) { // <<<--- ИСПОЛЬЗУЕМ ИМПОРТ
            type = "BaggageCar";
            additionalInfo = String.format("Макс. вес: %.1f", ((BaggageCar) vehicle).getMaxWeightCapacity());
        } else if (vehicle instanceof Locomotive) { // <<<--- ИСПОЛЬЗУЕМ ИМПОРТ
            type = "Locomotive";
            additionalInfo = "Тяг. усилие: " + ((Locomotive) vehicle).getTractionForce();
        } else {
            type = vehicle.getVehicleType();
            additionalInfo = "(Неизвестный тип)";
            logger.warn("Unknown Vehicle subclass encountered in getRowDataForTable: " + vehicle.getClass().getName());
        }

        logger.trace(String.format("Generated row data: [%d, %s, %d, %.1f, %d, %s]",
                rowNum, type, capacity, baggageCapacity, comfortLevel, additionalInfo));

        return new Object[]{rowNum, type, capacity, baggageCapacity, comfortLevel, additionalInfo};
    }

    /**
     * <p>Возвращает индекс выбранной строки в ПРЕДСТАВЛЕНИИ (view) таблицы.</p>
     * <p>Если ни одна строка не выбрана, возвращает -1.</p>
     *
     * @return Индекс выбранной строки в представлении или -1.
     */
    public int getSelectedRow() {
        return wagonsTable.getSelectedRow();
    }

    /**
     * <p>Применяет фильтры к таблице на основе значений, введенных пользователем.</p>
     */
    private void applyTableFilter() {
        logger.debug("Applying table filter based on user input.");
        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        // 1. Фильтр по типу вагона
        String selectedType = (String) wagonTypeFilter.getSelectedItem();
        if (selectedType != null && !"Все".equals(selectedType)) {
            logger.trace("Adding type filter: '" + selectedType + "' on column 1");
            filters.add(RowFilter.regexFilter("^" + Pattern.quote(selectedType) + "$", 1));
        }

        // 2. Фильтр по вместимости
        try {
            String minCapText = minCapacityField.getText().trim();
            Integer minCapacity = minCapText.isEmpty() ? null : Integer.parseInt(minCapText);
            String maxCapText = maxCapacityField.getText().trim();
            Integer maxCapacity = maxCapText.isEmpty() ? null : Integer.parseInt(maxCapText);

            if (minCapacity != null) {
                if (minCapacity < 0) throw new NumberFormatException("Min capacity cannot be negative");
                logger.trace("Adding min capacity filter: >= " + minCapacity + " on column 2");
                filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.AFTER, minCapacity - 1, 2));
            }
            if (maxCapacity != null) {
                if (maxCapacity < 0) throw new NumberFormatException("Max capacity cannot be negative");
                logger.trace("Adding max capacity filter: <= " + maxCapacity + " on column 2");
                filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.BEFORE, maxCapacity + 1, 2));
            }
            if (minCapacity != null && maxCapacity != null && minCapacity > maxCapacity) {
                logger.warn("Min capacity (" + minCapacity + ") is greater than max capacity (" + maxCapacity + "). Filter not applied.");
                JOptionPane.showMessageDialog(this, "Минимальная вместимость не может быть больше максимальной.", "Ошибка ввода", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            logger.warn("Invalid number format or negative value entered for capacity filter.", ex);
            JOptionPane.showMessageDialog(this, "Неверный формат или отрицательное значение для фильтра вместимости.", "Ошибка ввода", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 3. Применение фильтра
        RowFilter<Object, Object> combinedFilter = filters.isEmpty() ? null : RowFilter.andFilter(filters);
        sorter.setRowFilter(combinedFilter);
        logger.info("Table filter applied/updated successfully.");
    }

    /**
     * <p>Возвращает ссылку на объект {@link JTable}, используемый этой панелью.</p>
     * @return Ссылка на {@link JTable}.
     */
    public JTable getWagonsTable() {
        return wagonsTable;
    }
}