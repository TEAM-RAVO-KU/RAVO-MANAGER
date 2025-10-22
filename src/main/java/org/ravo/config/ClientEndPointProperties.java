package org.ravo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties(prefix = "client.endpoints")
public class ClientEndPointProperties {

    private String scaleDown;

    private String scaleUp;
}
