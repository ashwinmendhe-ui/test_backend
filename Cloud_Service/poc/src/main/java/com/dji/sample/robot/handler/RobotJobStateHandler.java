package com.dji.sample.robot.handler;

import com.dji.sample.repository.LiveStreamSessionRepository;
import com.dji.sample.robot.entity.RobotJobStateData;
import com.dji.sample.service.DeviceWebSocketPublisher;
import com.dji.sample.service.IDeviceRedisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RobotJobStateHandler {

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final DeviceWebSocketPublisher webSocketPublisher;
    private final LiveStreamSessionRepository liveStreamSessionRepository;
    private final IDeviceRedisService deviceRedisService;

    public void handle(String deviceSn, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode data = root.has("data") ? root.path("data") : root;

            String jobId = firstText(data, "job_id", "jobId");
            String status = firstText(
                    data,
                    "status",
                    "job_status",
                    "jobStatus"
            );
            String missionId = firstText(
                    data,
                    "mission_id",
                    "missionId"
            );
            String message = textOrNull(data, "message");

            log.info(
                    "Parsed robot job state. deviceSn={}, jobId={}, status={}, missionId={}, rawData={}",
                    deviceSn,
                    jobId,
                    status,
                    missionId,
                    data
            );

            RobotJobStateData jobState = new RobotJobStateData();
            jobState.setJobId(jobId);
            jobState.setStatus(status);
            jobState.setMissionId(missionId);
            jobState.setMessage(message);

            String jobKey =
                    "robot:" + deviceSn + ":jobId";

            String localStatusKey =
                    "robot:" + deviceSn + ":status";

            String prodStatusKey =
                    "status:" + deviceSn;

            String missionKey =
                    "robot:" + deviceSn + ":missionId";

            Set<String> terminalStates = Set.of(
                    "COMPLETED",
                    "COMPLETE",
                    "FAILED",
                    "CANCELLED",
                    "CANCELED",
                    "STOPPED",
                    "IDLE"
            );

            String storedJobId =
                    deviceRedisService.getRobotJobId(deviceSn);

            boolean hasActiveSession =
                    liveStreamSessionRepository
                            .findFirstByDeviceSnAndSessionStatusOrderByStartedAtDesc(
                                    deviceSn,
                                    "ACTIVE"
                            )
                            .isPresent();

            boolean isDeviceOnline =
                    deviceRedisService.getDeviceOnline(deviceSn) != null;

            boolean matchesStoredJob =
                    jobId != null
                            && storedJobId != null
                            && jobId.equals(storedJobId);

            boolean isTerminal =
                    status != null
                            && terminalStates.contains(
                                    status.toUpperCase()
                            );

            /*
             * Terminal state.
             *
             * Protect the current job from a delayed terminal
             * message belonging to an older robot job.
             *
             * Example:
             *
             *   old job A -> delayed STOPPED
             *   new job B -> currently STARTING/RUNNING
             *
             * The delayed STOPPED for A must not clear B.
             */
            if (isTerminal) {

                if (storedJobId != null
                        && jobId != null
                        && !matchesStoredJob) {

                    log.warn(
                            "Ignoring terminal state for stale robot job. deviceSn={}, incomingJobId={}, storedJobId={}, status={}",
                            deviceSn,
                            jobId,
                            storedJobId,
                            status
                    );

                    return;
                }

                stringRedisTemplate.delete(jobKey);
                stringRedisTemplate.delete(localStatusKey);
                stringRedisTemplate.delete(prodStatusKey);
                stringRedisTemplate.delete(missionKey);

                log.info(
                        "Robot job cleared. deviceSn={}, jobId={}, status={}",
                        deviceSn,
                        jobId,
                        status
                );

            } else {

                /*
                 * Protect the current job from a delayed non-terminal
                 * message belonging to an older job.
                 *
                 * Example:
                 *
                 *   current Redis jobId = B
                 *   delayed RUNNING arrives for job A
                 *
                 * A must never overwrite B.
                 */
                if (storedJobId != null
                        && jobId != null
                        && !matchesStoredJob) {

                    log.warn(
                            "Ignoring active state for stale robot job. deviceSn={}, incomingJobId={}, storedJobId={}, status={}",
                            deviceSn,
                            jobId,
                            storedJobId,
                            status
                    );

                    return;
                }

                /*
                 * If the physical robot is offline, a non-terminal
                 * state such as RUNNING must not recreate stale
                 * working state.
                 */
                if (!isDeviceOnline) {

                    stringRedisTemplate.delete(jobKey);
                    stringRedisTemplate.delete(localStatusKey);
                    stringRedisTemplate.delete(prodStatusKey);
                    stringRedisTemplate.delete(missionKey);

                    log.warn(
                            "Ignoring robot active state because device is offline. deviceSn={}, jobId={}, status={}",
                            deviceSn,
                            jobId,
                            status
                    );

                    return;
                }

                /*
                 * create_job is published before the ACTIVE
                 * LiveStreamSession is saved.
                 *
                 * Therefore RUNNING can legitimately arrive while
                 * hasActiveSession == false.
                 *
                 * LiveStreamService stores the expected jobId in
                 * Redis before publishing create_job, so a matching
                 * jobId means this is the job ROBOPILOT is currently
                 * starting.
                 */
                boolean trustedPendingJob =
                        !hasActiveSession
                                && matchesStoredJob;

                /*
                 * No ACTIVE DB session and the incoming robot job
                 * doesn't match the pending ROBOPILOT job.
                 *
                 * Ignore it without deleting Redis because it may
                 * simply be a delayed/orphan packet.
                 */
                if (!hasActiveSession
                        && !trustedPendingJob) {

                    log.warn(
                            "Ignoring robot working state without matching active/pending session. deviceSn={}, incomingJobId={}, storedJobId={}, status={}",
                            deviceSn,
                            jobId,
                            storedJobId,
                            status
                    );

                    return;
                }

                /*
                 * Keep runtime job state without expiry.
                 *
                 * Robot jobs may run for a long time and RUNNING
                 * may only be published when the state changes.
                 * Cleanup happens on terminal state, manual cleanup,
                 * or device-offline recovery.
                 */
                if (jobId != null) {
                    stringRedisTemplate
                            .opsForValue()
                            .set(
                                    jobKey,
                                    jobId
                            );
                }

                if (status != null) {
                    stringRedisTemplate
                            .opsForValue()
                            .set(
                                    localStatusKey,
                                    status
                            );

                    stringRedisTemplate
                            .opsForValue()
                            .set(
                                    prodStatusKey,
                                    status
                            );
                }

                /*
                 * Some RUNNING packets may not contain missionId.
                 *
                 * In that case do not delete/overwrite the mission
                 * that LiveStreamService stored during STARTING.
                 */
                if (missionId != null) {
                    stringRedisTemplate
                            .opsForValue()
                            .set(
                                    missionKey,
                                    missionId
                            );
                }

                log.info(
                        "Active robot job state stored. deviceSn={}, jobId={}, status={}, missionId={}, hasActiveSession={}, trustedPendingJob={}",
                        deviceSn,
                        jobId,
                        status,
                        missionId,
                        hasActiveSession,
                        trustedPendingJob
                );
            }

            webSocketPublisher.publishStatus(
                    deviceSn,
                    jobState
            );

            webSocketPublisher.publishDashboardStatus(
                    deviceSn,
                    status,
                    "robot-job-state"
            );

            log.info(
                    "Robot job state received. deviceSn={}, jobId={}, status={}, missionId={}, message={}",
                    deviceSn,
                    jobId,
                    status,
                    missionId,
                    message
            );

        } catch (Exception e) {

            log.error(
                    "Failed to handle robot job state. deviceSn={}, payload={}",
                    deviceSn,
                    payload,
                    e
            );
        }
    }

    private String textOrNull(
            JsonNode node,
            String fieldName
    ) {
        JsonNode value =
                node.path(fieldName);

        if (value.isMissingNode()
                || value.isNull()) {
            return null;
        }

        String text =
                value.asText();

        return text == null
                || text.isBlank()
                || "null".equalsIgnoreCase(text)
                ? null
                : text;
    }

    private String firstText(
            JsonNode node,
            String... fieldNames
    ) {
        for (String fieldName : fieldNames) {

            String value =
                    textOrNull(
                            node,
                            fieldName
                    );

            if (value != null) {
                return value;
            }
        }

        return null;
    }
}