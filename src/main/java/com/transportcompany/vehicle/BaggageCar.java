package com.transportcompany.vehicle;

import org.apache.log4j.Logger;

/**
 * <p>Представляет собой багажный вагон.</p>
 * <p>Наследует базовые характеристики от {@link AbstractVehicle} и добавляет
 * специфическую характеристику: максимальную грузоподъемность по весу ({@code maxWeightCapacity}).</p>
 *
 * <p><b>Наследует:</b> {@link AbstractVehicle}</p>
 *
 * <p><b>Ключевые особенности:</b></p>
 * <ul>
 *     <li>Пассажирская вместимость ({@code capacity}) обычно равна 0.</li>
 *     <li>Хранит {@code maxWeightCapacity} - максимальный вес груза, который может перевозить вагон (например, в килограммах).</li>
 *     <li>Реализует метод {@link #getVehicleType()}, который возвращает "BaggageCar".</li>
 *     <li>Содержит геттер {@link #getMaxWeightCapacity()} и сеттер {@link #setMaxWeightCapacity(double)}
 *         для управления максимальной грузоподъемностью, с валидацией на неотрицательность в сеттере.</li>
 * </ul>
 *
 * @see AbstractVehicle Родительский класс.
 * @see com.transportcompany.db.TrainDao Логика сохранения и загрузки, где {@code maxWeightCapacity} сохраняется как `additional_info`.
 */
public class BaggageCar extends AbstractVehicle {
    // Логгер для событий, специфичных для багажных вагонов
    private static final Logger logger = Logger.getLogger(BaggageCar.class);

    /**
     * Максимальная грузоподъемность вагона по весу (например, в килограммах).
     * Значение не должно быть отрицательным.
     */
    private double maxWeightCapacity;

    /**
     * <p>Конструктор багажного вагона.</p>
     * <p>Инициализирует базовые характеристики через конструктор родителя ({@link AbstractVehicle})
     * и устанавливает максимальную грузоподъемность.</p>
     * <p>Рекомендуется устанавливать {@code capacity} в 0 для багажных вагонов.</p>
     *
     * @param capacity Вместимость (обычно 0 для персонала, >= 0).
     * @param baggageCapacity Вместимость багажа (может быть равна maxWeightCapacity или иметь другую семантику, >= 0.0).
     * @param comfortLevel Уровень комфорта (обычно низкий или 0, >= 0).
     * @param maxWeightCapacity Максимальная грузоподъемность по весу (например, в кг, должна быть >= 0.0).
     * @throws IllegalArgumentException если базовые параметры некорректны (проверка в {@code super()})
     *                                  или если {@code maxWeightCapacity} отрицательное.
     */
    public BaggageCar(int capacity, double baggageCapacity, int comfortLevel, double maxWeightCapacity) {
        // 1. Вызов конструктора родителя
        super(capacity, baggageCapacity, comfortLevel);
        // Предупреждение, если вместимость пассажиров > 0
        if (capacity > 0) {
            logger.warn("BaggageCar created with non-zero passenger capacity: " + capacity + ". Consider setting it to 0.");
        }

        // 2. Валидация и установка специфического поля maxWeightCapacity
        if (maxWeightCapacity < 0) {
            String errorMsg = "Max weight capacity cannot be negative: " + maxWeightCapacity;
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        this.maxWeightCapacity = maxWeightCapacity;

        // Логгирование создания
        logger.debug(String.format("BaggageCar created: MaxWeight=%.1f, Capacity=%d, BaggageCap=%.1f",
                this.maxWeightCapacity, capacity, baggageCapacity));
    }

    /**
     * <p>Возвращает максимальную грузоподъемность вагона по весу.</p>
     *
     * @return Максимальная грузоподъемность (дробное неотрицательное число).
     */
    public double getMaxWeightCapacity() {
        return maxWeightCapacity;
    }

    /**
     * <p>Устанавливает новую максимальную грузоподъемность вагона.</p>
     * <p>Выполняет валидацию: значение не должно быть отрицательным.</p>
     *
     * @param maxWeightCapacity Новая максимальная грузоподъемность (должна быть >= 0.0).
     * @throws IllegalArgumentException если {@code maxWeightCapacity} отрицательное.
     */
    public void setMaxWeightCapacity(double maxWeightCapacity) {
        if (maxWeightCapacity < 0) {
            String errorMsg = "Attempt to set negative max weight capacity: " + maxWeightCapacity;
            logger.error(errorMsg + " for vehicle: " + this.toString());
            throw new IllegalArgumentException(errorMsg);
        }
        // Логгируем, если значение изменилось (сравнение double)
        if (Double.compare(this.maxWeightCapacity, maxWeightCapacity) != 0) {
            logger.debug(String.format("Updating maxWeightCapacity from %.1f to %.1f for vehicle ID: %d",
                    this.maxWeightCapacity, maxWeightCapacity, this.getId()));
            this.maxWeightCapacity = maxWeightCapacity;
        }
    }

    /**
     * <p>Возвращает тип вагона как строку "BaggageCar".</p>
     *
     * @return Строка "BaggageCar".
     */
    @Override
    public String getVehicleType() {
        return "BaggageCar";
    }

    // Метод toString() наследуется от AbstractVehicle.
    // Специфичная информация (макс. вес) не будет видна в стандартном toString,
    // но доступна через getMaxWeightCapacity().
}