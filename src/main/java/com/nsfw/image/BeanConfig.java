package com.nsfw.image;

import com.oujingzhou.censor.ImageCensor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfig {
    @Bean
    public ImageCensor getImageCenser() {
        return new ImageCensor();
    }
}
