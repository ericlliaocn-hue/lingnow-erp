package cc.lingnow.biz.file.strategy;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 文件存储策略接口
 *
 * @author LingNow Team
 */
public interface FileStorageStrategy {

    /**
     * 上传文件
     *
     * @param file 文件
     * @param path 存储路径(相对路径)
     * @return 访问URL
     */
    String upload(MultipartFile file, String path);

    /**
     * 上传文件(流)
     *
     * @param inputStream 输入流
     * @param path        存储路径
     * @param fileName    文件名
     * @return 访问URL
     */
    String upload(InputStream inputStream, String path, String fileName);

    /**
     * 删除文件
     *
     * @param path 文件路径
     * @return 是否成功
     */
    boolean delete(String path);

    /**
     * 获取存储平台类型
     *
     * @return 平台类型
     */
    String getPlatform();
}
