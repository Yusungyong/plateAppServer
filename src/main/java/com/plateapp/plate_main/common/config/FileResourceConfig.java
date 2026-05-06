package com.plateapp.plate_main.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FileResourceConfig implements WebMvcConfigurer {

    private final String uploadPath;
    private final String publicBasePath;

    public FileResourceConfig(
            @Value("${file.upload.path}") String uploadPath,
            @Value("${file.upload.public-base-path:/files}") String publicBasePath
    ) {
        this.uploadPath = uploadPath;
        this.publicBasePath = publicBasePath.startsWith("/") ? publicBasePath : "/" + publicBasePath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String handlerPattern = publicBasePath.endsWith("/") ? publicBasePath + "**" : publicBasePath + "/**";
        String location = "file:///" + uploadPath.replace('\\', '/');
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler(handlerPattern)
                .addResourceLocations(location);
    }
}
