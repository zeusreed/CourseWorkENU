package main.ui;

import com.transportcompany.train.Train; // Используется для получения данных
import org.apache.log4j.Logger; // Логгер

import javax.swing.*;
import java.awt.*;
import java.util.Locale; // Для форматирования чисел

/**
 * <p>Панель пользовательского интерфейса (Swing JPanel), предназначенная для отображения
 * основной информации о текущем выбранном поезде.</p>
 * <p>Отображает номер поезда, общую пассажировместимость и общую вместимость багажа.</p>
 * <p>Получает данные из объекта {@link Train} через метод {@link #setTrainInfo(Train)}.</p>
 *
 * <p><b>Структура:</b></p>
 * <ul>
 *     <li>Использует {@link BorderLayout}.</li>
 *     <li>Сверху содержит метку {@link JLabel} с заголовком "Информация о поезде:".</li>
 *     <li>В центре содержит нередактируемое текстовое поле {@link JTextArea}, обернутое в {@link JScrollPane},
 *         для отображения информации. Использование JTextArea позволяет легко отображать многострочный текст.</li>
 * </ul>
 *
 * <p><b>Использование:</b></p>
 * <p>Эта панель обычно является частью более крупных панелей (например, {@link AdminPanel} или {@link UserPanel}),
 * которые отвечают за получение объекта {@code Train} и передачу его в метод {@code setTrainInfo}.</p>
 *
 * @see Train Объект, данные которого отображаются.
 * @see AdminPanel Пример панели, использующей TrainInfoPanel.
 * @see UserPanel Пример панели, использующей TrainInfoPanel.
 * @see JPanel Базовый класс Swing для этой панели.
 */
public class TrainInfoPanel extends JPanel {

    // Логгер для событий этой панели (хотя здесь их мало)
    private static final Logger logger = Logger.getLogger(TrainInfoPanel.class);

    /** Текстовая область для вывода информации о поезде. */
    private final JTextArea trainInfoTextArea; // final, т.к. создается в конструкторе

    /**
     * <p>Конструктор TrainInfoPanel.</p>
     * <p>Создает и настраивает компоненты пользовательского интерфейса панели.</p>
     */
    public TrainInfoPanel() {
        super(new BorderLayout(5, 5)); // Устанавливаем BorderLayout с отступами
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Внешние отступы панели

        logger.debug("Initializing TrainInfoPanel...");

        // 1. Заголовок панели
        JLabel trainInfoLabel = new JLabel("Информация о поезде:");
        trainInfoLabel.setFont(trainInfoLabel.getFont().deriveFont(Font.BOLD)); // Жирный шрифт для заголовка
        add(trainInfoLabel, BorderLayout.NORTH); // Добавляем заголовок сверху

        // 2. Текстовая область для информации
        trainInfoTextArea = new JTextArea(3, 40); // Задаем примерное количество строк и столбцов
        trainInfoTextArea.setEditable(false); // Запрещаем редактирование пользователем
        trainInfoTextArea.setLineWrap(true); // Автоматический перенос строк
        trainInfoTextArea.setWrapStyleWord(true); // Перенос по словам
        trainInfoTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)); // Моноширинный шрифт для выравнивания
        trainInfoTextArea.setBackground(this.getBackground()); // Фон как у панели
        trainInfoTextArea.setOpaque(false); // Сделать фон прозрачным (если нужно, чтобы фон панели просвечивал)

        // 3. Панель прокрутки для текстовой области (на случай длинной информации)
        JScrollPane scrollPane = new JScrollPane(trainInfoTextArea);
        // Убираем рамку у скролл-панели, чтобы она не выделялась
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER); // Добавляем скролл с текстом в центр

        // Устанавливаем начальное сообщение
        setTrainInfo(null); // Показываем "Поезд не загружен" при инициализации

        logger.debug("TrainInfoPanel initialized.");
    }

    /**
     * <p>Устанавливает (или обновляет) информацию о поезде для отображения на панели.</p>
     * <p>Формирует текстовое представление данных поезда и выводит его в {@code trainInfoTextArea}.</p>
     * <p>Если переданный {@code train} равен {@code null}, отображается сообщение "Поезд не загружен.".</p>
     *
     * @param train Объект {@link Train}, информацию о котором нужно отобразить, или {@code null}.
     */
    public void setTrainInfo(Train train) {
        // Используем StringBuilder для эффективного построения строки
        StringBuilder sb = new StringBuilder();

        if (train != null) {
            // Если поезд есть, форматируем и добавляем его данные
            logger.debug("Setting train info for train: '" + train.getTrainNumber() + "'");
            sb.append("Номер поезда:          ").append(train.getTrainNumber()).append("\n");
            sb.append("Общая вмест. (пасс.): ").append(train.getTotalPassengerCapacity()).append("\n");
            // Форматируем вместимость багажа до одного знака после запятой
            sb.append("Общая вмест. (багаж): ").append(String.format(Locale.US, "%.1f", train.getTotalBaggageCapacity())); // Используем Locale.US для точки в качестве разделителя
        } else {
            // Если поезд не передан (null)
            logger.debug("Setting train info to 'not loaded' state.");
            sb.append("Поезд не выбран или не загружен.");
        }

        // Устанавливаем сформированный текст в JTextArea
        // Делаем это в потоке EDT для безопасности Swing
        final String infoText = sb.toString();
        SwingUtilities.invokeLater(() -> {
            trainInfoTextArea.setText(infoText);
            // Устанавливаем курсор в начало текста (если нужно)
            trainInfoTextArea.setCaretPosition(0);
        });
    }
}