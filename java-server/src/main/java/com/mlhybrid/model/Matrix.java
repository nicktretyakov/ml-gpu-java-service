package com.mlhybrid.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Matrix representation for the ML Hybrid System
 */
public class Matrix {

    private int rows;
    private int cols;
    private List<Float> data;

    public Matrix() {
        this.data = new ArrayList<>();
    }

    public Matrix(int rows, int cols, List<Float> data) {
        this.rows = rows;
        this.cols = cols;
        this.data = data;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getCols() {
        return cols;
    }

    public void setCols(int cols) {
        this.cols = cols;
    }

    public List<Float> getData() {
        return data;
    }

    public void setData(List<Float> data) {
        this.data = data;
    }

    /**
     * Convert this matrix to its gRPC representation
     */
    public com.mlhybrid.grpc.Matrix toGrpcMatrix() {
        return com.mlhybrid.grpc.Matrix.newBuilder()
            .setRows(rows)
            .setCols(cols)
            .addAllData(data)
            .build();
    }

    /**
     * Create a Matrix from its gRPC representation
     */
    public static Matrix fromGrpcMatrix(com.mlhybrid.grpc.Matrix grpcMatrix) {
        return new Matrix(
            grpcMatrix.getRows(),
            grpcMatrix.getCols(),
            grpcMatrix.getDataList()
        );
    }

    /**
     * Pretty print the matrix
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Matrix %dx%d:%n", rows, cols));

        for (int i = 0; i < rows; i++) {
            sb.append("[");
            for (int j = 0; j < cols; j++) {
                int index = i * cols + j;
                if (index < data.size()) {
                    sb.append(String.format("%8.4f", data.get(index)));
                    if (j < cols - 1) {
                        sb.append(", ");
                    }
                }
            }
            sb.append("]\n");
        }

        return sb.toString();
    }
}
