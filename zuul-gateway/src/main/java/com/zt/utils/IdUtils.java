package com.zt.utils;

import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class IdUtils {

    public String getUUId() {
        return UUID.randomUUID().toString();
    }

}
