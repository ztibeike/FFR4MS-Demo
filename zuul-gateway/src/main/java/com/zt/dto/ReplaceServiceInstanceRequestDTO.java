package com.zt.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ReplaceServiceInstanceRequestDTO {

    private String serviceName;

    private String downInstanceHost;

    private Integer downInstancePort;

    private String replaceInstanceHost;

    private Integer replaceInstancePort;

}
