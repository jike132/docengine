package com.nsfocus.reportengine;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.Text;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.jaxen.JaxenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;

@Configuration
@EnableScheduling
public class GenerateReportTask implements SchedulingConfigurer {
    //注入application.properties里面的配置web.upload-path
    @Value("${web.upload-path}")
    private String uploadPath;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
                //1.添加任务内容(Runnable)
                () -> {
                    LocalDateTime nowtime = LocalDateTime.now();
                    String datasource = uploadPath + "/img/datasource";
                    File dir = new File(datasource);
                    File[] files = dir.listFiles();
                    LocalDateTime time1 = null;
                    String format = "yyyy-MM-dd HH:mm:ss";
                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
                    for(File wfile : files){
                        String configContent = readFileByLines(wfile);
                        JSONObject jsonConfig = JSON.parseObject(configContent);
                        String strTime = jsonConfig.getString("time");
                        int interval = Integer.parseInt(jsonConfig.getString("interval"));
                        String strStart = jsonConfig.getString("start");
                        String startTime = strStart + " " + strTime + ":00";
                        time1 = LocalDateTime.parse(startTime, dateTimeFormatter);
                        if(time1.isBefore(nowtime)){//time1 < nowtime
                            long mins = Duration.between(time1, nowtime).toMinutes();
                            if(mins % interval == 0){
                                String fileName = wfile.getName();
                                fileName = fileName.substring(0, fileName.lastIndexOf("."));
                                System.out.println(nowtime.toLocalTime() + " 执行动态定时任务: " + fileName);
                                ExportReport(fileName, uploadPath, false);
                            }
                        }
                    }
                },
                triggerContext -> {
                    String cron = "0 */1 * * * ?";
                    return new CronTrigger(cron).nextExecutionTime(triggerContext);
                }
        );
    }

    public static String ExportReport(String md5, String basePath, boolean excuteNow){
        try{
            String template = basePath + "/img/template/" + md5 + ".html";
            String dataSource = basePath + "/img/datasource/" + md5 + ".json";
            //load datasource
            String configContent = readFileByLines(dataSource);
            JSONObject jsonConfig = JSON.parseObject(configContent);
            //load template
            String content = readFileByLines(template);
            content = checkHtmlTag(content);
            InputStream templatein = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));//new FileInputStream(new File(template));
            SAXReader sax = new SAXReader();
            Document doc = sax.read(templatein);
            Element root = doc.getRootElement();
            List<Element> list = root.elements();
            Element tableNode = null;
            for (Element ele : list) {
                if(ele.getName().equals("table")){
                    tableNode = ele;
                    break;
                }
                List<Node> nodelist = ele.selectNodes("table");
                if(nodelist.size() > 0 && nodelist.get(0) instanceof Element){
                    tableNode = (Element) nodelist.get(0);
                    break;
                }
            }
            //format query sql
            if(tableNode != null){
                List<Element> list1 = tableNode.elements();
                if(list1.size() == 0)
                    return null;
                if(list1.get(0).getName().equals("tbody")){
                    list1 = list1.get(0).elements();
                }
                JSONObject bindobj = jsonConfig.getJSONObject("bind");
                StringBuffer sb = new StringBuffer("select ");
                int idx = 0;
                boolean bfirst = true;
                String key, value, table, dateColumn = null;
                StringBuffer tables = new StringBuffer();
                for (Element trNode : list1) {
                    if(bfirst){
                        List<Element> list2 = trNode.elements();
                        for (Element tdNode : list2) {
                            key = tdNode.getStringValue();
                            if(key == null || key.isEmpty())
                                break;
                            value = bindobj.getString(key);
                            if(sb.length() != 7)
                                sb.append(",");
                            sb.append(value);
                            idx = value.indexOf('.');
                            if(idx > 0){
                                table = value.substring(0, idx);
                                if(tables.indexOf(table) < 0){
                                    if(tables.length() != 0)
                                        tables.append(",");
                                    tables.append(table);
                                }
                            }
                            //
                            if(key.equals("日期"))
                                dateColumn = value;
                        }
                        bfirst = false;
                    }else{
                        tableNode.remove(trNode);//only remain header
                    }
                }
                Date now = new Date();
                if(tables.length() > 0){
                    sb.append(" from ");
                    sb.append(tables);
                    JSONObject relationobj = jsonConfig.getJSONObject("relation");
                    if(!relationobj.isEmpty()){
                        sb.append(" where ");
                        Set<String> keys = relationobj.keySet();
                        String firstKey = keys.iterator().next();
                        for(String wkey : keys){
                            if(!wkey.equals(firstKey))
                                sb.append(" and ");
                            sb.append(wkey);
                            sb.append("=");
                            sb.append(relationobj.getString(wkey));
                        }
                    }
                    JSONObject taskobj = jsonConfig.getJSONObject("task");
                    if(dateColumn != null && taskobj != null){
                        if(sb.indexOf(" and ") < 0)
                            sb.append(" and ");
                        sb.append(dateColumn);
                        sb.append(">=\'");
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(now);
                        if(excuteNow){//立即执行
                            String strtime = taskobj.getString("time");
                            idx = strtime.indexOf(':');
                            calendar.set(Calendar.HOUR, Integer.parseInt(strtime.substring(0, idx)));
                            calendar.set(Calendar.MINUTE, Integer.parseInt(strtime.substring(idx + 1)));
                            strtime = taskobj.getString("start");
                            idx = strtime.indexOf('-');
                            calendar.set(Calendar.YEAR, Integer.parseInt(strtime.substring(0, idx)));
                            int idx1 = strtime.indexOf('-', idx + 1);
                            calendar.set(Calendar.MONTH, Integer.parseInt(strtime.substring(idx + 1, idx1)));
                            idx = strtime.indexOf('-', idx1 + 1);
                            calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(strtime.substring(idx + 1)));
                        }else{
                            calendar.add(Calendar.MINUTE, -taskobj.getInteger("interval"));
                        }
                        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        sb.append(format.format(calendar.getTime()));
                        sb.append("\'");
                    }
                }
                //operate database
                String url = getDBUrl(jsonConfig.getString("database"), jsonConfig.getString("dbip"),
                        jsonConfig.getString("dbport"), jsonConfig.getString("dbname"));
                String username = jsonConfig.getString("dbuser");
                String password = jsonConfig.getString("dbpass");
                SimpleDateFormat sdfstam = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                SimpleDateFormat sdfdate = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat sdftime = new SimpleDateFormat("HH:mm:ss");
                try{
                    Connection connection = DriverManager.getConnection(url, username, password);
                    Statement statement = connection.createStatement();
                    ResultSet resultSet = statement.executeQuery(sb.toString());
                    while (resultSet.next()) {
                        for (Element trNode : list1) {
                            Element newtr = trNode.createCopy();
                            tableNode.add(newtr);
                            List<Element> list2 = trNode.elements();
                            List<Element> list3 = newtr.elements();
                            ResultSetMetaData rsmd = resultSet.getMetaData();
                            idx = 0;
                            for (Element tdNode : list2) {
                                key = tdNode.getStringValue();
                                if(key == null || key.isEmpty())
                                    break;
                                value = bindobj.getString(key);
                                int wtype = rsmd.getColumnType(idx + 1);
                                if (wtype == Types.VARCHAR || wtype == Types.CHAR) {
                                    replaceTdNodeText(list3.get(idx), resultSet.getString(value));
                                } else if(wtype == Types.DATE){
                                    Date dateItem = resultSet.getDate(value);
                                    replaceTdNodeText(list3.get(idx), sdfdate.format(dateItem));
                                }else if(wtype == Types.TIME || wtype == Types.TIME_WITH_TIMEZONE){
                                    Date dateItem = resultSet.getTime(value);
                                    replaceTdNodeText(list3.get(idx), sdftime.format(dateItem));
                                }else if(wtype == Types.TIMESTAMP || wtype == Types.TIMESTAMP_WITH_TIMEZONE){
                                    Date dateItem = resultSet.getTimestamp(value);
                                    replaceTdNodeText(list3.get(idx), sdfstam.format(dateItem));
                                } else {
                                    replaceTdNodeText(list3.get(idx), String.valueOf(resultSet.getObject(value)));
                                }
                                idx++;
                            }
                            break;//only header
                        }
                        //System.out.println();
                    }
                    resultSet.close();
                    statement.close();
                    connection.close();
                    //export report
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                    String filePath = basePath + "/img/reports/" + sdf.format(now) + ".html";
                    File file = new File(filePath);
                    doc.getRootElement().asXML();
                    OutputFormat prettyPrint = OutputFormat.createPrettyPrint();
                    prettyPrint.setSuppressDeclaration(true);
                    XMLWriter writer = new XMLWriter(new FileOutputStream(file), prettyPrint);
                    writer.write(doc);
                    return filePath;
                }catch(SQLException e){
                    e.printStackTrace();
                }
            }
            templatein.close();
        }catch(FileNotFoundException e){
            e.printStackTrace();
        }catch(IOException e1){
            e1.printStackTrace();
        }catch(org.dom4j.DocumentException e2){
            e2.printStackTrace();
        }
        return null;
    }

    public static void replaceTdNodeText(Element newtd, String newValue){
        Iterator<Node> iterator = newtd.nodeIterator();
        while (iterator.hasNext()) {
            Node node = iterator.next();
            if (node instanceof Element) {
                Element eleNode = (Element) node;
                replaceTdNodeText(eleNode, newValue);
            }
            if (node instanceof Text) {
                Text text = (Text) node;
                text.setText(newValue);
                return;
            }
            //if (node instanceof CDATA) {
            //    CDATA dataNode = (CDATA) node;
            //    buffer.append(dataNode.getText());
            //}
            //if (node instanceof Comment) {
            //    Comment comNode = (Comment) node;
            //    buffer.append(comNode.getText());
            //}
        }
    }

    public static String getDBUrl(String dbType, String ip, String port, String dbName){
        if(dbType.equals("mysql"))
            return "jdbc:mysql://"+ip+":"+port+"/"+dbName+"?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        else if(dbType.equals("postgresql"))
            return "jdbc:postgresql://"+ip+":"+port+"/"+dbName;
        else if(dbType.equals("sqlserver"))
            return "jdbc:sqlserver://"+ip+":"+port+";databaseName="+dbName;
        return "";
    }

    public static String readFileByLines(String fileName) {
        FileInputStream file = null;
        BufferedReader reader = null;
        InputStreamReader inputFileReader = null;
        String content = "";
        String tempString = null;
        try {
            file = new FileInputStream(fileName);
            inputFileReader = new InputStreamReader(file, "utf-8");
            reader = new BufferedReader(inputFileReader);
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                content += tempString;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        return content;
    }

    public static String readFileByLines(File wfile) {
        FileInputStream file = null;
        BufferedReader reader = null;
        InputStreamReader inputFileReader = null;
        String content = "";
        String tempString = null;
        try {
            file = new FileInputStream(wfile);
            inputFileReader = new InputStreamReader(file, "utf-8");
            reader = new BufferedReader(inputFileReader);
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                content += tempString;
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        return content;
    }

    public static String checkHtmlTag(String content){
        StringBuffer html = new StringBuffer(content);
        int idx = 0, minIdx = 0;
        int[] idx1 = {0,0,0,0,0,0,0};
        while(true){
            minIdx = Integer.MAX_VALUE;
            idx1[0] = html.indexOf("<meta ", idx);
            idx1[1] = html.indexOf("<link ", idx);
            idx1[2] = html.indexOf("<img ", idx);
            idx1[3] = html.indexOf("<META ", idx);
            idx1[4] = html.indexOf("<LINK ", idx);
            idx1[5] = html.indexOf("<IMG ", idx);
            idx1[6] = html.indexOf("&nbsp;", idx);
            for(int i = 0;i < 7;i++){
                if(idx1[i] != -1 && idx1[i] < minIdx)
                    minIdx = idx1[i];
            }
            if(minIdx == Integer.MAX_VALUE)
                break;
            String item = html.substring(minIdx, minIdx + 6);
            if(item.equals("&nbsp;")){
                html.delete(minIdx, minIdx + 6);
            }else{
                idx = html.indexOf(">", minIdx + 5);
                if(idx <= 0)
                    break;
                if(html.charAt(idx - 1) != '/' || !html.substring(idx + 1, idx + 3).equals("</")){
                    html.insert(idx, '/');
                    idx++;
                }
                idx++;
            }
        }
        return html.toString();
    }
}
