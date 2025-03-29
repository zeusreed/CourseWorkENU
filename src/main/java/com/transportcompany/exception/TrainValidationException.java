package com.transportcompany.exception;

/**
 * <p>Специализированное проверяемое исключение (checked exception), используемое для сигнализации об ошибках,
 * связанных с бизнес-логикой или операциями над объектами поезда ({@link com.transportcompany.train.Train})
 * и его компонентами ({@link com.transportcompany.vehicle.Vehicle}), а также при взаимодействии с базой данных
 * через DAO классы ({@link com.transportcompany.db.TrainDao}, {@link com.transportcompany.db.UserDao}).</p>
 *
 * <p><b>Почему кастомное исключение?</b></p>
 * <ul>
 *     <li><b>Семантика:</b> Четко указывает на тип произошедшей проблемы (ошибка валидации, ошибка операции с поездом/БД).</li>
 *     <li><b>Обработка:</b> Позволяет вызывающему коду (например, в UI или контроллере) специфически обрабатывать
 *         именно эти ошибки, отличая их от других стандартных исключений Java (например, {@code NullPointerException}).</li>
 *     <li><b>Checked Exception:</b> Наследуется от {@link Exception}, что делает его проверяемым. Это заставляет
 *         разработчика либо обработать исключение (в блоке {@code try-catch}), либо пробросить его дальше
 *         (объявив в {@code throws} сигнатуры метода), что повышает надежность кода, т.к. такие ошибки
 *         сложнее случайно проигнорировать.</li>
 *     <li><b>Передача причины:</b> Позволяет инкапсулировать исходное исключение (например, {@code SQLException} из DAO)
 *         с помощью конструктора {@link #TrainValidationException(String, Throwable)}, сохраняя полную информацию об ошибке
 *         для логгирования и отладки.</li>
 * </ul>
 *
 * <p><b>Примеры использования:</b></p>
 * <ul>
 *     <li>Попытка добавить {@code null} вагон в поезд (в {@link com.transportcompany.train.Train#addVehicle(com.transportcompany.vehicle.Vehicle)}).</li>
 *     <li>Ошибка при сохранении поезда в БД (в {@link com.transportcompany.db.TrainDao#saveTrain(com.transportcompany.train.Train)}).</li>
 *     <li>Попытка зарегистрировать пользователя с уже существующим именем (в {@link com.transportcompany.db.UserDao#registerUser(String, String, String)}).</li>
 * </ul>
 *
 * @see Exception Базовый класс для проверяемых исключений.
 */
public class TrainValidationException extends Exception {

    /**
     * <p>Конструктор с одним параметром - сообщением об ошибке.</p>
     * <p>Используется, когда причина ошибки не связана с другим исключением или причина не важна
     * для вызывающего кода (но может быть залоггирована до вызова конструктора).</p>
     *
     * @param message Детальное сообщение, описывающее причину ошибки. Должно быть информативным
     *                для разработчика и, возможно, для пользователя (после обработки в UI).
     */
    public TrainValidationException(String message) {
        // Вызов конструктора суперкласса Exception(String message)
        super(message);
        // Здесь можно добавить логгирование самого факта создания исключения, если нужно
        // Logger.getLogger(TrainValidationException.class).warn("TrainValidationException created: " + message);
    }

    /**
     * <p>Конструктор с сообщением об ошибке и исходной причиной (cause).</p>
     * <p>Это предпочтительный конструктор при перехвате другого исключения (например, {@code SQLException}),
     * так как он сохраняет всю цепочку вызовов (stack trace) и исходную причину.</p>
     *
     * @param message Детальное сообщение, описывающее контекст ошибки (например, "Ошибка при сохранении поезда в БД").
     * @param cause   Исходное исключение, которое привело к возникновению {@code TrainValidationException}.
     *                Позволяет сохранить полную информацию об ошибке для диагностики.
     */
    public TrainValidationException(String message, Throwable cause) {
        // Вызов конструктора суперкласса Exception(String message, Throwable cause)
        super(message, cause);
        // Logger.getLogger(TrainValidationException.class).error("TrainValidationException created: " + message, cause);
    }
}