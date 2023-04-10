package com.nsfocus.reportengine.controller;

import com.nsfocus.reportengine.ueditor.ActionEnter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@RestController
@RequestMapping(value = "/ueditor")
public class UEditorController {

    //注入application.properties里面的配置web.upload-path
    @Value("${web.upload-path}")
    private String uploadPath;

    @RequestMapping(value="/test",method = RequestMethod.POST)
    public String test() {
        return "hello";
    }

    @RequestMapping("/config")
    public void config(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("application/json");
        String rootPath = uploadPath;
        try {
            String exec = new ActionEnter(request, rootPath).exec();
            PrintWriter writer = response.getWriter();
            writer.write(exec);
            writer.flush();
            writer.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @CrossOrigin("*")
    @ResponseBody
    @RequestMapping(value = "/uploadimage", method = RequestMethod.POST)
    public java.util.Map<String, String> uploadimage(@RequestParam(value = "upfile") MultipartFile upfile) {
        Map<String, String> map = new HashMap<>();
        String fileName = upfile.getOriginalFilename();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String filename = sdf.format(new Date()) + new Random().nextInt(1000);
        String fileExt = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        filename = filename + "." + fileExt;//存入虚拟目录后的文件名
        String rootPath = uploadPath + "/img/file";
        File uploadedFile = new File(rootPath, filename);//存入虚拟目录后的文件
        try {
            upfile.transferTo(uploadedFile);//上传

            map.put("url", "/img/file/" + filename);//这个url是前台回显路径（回显路径为config.json中的imageUrlPrefix+此处的url）

            map.put("state", "SUCCESS");
            map.put("original", "");
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }


}
