package cc.lingnow.biz.file.strategy.impl;

import cc.lingnow.biz.file.strategy.FileStorageStrategy;
import cc.lingnow.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * 七牛云Kodo存储策略
 *
 * @author LingNow Team
 */
@Slf4j
@Component
public class QiniuFileStorageStrategy implements FileStorageStrategy {

    @Override
    public String upload(MultipartFile file, String path) {
        throw new BusinessException("七牛云存储尚未实现(缺少依赖)");
    }

    @Override
    public String upload(InputStream inputStream, String path, String fileName) {
        throw new BusinessException("七牛云存储尚未实现(缺少依赖)");
    }

    @Override
    public boolean delete(String path) {
        return false;
    }

    @Override
    public String getPlatform() {
        return "QINIU";
    }
}
