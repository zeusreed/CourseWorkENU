package com.transportcompany.vehicle;

import org.apache.log4j.Logger;

/**
 * <p>Представляет собой вагон-ресторан.</p>
 * <p>Наследует базовые характеристики от {@link AbstractVehicle} и добавляет
 * специфическую характеристику: количество столов ({@code numberOfTables}).</p>
 *
 * <p><b>Наследует:</b> {@link AbstractVehicle}</p>
 *
 * <p><b>Ключевые особенности:</b></p>
 * <ul>
 *     <li>Хранит {@code numberOfTables} - количество посадочных столов в вагоне.</li>
 *     <li>Реализует метод {@link #getVehicleType()}, который возвращает "RestaurantCar".</li>
 *     <li>Содержит геттер {@link #getNumberOfTables()} и сеттер {@link #setNumberOfTables(int)}
 *         для управления количеством столов, с валидацией на неотрицательность в сеттере.</li>
 * </ul>
 *
 * @see AbstractVehicle Родительский класс.
 * @see com.transportcompany.db.TrainDao Логика сохранения и загрузки, где {@code numberOfTables} сохраняется как `additional_info`.
 */
public class RestaurantCar extends AbstractVehicle {
    // Логгер для событий, специфичных для вагонов-ресторанов
    private static final Logger logger = Logger.getLogger(RestaurantCar.class);

    /**
     * Количество столов в вагоне-ресторане.
     * Значение не должно быть отрицательным.
     */
    private int numberOfTables;

    /**
     * <p>Конструктор вагона-ресторана.</p>
     * <p>Инициализирует базовые характеристики через конструктор родителя ({@link AbstractVehicle})
     * и устанавливает количество столов.</p>
     *
     * @param capacity Вместимость (обычно относится к персоналу или максимальному числу посетителей, >= 0).
     * @param baggageCapacity Вместимость багажа (обычно 0 или минимальная, >= 0.0).
     * @param comfortLevel Уровень комфорта (может быть выше среднего, >= 0).
     * @param numberOfTables Количество столов в ресторане (должно быть >= 0).
     * @throws IllegalArgumentException если базовые параметры некорректны (проверка в {@code super()})
     *                                  или если {@code numberOfTables} отрицательное.
     */
    public RestaurantCar(int capacity, double baggageCapacity, int comfortLevel, int numberOfTables) {
        // 1. Вызов конструктора родителя
        super(capacity, baggageCapacity, comfortLevel);

        // 2. Валидация и установка специфического поля numberOfTables
        if (numberOfTables < 0) {
            String errorMsg = "Number of tables cannot be negative: " + numberOfTables;
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        this.numberOfTables = numberOfTables;

        // Логгирование создания
        logger.debug("RestaurantCar created: Tables=" + this.numberOfTables + ", Capacity=" + capacity + ", Comfort=" + comfortLevel);
    }

    /**
     * <p>Возвращает количество столов в вагоне-ресторане.</p>
     *
     * @return Количество столов (целое неотрицательное число).
     */
    public int getNumberOfTables() {
        return numberOfTables;
    }

    /**
     * <p>Устанавливает новое количество столов.</p>
     * <p>Выполняет валидацию: количество столов не должно быть отрицательным.</p>
     *
     * @param numberOfTables Новое количество столов (должно быть >= 0).
     * @throws IllegalArgumentException если {@code numberOfTables} отрицательное.
     */
    public void setNumberOfTables(int numberOfTables) {
        if (numberOfTables < 0) {
            String errorMsg = "Attempt to set negative number of tables: " + numberOfTables;
            logger.error(errorMsg + " for vehicle: " + this.toString());
            throw new IllegalArgumentException(errorMsg);
        }
        // Логгируем, если значение изменилось
        if (this.numberOfTables != numberOfTables) {
            logger.debug("Updating numberOfTables from " + this.numberOfTables + " to " + numberOfTables + " for vehicle ID: " + this.getId());
            this.numberOfTables = numberOfTables;
        }
    }

    /**
     * <p>Возвращает тип вагона как строку "RestaurantCar".</p>
     *
     * @return Строка "RestaurantCar".
     */
    @Override
    public String getVehicleType() {
        return "RestaurantCar";
    }

    // Метод toString() наследуется от AbstractVehicle и использует getVehicleType(),
    // поэтому он будет корректно отображать "RestaurantCar" и базовые параметры.
    // Специфичная информация (кол-во столов) в toString базового класса не попадает,
    // но ее можно увидеть через геттер getNumberOfTables() или при отладке.
}