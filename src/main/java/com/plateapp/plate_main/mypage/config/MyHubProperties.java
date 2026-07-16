package com.plateapp.plate_main.mypage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "features.my-hub")
public class MyHubProperties {

    private boolean enabled;
    private boolean imageVisibilityReady;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isImageVisibilityReady() {
        return imageVisibilityReady;
    }

    public void setImageVisibilityReady(boolean imageVisibilityReady) {
        this.imageVisibilityReady = imageVisibilityReady;
    }
}
