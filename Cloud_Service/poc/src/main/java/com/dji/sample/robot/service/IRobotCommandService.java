package com.dji.sample.robot.service;

import com.dji.sample.robot.entity.RobotCommandData;

public interface IRobotCommandService {

    void createJob(String robotId, String commandId, String jobId, Object payload);

    void startJob(String robotId, String commandId, String jobId);

    void ackJob(String robotId, String commandId, String jobId);

    void cancelJob(String robotId, String commandId, String jobId);

    void sendCommand(String robotId, RobotCommandData commandData);

    void cleanJob(String robotId);
}