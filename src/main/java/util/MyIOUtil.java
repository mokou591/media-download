package util;

import cfg.MyCfg;
import com.machinepublishers.jbrowserdriver.JBrowserDriver;
import com.machinepublishers.jbrowserdriver.Settings;
import com.machinepublishers.jbrowserdriver.Timezone;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import static java.util.concurrent.TimeUnit.SECONDS;

public class MyIOUtil {

    public static final String CACHE_PATH = MyCfg.workPath + "/cache";
    static{
        autoMkdir(CACHE_PATH);
    }

    public static void autoMkdir(String s) {
        File cacheDir = new File(s);
        if(!cacheDir.exists()){
            cacheDir.mkdirs();
            System.out.println("make dir: "+cacheDir.getAbsolutePath());
        }
    }

    private static JBrowserDriver driver;

    private static OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, SECONDS)
            .readTimeout(30, SECONDS)
            .writeTimeout(30, SECONDS)
            .retryOnConnectionFailure(true)
            .build();

    public static Integer waitMillis = 1000;

    public static void writeLines(Collection<?> lines, File file) {
        try {
            IOUtils.writeLines(lines, System.lineSeparator(), new FileOutputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> readLine(File file) {
        try {
            return IOUtils.readLines(new FileInputStream(file), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<String> readLine(String path) {
        return readLine(new File(path));
    }

    public static String readStr(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void lazyDownloadMedia(String pathNeedSlash, String filename, String mediaUrl) {
        lazyDownloadMedia(pathNeedSlash, filename, mediaUrl, client);
    }

    public static void lazyDownloadMedia(String pathNeedSlash, String filename, String mediaUrl, OkHttpClient client) {
        try {
            File dir = new File(pathNeedSlash);
            if (!dir.exists()) {
                dir.mkdir();
            }

            File file = new File(pathNeedSlash + filename);
            if (file.exists()) return;

            //req http
            Call call = client.newCall(new Request.Builder()
                    .url(mediaUrl)
                    .build());
            Response resp = call.execute();
            if (resp.code() != 200) {
                resp.close();
                return;
            }
            byte[] bytes = resp.body().bytes();
            waitAFew();
            IOUtils.write(bytes, new FileOutputStream(file));
            resp.close();
        } catch (Exception e) {
            System.err.println(filename + ", " + mediaUrl);
            e.printStackTrace();
        }
    }

    public static void lazyDownloadMedia(String path, String url) {
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        String filename = getMediaFileName(url);
        lazyDownloadMedia(path, filename, url, client);
    }

    public static String getMediaFileName(String url) {
        String filename = StringUtils.substringAfterLast(url, "/");
        if (filename.length() <= 10) {
            filename = StringUtils.substring(url, url.length() - 20, url.length());
            filename = filename.replaceAll("/", "_");
        }
        filename = URLDecoder.decode(filename);
        return filename;
    }

    public static String lazyGetByUrl(String url) {
        String host = StringUtils.substringBetween(url, "://", "/").replace(":", "");
        String endName = StringUtils.substringAfterLast(url, "/");
        if (endName.length() > 100) {
            endName = endName.substring(0, 20) + "--" + endName.substring(endName.length() - 20);
        }

        //use cache
        String hostDirStr = CACHE_PATH + "/" + host;
        File hostDirFile = new File(hostDirStr);
        if (!hostDirFile.exists()) {
            hostDirFile.mkdir();
        }
        File tgtFile = new File(hostDirStr + "/" + endName);
        if (tgtFile.exists()) {
            return readStr(tgtFile);
        }

        //http
        String content = getByUrl(url);
        writeFile(tgtFile, content);
        return content;
    }

    public static String lazyGetByUrlHeadless(String url) {
        String host = StringUtils.substringBetween(url, "://", "/").replace(":", "");
        String endName = StringUtils.substringAfterLast(url, "/");
        if (endName.length() > 100) {
            endName = endName.substring(0, 20) + "--" + endName.substring(endName.length() - 20);
        }

        //use cache
        String hostDirStr = CACHE_PATH + "/" + host;
        File hostDirFile = new File(hostDirStr);
        if (!hostDirFile.exists()) {
            hostDirFile.mkdir();
        }
        File tgtFile = new File(hostDirStr + "/" + endName);
        if (tgtFile.exists()) {
            return readStr(tgtFile);
        }

        //http
        String content = getByUrlUseHeadless(url);
        writeFile(tgtFile, content);
        return content;
    }

    public static String getByUrl(String url, OkHttpClient client, Headers headers) {
        try {
            //req http
            Request.Builder bdr = new Request.Builder()
                    .url(url);
            if (headers != null) {
                bdr.headers(headers);
            }
            bdr.header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_14_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36");
            Call call = client.newCall(bdr.build());
            Response resp = call.execute();
            waitAFew();
            if (resp.code() != 200) {
                resp.close();
                return url;
            }
            return resp.body().string();
        } catch (Exception e) {
            System.err.println("UrlError:" + url);
            e.printStackTrace();
        }
        return null;
    }

    public static void writeFile(String fileName, String content) {
        writeFile(MyCfg.workPath, fileName, content);
    }

    public static void writeFile(String path, String fileName, String content) {
        File file = new File(path + fileName);
        if (file.exists()) {
            file.delete();
        }
        writeFile(file, content);
    }

    public static void writeFile(File file, String content) {
        try {
            file.createNewFile();
            IOUtils.write(content, new FileOutputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getByUrl(String url) {
        return getByUrl(url, client, null);
    }

    public static File getUrlAndLazySave(String url, String path, String fileName) {
        File file = new File(path + fileName);
        if (file.exists()) {
            return file;
        }
        //get and write
        try {
            String content = getByUrl(url);
            file.createNewFile();
            IOUtils.write(content, new FileOutputStream(file));
            return file;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String readStr(File file) {
        return readStr(file.getAbsolutePath());
    }

    public static void setWaitMillis(Integer waitMillis) {
        MyIOUtil.waitMillis = waitMillis;
    }

    public static void waitAFew() {
        try {
            Thread.sleep(waitMillis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String getFileNameByUrl(String url) {
        return URLEncoder.encode(StringUtils.substringAfterLast(url, "/"));
    }

    public static void writeCsv(String path, String fileName, List<String> headers, List<List<String>> rows) {
        StringBuilder bdr = new StringBuilder();
        bdr.append(String.join(",", headers));

        for (List<String> row : rows) {
            bdr.append(System.lineSeparator());
            bdr.append(String.join(",", row));
        }

        //write file
        writeFile(path, fileName, bdr.toString());
    }

    public static String getByUrlUseHeadless(String url) {
        checkInitHeadless();

        driver.get(url);
        String pageSource = driver.getPageSource();
        return pageSource;
    }

    private static void checkInitHeadless() {
        if (driver == null) {
            driver = new JBrowserDriver(Settings.builder()
                    .timezone(Timezone.ASIA_SHANGHAI)
                    .logTrace(false)
                    .logWire(false)
                    .loggerLevel(Level.OFF)
                    .hostnameVerification(false)
                    .ajaxResourceTimeout(5000)
                    .connectTimeout(5000)
                    .quickRender(true)
                    .headless(true)
                    .build());
        }
    }
}
