package com.mlhybrid.model;

import java.util.List;

public class ComputeResponse {

    private List<Float> result;
    private String taskId;
    private String status;

    public ComputeResponse() {}

    public ComputeResponse(List<Float> result, String taskId, String status) {
        this.result = result;
        this.taskId = taskId;
        this.status = status;
    }

    public List<Float> getResult() {
        return result;
    }

    public void setResult(List<Float> result) {
        this.result = result;
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

    @Override
    public String toString() {
        return (
            "ComputeResponse{" +
            "result.size=" +
            (result != null ? result.size() : 0) +
            ", taskId='" +
            taskId +
            '\'' +
            ", status='" +
            status +
            '\'' +
            '}'
        );
    }
}
