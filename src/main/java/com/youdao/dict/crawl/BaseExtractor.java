package com.youdao.dict.crawl;

import cn.edu.hfut.dmic.webcollector.model.Page;
import cn.edu.hfut.dmic.webcollector.util.JsoupUtils;
import com.youdao.dict.bean.ParserPage;
import com.youdao.dict.score.LeveDis;
import com.youdao.dict.souplang.Context;
import com.youdao.dict.souplang.SoupLang;
import com.youdao.dict.util.OImageConfig;
import com.youdao.dict.util.OImageUploader;
import lombok.extern.apachecommons.CommonsLog;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by liuhl on 15-8-17.
 */
@CommonsLog
public class BaseExtractor {
    public static long MINSIZE = 512;
    public ParserPage p = new ParserPage();
    String url;
    Document doc;
    Page page;
    Element content;

    public ParserPage getParserPage() {
        return p;
    }

    public BaseExtractor(Page page) {
        this.page = page;
        url = page.getUrl();
        doc = page.getDoc();
        JsoupUtils.makeAbs(doc, url);
        p.setHost(getHost(url));
        p.setUrl(url);
    }

    public boolean extractor() {
        return init() && extractorTitle() && extractorType() && extractorTime() && extractorAndUploadImg() && extractorContent() && extractorTags();
    }

    public boolean init() {
        return false;
    }

    public boolean extractorTitle() {
        return false;
    }

    public boolean extractorType() {
        return false;
    }

    public boolean extractorTime() {
        return false;
    }

    public boolean extractorAndUploadImg() {
        log.info("*****extractorAndUploadImg*****");
        if (content == null || p == null) {
            return false;
        }
        try {
            Elements imgs = content.select("img");
            String mainImage = null;
            for (Element img : imgs) {
                String imageUrl = img.attr("src");
                img.removeAttr("width");
                img.removeAttr("WIDTH");
                img.removeAttr("height");
                img.removeAttr("HEIGHT");
                img.attr("style", "width:100%;");
                OImageUploader uploader = new OImageUploader();
                uploader.setProxy("proxy.corp.youdao.com", "3456");
                long id = uploader.deal(imageUrl);
                URL newUrl = new OImageConfig().getImageSrc(id, "dict-consult");
                img.attr("src", newUrl.toString());
                if (mainImage == null) {
                    mainImage = newUrl.toString();
                }
            }
            if (mainImage != null) {
                p.setMainimage(mainImage);
                log.info("*****extractorAndUploadImg  success*****");
                return true;
            }
            log.info("*****extractorAndUploadImg  failed*****");
            return false;
        } catch (Exception e) {
            log.info("*****extractorAndUploadImg  failed*****");
            return false;
        }
    }

    public boolean extractorContent() {
        log.info("*****extractorContent*****");
        if (content == null || p == null || content.text().length() < MINSIZE) {
            return false;
        }
        String contentHtml = content.html();
        contentHtml = contentHtml.replaceAll("<(?!img|br|p|/p).*?>", "");//去除所有标签，只剩img,br,p
        contentHtml = contentHtml.replaceAll("\\\\s*|\\t|\\r|\\n", "");//去除换行符制表符/r,/n,/t
        p.setContent(contentHtml);
        log.info("*****extractorContent  success*****");
        return true;
    }

    public boolean extractorTags() {
        log.info("*****extractorTags*****");
        if (content == null) {
            log.info("*****extractorTags  failed*****");
            return false;
        }
        try {
            String contentStr = content.text();
            LeveDis leveDis = LeveDis.getInstance(LeveDis.p);
            String tags = leveDis.tag(contentStr, 5);
            p.setLabel(tags);
            int level = leveDis.compFileLevel(leveDis.compLevel(contentStr));
            p.setLevel(String.valueOf(level));
            log.info("*****extractorTags  success*****");
            return true;
        } catch (Exception e) {
            log.info("*****extractorTags  failed*****");
            return false;
        }
    }

    public static String getHost(String url) {
        if (url == null || url.trim().equals("")) {
            return "";
        }
        String host = "";
        Pattern p = Pattern.compile("(?<=//|)((\\w)+\\.)+\\w+");
        Matcher matcher = p.matcher(url);
        if (matcher.find()) {
            host = matcher.group();
        }
        return host;
    }

}
