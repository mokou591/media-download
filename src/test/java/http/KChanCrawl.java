package http;

import cfg.MyCfg;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import util.MyIOUtil;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class KChanCrawl {

    private static String path = MyCfg.workPath + "/konachan";

    static {
        MyIOUtil.autoMkdir(path);
        System.out.println("save path:" + path);
    }

    @Test
    public void test1() {
        download("", 1, 1);
    }

    void download(String keyword, int startPage, int endPage) {
        for (int i = startPage; i <= endPage; i++) {
            String pageUrl = "https://konachan.net/post?tags=" + keyword + "&page=" + i;
            System.out.println("page" + i + ": " + pageUrl);

            String html = MyIOUtil.lazyGetByUrl(pageUrl);
            List<String> imgUrlList = Arrays.stream(StringUtils.substringsBetween(html, "<a class=\"directlink largeimg\" href=\"", "\"><span class=\"directlink-info\">"))
                    .distinct().collect(Collectors.toList());
            System.out.println("pic count: " + imgUrlList.size());
            imgUrlList.forEach(x -> MyIOUtil.lazyDownloadMedia(path, x));
        }
    }
}
