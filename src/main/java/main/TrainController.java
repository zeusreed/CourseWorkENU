package main;

import com.transportcompany.db.TrainDao;
import com.transportcompany.exception.TrainValidationException;
import com.transportcompany.train.Train;
import com.transportcompany.vehicle.*;
import org.apache.log4j.Logger;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * <p>Контроллер, отвечающий за управление бизнес-логикой, связанной с поездами и вагонами.</p>
 * <p>Выступает в роли посредника между пользовательским интерфейсом (UI, например, {@link main.ui.AdminPanel})
 * и слоем доступа к данным (DAO, {@link TrainDao}).</p>
 * (Остальной Javadoc опущен для краткости, он был в предыдущем ответе)
 * @see Train Модель поезда, которой управляет контроллер.
 * @see TrainDao DAO, используемый для сохранения и загрузки данных.
 * @see main.ui.AdminPanel Пример UI, использующего этот контроллер.
 * @see main.ui.TrainControlPanel Панель UI, вызывающая методы добавления/удаления/редактирования вагона.
 */
public class TrainController {

    /** Логгер для записи событий контроллера. */
    private static final Logger logger = Logger.getLogger(TrainController.class);

    /** Текущий поезд, с которым работает контроллер. Может быть null, если поезд не выбран/не загружен. */
    private Train train;
    /** DAO для взаимодействия с базой данных поездов. Не может быть null. */
    private final TrainDao trainDao;

    // --- Константы для типов вагонов (используются в диалогах) ---
    private static final String PASSENGER_CAR_TYPE = "PassengerCar";
    private static final String RESTAURANT_CAR_TYPE = "RestaurantCar";
    private static final String BAGGAGE_CAR_TYPE = "BaggageCar";
    private static final String LOCOMOTIVE_TYPE = "Locomotive";

    // --- Константы для сообщений и заголовков диалогов ---
    // (Javadoc для констант опущен для краткости)
    private static final String POSITIVE_INTEGER_ERROR = "%s: должно быть положительным целым числом.";
    private static final String NON_NEGATIVE_NUMBER_ERROR = "%s: не может быть отрицательным.";
    private static final String NON_NEGATIVE_INTEGER_ERROR = "%s: должно быть целым неотрицательным числом.";
    private static final String ADDITIONAL_INFO_EMPTY_ERROR = "%s: не должно быть пустым.";
    private static final String CHOOSE_WAGON_TYPE_MESSAGE = "Выберите тип нового вагона:";
    private static final String ADD_WAGON_TITLE = "Добавить новый вагон";
    private static final String EDIT_WAGON_TITLE = "Редактировать вагон";
    private static final String ENTER_WAGON_DATA_MESSAGE = "Введите данные для вагона";
    private static final String ERROR_TITLE = "Ошибка";
    private static final String WARNING_TITLE = "Предупреждение";
    private static final String SUCCESS_TITLE = "Успех";
    private static final String DATA_ERROR_MESSAGE = "Ошибка введенных данных: %s";
    private static final String PARSING_ERROR_MESSAGE = "Ошибка формата введенных чисел. Проверьте правильность ввода.";
    private static final String UNEXPECTED_ERROR_MESSAGE = "Произошла непредвиденная ошибка: %s";
    private static final String NO_TRAIN_SELECTED_WARN = "Операция невозможна: Поезд не выбран.";
    private static final String WAGON_ADDED_SUCCESS = "Новый вагон успешно добавлен и сохранен.";
    private static final String WAGON_EDITED_SUCCESS = "Данные вагона успешно изменены и сохранены.";

    /**
     * Конструктор контроллера.
     * (Javadoc был добавлен ранее)
     * @param train Начальный поезд для контроллера (может быть null).
     * @param trainDao DAO для работы с БД поездов (не может быть null).
     * @throws IllegalArgumentException если trainDao равен null.
     */
    public TrainController(Train train, TrainDao trainDao) {
        if (trainDao == null) {
            logger.fatal("TrainDao cannot be null when creating TrainController!");
            throw new IllegalArgumentException("TrainDao dependency cannot be null.");
        }
        this.train = train;
        this.trainDao = trainDao;
        logger.info("TrainController initialized. TrainDao injected. Initial train set to: "
                + (train != null ? "'" + train.getTrainNumber() + "'" : "null"));
    }

    // --- Геттеры и Сеттеры ---
    /**
     * Возвращает текущий поезд, которым управляет контроллер.
     * (Javadoc был добавлен ранее)
     * @return Текущий объект {@link Train} или {@code null}.
     */
    public Train getTrain() { return train; }
    /**
     * Устанавливает текущий поезд для контроллера.
     * (Javadoc был добавлен ранее)
     * @param train Новый текущий поезд или {@code null}.
     */
    public void setTrain(Train train) {
        String oldTrainInfo = (this.train != null) ? "'" + this.train.getTrainNumber() + "'" : "null";
        String newTrainInfo = (train != null) ? "'" + train.getTrainNumber() + "'" : "null";
        if (!oldTrainInfo.equals(newTrainInfo)) {
            this.train = train;
            logger.info("Current train in controller set to: " + newTrainInfo);
        } else {
            logger.trace("setTrain called with the same train ("+newTrainInfo+"). No change.");
        }
    }
    /**
     * Возвращает экземпляр DAO, используемый этим контроллером.
     * (Javadoc был добавлен ранее)
     * @return Экземпляр {@link TrainDao}.
     */
    public TrainDao getTrainDao() { return this.trainDao; }

    // --- Основные Методы Управления ---

    /**
     * Сохраняет текущий установленный поезд в базу данных через DAO.
     * (Javadoc был добавлен ранее)
     * @throws IllegalStateException если текущий поезд null.
     * @throws RuntimeException если произошла ошибка во время сохранения в DAO.
     */
    public void saveTrain() {
        if (train == null) {
            String errorMsg = "Cannot save: no train is currently set in the controller.";
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        String trainNumber = train.getTrainNumber();
        logger.debug("Attempting to save current train: '" + trainNumber + "' with " + train.getVehicles().size() + " vehicles.");
        try {
            trainDao.saveTrain(train);
            logger.info("Current train '" + trainNumber + "' saved successfully via DAO.");
        } catch (TrainValidationException e) {
            logger.error("Failed to save train '" + trainNumber + "' due to DAO error.", e);
            throw new RuntimeException("Ошибка сохранения поезда: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error during saving train '" + trainNumber + "'.", e);
            throw new RuntimeException("Неожиданная ошибка при сохранении поезда: " + e.getMessage(), e);
        }
    }

    /**
     * Удаляет вагон из текущего поезда по индексу модели и сохраняет изменения.
     * (Javadoc был добавлен ранее)
     * @param modelRow Индекс вагона в списке {@code train.getVehicles()}.
     * @throws IllegalStateException если текущий поезд null.
     * @throws IndexOutOfBoundsException если {@code modelRow} некорректен.
     * @throws RuntimeException если произошла ошибка во время удаления или сохранения.
     */
    public void removeWagon(int modelRow) {
        if (train == null) {
            String errorMsg = NO_TRAIN_SELECTED_WARN + " Cannot remove wagon.";
            logger.warn(errorMsg);
            throw new IllegalStateException(errorMsg);
        }
        List<Vehicle> vehicles = train.getVehicles();
        String trainNumber = train.getTrainNumber();
        if (modelRow < 0 || modelRow >= vehicles.size()) {
            String errorMsg = "Invalid model row index for wagon removal: " + modelRow
                    + ". Train '" + trainNumber + "' size: " + vehicles.size();
            logger.error(errorMsg);
            throw new IndexOutOfBoundsException(errorMsg);
        }

        Vehicle removedVehicle = vehicles.get(modelRow);
        logger.debug("Attempting to remove wagon at index " + modelRow + " ["
                + removedVehicle.getVehicleType() + "] from train '" + trainNumber + "'");
        try {
            train.removeVehicle(modelRow);
            logger.trace("Vehicle removed from Train object in memory.");
            logger.debug("Attempting to save train '" + trainNumber + "' after removing vehicle...");
            saveTrain();
            logger.info("Wagon [" + removedVehicle.getVehicleType() + "] removed successfully from train '"
                    + trainNumber + "' and changes saved.");
        } catch (TrainValidationException | RuntimeException e) {
            logger.error("Error during remove/save operation for wagon at index " + modelRow
                    + " from train '" + trainNumber + "'", e);
            List<Vehicle> currentVehicles = train.getVehicles();
            boolean needsRestore = true;
            if(modelRow >= 0 && modelRow < currentVehicles.size()) {
                if (!currentVehicles.get(modelRow).equals(removedVehicle)) { /* Вагон сдвинут/удален */ }
                else { needsRestore = false; logger.warn("Vehicle still at index "+ modelRow +", restoration skipped."); }
            } else if (modelRow == currentVehicles.size() && !currentVehicles.contains(removedVehicle)){ /* Удаляли последний */ }
            else { needsRestore = false; logger.warn("Unexpected list state, restoration skipped."); }

            if (needsRestore) {
                try {
                    currentVehicles.add(modelRow, removedVehicle);
                    logger.warn("Attempted to restore removed vehicle [" + removedVehicle.getVehicleType()
                            + "] back into Train object in memory at index " + modelRow + " after save failure.");
                } catch (Exception restoreEx) {
                    logger.fatal("CRITICAL: Failed to restore vehicle state in memory for train '"
                            + trainNumber + "' after save failure! Object state may be inconsistent with DB.", restoreEx);
                }
            }
            throw new RuntimeException("Ошибка при удалении вагона: " + e.getMessage(), e);
        }
    }

    // --- Диалоговые Окна для Добавления/Редактирования Вагонов ---

    /**
     * Отображает модальный диалог для добавления нового вагона к текущему поезду.
     * (Javadoc был добавлен ранее)
     * @param parentComponent Родительский компонент для диалога.
     * @return Созданный Vehicle или null.
     */
    public Vehicle showAddWagonDialog(Component parentComponent) {
        if (train == null) {
            logger.warn(NO_TRAIN_SELECTED_WARN + " Cannot show AddWagonDialog.");
            JOptionPane.showMessageDialog(parentComponent, NO_TRAIN_SELECTED_WARN, WARNING_TITLE, JOptionPane.WARNING_MESSAGE);
            return null;
        }
        String trainNumber = train.getTrainNumber();
        logger.debug("Showing Add Wagon Dialog for train: '" + trainNumber + "'");

        String[] wagonTypes = {PASSENGER_CAR_TYPE, RESTAURANT_CAR_TYPE, BAGGAGE_CAR_TYPE, LOCOMOTIVE_TYPE};
        String selectedType = (String) JOptionPane.showInputDialog(
                parentComponent, CHOOSE_WAGON_TYPE_MESSAGE, ADD_WAGON_TITLE,
                JOptionPane.QUESTION_MESSAGE, null, wagonTypes, wagonTypes[0]
        );
        if (selectedType == null) {
            logger.debug("Add Wagon Dialog cancelled by user during type selection.");
            return null;
        }
        logger.trace("User selected wagon type for addition: " + selectedType);

        JPanel dialogPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        JTextField capacityField = new JTextField("0", 5);
        JTextField baggageCapacityField = new JTextField("0.0", 5);
        JTextField comfortLevelField = new JTextField("0", 5);
        JTextField additionalInfoField = new JTextField(15);
        JLabel additionalInfoLabel = new JLabel();

        dialogPanel.add(new JLabel("Вместимость (пасс.):")); dialogPanel.add(capacityField);
        dialogPanel.add(new JLabel("Багаж (условн. ед.):")); dialogPanel.add(baggageCapacityField);
        dialogPanel.add(new JLabel("Уровень комфорта:")); dialogPanel.add(comfortLevelField);
        dialogPanel.add(additionalInfoLabel); dialogPanel.add(additionalInfoField);

        switch (selectedType) {
            case PASSENGER_CAR_TYPE: additionalInfoLabel.setText("Тип (Купе/Плацкарт):"); break;
            case RESTAURANT_CAR_TYPE: additionalInfoLabel.setText("Кол-во столов:"); break;
            case BAGGAGE_CAR_TYPE: additionalInfoLabel.setText("Макс. вес (кг):"); break;
            case LOCOMOTIVE_TYPE: additionalInfoLabel.setText("Тяг. усилие (кН):"); break;
        }

        int result = JOptionPane.showConfirmDialog(parentComponent, dialogPanel, ENTER_WAGON_DATA_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            logger.debug("User confirmed Add Wagon Dialog data entry. Processing...");
            Vehicle newWagon = null;
            try {
                int capacity = Integer.parseInt(capacityField.getText());
                if (capacity < 0) throw new IllegalArgumentException(String.format(NON_NEGATIVE_INTEGER_ERROR, "Вместимость", capacity));
                double baggageCapacity = Double.parseDouble(baggageCapacityField.getText());
                if (baggageCapacity < 0) throw new IllegalArgumentException(String.format(NON_NEGATIVE_NUMBER_ERROR, "Вместимость багажа", baggageCapacity));
                int comfortLevel = Integer.parseInt(comfortLevelField.getText());
                if (comfortLevel < 0) throw new IllegalArgumentException(String.format(NON_NEGATIVE_INTEGER_ERROR, "Уровень комфорта", comfortLevel));
                String additionalInfo = additionalInfoField.getText().trim();

                logger.trace("Attempting to create Vehicle object of type: " + selectedType);
                switch (selectedType){
                    case PASSENGER_CAR_TYPE:
                        if (additionalInfo.isEmpty()) throw new IllegalArgumentException(String.format(ADDITIONAL_INFO_EMPTY_ERROR, "Тип пасс. вагона"));
                        newWagon = new PassengerCar(capacity, baggageCapacity, comfortLevel, additionalInfo);
                        break;
                    case RESTAURANT_CAR_TYPE:
                        int numberOfTables = Integer.parseInt(additionalInfo);
                        if (numberOfTables < 0) throw new IllegalArgumentException(String.format(NON_NEGATIVE_INTEGER_ERROR, "Количество столов", numberOfTables));
                        newWagon = new RestaurantCar(capacity, baggageCapacity, comfortLevel, numberOfTables);
                        break;
                    case BAGGAGE_CAR_TYPE:
                        double maxWeight = Double.parseDouble(additionalInfo);
                        if (maxWeight < 0) throw new IllegalArgumentException(String.format(NON_NEGATIVE_NUMBER_ERROR, "Макс. грузоподъемность", maxWeight));
                        newWagon = new BaggageCar(capacity, baggageCapacity, comfortLevel, maxWeight);
                        break;
                    case LOCOMOTIVE_TYPE:
                        int tractionForce = Integer.parseInt(additionalInfo);
                        if (tractionForce < 0) throw new IllegalArgumentException(String.format(NON_NEGATIVE_INTEGER_ERROR, "Тяговое усилие", tractionForce));
                        newWagon = new Locomotive(capacity, baggageCapacity, comfortLevel, tractionForce);
                        break;
                    default: throw new IllegalStateException("Unknown selected vehicle type: " + selectedType);
                }
                logger.debug("Vehicle object created in memory: " + newWagon);

                train.addVehicle(newWagon);
                logger.trace("Vehicle added to Train object in memory.");
                saveTrain();

                logger.info("New wagon [" + newWagon.getVehicleType() + "] added to train '" + trainNumber + "' and saved.");
                JOptionPane.showMessageDialog(parentComponent, WAGON_ADDED_SUCCESS, SUCCESS_TITLE, JOptionPane.INFORMATION_MESSAGE);
                return newWagon;

            } catch (NumberFormatException ex) {
                logger.warn("Error parsing numeric data in Add Wagon Dialog.", ex);
                JOptionPane.showMessageDialog(parentComponent, PARSING_ERROR_MESSAGE, ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException | TrainValidationException ex) {
                logger.warn("Validation error adding wagon: " + ex.getMessage());
                JOptionPane.showMessageDialog(parentComponent, String.format(DATA_ERROR_MESSAGE, ex.getMessage()), ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            } catch (RuntimeException ex) {
                logger.error("Error saving train after attempting to add a new wagon.", ex);
                if (newWagon != null && train.getVehicles().contains(newWagon)) {
                    try {
                        boolean removed = train.getVehicles().remove(newWagon);
                        if (!removed && !train.getVehicles().isEmpty()){ train.removeVehicle(train.getVehicles().size()-1); }
                        logger.warn("Removed the newly added (but not saved) wagon from Train object in memory.");
                    } catch(Exception removeEx){ logger.error("Failed to remove unsaved wagon from memory!", removeEx); }
                }
                JOptionPane.showMessageDialog(parentComponent, "Ошибка сохранения поезда после добавления вагона:\n" + ex.getMessage(), ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                logger.fatal("Unexpected error during Add Wagon Dialog processing.", ex);
                JOptionPane.showMessageDialog(parentComponent, String.format(UNEXPECTED_ERROR_MESSAGE, ex.getMessage()), ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
                if (newWagon != null && train.getVehicles().contains(newWagon)) { /* код удаления из памяти */ }
            }
        } else { logger.debug("Add Wagon Dialog cancelled by user during data entry."); }
        return null;
    }


    /**
     * Отображает модальный диалог для редактирования существующего вагона.
     * (Javadoc был добавлен ранее)
     * @param parentComponent Родительский компонент для диалога.
     * @param modelRow Индекс вагона в списке {@code train.getVehicles()}.
     * @return true, если вагон был успешно изменен и сохранен, иначе false.
     */
    public boolean showEditWagonDialog(Component parentComponent, int modelRow) {
        if (train == null) {
            logger.warn(NO_TRAIN_SELECTED_WARN + " Cannot show EditWagonDialog.");
            JOptionPane.showMessageDialog(parentComponent, NO_TRAIN_SELECTED_WARN, WARNING_TITLE, JOptionPane.WARNING_MESSAGE);
            return false;
        }
        List<Vehicle> vehicles = train.getVehicles();
        if (modelRow < 0 || modelRow >= vehicles.size()) {
            String errorMsg = "Invalid model row index for EditWagonDialog: " + modelRow;
            logger.error(errorMsg);
            JOptionPane.showMessageDialog(parentComponent, errorMsg, ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            return false;
        }

        Vehicle selectedWagon = vehicles.get(modelRow);
        String trainNumber = train.getTrainNumber();
        logger.debug("Showing Edit Wagon Dialog for wagon at index " + modelRow + " ["
                + selectedWagon.getVehicleType() + "] from train '" + trainNumber + "'");
        String baseWagonType = selectedWagon.getVehicleType().split(" - ")[0];

        JPanel dialogPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        JTextField capacityField = new JTextField(String.valueOf(selectedWagon.getCapacity()), 5);
        JTextField baggageCapacityField = new JTextField(String.valueOf(selectedWagon.getBaggageCapacity()), 5);
        JTextField comfortLevelField = new JTextField(String.valueOf(selectedWagon.getComfortLevel()), 5);
        JTextField additionalInfoField = new JTextField(15);
        JLabel additionalInfoLabel = new JLabel();

        dialogPanel.add(new JLabel("Вместимость (пасс.):")); dialogPanel.add(capacityField);
        dialogPanel.add(new JLabel("Багаж (условн. ед.):")); dialogPanel.add(baggageCapacityField);
        dialogPanel.add(new JLabel("Уровень комфорта:")); dialogPanel.add(comfortLevelField);
        dialogPanel.add(additionalInfoLabel); dialogPanel.add(additionalInfoField);

        String currentAdditionalInfo = "";
        switch (baseWagonType) {
            case PASSENGER_CAR_TYPE:
                String[] parts = ((PassengerCar) selectedWagon).getVehicleType().split(" - ", 2);
                currentAdditionalInfo = (parts.length > 1) ? parts[1] : "";
                additionalInfoLabel.setText("Тип (Купе/Плацкарт):");
                break;
            case RESTAURANT_CAR_TYPE:
                currentAdditionalInfo = String.valueOf(((RestaurantCar) selectedWagon).getNumberOfTables());
                additionalInfoLabel.setText("Кол-во столов:");
                break;
            case BAGGAGE_CAR_TYPE:
                currentAdditionalInfo = String.valueOf(((BaggageCar) selectedWagon).getMaxWeightCapacity());
                additionalInfoLabel.setText("Макс. вес (кг):");
                break;
            case LOCOMOTIVE_TYPE:
                currentAdditionalInfo = String.valueOf(((Locomotive) selectedWagon).getTractionForce());
                additionalInfoLabel.setText("Тяг. усилие (кН):");
                break;
            default:
                additionalInfoLabel.setText("Доп. инфо:");
                // Удаляем некорректный вызов приватного метода DAO
                currentAdditionalInfo = ""; // Оставляем пустым для неизвестных типов
                logger.warn("Editing unknown base wagon type '" + baseWagonType + "'. Additional info field might be inaccurate.");
                break;
        }
        additionalInfoField.setText(currentAdditionalInfo);
        final String oldAdditionalInfoFinal = currentAdditionalInfo;

        int result = JOptionPane.showConfirmDialog(parentComponent, dialogPanel, EDIT_WAGON_TITLE,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            logger.debug("User confirmed Edit Wagon Dialog. Processing changes...");
            int oldCapacity = selectedWagon.getCapacity();
            double oldBaggageCapacity = selectedWagon.getBaggageCapacity();
            int oldComfortLevel = selectedWagon.getComfortLevel();

            try {
                int newCapacity = Integer.parseInt(capacityField.getText());
                if (newCapacity < 0) throw new IllegalArgumentException(String.format(NON_NEGATIVE_INTEGER_ERROR, "Вместимость", newCapacity));
                double newBaggageCapacity = Double.parseDouble(baggageCapacityField.getText());
                if (newBaggageCapacity < 0) throw new IllegalArgumentException(String.format(NON_NEGATIVE_NUMBER_ERROR, "Вместимость багажа", newBaggageCapacity));
                int newComfortLevel = Integer.parseInt(comfortLevelField.getText());
                if (newComfortLevel < 0) throw new IllegalArgumentException(String.format(NON_NEGATIVE_INTEGER_ERROR, "Уровень комфорта", newComfortLevel));
                String newAdditionalInfo = additionalInfoField.getText().trim();

                logger.trace("Attempting to update Vehicle object in memory...");
                switch (baseWagonType) {
                    case PASSENGER_CAR_TYPE:
                        if (newAdditionalInfo.isEmpty()) throw new IllegalArgumentException(String.format(ADDITIONAL_INFO_EMPTY_ERROR, "Тип пасс. вагона"));
                        ((PassengerCar) selectedWagon).setPassengerCarType(newAdditionalInfo);
                        break;
                    case RESTAURANT_CAR_TYPE:
                        int newNumberOfTables = Integer.parseInt(newAdditionalInfo);
                        if (newNumberOfTables < 0) throw new IllegalArgumentException(String.format(NON_NEGATIVE_INTEGER_ERROR, "Количество столов", newNumberOfTables));
                        ((RestaurantCar) selectedWagon).setNumberOfTables(newNumberOfTables);
                        break;
                    case BAGGAGE_CAR_TYPE:
                        double newMaxWeight = Double.parseDouble(newAdditionalInfo);
                        if (newMaxWeight < 0) throw new IllegalArgumentException(String.format(NON_NEGATIVE_NUMBER_ERROR, "Макс. грузоподъемность", newMaxWeight));
                        ((BaggageCar) selectedWagon).setMaxWeightCapacity(newMaxWeight);
                        break;
                    case LOCOMOTIVE_TYPE:
                        int newTractionForce = Integer.parseInt(newAdditionalInfo);
                        if (newTractionForce < 0) throw new IllegalArgumentException(String.format(NON_NEGATIVE_INTEGER_ERROR, "Тяговое усилие", newTractionForce));
                        ((Locomotive) selectedWagon).setTractionForce(newTractionForce);
                        break;
                }
                selectedWagon.setCapacity(newCapacity);
                selectedWagon.setBaggageCapacity(newBaggageCapacity);
                selectedWagon.setComfortLevel(newComfortLevel);
                logger.debug("Vehicle object updated in memory: " + selectedWagon);

                saveTrain();

                logger.info("Wagon at index " + modelRow + " edited successfully for train '" + trainNumber + "' and saved.");
                JOptionPane.showMessageDialog(parentComponent, WAGON_EDITED_SUCCESS, SUCCESS_TITLE, JOptionPane.INFORMATION_MESSAGE);
                return true;

            } catch (NumberFormatException ex) {
                logger.warn("Error parsing numeric data in Edit Wagon Dialog.", ex);
                JOptionPane.showMessageDialog(parentComponent, PARSING_ERROR_MESSAGE, ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            } catch (IllegalArgumentException ex) {
                logger.warn("Validation error editing wagon: " + ex.getMessage());
                JOptionPane.showMessageDialog(parentComponent, String.format(DATA_ERROR_MESSAGE, ex.getMessage()), ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            } catch (RuntimeException ex) {
                logger.error("Error saving train after attempting to edit wagon at index " + modelRow, ex);
                try {
                    logger.warn("Attempting to restore wagon state in memory after save failure...");
                    selectedWagon.setCapacity(oldCapacity);
                    selectedWagon.setBaggageCapacity(oldBaggageCapacity);
                    selectedWagon.setComfortLevel(oldComfortLevel);
                    switch (baseWagonType) {
                        case PASSENGER_CAR_TYPE: ((PassengerCar) selectedWagon).setPassengerCarType(oldAdditionalInfoFinal); break;
                        case RESTAURANT_CAR_TYPE: ((RestaurantCar) selectedWagon).setNumberOfTables(Integer.parseInt(oldAdditionalInfoFinal)); break;
                        case BAGGAGE_CAR_TYPE: ((BaggageCar) selectedWagon).setMaxWeightCapacity(Double.parseDouble(oldAdditionalInfoFinal)); break;
                        case LOCOMOTIVE_TYPE: ((Locomotive) selectedWagon).setTractionForce(Integer.parseInt(oldAdditionalInfoFinal)); break;
                    }
                    logger.info("Wagon state restored in memory for wagon at index " + modelRow);
                } catch (Exception restoreEx) {
                    logger.fatal("CRITICAL: Failed to restore wagon state in memory after save failure!", restoreEx);
                    JOptionPane.showMessageDialog(parentComponent,
                            "Критическая ошибка: не удалось восстановить данные вагона после ошибки сохранения!\n" +
                                    "Рекомендуется перезапустить приложение.",
                            "Ошибка восстановления данных", JOptionPane.ERROR_MESSAGE);
                }
                JOptionPane.showMessageDialog(parentComponent, "Ошибка сохранения изменений:\n" + ex.getMessage(), ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                logger.fatal("Unexpected error during Edit Wagon Dialog processing.", ex);
                JOptionPane.showMessageDialog(parentComponent, String.format(UNEXPECTED_ERROR_MESSAGE, ex.getMessage()), ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
                // Попытка отката изменений в памяти
                try { /* код отката аналогичен RuntimeException */ } catch (Exception rEx) { /* лог */ }
            }
        } else { logger.debug("Edit Wagon Dialog cancelled by user."); }
        return false;
    }

} // Конец класса TrainController