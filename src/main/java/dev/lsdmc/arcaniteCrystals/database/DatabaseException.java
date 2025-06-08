package dev.lsdmc.arcaniteCrystals.database;

/**
 * Custom exception for database-related errors.
 * Provides better error context and handling.
 */
public class DatabaseException extends RuntimeException {
    
    public DatabaseException(String message) {
        super(message);
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public DatabaseException(Throwable cause) {
        super(cause);
    }
} 