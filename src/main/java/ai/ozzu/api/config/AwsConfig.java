package ai.ozzu.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Bean
    public S3Client s3Client(MediaProperties props) {

        return S3Client.builder()
                .region(Region.of(props.getRegion()))
                .build();
    }
}
