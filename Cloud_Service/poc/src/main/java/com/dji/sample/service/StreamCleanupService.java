package com.dji.sample.service;

import com.dji.sample.entity.LiveStreamSession;

public interface StreamCleanupService {

    /**
     * Cleans all runtime resources for a stream.
     *
     * @param requestDeviceSn  Device SN used by the frontend and robot commands
     * @param physicalStreamSn Physical stream SN stored in livestream_sessions
     * @param reason           Cleanup trigger, such as MANUAL_STOP or HEARTBEAT_TIMEOUT
     * @param createHistory    Whether a history/report record should be created
     * @return the stopped session, or null when no active session exists
     */
    LiveStreamSession cleanupStream(
            String requestDeviceSn,
            String physicalStreamSn,
            String reason,
            boolean createHistory
    );
}