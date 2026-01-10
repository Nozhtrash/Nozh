package dev.nozh.core.capability;

import java.util.Optional;

/**
 * Resultado de ejecución de una acción de capability.
 * 
 * <p>Encapsula estado de éxito, mensajes de error, y snapshot
 * para rollback. Soporta estados granulares para compatibilidad.
 * 
 * <p><b>Backward Compatibility:</b> Mantiene compatibilidad con
 * contratos previos que esperan Status enum.
 * 
 * @author Nozh Team
 * @since 0.5.0 (consolidado desde v0.2.0-alpha)
 */
public final class ActionResult {
    
    /**
     * Estados de resultado (compatible con contratos previos).
     */
    public enum Status {
        /** Acción ejecutada exitosamente */
        SUCCESS,
        
        /** Acción falló por error técnico */
        ERROR,
        
        /** Parámetros inválidos */
        INVALID,
        
        /** No hubo cambio (ya estaba en estado objetivo) */
        NO_CHANGE
    }
    
    private final Status status;
    private final String message;
    private final StateSnapshot snapshot;
    private final String method; // "Sodium", "Vanilla", "Iris", etc.
    private final long executionTimeNanos;
    
    private ActionResult(Status status, String message, StateSnapshot snapshot, 
                        String method, long executionTimeNanos) {
        this.status = status;
        this.message = message;
        this.snapshot = snapshot;
        this.method = method;
        this.executionTimeNanos = executionTimeNanos;
    }
    
    // ========== Factory Methods (primary API) ==========
    
    /**
     * Crea resultado de éxito con snapshot.
     */
    public static ActionResult success(StateSnapshot snapshot) {
        return new ActionResult(Status.SUCCESS, "Success", snapshot, "Vanilla", 0);
    }
    
    /**
     * Crea resultado de éxito con snapshot y método específico.
     */
    public static ActionResult success(StateSnapshot snapshot, String method) {
        return new ActionResult(Status.SUCCESS, "Success", snapshot, method, 0);
    }
    
    /**
     * Crea resultado de éxito con timing information.
     */
    public static ActionResult success(StateSnapshot snapshot, String method, long executionNanos) {
        return new ActionResult(Status.SUCCESS, "Success", snapshot, method, executionNanos);
    }
    
    /**
     * Crea resultado de error.
     */
    public static ActionResult error(String errorMessage) {
        return new ActionResult(Status.ERROR, errorMessage, null, null, 0);
    }
    
    /**
     * Crea resultado de parámetros inválidos.
     */
    public static ActionResult invalid(String validationMessage) {
        return new ActionResult(Status.INVALID, validationMessage, null, null, 0);
    }
    
    /**
     * Crea resultado de "sin cambio" (idempotente).
     */
    public static ActionResult noChange(String reason) {
        return new ActionResult(Status.NO_CHANGE, reason, null, null, 0);
    }
    
    /**
     * Crea resultado de "sin cambio" con snapshot del estado actual.
     */
    public static ActionResult noChange(StateSnapshot currentState) {
        return new ActionResult(Status.NO_CHANGE, "Already at target value", currentState, null, 0);
    }
    
    // ========== Query Methods ==========
    
    /**
     * @return true si la acción fue exitosa
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
    
    /**
     * @return true si hay un snapshot disponible para rollback
     */
    public boolean canRollback() {
        return snapshot != null;
    }
    
    /**
     * @return true si hubo un cambio real (no NO_CHANGE)
     */
    public boolean hasChanges() {
        return status != Status.NO_CHANGE;
    }
    
    // ========== Getters ==========
    
    public Status getStatus() {
        return status;
    }
    
    public String getMessage() {
        return message;
    }
    
    /**
     * @return mensaje de error, o null si fue exitoso
     */
    public String getError() {
        return status != Status.SUCCESS ? message : null;
    }
    
    /**
     * @return snapshot para rollback, o null si no está disponible
     */
    public StateSnapshot getSnapshot() {
        return snapshot;
    }
    
    /**
     * @return snapshot como Optional
     */
    public Optional<StateSnapshot> getSnapshotOptional() {
        return Optional.ofNullable(snapshot);
    }
    
    /**
     * @return método usado para la acción ("Sodium", "Vanilla", etc.)
     */
    public String getMethod() {
        return method != null ? method : "Unknown";
    }
    
    /**
     * @return tiempo de ejecución en nanosegundos, o 0 si no fue medido
     */
    public long getExecutionTimeNanos() {
        return executionTimeNanos;
    }
    
    /**
     * @return tiempo de ejecución en milisegundos
     */
    public double getExecutionTimeMs() {
        return executionTimeNanos / 1_000_000.0;
    }
    
    // ========== Transformation ==========
    
    /**
     * Agrega timing information a este resultado.
     */
    public ActionResult withTiming(long executionNanos) {
        return new ActionResult(status, message, snapshot, method, executionNanos);
    }
    
    /**
     * Agrega o cambia el método usado.
     */
    public ActionResult withMethod(String newMethod) {
        return new ActionResult(status, message, snapshot, newMethod, executionTimeNanos);
    }
    
    // ========== Display ==========
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ActionResult[");
        sb.append(status);
        
        if (message != null && !message.equals("Success")) {
            sb.append(": ").append(message);
        }
        
        if (method != null) {
            sb.append(", method=").append(method);
        }
        
        if (snapshot != null) {
            sb.append(", hasSnapshot=true");
        }
        
        if (executionTimeNanos > 0) {
            sb.append(String.format(", time=%.2fms", getExecutionTimeMs()));
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * @return representación user-friendly para logging
     */
    public String toDisplayString() {
        switch (status) {
            case SUCCESS:
                return String.format("✓ %s ejecutado via %s", 
                    snapshot != null ? snapshot.getPrimaryKey() : "Action", getMethod());
            case ERROR:
                return String.format("✗ Error: %s", message);
            case INVALID:
                return String.format("⚠ Inválido: %s", message);
            case NO_CHANGE:
                return String.format("➜ Sin cambios: %s", message);
            default:
                return toString();
        }
    }
}