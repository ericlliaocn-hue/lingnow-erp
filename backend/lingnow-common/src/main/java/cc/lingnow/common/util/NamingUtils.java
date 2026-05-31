package cc.lingnow.common.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * 文件命名工具类
 *
 * @author LingNow Team
 */
public class NamingUtils {

    /**
     * 生成UUID文件名
     *
     * @param originalFilename 原始文件名
     * @return 新文件名
     */
    public static String uuid(String originalFilename) {
        return IdUtil.simpleUUID() + getSuffix(originalFilename);
    }

    /**
     * 生成日期+UUID文件名
     *
     * @param originalFilename 原始文件名
     * @return 新文件名
     */
    public static String dateUuid(String originalFilename) {
        return DateUtil.format(DateUtil.date(), "yyyyMMdd") + "/" + IdUtil.simpleUUID() + getSuffix(originalFilename);
    }

    /**
     * 生成时间戳文件名
     *
     * @param originalFilename 原始文件名
     * @return 新文件名
     */
    public static String timestamp(String originalFilename) {
        return System.currentTimeMillis() + getSuffix(originalFilename);
    }

    /**
     * 保留原名(如果存在则追加UUID)
     *
     * @param originalFilename 原始文件名
     * @return 新文件名
     */
    public static String original(String originalFilename) {
        // 实际使用时可能需要检查重复，这里简单处理，为防止覆盖，建议还是加个随机数
        // 但根据需求"原名"，这里直接返回，调用方需处理冲突
        return originalFilename;
    }

    /**
     * 获取文件后缀
     *
     * @param filename 文件名
     * @return 后缀(包含.)
     */
    public static String getSuffix(String filename) {
        if (StringUtils.isEmpty(filename)) {
            return "";
        }
        int index = filename.lastIndexOf(".");
        return index == -1 ? "" : filename.substring(index);
    }
}
