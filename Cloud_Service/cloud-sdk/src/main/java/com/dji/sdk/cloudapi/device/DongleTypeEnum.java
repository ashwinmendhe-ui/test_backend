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
public enum DongleTypeEnum {

    OLD_DONGLE(6),

    SUPPORTED_ESIM(10),
    
    UNKNOWN(-1),

    ;
    
    private static final Logger log = LoggerFactory.getLogger(DongleTypeEnum.class);

    private final int type;

    DongleTypeEnum(int type) {
        this.type = type;
    }

    @JsonValue
    public int getType() {
        return type;
    }

    @JsonCreator
    public static DongleTypeEnum find(int type) {
        return Arrays.stream(values()).filter(typeEnum -> typeEnum.type == type).findAny()
            .orElseGet(() -> {
                log.warn("Unknown DongleTypeEnum value: {}, using UNKNOWN", type);
                return UNKNOWN;
            });
    }

}
