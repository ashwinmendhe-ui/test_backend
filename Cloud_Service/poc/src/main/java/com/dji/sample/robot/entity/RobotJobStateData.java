package com.dji.sample.robot.entity;

import lombok.Data;

@Data
public class RobotJobStateData {

    private String jobId;
    private String status;     // IDLE / PENDING / RUNNING / COMPLETED / FAILED / CANCELED
    private String missionId;
    private String message;
}