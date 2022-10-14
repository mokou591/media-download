package cfg;

import util.MyIOUtil;

public class MyCfg {
    /**
     * No Slash at the end
     */
    public static final String workPath = "/mediaDownload";

    static {
        MyIOUtil.autoMkdir(workPath);
    }
}
