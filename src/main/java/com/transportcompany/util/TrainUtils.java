package com.transportcompany.util;

import com.transportcompany.vehicle.Vehicle; // Импорт интерфейса вагона
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator; // Для методов сравнения
import java.util.List;
import java.util.stream.Collectors; // Для сбора результатов стрима

/**
 * <p>Утилитарный класс, предоставляющий статические методы для работы со списками вагонов ({@code List<Vehicle>}).</p>
 * <p>Этот класс содержит операции, которые не привязаны к конкретному объекту {@link com.transportcompany.train.Train},
 * а могут применяться к любому списку вагонов (например, к результатам поиска или к копии списка вагонов поезда).</p>
 *
 * <p><b>Ключевые характеристики:</b></p>
 * <ul>
 *     <li>Содержит только статические методы. Экземпляр класса создать нельзя (хотя конструктор по умолчанию доступен, лучше сделать его приватным).</li>
 *     <li>Предоставляет методы для сортировки списка вагонов по разным критериям (вместимость, комфорт, багаж).</li>
 *     <li>Методы сортировки <b>не изменяют</b> исходный список, а возвращают <b>новый</b> отсортированный список. Это безопасный подход (неизменяемость входных данных).</li>
 *     <li>Использует Stream API для элегантной и функциональной сортировки.</li>
 *     <li>Обрабатывает случаи, когда на вход подается {@code null} или пустой список (возвращает новый пустой список).</li>
 * </ul>
 *
 * <p><b>Пример использования:</b></p>
 * <pre>{@code
 * Train train = ... // Получаем объект поезда
 * List<Vehicle> originalWagons = train.getVehicles(); // Получаем копию списка
 * List<Vehicle> sortedByComfort = TrainUtils.sortVehiclesByComfortLevel(originalWagons);
 * // originalWagons остался неизменным
 * // sortedByComfort содержит вагоны, отсортированные по комфорту
 * }</pre>
 *
 * @see Vehicle Интерфейс вагона, объекты которого сортируются.
 * @see java.util.Comparator Используется для определения порядка сортировки.
 * @see java.util.stream.Stream Используется для выполнения сортировки.
 */
public final class TrainUtils { // Делаем класс final, т.к. он не предназначен для наследования
    private static final Logger logger = Logger.getLogger(TrainUtils.class);

    /**
     * Приватный конструктор, чтобы предотвратить создание экземпляров утилитарного класса.
     */
    private TrainUtils() {
        // Бросаем исключение, если кто-то попытается создать экземпляр через рефлексию
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * <p>Сортирует переданный список вагонов по возрастанию пассажирской вместимости.</p>
     * <p>Не изменяет исходный список.</p>
     *
     * @param vehicles Список вагонов ({@link Vehicle}) для сортировки. Может быть {@code null}.
     * @return Новый список {@code List<Vehicle>}, содержащий вагоны из исходного списка,
     *         отсортированные по вместимости. Если {@code vehicles} равен {@code null} или пуст,
     *         возвращается новый пустой {@code ArrayList}.
     * @see Vehicle#getCapacity() Используется как ключ сортировки.
     * @see Comparator#comparingInt(java.util.function.ToIntFunction) Метод для создания компаратора по int полю.
     */
    public static List<Vehicle> sortVehiclesByCapacity(List<Vehicle> vehicles) {
        logger.debug("Attempting to sort vehicles by capacity. Input list size: " + (vehicles != null ? vehicles.size() : "null"));
        // Проверка на null или пустоту - возвращаем новый пустой список, а не null
        if (vehicles == null || vehicles.isEmpty()) {
            logger.debug("Input list is null or empty. Returning new empty list.");
            return new ArrayList<>(); // Возвращаем пустой список, а не null
        }

        // Stream API для сортировки:
        // 1. vehicles.stream(): Создаем поток из элементов списка.
        // 2. sorted(...): Сортируем элементы потока.
        //    Comparator.comparingInt(Vehicle::getCapacity): Создаем компаратор, который сравнивает вагоны
        //       на основе целочисленного значения, возвращаемого методом getCapacity().
        // 3. collect(Collectors.toList()): Собираем отсортированные элементы потока в новый список.
        List<Vehicle> sortedList = vehicles.stream()
                .sorted(Comparator.comparingInt(Vehicle::getCapacity))
                .collect(Collectors.toList());

        logger.debug("Sorting by capacity complete. Output list size: " + sortedList.size());
        return sortedList;
    }

    /**
     * <p>Сортирует переданный список вагонов по возрастанию уровня комфорта.</p>
     * <p>Не изменяет исходный список.</p>
     *
     * @param vehicles Список вагонов ({@link Vehicle}) для сортировки. Может быть {@code null}.
     * @return Новый список {@code List<Vehicle>}, содержащий вагоны из исходного списка,
     *         отсортированные по уровню комфорта. Если {@code vehicles} равен {@code null} или пуст,
     *         возвращается новый пустой {@code ArrayList}.
     * @see Vehicle#getComfortLevel() Используется как ключ сортировки.
     * @see Comparator#comparingInt(java.util.function.ToIntFunction)
     */
    public static List<Vehicle> sortVehiclesByComfortLevel(List<Vehicle> vehicles) {
        logger.debug("Attempting to sort vehicles by comfort level. Input list size: " + (vehicles != null ? vehicles.size() : "null"));
        if (vehicles == null || vehicles.isEmpty()) {
            logger.debug("Input list is null or empty. Returning new empty list.");
            return new ArrayList<>();
        }

        // Аналогично сортировке по вместимости, но используем getComfortLevel()
        List<Vehicle> sortedList = vehicles.stream()
                .sorted(Comparator.comparingInt(Vehicle::getComfortLevel))
                .collect(Collectors.toList());

        logger.debug("Sorting by comfort level complete. Output list size: " + sortedList.size());
        return sortedList;
    }

    /**
     * <p>Сортирует переданный список вагонов по возрастанию вместимости багажа.</p>
     * <p>Не изменяет исходный список.</p>
     *
     * @param vehicles Список вагонов ({@link Vehicle}) для сортировки. Может быть {@code null}.
     * @return Новый список {@code List<Vehicle>}, содержащий вагоны из исходного списка,
     *         отсортированные по вместимости багажа. Если {@code vehicles} равен {@code null} или пуст,
     *         возвращается новый пустой {@code ArrayList}.
     * @see Vehicle#getBaggageCapacity() Используется как ключ сортировки.
     * @see Comparator#comparingDouble(java.util.function.ToDoubleFunction) Метод для создания компаратора по double полю.
     */
    public static List<Vehicle> sortVehiclesByBaggageCapacity(List<Vehicle> vehicles) {
        logger.debug("Attempting to sort vehicles by baggage capacity. Input list size: " + (vehicles != null ? vehicles.size() : "null"));
        if (vehicles == null || vehicles.isEmpty()) {
            logger.debug("Input list is null or empty. Returning new empty list.");
            return new ArrayList<>();
        }

        // Используем comparingDouble для сравнения значений double
        List<Vehicle> sortedList = vehicles.stream()
                .sorted(Comparator.comparingDouble(Vehicle::getBaggageCapacity))
                .collect(Collectors.toList());

        logger.debug("Sorting by baggage capacity complete. Output list size: " + sortedList.size());
        return sortedList;
    }
}