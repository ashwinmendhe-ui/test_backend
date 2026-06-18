package com.dji.sdk.cloudapi.device;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * @author sean
 * @version 1.7
 * @date 2023/10/20
 */
public enum EsimActivateStateEnum {

    INACTIVATED(0),

    ACTIVATED(1),
    
    UNKNOWN(-1),

    ;
    
    private static final Logger log = LoggerFactory.getLogger(EsimActivateStateEnum.class);

    private final int state;

    EsimActivateStateEnum(int state) {
        this.state = state;
    }

    @JsonValue
    public int getState() {
        return state;
    }

    @JsonCreator
    public static EsimActivateStateEnum find(int state) {
        return Arrays.stream(values()).filter(stateEnum -> stateEnum.state == state).findAny()
            .orElseGet(() -> {
                log.warn("Unknown EsimActivateStateEnum value: {}, using UNKNOWN", state);
                return UNKNOWN;
            });
    }

}
