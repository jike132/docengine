package com.nsfocus.reportengine.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;


@Controller
public class TestController {
    @Autowired
    FileUploadConfig fileUploadConfig;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    @ResponseBody
    @RequestMapping(value = "/test",method = {RequestMethod.GET} )
    public String test(){
        log.error("=========hello========");
        return "success";
    }

    @RequestMapping("/index")
    public String success(Map<String,Object> map){

        map.put("t","hello");
        return "index";
    }
    @RequestMapping("/report")
    public String report(Map<String,Object> map){

        map.put("t","hello");
        return "report";
    }

}
