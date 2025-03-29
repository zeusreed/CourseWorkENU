package com.transportcompany.train;

import com.transportcompany.vehicle.Vehicle; // Импорт интерфейса вагона
import org.apache.log4j.Logger;
import java.util.ArrayList;
import java.util.Collections; // Для Collections.unmodifiableList, если решим возвращать неизменяемый список
import java.util.Comparator; // Для сортировки
import java.util.List;
import java.util.stream.Collectors;
import com.transportcompany.exception.TrainValidationException; // Для исключений при добавлении/удалении

/**
 * <p>Представляет собой железнодорожный поезд, состоящий из последовательности
 * подвижного состава ({@link Vehicle}).</p>
 * <p>Этот класс инкапсулирует номер поезда и список его вагонов/локомотивов.
 * Он предоставляет методы для добавления, удаления вагонов, расчета суммарных характеристик
 * (вместимость пассажиров, багажа) и поиска вагонов по критериям.</p>
 *
 * <p><b>Ключевые аспекты:</b></p>
 * <ul>
 *     <li>Хранит уникальный {@code trainNumber}.</li>
 *     <li>Содержит список {@code vehicles} типа {@code List<Vehicle>}, что позволяет
 *         хранить объекты разных классов ({@code PassengerCar}, {@code Locomotive} и т.д.)
 *         благодаря полиморфизму.</li>
 *     <li>Использует Stream API для агрегатных вычислений (суммарная вместимость) и поиска.</li>
 *     <li>Обеспечивает инкапсуляцию списка вагонов: метод {@link #getVehicles()} возвращает
 *         копию списка, предотвращая прямое изменение внутреннего состояния объекта {@code Train} извне.</li>
 * </ul>
 *
 * @see Vehicle Интерфейс, представляющий элемент поезда.
 * @see \\TrainController Класс, управляющий объектами Train и взаимодействующий с UI.
 * @see com.transportcompany.db.TrainDao Класс, отвечающий за сохранение и загрузку объектов Train в/из БД.
 */
public class Train {
    // Логгер для событий, связанных с объектами Train
    private static final Logger logger = Logger.getLogger(Train.class);

    /**
     * Номер поезда (строка). Должен быть уникальным идентификатором поезда.
     * Не может быть null. В конструкторе выполняется trim().
     */
    private final String trainNumber; // Делаем final, т.к. номер поезда обычно не меняется после создания

    /**
     * Список подвижного состава (вагонов, локомотивов) в поезде.
     * Используется интерфейс {@link Vehicle} для поддержки разных типов вагонов.
     * Порядок элементов в списке важен и соответствует их расположению в поезде.
     * Инициализируется пустым списком в конструкторе.
     */
    private final List<Vehicle> vehicles; // Делаем final и инициализируем в конструкторе

    /**
     * <p>Конструктор класса Train.</p>
     * <p>Создает новый поезд с заданным номером. Инициализирует пустой список вагонов.</p>
     *
     * @param trainNumber Номер поезда. Не должен быть null. Лишние пробелы по краям будут удалены.
     * @throws IllegalArgumentException Если {@code trainNumber} равен null или после удаления пробелов становится пустым.
     *                                  (Хотя ТЗ требовало проверку только на null, добавим и на пустоту для надежности).
     */
    public Train(String trainNumber) {
        if (trainNumber == null) {
            // Выбрасываем исключение, если номер null - это критично
            String errorMsg = "Train number cannot be null.";
            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
        // Удаляем пробелы по краям
        String trimmedTrainNumber = trainNumber.trim();
        if (trimmedTrainNumber.isEmpty()) {
            // Выбрасываем исключение, если номер стал пустым после trim
            String errorMsg = "Train number cannot be empty or contain only whitespace.";
            logger.error(errorMsg + " Original value: '" + trainNumber + "'");
            throw new IllegalArgumentException(errorMsg);
        }

        this.trainNumber = trimmedTrainNumber;
        // Инициализируем список вагонов как изменяемый ArrayList внутри объекта Train
        this.vehicles = new ArrayList<>();
        logger.info("Train object created with number: '" + this.trainNumber + "'");
    }

    /**
     * <p>Добавляет вагон (или локомотив) в конец поезда.</p>
     *
     * @param vehicle Добавляемый вагон (объект, реализующий интерфейс {@link Vehicle}). Не должен быть null.
     * @throws TrainValidationException Если переданный {@code vehicle} равен null.
     *                                  (Используем кастомное исключение, т.к. это ошибка бизнес-логики).
     */
    public void addVehicle(Vehicle vehicle) throws TrainValidationException {
        if (vehicle == null) {
            String errorMsg = "Cannot add a null vehicle to the train '" + this.trainNumber + "'.";
            logger.error(errorMsg);
            // Используем TrainValidationException, т.к. это нарушение правил формирования поезда
            throw new TrainValidationException(errorMsg);
        }
        // Добавляем вагон в конец внутреннего списка
        this.vehicles.add(vehicle);
        logger.debug("Vehicle added to train '" + this.trainNumber + "': " + vehicle.getVehicleType() + " (ID: " + vehicle.getId() + ")");
    }

    /**
     * <p>Рассчитывает и возвращает общую пассажирскую вместимость всего поезда.</p>
     * <p>Суммирует значения, возвращаемые методом {@link Vehicle#getCapacity()} для каждого
     * вагона в поезде, используя Stream API.</p>
     *
     * @return Суммарная пассажировместимость поезда (целое число).
     */
    public int getTotalPassengerCapacity() {
        // Stream API: получаем поток из списка вагонов,
        // mapToInt: преобразуем каждый вагон в его вместимость (int),
        // sum: суммируем все полученные значения int.
        int totalCapacity = vehicles.stream()
                .mapToInt(Vehicle::getCapacity) // Эквивалентно v -> v.getCapacity()
                .sum();
        logger.trace("Calculated total passenger capacity for train '" + trainNumber + "': " + totalCapacity);
        return totalCapacity;
    }

    /**
     * <p>Рассчитывает и возвращает общую вместимость багажа всего поезда.</p>
     * <p>Суммирует значения, возвращаемые методом {@link Vehicle#getBaggageCapacity()} для
     * каждого вагона в поезде, используя Stream API.</p>
     *
     * @return Суммарная вместимость багажа поезда (дробное число).
     */
    public double getTotalBaggageCapacity() {
        // Аналогично getTotalPassengerCapacity, но для double и багажа.
        double totalBaggage = vehicles.stream()
                .mapToDouble(Vehicle::getBaggageCapacity) // Используем mapToDouble для double
                .sum();
        logger.trace(String.format("Calculated total baggage capacity for train '%s': %.1f", trainNumber, totalBaggage));
        return totalBaggage;
    }

    /**
     * <p>Возвращает список вагонов, составляющих поезд.</p>
     * <p><b>Важно:</b> Возвращает <b>копию</b> внутреннего списка вагонов.
     * Это сделано для защиты внутреннего состояния объекта {@code Train} (инкапсуляция).
     * Изменения, внесенные в возвращенный список, не повлияют на сам поезд.
     * Для добавления/удаления вагонов следует использовать методы {@link #addVehicle(Vehicle)}
     * и {@link #removeVehicle(int)}.</p>
     * <p>Альтернативный вариант - возвращать неизменяемое представление списка
     * ({@code Collections.unmodifiableList(this.vehicles)}), что еще строже, но может быть
     * менее удобно для некоторых операций (например, если внешний код хочет отсортировать копию).</p>
     *
     * @return Копия списка {@link Vehicle}, содержащего вагоны поезда.
     */
    public List<Vehicle> getVehicles() {
        // Создаем новую копию ArrayList на основе внутреннего списка vehicles.
        List<Vehicle> vehiclesCopy = new ArrayList<>(this.vehicles);
        logger.trace("Returning a copy of the vehicle list for train '" + trainNumber + "' with size: " + vehiclesCopy.size());
        return vehiclesCopy;
        // Альтернатива: вернуть неизменяемый вид
        // return Collections.unmodifiableList(this.vehicles);
    }

    /**
     * <p>Находит и возвращает список вагонов в поезде, уровень комфорта которых
     * находится в заданном диапазоне (включительно).</p>
     * <p>Использует Stream API для фильтрации списка вагонов.</p>
     *
     * @param minComfort Минимальный допустимый уровень комфорта (включительно).
     * @param maxComfort Максимальный допустимый уровень комфорта (включительно).
     * @return Список вагонов ({@link Vehicle}), удовлетворяющих условию. Если таких вагонов нет, возвращается пустой список.
     * @throws IllegalArgumentException если {@code minComfort > maxComfort}.
     */
    public List<Vehicle> findVehiclesByComfortLevelRange(int minComfort, int maxComfort) {
        // Валидация входных параметров диапазона
        if (minComfort > maxComfort) {
            String errorMsg = "Minimum comfort level (" + minComfort + ") cannot be greater than maximum comfort level (" + maxComfort + ").";
            logger.error(errorMsg + " Train: '" + trainNumber + "'");
            throw new IllegalArgumentException(errorMsg);
        }
        logger.debug("Finding vehicles in train '" + trainNumber + "' with comfort level between " + minComfort + " and " + maxComfort);

        // Stream API:
        // stream(): получаем поток из списка вагонов.
        // filter(): оставляем только те вагоны (v), для которых лямбда-выражение возвращает true.
        //           Лямбда проверяет, что уровень комфорта вагона >= minComfort И <= maxComfort.
        // collect(Collectors.toList()): собираем отфильтрованные вагоны в новый список.
        List<Vehicle> foundVehicles = vehicles.stream()
                .filter(v -> v.getComfortLevel() >= minComfort && v.getComfortLevel() <= maxComfort)
                .collect(Collectors.toList());

        logger.debug("Found " + foundVehicles.size() + " vehicles matching comfort range [" + minComfort + "-" + maxComfort + "] in train '" + trainNumber + "'.");
        return foundVehicles; // Возвращаем новый список с результатами
    }

    /**
     * <p>Удаляет вагон из поезда по его индексу в списке.</p>
     *
     * @param index Индекс удаляемого вагона (начиная с 0).
     * @throws TrainValidationException если индекс выходит за пределы допустимого диапазона
     *                                  (меньше 0 или больше либо равен текущему количеству вагонов).
     *                                  Используется кастомное исключение для согласованности с addVehicle.
     */
    public void removeVehicle(int index) throws TrainValidationException {
        // Проверка корректности индекса
        if (index < 0 || index >= vehicles.size()) {
            String errorMsg = "Invalid vehicle index for removal: " + index + ". Train '" + trainNumber + "' has " + vehicles.size() + " vehicles (indices 0 to " + (vehicles.size() - 1) + ").";
            logger.error(errorMsg);
            // Используем TrainValidationException, т.к. это ошибка операции над поездом
            throw new TrainValidationException(errorMsg);
        }
        // Удаляем вагон из внутреннего списка по индексу
        Vehicle removedVehicle = vehicles.remove(index); // Метод remove возвращает удаленный элемент
        logger.info("Vehicle removed from train '" + this.trainNumber + "' at index " + index + ": " + removedVehicle.getVehicleType() + " (ID: " + removedVehicle.getId() + ")");
    }

    /**
     * <p>Возвращает номер поезда.</p>
     *
     * @return Номер поезда (строка).
     */
    public String getTrainNumber() {
        return trainNumber;
    }

    /**
     * <p>Возвращает строковое представление поезда.</p>
     * <p>Включает номер поезда и краткую информацию о количестве вагонов.</p>
     * <p>Для детального списка вагонов используйте {@link #getVehicles()}.</p>
     *
     * @return Строка, описывающая поезд.
     */
    @Override
    public String toString() {
        // Возвращаем номер и количество вагонов для краткости
        return "Train{" +
                "trainNumber='" + trainNumber + '\'' +
                ", numberOfVehicles=" + vehicles.size() +
                '}';
        // Если нужен полный список вагонов в toString (не рекомендуется для больших поездов):
        // return "Train{" +
        //        "trainNumber='" + trainNumber + '\'' +
        //        ", vehicles=" + vehicles + // Вызовет toString() для каждого Vehicle
        //        '}';
    }

    // --- Дополнительные возможные методы (не реализованы, но могут быть полезны) ---

     /*
      * Сортирует ВНУТРЕННИЙ список вагонов поезда по заданному критерию.
      * Внимание: изменяет порядок вагонов в самом объекте Train!
      *
      * @param comparator Компаратор для сравнения вагонов (например, Comparator.comparingInt(Vehicle::getComfortLevel)).
      *
     public void sortInternalVehicles(Comparator<Vehicle> comparator) {
         if (comparator == null) {
              logger.warn("Attempted to sort internal vehicles of train '" + trainNumber + "' with a null comparator. Sorting skipped.");
              return;
          }
          logger.info("Sorting internal vehicles of train '" + trainNumber + "' using comparator: " + comparator.toString());
          this.vehicles.sort(comparator);
          // После внутренней сортировки, возможно, потребуется пересохранить поезд в БД,
          // если порядок важен для персистентности.
     }
     */

     /*
      * Возвращает вагон по его индексу.
      *
      * @param index Индекс вагона.
      * @return Вагон по указанному индексу.
      * @throws IndexOutOfBoundsException если индекс некорректен.
      *
     public Vehicle getVehicleByIndex(int index) {
          if (index < 0 || index >= vehicles.size()) {
               throw new IndexOutOfBoundsException("Invalid index: " + index + " for train '" + trainNumber + "' with size " + vehicles.size());
           }
          return this.vehicles.get(index);
     }
      */
}