package com.prasanth.namana.tmobile.Sevice;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface UploadService {
    String processFile(MultipartFile file) throws IOException;
}
