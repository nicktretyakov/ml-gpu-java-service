package com.mlhybrid.model;

/**
 * Response object for matrix compute operations
 */
public class MatrixComputeResponse {

    private Matrix resultMatrix;
    private String taskId;
    private String status;
    private String errorMessage;
    private long executionTimeMs;

    public MatrixComputeResponse() {}

    public MatrixComputeResponse(
        Matrix resultMatrix,
        String taskId,
        String status
    ) {
        this.resultMatrix = resultMatrix;
        this.taskId = taskId;
        this.status = status;
    }

    public MatrixComputeResponse(
        Matrix resultMatrix,
        String taskId,
        String status,
        String errorMessage,
        long executionTimeMs
    ) {
        this.resultMatrix = resultMatrix;
        this.taskId = taskId;
        this.status = status;
        this.errorMessage = errorMessage;
        this.executionTimeMs = executionTimeMs;
    }

    public Matrix getResultMatrix() {
        return resultMatrix;
    }

    public void setResultMatrix(Matrix resultMatrix) {
        this.resultMatrix = resultMatrix;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}
