package org.upn.edu.pe.model;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileUploadConfig {
	 @Value("${upload.dir}")
	    private String uploadDir;

	    public String getUploadDir() {
	        return uploadDir;
	    }
}
