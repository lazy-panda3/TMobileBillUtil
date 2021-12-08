package com.prasanth.namana.tmobile.controller;

import com.prasanth.namana.tmobile.Sevice.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/")
public class UploadController {

    @Autowired
    public UploadService uploadService;

    @ResponseBody
    @PostMapping("upload")
    public String uploadFile(@RequestParam("file") MultipartFile multipartFile) throws IOException {

       // MultipartFile multipartFile = (MultipartFile) request.getAttribute("file");
        return uploadService.processFile(multipartFile);
    }
    @RequestMapping(method = RequestMethod.GET)
    public String index(){
        return "forward:/uploadFile.html";
    }


    @ResponseBody
    @GetMapping("uploady")
    public  String uploadFile(){
        return "This is a test";
    }
}
