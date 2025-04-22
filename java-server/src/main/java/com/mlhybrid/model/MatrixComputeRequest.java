package com.mlhybrid.model;

/**
 * Request object for matrix compute operations
 */
public class MatrixComputeRequest {

    private Matrix matrixA;
    private Matrix matrixB;
    private String taskId;
    private String operation;

    public MatrixComputeRequest() {}

    public MatrixComputeRequest(
        Matrix matrixA,
        Matrix matrixB,
        String taskId,
        String operation
    ) {
        this.matrixA = matrixA;
        this.matrixB = matrixB;
        this.taskId = taskId;
        this.operation = operation;
    }

    public Matrix getMatrixA() {
        return matrixA;
    }

    public void setMatrixA(Matrix matrixA) {
        this.matrixA = matrixA;
    }

    public Matrix getMatrixB() {
        return matrixB;
    }

    public void setMatrixB(Matrix matrixB) {
        this.matrixB = matrixB;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }
}
