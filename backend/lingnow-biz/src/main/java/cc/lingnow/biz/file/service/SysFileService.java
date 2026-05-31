package cc.lingnow.biz.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cc.lingnow.biz.file.entity.SysFile;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 *
 * @author LingNow Team
 */
public interface SysFileService extends IService<SysFile> {

    /**
     * 上传文件
     *
     * @param file 文件
     * @return 访问URL
     */
    String upload(MultipartFile file);

    /**
     * 上传文件 (带业务ID)
     *
     * @param file       文件
     * @param businessId 业务ID
     * @return 访问URL
     */
    String upload(MultipartFile file, Long businessId);

    /**
     * 上传文件 (带业务ID和类型)
     *
     * @param file         文件
     * @param businessId   业务ID
     * @param businessType 业务类型
     * @return 访问URL
     */
    String upload(MultipartFile file, Long businessId, String businessType);

    /**
     * 上传文件 (流)
     *
     * @param inputStream      输入流
     * @param originalFilename 原始文件名
     * @param size             文件大小
     * @param businessId       业务ID
     * @param businessType     业务类型
     * @return 访问URL
     */
    String upload(java.io.InputStream inputStream, String originalFilename, long size, Long businessId, String businessType);

    /**
     * 删除文件
     *
     * @param url 文件URL
     * @return 是否成功
     */
    boolean deleteFile(String url);

    /**
     * 删除文件
     *
     * @param id 文件ID
     * @return 是否成功
     */
    boolean deleteFile(Long id);

    /**
     * 分片上传
     *
     * @param chunk       分片文件
     * @param chunkNumber 当前分片索引
     * @param totalChunks 总分片数
     * @param identifier  文件MD5
     * @param filename    文件名
     * @return 是否上传成功
     */
    boolean uploadChunk(MultipartFile chunk, Integer chunkNumber, Integer totalChunks, String identifier, String filename);

    /**
     * 合并分片
     *
     * @param identifier 文件MD5
     * @param filename   文件名
     * @return 访问URL
     */
    String mergeChunks(String identifier, String filename);
}
