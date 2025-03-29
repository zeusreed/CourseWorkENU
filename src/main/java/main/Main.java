package main;

import javax.swing.SwingUtilities; // Для запуска GUI в потоке EDT
import javax.swing.JOptionPane;   // <<<--- ДОБАВЛЕН ИМПОРТ
import main.ui.MainGUI; // Главный класс GUI
import org.apache.log4j.Logger; // Логгер

/**
 * <p>Главный класс приложения (точка входа).</p>
 * <p>Отвечает только за запуск графического пользовательского интерфейса (GUI)
 * в потоке обработки событий Swing (Event Dispatch Thread - EDT).</p>
 *
 * <p><b>Основные действия:</b></p>
 * <ol>
 *     <li>Метод {@code main(String[] args)} является точкой входа, вызываемой JVM.</li>
 *     <li>Использует {@link SwingUtilities#invokeLater(Runnable)} для безопасного создания
 *         и отображения главного окна {@link MainGUI} в EDT. Это стандартная практика
 *         при работе со Swing для предотвращения проблем с потокобезопасностью GUI.</li>
 *     <li>Вся дальнейшая логика инициализации (настройка Log4j, создание DAO, запуск сервера H2,
 *         создание компонентов GUI) выполняется внутри конструктора {@link MainGUI}.</li>
 * </ol>
 *
 * <p><b>Конфигурация Log4j:</b></p>
 * <p>Настройка Log4j происходит в конструкторе {@code MainGUI}. Этот класс {@code Main}
 * не занимается напрямую конфигурацией логгирования.</p>
 *
 * @see MainGUI Класс главного окна приложения.
 * @see SwingUtilities Утилиты для работы с потоками Swing.
 */
public class Main {

    // Логгер для самого старта (хотя основное логгирование в MainGUI)
    private static final Logger logger = Logger.getLogger(Main.class);

    /**
     * <p>Точка входа в приложение.</p>
     * <p>Создает и запускает главное окно {@link MainGUI} в потоке EDT.</p>
     *
     * @param args Аргументы командной строки (в данном приложении не используются).
     */
    public static void main(String[] args) {
        // Используем System.out для самого первого сообщения, т.к. Log4j еще не настроен
        System.out.println("Application main method started. Scheduling GUI creation in EDT...");

        // Запускаем создание и отображение GUI в потоке обработки событий Swing (EDT)
        SwingUtilities.invokeLater(() -> {
            // Этот код выполнится в EDT
            System.out.println("EDT: Creating MainGUI instance...");
            try {
                // Создаем экземпляр главного окна.
                // Конструктор MainGUI выполнит всю инициализацию.
                new MainGUI();
                // Log4j уже должен быть настроен внутри MainGUI
                // logger.info("MainGUI created and application running."); // Логгируется из MainGUI
            } catch (Throwable t) { // Ловим Throwable на всякий случай (включая Error)
                // Критическая ошибка во время инициализации GUI
                System.err.println("CRITICAL ERROR during MainGUI instantiation: " + t.getMessage());
                t.printStackTrace(); // Печатаем стектрейс в консоль
                // Показываем сообщение об ошибке пользователю
                // Используем JOptionPane напрямую, т.к. logger может быть не инициализирован
                JOptionPane.showMessageDialog(null, // null родитель, т.к. окно могло не создаться
                        "Критическая ошибка при запуске приложения:\n" + t.getMessage() +
                                "\nПриложение будет закрыто. См. консоль для деталей.",
                        "Ошибка Запуска", JOptionPane.ERROR_MESSAGE);
                System.exit(1); // Завершаем работу с кодом ошибки
            }
        });

        System.out.println("Application main method finished scheduling GUI. Application continues in EDT.");
    }
} // Конец класса Main