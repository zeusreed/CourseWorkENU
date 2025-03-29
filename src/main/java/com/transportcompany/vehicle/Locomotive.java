package com.transportcompany.vehicle;

import org.apache.log4j.Logger;

/**
 * <p>Представляет собой локомотив (тяговую единицу поезда).</p>
 * <p>Наследует базовые характеристики от {@link AbstractVehicle}, но многие из них
 * (вместимость пассажиров, багажа, комфорт) обычно имеют нулевые или минимальные значения.</p>
 * <p>Добавляет специфическую характеристику: сила тяги ({@code tractionForce}).</p>
 *
 * <p><b>Наследует:</b> {@link AbstractVehicle}</p>
 *
 * <p><b>Ключевые особенности:</b></p>
 * <ul>
 *     <li>Пассажирская вместимость ({@code capacity}) и вместимость багажа ({@code baggageCapacity}) обычно равны 0.</li>
 *     <li>Уровень комфорта ({@code comfortLevel}) обычно не применим или равен 0.</li>
 *     <li>Хранит {@code tractionForce} - силу тяги локомотива (например, в килоньютонах).</li>
 *     <li>Реализует метод {@link #getVehicleType()}, который возвращает "Locomotive".</li>
 *     <li>Содержит геттер {@link #getTractionForce()} и сеттер {@link #setTractionForce(int)}
 *         для управления силой тяги, с валидацией на неотрицательность в сеттере.</li>
 * </ul>
 *
 * @see AbstractVehicle Родительский класс.
 * @see com.transportcompany.db.TrainDao Логика сохранения и загрузки, где {@code tractionForce} сохраняется как `additional_info`.
 */
public class Locomotive extends AbstractVehicle {
    // Логгер для событий, специфичных для локомотивов
    private static final Logger logger = Logger.getLogger(Locomotive.class);

    /**
     * Сила тяги локомотива (например, в килоньютонах).
     * Значение не должно быть отрицательным.
     */
    private int tractionForce;

    /**
     * <p>Конструктор локомотива.</p>
     * <p>Инициализирует базовые характеристики через конструктор родителя ({@link AbstractVehicle})
     * и устанавливает силу тяги.</p>
     * <p>Рекомендуется устанавливать {@code capacity}, {@code baggageCapacity} и {@code comfortLevel} в 0.</p>
     *
     * @param capacity Вместимость (обычно 0 для экипажа, >= 0).
     * @param baggageCapacity Вместимость багажа (обычно 0, >= 0.0).
     * @param comfortLevel Уровень комфорта (обычно 0, >= 0).
     * @param tractionForce Сила тяги (например, в кН, должна быть >= 0).
     * @throws IllegalArgumentException если базовые параметры некорректны (проверка в {@code super()})
     *                                  или если {@code tractionForce} отрицательное.
     */
    public Locomotive(int capacity, double baggageCapacity, int comfortLevel, int tractionForce) {
        // 1. Вызов конструктора родителя
        super(capacity, baggageCapacity, comfortLevel);
        // Предупреждения, если базовые параметры не нулевые
        if (capacity > 0) {
            logger.warn("Locomotive created with non-zero passenger capacity: " + capacity + ". Consider setting it to 0.");
        }
        if (Double.compare(baggageCapacity, 0.0) != 0) {
            logger.warn("Locomotive created with non-zero baggage capacity: " + baggageCapacity + ". Consider setting it to 0.");
        }
        if (comfortLevel > 0) {
            logger.warn("Locomotive created with non-zero comfort level: " + comfortLevel + ". Consider setting it to 0.");
        }

        // 2. Валидация и установка специфического поля tractionForce
        if (tractionForce < 0) {
            String errorMsg = "Traction force cannot be negative: " + tractionForce;
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        this.tractionForce = tractionForce;

        // Логгирование создания
        logger.debug("Locomotive created: TractionForce=" + this.tractionForce + " kN"); // Предполагаем кН
    }

    /**
     * <p>Возвращает силу тяги локомотива.</p>
     *
     * @return Сила тяги (целое неотрицательное число).
     */
    public int getTractionForce() {
        return tractionForce;
    }

    /**
     * <p>Устанавливает новую силу тяги локомотива.</p>
     * <p>Выполняет валидацию: значение не должно быть отрицательным.</p>
     *
     * @param tractionForce Новая сила тяги (должна быть >= 0).
     * @throws IllegalArgumentException если {@code tractionForce} отрицательное.
     */
    public void setTractionForce(int tractionForce) {
        if (tractionForce < 0) {
            String errorMsg = "Attempt to set negative traction force: " + tractionForce;
            logger.error(errorMsg + " for vehicle: " + this.toString());
            throw new IllegalArgumentException(errorMsg);
        }
        // Логгируем, если значение изменилось
        if (this.tractionForce != tractionForce) {
            logger.debug("Updating tractionForce from " + this.tractionForce + " to " + tractionForce + " for vehicle ID: " + this.getId());
            this.tractionForce = tractionForce;
        }
    }

    /**
     * <p>Возвращает тип вагона как строку "Locomotive".</p>
     *
     * @return Строка "Locomotive".
     */
    @Override
    public String getVehicleType() {
        return "Locomotive";
    }

    // Метод toString() наследуется от AbstractVehicle.
    // Специфичная информация (сила тяги) не будет видна в стандартном toString,
    // но доступна через getTractionForce().
}