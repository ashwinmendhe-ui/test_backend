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
public enum SimCardStateEnum {

    NO_CARD(0),

    INSERTED(1),
    
    UNKNOWN(-1),

    ;
    
    private static final Logger log = LoggerFactory.getLogger(SimCardStateEnum.class);

    private final int state;

    SimCardStateEnum(int state) {
        this.state = state;
    }

    @JsonValue
    public int getState() {
        return state;
    }

    @JsonCreator
    public static SimCardStateEnum find(int state) {
        return Arrays.stream(values()).filter(stateEnum -> stateEnum.state == state).findAny()
                .orElseGet(() -> {
                    log.warn("Unknown SimCardStateEnum value: {}, using UNKNOWN", state);
                    return UNKNOWN;
                });
    }

}
