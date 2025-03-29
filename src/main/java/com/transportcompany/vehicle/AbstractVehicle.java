package com.transportcompany.vehicle;

import org.apache.log4j.Logger;

/**
 * <p>Абстрактный базовый класс для всех типов подвижного состава ({@link Vehicle}).</p>
 * <p>Предоставляет общую реализацию для хранения базовых характеристик (вместимость, багаж, комфорт)
 * и их геттеров/сеттеров, а также базовую валидацию в сеттерах и конструкторе.</p>
 * <p>Также содержит поле `id` для идентификатора из базы данных.</p>
 *
 * <p><b>Почему абстрактный класс?</b></p>
 * <ul>
 *     <li><b>DRY (Don't Repeat Yourself):</b> Избегаем дублирования кода для общих полей и методов во всех классах-наследниках.</li>
 *     <li><b>Общее поведение:</b> Реализует базовую валидацию (проверка на отрицательные значения) в одном месте.</li>
 *     <li><b>Основа для наследования:</b> Наследники должны реализовать только абстрактный метод {@link #getVehicleType()} и добавить свою специфику.</li>
 * </ul>
 *
 * <p><b>Реализует:</b> {@link Vehicle}</p>
 *
 * @see Vehicle Интерфейс, который реализует этот класс.
 * @see PassengerCar Пример конкретного наследника.
 * @see RestaurantCar Пример конкретного наследника.
 * @see BaggageCar Пример конкретного наследника.
 * @see Locomotive Пример конкретного наследника.
 */
public abstract class AbstractVehicle implements Vehicle {
    // Логгер для записи событий, связанных с этим классом и его наследниками
    private static final Logger logger = Logger.getLogger(AbstractVehicle.class);

    /**
     * Уникальный идентификатор вагона из базы данных.
     * 0 по умолчанию для новых вагонов.
     * @see Vehicle#getId()
     * @see Vehicle#setId(int)
     */
    private int id = 0; // Инициализация по умолчанию

    /**
     * Пассажирская вместимость. Не может быть отрицательной.
     * @see Vehicle#getCapacity()
     * @see Vehicle#setCapacity(int)
     */
    private int capacity;

    /**
     * Вместимость багажа. Не может быть отрицательной.
     * @see Vehicle#getBaggageCapacity()
     * @see Vehicle#setBaggageCapacity(double)
     */
    private double baggageCapacity;

    /**
     * Уровень комфорта. Не может быть отрицательным.
     * @see Vehicle#getComfortLevel()
     * @see Vehicle#setComfortLevel(int)
     */
    private int comfortLevel;


    /**
     * <p>Конструктор для базовой инициализации вагона.</p>
     * <p>Принимает основные характеристики и выполняет их валидацию (проверка на неотрицательность).</p>
     *
     * @param capacity Вместимость пассажиров (должна быть >= 0).
     * @param baggageCapacity Вместимость багажа (должна быть >= 0.0).
     * @param comfortLevel Уровень комфорта (должен быть >= 0).
     * @throws IllegalArgumentException если какой-либо из параметров имеет недопустимое (отрицательное) значение.
     */
    public AbstractVehicle(int capacity, double baggageCapacity, int comfortLevel) {
        // Валидация входных параметров - критически важна для консистентности данных
        if (capacity < 0) {
            String errorMsg = "Capacity cannot be negative: " + capacity;
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        if (baggageCapacity < 0) {
            String errorMsg = "Baggage capacity cannot be negative: " + baggageCapacity;
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        if (comfortLevel < 0) {
            String errorMsg = "Comfort level cannot be negative: " + comfortLevel;
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        this.capacity = capacity;
        this.baggageCapacity = baggageCapacity;
        this.comfortLevel = comfortLevel;
        // Логгируем создание объекта с базовыми параметрами на уровне DEBUG
        logger.debug(String.format("AbstractVehicle constructed: capacity=%d, baggage=%.1f, comfort=%d",
                capacity, baggageCapacity, comfortLevel));
        // В логгере наследника будет указан конкретный тип создаваемого вагона.
    }

    /**
     * <p>Абстрактный метод, который должны реализовать все классы-наследники.</p>
     * <p>Возвращает строку, однозначно идентифицирующую конкретный тип вагона.</p>
     *
     * @return Строка с типом вагона (например, "PassengerCar - Купе").
     * @see Vehicle#getVehicleType()
     */
    @Override
    public abstract String getVehicleType();

    /**
     * <p>Возвращает строковое представление базовой информации о вагоне.</p>
     * <p>Используется для логгирования и отладки. Включает тип вагона (полученный через {@link #getVehicleType()})
     * и основные характеристики.</p>
     *
     * @return Строка с основной информацией о вагоне.
     */
    @Override
    public String toString() {
        // Используем String.format для аккуратного вывода
        return String.format("Type: %s, Capacity: %d, Baggage: %.1f, Comfort: %d, DB_ID: %d",
                getVehicleType(), capacity, baggageCapacity, comfortLevel, id);
    }

    // --- Реализация геттеров интерфейса Vehicle ---

    @Override
    public int getCapacity(){
        return this.capacity;
    }

    @Override
    public double getBaggageCapacity(){
        return this.baggageCapacity;
    }

    @Override
    public int getComfortLevel(){
        return this.comfortLevel;
    }

    @Override
    public int getId() {
        return id;
    }

    // --- Реализация сеттеров интерфейса Vehicle с валидацией ---

    /**
     * {@inheritDoc}
     * <p>В этой реализации также выполняется проверка на неотрицательность.</p>
     */
    @Override
    public void setCapacity(int capacity) {
        if (capacity < 0) {
            String errorMsg = "Attempt to set negative capacity: " + capacity;
            logger.error(errorMsg + " for vehicle: " + this.toString());
            throw new IllegalArgumentException(errorMsg);
        }
        if (this.capacity != capacity) { // Логгируем только если значение действительно меняется
            logger.debug("Updating capacity from " + this.capacity + " to " + capacity + " for vehicle: " + this.getVehicleType() + " (ID: " + this.id + ")");
            this.capacity = capacity;
        }
    }

    /**
     * {@inheritDoc}
     * <p>В этой реализации также выполняется проверка на неотрицательность.</p>
     */
    @Override
    public void setBaggageCapacity(double baggageCapacity) {
        if (baggageCapacity < 0) {
            String errorMsg = "Attempt to set negative baggage capacity: " + baggageCapacity;
            logger.error(errorMsg + " for vehicle: " + this.toString());
            throw new IllegalArgumentException(errorMsg);
        }
        // Сравниваем double с некоторой точностью или просто проверяем на неравенство
        if (Double.compare(this.baggageCapacity, baggageCapacity) != 0) {
            logger.debug(String.format("Updating baggage capacity from %.1f to %.1f for vehicle: %s (ID: %d)",
                    this.baggageCapacity, baggageCapacity, this.getVehicleType(), this.id));
            this.baggageCapacity = baggageCapacity;
        }
    }

    /**
     * {@inheritDoc}
     * <p>В этой реализации также выполняется проверка на неотрицательность.</p>
     */
    @Override
    public void setComfortLevel(int comfortLevel) {
        if (comfortLevel < 0) {
            String errorMsg = "Attempt to set negative comfort level: " + comfortLevel;
            logger.error(errorMsg + " for vehicle: " + this.toString());
            throw new IllegalArgumentException(errorMsg);
        }
        if (this.comfortLevel != comfortLevel) {
            logger.debug("Updating comfort level from " + this.comfortLevel + " to " + comfortLevel + " for vehicle: " + this.getVehicleType() + " (ID: " + this.id + ")");
            this.comfortLevel = comfortLevel;
        }
    }

    /**
     * {@inheritDoc}
     * <p>Устанавливает ID, обычно полученный из БД.</p>
     */
    @Override
    public void setId(int id) {
        if (this.id != 0 && this.id != id) {
            // Предупреждаем, если ID пытаются изменить после того, как он уже был установлен (кроме установки с 0)
            logger.warn("Changing existing DB ID for vehicle " + this.getVehicleType() + " from " + this.id + " to " + id);
        } else if (this.id == 0 && id != 0) {
            logger.debug("Setting DB ID for vehicle " + this.getVehicleType() + " to " + id);
        }
        this.id = id;
    }
}