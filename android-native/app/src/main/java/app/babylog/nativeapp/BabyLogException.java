package app.babylog.nativeapp;

public class BabyLogException extends Exception {
    public BabyLogException(String message) {
        super(message);
    }

    public BabyLogException(String message, Throwable cause) {
        super(message, cause);
    }

    public static final class ValidationException extends BabyLogException {
        public ValidationException(String message) {
            super(message);
        }

        public ValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final class NotFoundException extends BabyLogException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    public static final class StorageException extends BabyLogException {
        public StorageException(String message) {
            super(message);
        }

        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
