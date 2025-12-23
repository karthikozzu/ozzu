package ai.ozzu.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.db")
public record DbProperties(
        int writePoolSizeHint,
        int readPoolSizeHint
) {
    public DbProperties {
        // safe defaults
        if (writePoolSizeHint <= 0) writePoolSizeHint = 30;
        if (readPoolSizeHint <= 0) readPoolSizeHint = 30;
    }
}
