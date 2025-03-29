package com.transportcompany.vehicle;

import org.apache.log4j.Logger;

/**
 * <p>Представляет собой пассажирский вагон.</p>
 * <p>Наследует базовые характеристики (вместимость, багаж, комфорт) от {@link AbstractVehicle}
 * и добавляет специфическую характеристику: тип пассажирского вагона (например, "Купе", "Плацкарт", "СВ").</p>
 *
 * <p><b>Наследует:</b> {@link AbstractVehicle}</p>
 *
 * <p><b>Ключевые особенности:</b></p>
 * <ul>
 *     <li>Хранит {@code passengerCarType} - строковое описание конкретного типа вагона.</li>
 *     <li>Реализует метод {@link #getVehicleType()}, который возвращает комбинированную строку,
 *         включающую базовый тип ("PassengerCar") и конкретный тип (например, "PassengerCar - Купе").
 *         Это важно для отображения в UI и сохранения в БД (где сохраняется только конкретный тип как доп. информация).</li>
 *     <li>Содержит сеттер {@link #setPassengerCarType(String)} для изменения типа вагона, с валидацией на пустую строку.</li>
 * </ul>
 *
 * @see AbstractVehicle Родительский класс.
 * @see com.transportcompany.db.TrainDao Логика сохранения и загрузки этого типа вагона, где {@code passengerCarType} сохраняется в поле `additional_info`.
 */
public class PassengerCar extends AbstractVehicle {
    // Логгер для событий, специфичных для пассажирских вагонов
    private static final Logger logger = Logger.getLogger(PassengerCar.class);

    /**
     * Конкретный тип пассажирского вагона (например, "Купе", "Плацкарт", "СВ").
     * Не должен быть null или пустым.
     */
    private String passengerCarType;

    /**
     * <p>Конструктор пассажирского вагона.</p>
     * <p>Инициализирует базовые характеристики через конструктор родителя ({@link AbstractVehicle})
     * и устанавливает специфический тип пассажирского вагона.</p>
     *
     * @param capacity Вместимость пассажиров (>= 0).
     * @param baggageCapacity Вместимость багажа (>= 0.0).
     * @param comfortLevel Уровень комфорта (>= 0).
     * @param passengerCarType Тип пассажирского вагона (например, "Купе"). Не должен быть null или пустым.
     * @throws IllegalArgumentException если базовые параметры некорректны (проверка в {@code super()})
     *                                  или если {@code passengerCarType} равен null или пустой/состоит из пробелов.
     */
    public PassengerCar(int capacity, double baggageCapacity, int comfortLevel, String passengerCarType) {
        // 1. Вызов конструктора родителя для инициализации и валидации базовых полей
        super(capacity, baggageCapacity, comfortLevel);

        // 2. Валидация и установка специфического поля passengerCarType
        if (passengerCarType == null || passengerCarType.trim().isEmpty()) {
            String errorMsg = "Passenger car type cannot be null or empty.";
            // Логгируем ошибку перед выбрасыванием исключения
            logger.error(errorMsg + " Provided value: '" + passengerCarType + "'");
            throw new IllegalArgumentException(errorMsg);
        }
        this.passengerCarType = passengerCarType.trim(); // Сохраняем без лишних пробелов

        // Логгируем создание конкретного типа вагона
        logger.debug("PassengerCar created: Type='" + this.passengerCarType + "', Capacity=" + capacity + ", Comfort=" + comfortLevel);
    }

    /**
     * <p>Возвращает тип вагона в формате "PassengerCar - [КонкретныйТип]".</p>
     * <p>Например: "PassengerCar - Купе".</p>
     * <p>Эта строка используется для отображения в UI, а часть до " - " ("PassengerCar") используется
     * для идентификации базового типа в базе данных.</p>
     *
     * @return Комбинированная строка типа вагона.
     */
    @Override
    public String getVehicleType() {
        // Соединяем базовый тип с конкретным типом
        return "PassengerCar - " + passengerCarType;
    }

    /**
     * <p>Устанавливает новый тип пассажирского вагона (например, "Плацкарт").</p>
     * <p>Выполняет валидацию: новый тип не должен быть null или пустым/состоять из пробелов.</p>
     *
     * @param passengerCarType Новый тип вагона.
     * @throws IllegalArgumentException если {@code passengerCarType} равен null или пустой/состоит из пробелов.
     */
    public void setPassengerCarType(String passengerCarType) {
        if (passengerCarType == null || passengerCarType.trim().isEmpty()) {
            String errorMsg = "Passenger car type cannot be set to null or empty.";
            logger.error(errorMsg + " Attempted value: '" + passengerCarType + "' for vehicle: " + this.toString());
            throw new IllegalArgumentException(errorMsg);
        }
        String trimmedType = passengerCarType.trim();
        // Логгируем изменение, если тип действительно отличается
        if (!this.passengerCarType.equals(trimmedType)) {
            logger.debug("Updating passengerCarType from '" + this.passengerCarType + "' to '" + trimmedType + "' for vehicle ID: " + this.getId());
            this.passengerCarType = trimmedType;
        }
    }

    // Геттер для получения ТОЛЬКО специфичного типа (Купе, Плацкарт и т.д.)
    // Может быть полезен, но не является частью основного интерфейса Vehicle
    /**
     * Возвращает конкретный тип пассажирского вагона (например, "Купе", "Плацкарт").
     * В отличие от {@link #getVehicleType()}, не содержит префикс "PassengerCar - ".
     * @return Строка с конкретным типом вагона.
     */
    public String getSpecificPassengerType() {
        return this.passengerCarType;
    }

    // Метод toString() наследуется от AbstractVehicle и использует getVehicleType(),
    // поэтому он будет корректно отображать полный тип "PassengerCar - [КонкретныйТип]".
}