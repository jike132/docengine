package com.nsfocus.reportengine.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.nsfocus.reportengine.GenerateReportTask;
import fr.opensagres.poi.xwpf.converter.core.ImageManager;
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLConverter;
import fr.opensagres.poi.xwpf.converter.xhtml.XHTMLOptions;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.ooxml.extractor.POIXMLTextExtractor;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;


@RestController
public class POIWrapper {
    //注入application.properties里面的配置web.upload-path
    @Value("${web.upload-path}")
    private String uploadPath;
    /**
     * 设置页边距
     *
     * @param document doc对象
     * @param left     左边距
     * @param right    右边距
     * @param top      上边距
     * @param bottom   下边距
     */
    public static void setPageMargin(XWPFDocument document, long left, long right, long top, long bottom) {
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        CTPageMar pageMar = sectPr.addNewPgMar();
        pageMar.setLeft(BigInteger.valueOf(left));
        pageMar.setRight(BigInteger.valueOf(right));
        pageMar.setTop(BigInteger.valueOf(top));
        pageMar.setBottom(BigInteger.valueOf(bottom));
    }

    public static void setPageSize(XWPFDocument document, long width, long height) {
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        CTPageSz pgsz = sectPr.isSetPgSz() ? sectPr.getPgSz() : sectPr.addNewPgSz();
        pgsz.setW(BigInteger.valueOf(width));
        pgsz.setH(BigInteger.valueOf(height));
    }

    /**
     * 保存文件
     *
     * @param document doc对象
     * @param savePath 保存路径
     * @param fileName 文件名称
     */
    public static void saveDoc(XWPFDocument document, String savePath, String fileName) {
        File file = new File(savePath);
        if (!file.exists()) {
            // 判断生成目录是否存在，不存在时创建目录。
            file.mkdirs();
        }
        // 保存
        fileName += ".docx";
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(new File(savePath + File.separator + fileName));
            document.write(out);
            out.flush();
            out.close();
            document.close();
        }catch(IOException e) {
            System.out.print("Open failed %s" + e.toString());
        }
    }

    @RequestMapping(value = "/downloadreport",method = RequestMethod.GET)
    public void downloadReport(HttpServletRequest request, HttpServletResponse response){
        do{
            //from name to id
            String report = request.getParameter("report");
            if(report == null)
                break;
            String rootPath = uploadPath + "/img/reports/" + report + ".html";
            String htmlContent = readFileByLines(rootPath);
            if(htmlContent.isEmpty())
                break;
            String strReplaced = ImgPathRelativeToAbsolute(htmlContent, uploadPath);
            htmlToWord(strReplaced, report, response);
            return;
        }while(false);
        try {
            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write("{\"code\":404,\"msg\":\"template not exist\"}");
            writer.flush();
            writer.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    @ResponseBody
    @RequestMapping(value = "/loadreports",method = RequestMethod.GET)
    public JSONArray loadReports() {
        String reports = uploadPath + "/img/reports";
        File dir = new File(reports);
        File[] files = dir.listFiles();
        JSONArray result = new JSONArray();
        for(File wfile: files){
            String fileName = wfile.getName();
            fileName = fileName.substring(0, fileName.lastIndexOf("."));
            result.add(fileName);
        }
        return result;
    }

    @RequestMapping(value = "/executedatasource",method = RequestMethod.POST)
    public void executeDataSource(HttpServletRequest request, HttpServletResponse response){
        String wid = saveDataSource(request, response);
        if(wid != null)
            GenerateReportTask.ExportReport(wid, uploadPath, true);
    }

    @RequestMapping(value = "/savedatasource",method = RequestMethod.POST)
    public String saveDataSource(HttpServletRequest request, HttpServletResponse response){
        String templateName = request.getParameter("template");
        String datasource = request.getParameter("html");
        String result = "{\"code\":402,\"msg\":\"parameter error\"}";
        String wid = null;
        do {
            if(templateName == null || datasource == null)
                break;
            wid = nameToId(templateName);
            if(wid == null)
                break;
            String htmlPath = uploadPath + "/img/datasource/" + wid + ".json";
            File htmlFile = new File(htmlPath);
            try {
                Writer write = new OutputStreamWriter(new FileOutputStream(htmlFile), StandardCharsets.UTF_8);
                write.write(datasource);
                write.flush();
                write.close();
                result = "{\"code\":200,\"msg\":\"save success\"}";
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }while(false);
        try {
            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write(result);
            writer.flush();
            writer.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return wid;
    }

    @ResponseBody
    @RequestMapping(value = "/loaddatasource",method = RequestMethod.GET)
    public JSONObject loadDataSource(@RequestParam("name") String templateName) {
        String wid = nameToId(templateName);
        if(wid == null)
            return JSON.parseObject("{\"code\":404,\"msg\":\"template not exist\"}");
        String mappath = uploadPath + "/img/datasource/" + wid + ".json";
        String configContent = readFileByLines(mappath);
        return JSON.parseObject(configContent);
    }

    @ResponseBody
    @RequestMapping(value = "/loadtemplate",method = RequestMethod.GET)
    public JSONArray loadTemplate() {
        String mappath = uploadPath + "/img/nametoid.json";
        String configContent = readFileByLines(mappath);
        JSONObject jsonConfig = JSON.parseObject(configContent);
        JSONArray result = new JSONArray();
        Set<String> keys = jsonConfig.keySet();
        for (String str : keys) {
            result.add(str);
        }
        return result;
    }

    @ResponseBody
    @RequestMapping(value = "/savedoc",method = RequestMethod.POST)
    public JSONObject saveDoc(@RequestParam("name") String templateName, @RequestParam("html") String html) {
        String md5 = nameToId(templateName);
        if(md5 == null)
            return JSON.parseObject("{\"code\":404,\"msg\":\"template not exist\"}");
        String htmlPath = uploadPath + "/img/template/" + md5 + ".html";
        File htmlFile = new File(htmlPath);
        try {
            Writer write = new OutputStreamWriter(new FileOutputStream(htmlFile), StandardCharsets.UTF_8);
            write.write(html);
            write.flush();
            write.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return JSON.parseObject("{\"code\":500,\"msg\":\"server internal error\"}");
        } catch(IOException e1){
            e1.printStackTrace();
            return JSON.parseObject("{\"code\":500,\"msg\":\"server internal error\"}");
        }
        return JSON.parseObject("{\"code\":200,\"msg\":\"save success\"}");
    }

    @ResponseBody
    @RequestMapping(value = "/loaddoc",method = RequestMethod.GET)
    public JSONObject loadDoc(@RequestParam("template") String templateName) {
        String md5 = nameToId(templateName);
        if(md5 == null)
            return JSON.parseObject("{\"code\":404,\"msg\":\"template not exist\"}");
        String mappath = uploadPath + "/img/template/" + md5 + ".html";
        return uploadDocOutput(mappath, templateName);
    }

    @ResponseBody
    @RequestMapping(value = "/uploaddoc",method = RequestMethod.POST)
    public JSONObject uploadDoc(@RequestParam("name") String templateName, @RequestPart("file") MultipartFile headerImg) {
        try {
            //check template
            String md5 = updateNameToIdMap(templateName, headerImg.getInputStream());
            if(md5.indexOf("\"code\"") >= 0)
                return JSON.parseObject(md5);
            String path = headerImg.getOriginalFilename();
            String mappath = uploadPath + "/img/template/" + md5 + ".html";
            if (path != null && path.endsWith(".doc")) {
                WordExtractor extractor = new WordExtractor(headerImg.getInputStream());
                String buffer = extractor.getText();
                extractor.close();
                try {
                    path = Word2003ToHtml(headerImg.getInputStream(), mappath, md5);
                    return uploadDocOutput(path, templateName);
                } catch (TransformerException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (path != null && path.endsWith(".docx")) {
                XWPFDocument document = new XWPFDocument(headerImg.getInputStream());
                POIXMLTextExtractor extractor = new XWPFWordExtractor(document);
                String buffer = extractor.getText();
                extractor.close();
                try {
                    path = Word2007ToHtml(headerImg.getInputStream(), mappath, md5);
                    return uploadDocOutput(path, templateName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                return JSON.parseObject("{\"code\":409,\"msg\":\"not a word file\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return JSON.parseObject("{\"code\":500,\"msg\":\"server internal error\"}");
    }

    @RequestMapping(value = "/downloaddoc",method = RequestMethod.GET)
    public void downloadDoc(HttpServletRequest request, HttpServletResponse response){
        do{
            //from name to id
            String templateName = request.getParameter("template");
            if(templateName == null)
                break;
            String wid = nameToId(templateName);
            if(wid == null){
                break;
            }else{
                String rootPath = uploadPath + "/img/template/" + wid + ".html";
                String htmlContent = readFileByLines(rootPath);
                if(htmlContent.isEmpty())
                    break;
                String strReplaced = ImgPathRelativeToAbsolute(htmlContent, uploadPath);
                if(strReplaced == null)
                    break;
                htmlToWord(strReplaced, templateName, response);
                return;
            }
        }while(false);
        try {
            response.setContentType("application/json");
            PrintWriter writer = response.getWriter();
            writer.write("{\"code\":404,\"msg\":\"template not exist\"}");
            writer.flush();
            writer.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

        /*XWPFDocument document = new XWPFDocument();
        setPageSize(document, 16840, 11907);
        setPageMargin(document, 567, 567, 567, 567);
        //
        XWPFParagraph titleParagraph = document.createParagraph();
        //设置段落居中
        titleParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleParagraphRun = titleParagraph.createRun();
        titleParagraphRun.setText("Java PoI");
        titleParagraphRun.setColor("000000");
        titleParagraphRun.setFontSize(20);
        //段落
        XWPFParagraph firstParagraph = document.createParagraph();
        XWPFRun run = firstParagraph.createRun();
        run.setText("Java POI 生成word文件。");
        run.setColor("696969");
        run.setFontSize(16);
        //设置段落背景颜色
        CTShd cTShd = run.getCTR().addNewRPr().addNewShd();
        cTShd.setVal(STShd.CLEAR);
        cTShd.setFill("97FFFF");
        //换行
        XWPFParagraph paragraph1 = document.createParagraph();
        XWPFRun paragraphRun1 = paragraph1.createRun();
        paragraphRun1.setText("\r");
        //基本信息表格
        XWPFTable infoTable = document.createTable();
        //去表格边框
        infoTable.getCTTbl().getTblPr().unsetTblBorders();
        //列宽自动分割
        CTTblWidth infoTableWidth = infoTable.getCTTbl().addNewTblPr().addNewTblW();
        infoTableWidth.setType(STTblWidth.DXA);
        infoTableWidth.setW(BigInteger.valueOf(9072));
        //表格第一行
        XWPFTableRow infoTableRowOne = infoTable.getRow(0);
        infoTableRowOne.getCell(0).setText("职位");
        infoTableRowOne.addNewTableCell().setText(": Java 开发工程师");
        //表格第二行
        XWPFTableRow infoTableRowTwo = infoTable.createRow();
        infoTableRowTwo.getCell(0).setText("姓名");
        infoTableRowTwo.getCell(1).setText(": seawater");
        //表格第三行
        XWPFTableRow infoTableRowThree = infoTable.createRow();
        infoTableRowThree.getCell(0).setText("生日");
        infoTableRowThree.getCell(1).setText(": xxx-xx-xx");
        //表格第四行
        XWPFTableRow infoTableRowFour = infoTable.createRow();
        infoTableRowFour.getCell(0).setText("性别");
        infoTableRowFour.getCell(1).setText(": 男");
        //表格第五行
        XWPFTableRow infoTableRowFive = infoTable.createRow();
        infoTableRowFive.getCell(0).setText("现居地");
        infoTableRowFive.getCell(1).setText(": xx");
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        XWPFHeaderFooterPolicy policy = new XWPFHeaderFooterPolicy(document, sectPr);
        //添加页眉
        CTP ctpHeader = CTP.Factory.newInstance();
        CTR ctrHeader = ctpHeader.addNewR();
        CTText ctHeader = ctrHeader.addNewT();
        String headerText = "ctpHeader";
        ctHeader.setStringValue(headerText);
        XWPFParagraph headerParagraph = new XWPFParagraph(ctpHeader, document);
        //设置为右对齐
        headerParagraph.setAlignment(ParagraphAlignment.RIGHT);
        XWPFParagraph[] parsHeader = new XWPFParagraph[1];
        parsHeader[0] = headerParagraph;
        policy.createHeader(XWPFHeaderFooterPolicy.DEFAULT, parsHeader);
        //添加页脚
        CTP ctpFooter = CTP.Factory.newInstance();
        CTR ctrFooter = ctpFooter.addNewR();
        CTText ctFooter = ctrFooter.addNewT();
        String footerText = "ctpFooter";
        ctFooter.setStringValue(footerText);
        XWPFParagraph footerParagraph = new XWPFParagraph(ctpFooter, document);
        headerParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFParagraph[] parsFooter = new XWPFParagraph[1];
        parsFooter[0] = footerParagraph;
        policy.createFooter(XWPFHeaderFooterPolicy.DEFAULT, parsFooter);*/
        //
        //XWPFParagraph paragraph = document.createParagraph();
        //XWPFRun xwpfRun = paragraph.createRun();
        //xwpfRun.setText("123123fsdfsfsf");
        //xwpfRun.setText("将字体设置为宋体！");
        //xwpfRun.setFontFamily("宋体");
        //xwpfRun.setText("设置为12磅！");
        //xwpfRun.setFontSize(12);
        //xwpfRun.setText("将字体设置为五号字！");
        //CTRPr ctrPr = xwpfRun.getCTR().addNewRPr();
        //// 假如我想将字号设置为五号字, 五号字对应的磅数是10.5, 传入的值是磅数*2
        //ctrPr.addNewSzCs().setVal(BigInteger.valueOf(21));
        //ctrPr.addNewSz().setVal(BigInteger.valueOf(21));
        //xwpfRun.setText("bold bold");
        //xwpfRun.setBold(true);
        //xwpfRun.setText("斜体");
        //xwpfRun.setItalic(true);
        //xwpfRun.setText("删除线");
        //xwpfRun.setStrikeThrough(true);
        //xwpfRun.setText("下划线");
        //xwpfRun.setUnderline(UnderlinePatterns.SINGLE);
        //xwpfRun.setText("红色");
        //xwpfRun.setColor("FF0000");
        //xwpfRun.setText("红色");
        //ctrPr.addNewShd().setFill("FF0000");
        //
        //String savePath = "D:\\poi";
        //String fileName = "PoiWord";
        //saveDoc(document, savePath, fileName);
    }

    public static String Word2003ToHtml(InputStream in, String htmlPath, String md5) throws IOException, TransformerException, ParserConfigurationException {
        //final String file = "D:/poi/test.doc";
        //InputStream in = new FileInputStream(new File(file));
        HWPFDocument wordDocument = new HWPFDocument(in);
        WordToHtmlConverter wordToHtmlConverter = new WordToHtmlConverter(DocumentBuilderFactory.newInstance().newDocumentBuilder() .newDocument());
        wordToHtmlConverter.setPicturesManager((content, pictureType, suggestedName, widthInches, heightInches) -> {
            //内嵌base64
            //BufferedImage bufferedImage = ImgUtil.toImage(content);
            //String base64Img = ImgUtil.toBase64(bufferedImage, pictureType.getExtension());
            //  带图片的word，则将图片转为base64编码，保存在一个页面中
            //StringBuilder sb = (new StringBuilder(base64Img.length() + "data:;base64,".length()).append("data:;base64,").append(base64Img));
            //return sb.toString();
            //写文件
            int idx = htmlPath.lastIndexOf('/');
            if(idx <= 0)
                return "";
            idx = htmlPath.lastIndexOf('/', idx - 1);
            if(idx <= 0)
                return "";
            String filePath = htmlPath.substring(0, idx + 1) + "file/";
            File folder = new File(filePath + md5);
            if (!folder.exists()) {
                folder.mkdirs();
            }
            String fileName = md5 + "/" + String.valueOf(new Date().getTime()) + suggestedName;
            filePath += fileName;
            try{
                FileOutputStream out = new FileOutputStream(filePath);
                out.write(content);
                out.close();
            }catch (IOException e){
                e.printStackTrace();
                return "";
            }
            return "/img/file/" + fileName;
        });

        // 解析word文档
        wordToHtmlConverter.processDocument(wordDocument);
        // 生成html文件地址
        File htmlFile = new File(htmlPath);
        OutputStream outStream = new FileOutputStream(htmlFile);
        DOMSource domSource = new DOMSource(wordToHtmlConverter.getDocument());
        StreamResult streamResult = new StreamResult(outStream);
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer serializer = factory.newTransformer();
        serializer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
        serializer.setOutputProperty(OutputKeys.INDENT, "yes");
        serializer.setOutputProperty(OutputKeys.METHOD, "html");
        serializer.transform(domSource, streamResult);
        outStream.close();
        return htmlFile.getAbsolutePath();
    }

    public static String Word2007ToHtml(InputStream in, String htmlPath, String md5) throws IOException {
        ZipSecureFile.setMinInflateRatio(-1.0d);
        //File wordFile = new File("D:/poi/test.docx");
        //InputStream in = new FileInputStream(wordFile);
        XWPFDocument document = new XWPFDocument(in);
        // 带图片的word，则将图片转为base64编码，保存在一个页面中
        //XHTMLOptions options = XHTMLOptions.create().indent(4).setImageManager(new Base64EmbedImgManager());
        //存文件
        int idx = htmlPath.lastIndexOf('/');
        if(idx <= 0)
            return "";
        idx = htmlPath.lastIndexOf('/', idx - 1);
        if(idx <= 0)
            return "";
        idx = htmlPath.lastIndexOf('/', idx - 1);
        if(idx <= 0)
            return "";
        String filePath = htmlPath.substring(0, idx + 1);//static folder
        File folder = new File(filePath);
        XHTMLOptions options = XHTMLOptions.create();
        options.setImageManager(new ImageManager(folder, "/img/file/" + md5));
        options.setIgnoreStylesIfUnused(false);
        options.setFragment(true);
        // 3) 将 XWPFDocument转换成XHTML
        File htmlFile = new File(htmlPath);
        OutputStream out = new FileOutputStream(htmlFile);
        XHTMLConverter.getInstance().convert(document, out, options);
        return htmlFile.getAbsolutePath();
    }

    public static void htmlToWord(String htmlContent, String templateName, HttpServletResponse response){
        StringBuffer sbf = new StringBuffer();
        // 这里拼接一下html标签,便于word文档能够识别
        sbf.append("<html " +
                "xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:w=\"urn:schemas-microsoft-com:office:word\" xmlns:m=\"http://schemas.microsoft.com/office/2004/12/omml\" xmlns=\"http://www.w3.org/TR/REC-html40\"" + //将版式从web版式改成页面试图
                ">");
        sbf.append("<head>" +
                "<!--[if gte mso 9]><xml><w:WordDocument><w:View>Print</w:View><w:TrackMoves>false</w:TrackMoves><w:TrackFormatting/><w:ValidateAgainstSchemas/><w:SaveIfXMLInvalid>false</w:SaveIfXMLInvalid><w:IgnoreMixedContent>false</w:IgnoreMixedContent><w:AlwaysShowPlaceholderText>false</w:AlwaysShowPlaceholderText><w:DoNotPromoteQF/><w:LidThemeOther>EN-US</w:LidThemeOther><w:LidThemeAsian>ZH-CN</w:LidThemeAsian><w:LidThemeComplexScript>X-NONE</w:LidThemeComplexScript><w:Compatibility><w:BreakWrappedTables/><w:SnapToGridInCell/><w:WrapTextWithPunct/><w:UseAsianBreakRules/><w:DontGrowAutofit/><w:SplitPgBreakAndParaMark/><w:DontVertAlignCellWithSp/><w:DontBreakConstrainedForcedTables/><w:DontVertAlignInTxbx/><w:Word11KerningPairs/><w:CachedColBalance/><w:UseFELayout/></w:Compatibility><w:BrowserLevel>MicrosoftInternetExplorer4</w:BrowserLevel><m:mathPr><m:mathFont m:val=\"Cambria Math\"/><m:brkBin m:val=\"before\"/><m:brkBinSub m:val=\"--\"/><m:smallFrac m:val=\"off\"/><m:dispDef/><m:lMargin m:val=\"0\"/> <m:rMargin m:val=\"0\"/><m:defJc m:val=\"centerGroup\"/><m:wrapIndent m:val=\"1440\"/><m:intLim m:val=\"subSup\"/><m:naryLim m:val=\"undOvr\"/></m:mathPr></w:WordDocument></xml><![endif]-->" +
                "</head>");
        sbf.append("<body>");
        // 富文本内容
        sbf.append(htmlContent);
        sbf.append("</body></html>");

        // 必须要设置编码,避免中文就会乱码
        try{
            byte[] b = sbf.toString().getBytes("GBK");
            // 将字节数组包装到流中
            ByteArrayInputStream bais = new ByteArrayInputStream(b);
            //POIFSFileSystem poifs = new POIFSFileSystem();
            //DirectoryEntry directory = poifs.getRoot();
            //DirectoryEntry worddir = directory.createDirectory("word");
            //worddir.createDocument("document.xml", bais);
            //OutputStream ostream = new FileOutputStream(new File("D:\\poi\\PoiWord1.docx"));
            //poifs.writeFilesystem(ostream);
            //bais.close();
            //ostream.close();
            //创建 POIFSFileSystem 对象

            POIFSFileSystem poifs = new POIFSFileSystem();
            //获取DirectoryEntry
            DirectoryEntry directory = poifs.getRoot();
            //创建输出流
            //OutputStream out = new FileOutputStream("D:\\poi\\html_to_word.doc");
            response.setContentType("application/msword");//导出word格式
            response.addHeader("Content-Disposition", "p_w_upload;filename=" + new String((templateName + ".doc").getBytes(),  "iso-8859-1"));
            OutputStream out = response.getOutputStream();
            //创建文档,1.格式,2.HTML文件输入流
            directory.createDocument("WordDocument", bais);
            //写入
            poifs.writeFilesystem(out);
            out.close();
            System.out.println("htmlToWord success");
        }catch(UnsupportedEncodingException e){
            e.printStackTrace();
        }catch(IOException e1){
            e1.printStackTrace();
        }
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

    public String nameToId(String templateName){
        String configContent = readFileByLines(uploadPath + "/img/nametoid.json");
        try{
            JSONObject jsonConfig = JSON.parseObject(configContent);
            return jsonConfig.getString(templateName);
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        return null;
    }

    public String updateNameToIdMap(String templateName, InputStream inputStream){
        //check template
        String mappath = uploadPath + "/img/nametoid.json";
        String configContent = readFileByLines(mappath);
        JSONObject jsonConfig = JSON.parseObject(configContent);
        String wid = jsonConfig.getString(templateName);
        if(wid != null){
            return "{\"code\":406,\"msg\":\"template already exist\"}";
        }
        String md5 = String.valueOf(new Date().getTime()) + String.valueOf(100 + new Random().nextInt() % 900);
        jsonConfig.put(templateName, md5);
        createJsonFile(jsonConfig, mappath);
        //try {
        //    //md5 = getInputStreamMd5(inputStream);
        //    jsonConfig.put(templateName, md5);
        //    createJsonFile(jsonConfig, mappath);
        //}catch (IOException e){
        //    e.printStackTrace();
        //    return "{\"code\":407,\"msg\":\"file content incomplete\"}";
        //}
        return md5;
    }

    public static String getInputStreamMd5(InputStream inputStream) throws IOException {
        MessageDigest digest = DigestUtils.getMd5Digest();
        byte[] buffer = new byte[2048];
        int read = inputStream.read(buffer);
        while (read > -1) {
            // 计算MD5,顺便写到文件
            digest.update(buffer, 0, read);
            read = inputStream.read(buffer);
        }
        return Hex.encodeHexString(digest.digest());
    }

    public static boolean createJsonFile(Object jsonData, String filePath) {
        String content = JSON.toJSONString(jsonData, SerializerFeature.PrettyFormat, SerializerFeature.WriteMapNullValue,
                SerializerFeature.WriteDateUseDateFormat);
        try {
            File file = new File(filePath);
            // 创建上级目录
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            // 如果文件存在，则删除文件
            if (file.exists()) {
                file.delete();
            }
            // 创建文件
            file.createNewFile();
            // 写入文件
            Writer write = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
            write.write(content);
            write.flush();
            write.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static JSONObject uploadDocOutput(String fileName, String templateName){
        String content = readFileByLines(fileName);
        Base64.Encoder encoder = Base64.getEncoder();
        String result = null;
        try {
            result = encoder.encodeToString(content.getBytes("utf-8"));
        }catch(UnsupportedEncodingException e){
            e.printStackTrace();
        }
        JSONObject root = new JSONObject();
        root.put("templateName", templateName);
        root.put("html", result);
        return root;
    }

    public static String reverseHtmlTag(String content){
        StringBuffer html = new StringBuffer(content);
        int idx = 0, minIdx = 0;
        int[] idx1 = {0,0,0,0};
        while(true){
            minIdx = Integer.MAX_VALUE;
            idx1[0] = html.indexOf("<meta ", idx);
            idx1[1] = html.indexOf("<link ", idx);
            idx1[2] = html.indexOf("<META ", idx);
            idx1[3] = html.indexOf("<LINK ", idx);
            for(int i = 0;i < 4;i++){
                if(idx1[i] != -1 && idx1[i] < minIdx)
                    minIdx = idx1[i];
            }
            if(minIdx == Integer.MAX_VALUE)
                break;
            idx = html.indexOf("/>", minIdx + 6);
            if(idx <= 0)
                break;
            html.deleteCharAt(idx);
            idx++;
        }
        return html.toString();
    }

    public static String ImgPathRelativeToAbsolute(String htmlContent, String uploadPath){
        //image relative path to absolute path
        StringBuffer newContent = new StringBuffer();
        int idx = htmlContent.indexOf("<img src=\""), idx1 = 0, lastIdx = 0;
        if(idx < 0){
            newContent.append(htmlContent);
        }else{
            boolean berror = false;
            while(idx >= 0){
                idx1 = htmlContent.indexOf('\"', idx + 10);
                if(idx1 < 0){
                    berror = true;
                    break;
                }
                newContent.append(htmlContent.substring(lastIdx, idx + 10));
                newContent.append(uploadPath);
                lastIdx = idx + 10;
                idx = htmlContent.indexOf("<img src=\"", idx1 + 1);
            }
            if(berror)
                return null;
            newContent.append(htmlContent.substring(lastIdx));
        }
        return newContent.toString();
    }
}
