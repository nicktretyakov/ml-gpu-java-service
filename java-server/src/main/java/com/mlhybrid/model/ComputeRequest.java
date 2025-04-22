package com.mlhybrid.model;

import java.util.List;

public class ComputeRequest {

    private List<Float> data;
    private String taskId;
    private String taskType;

    public ComputeRequest() {}

    public ComputeRequest(List<Float> data, String taskId, String taskType) {
        this.data = data;
        this.taskId = taskId;
        this.taskType = taskType;
    }

    public List<Float> getData() {
        return data;
    }

    public void setData(List<Float> data) {
        this.data = data;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    @Override
    public String toString() {
        return (
            "ComputeRequest{" +
            "data.size=" +
            (data != null ? data.size() : 0) +
            ", taskId='" +
            taskId +
            '\'' +
            ", taskType='" +
            taskType +
            '\'' +
            '}'
        );
    }
}
