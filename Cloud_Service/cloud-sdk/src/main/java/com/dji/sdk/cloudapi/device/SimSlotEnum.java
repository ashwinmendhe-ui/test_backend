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
public enum SimSlotEnum {

    UNKNOWN(0),

    SIM(1),

    ESIM(2),

    ;
    
    private static final Logger log = LoggerFactory.getLogger(SimSlotEnum.class);

    private final int slot;

    SimSlotEnum(int slot) {
        this.slot = slot;
    }

    @JsonValue
    public int getSlot() {
        return slot;
    }

    @JsonCreator
    public static SimSlotEnum find(int slot) {
        return Arrays.stream(values()).filter(slotEnum -> slotEnum.slot == slot).findAny()
            .orElseGet(() -> {
                log.warn("Unknown SimSlotEnum value: {}, using UNKNOWN", slot);
                return UNKNOWN;
            });
    }

}
