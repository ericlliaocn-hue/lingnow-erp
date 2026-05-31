package cc.lingnow.biz.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.file.entity.SysFile;
import cc.lingnow.biz.file.mapper.SysFileMapper;
import cc.lingnow.biz.file.service.SysFileService;
import cc.lingnow.biz.file.strategy.FileStorageFactory;
import cc.lingnow.biz.file.strategy.FileStorageStrategy;
import cc.lingnow.common.util.NamingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务实现类
 *
 * @author LingNow Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements SysFileService {

    private final FileStorageFactory fileStorageFactory;

    @Override
    public String upload(MultipartFile file) {
        return upload(file, null, null);
    }

    @Override
    public String upload(MultipartFile file, Long businessId) {
        return upload(file, businessId, null);
    }

    @Override
    public String upload(MultipartFile file, Long businessId, String businessType) {
        try {
            return upload(file.getInputStream(), file.getOriginalFilename(), file.getSize(), businessId, businessType);
        } catch (java.io.IOException e) {
            throw new cc.lingnow.common.exception.BusinessException("文件上传失败");
        }
    }

    @Override
    public String upload(java.io.InputStream inputStream, String originalFilename, long size, Long businessId, String businessType) {
        // 获取当前策略
        FileStorageStrategy strategy = fileStorageFactory.getStrategy();

        // 生成文件名 (默认使用 日期+UUID 模式)
        String fileName = NamingUtils.dateUuid(originalFilename);

        // 上传文件
        String url = strategy.upload(inputStream, fileName, fileName);

        // 保存文件记录
        saveSysFile(originalFilename, fileName, url, size, strategy.getPlatform(), businessId, businessType);

        return url;
    }

    @Override
    public boolean deleteFile(String url) {
        // 根据URL查找文件记录
        SysFile sysFile = this.getOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysFile>()
                        .eq(SysFile::getFileUrl, url)
                        .last("LIMIT 1")
        );

        if (sysFile == null) {
            // 如果记录不存在，尝试直接删除（可能是旧数据或直接传入的路径）
            // 默认使用当前激活的策略
            FileStorageStrategy strategy = fileStorageFactory.getStrategy();
            return strategy.delete(url);
        }

        // 获取对应的策略
        FileStorageStrategy strategy = fileStorageFactory.getStrategy(sysFile.getStorageType());
        if (strategy == null) {
            log.warn("未找到对应存储策略: {}", sysFile.getStorageType());
            return false;
        }

        // 删除物理文件
        boolean deleted = strategy.delete(sysFile.getFilePath()); // 这里的path存的是相对路径
        if (deleted) {
            // 删除数据库记录
            return this.removeById(sysFile.getId());
        }
        return false;
    }

    @Override
    public boolean deleteFile(Long id) {
        SysFile sysFile = this.getById(id);
        if (sysFile == null) {
            return true;
        }

        // 获取对应的策略
        FileStorageStrategy strategy = fileStorageFactory.getStrategy(sysFile.getStorageType());
        if (strategy != null) {
            // 删除物理文件
            strategy.delete(sysFile.getFilePath());
        }

        // 删除数据库记录
        return this.removeById(id);
    }

    @Override
    public boolean uploadChunk(MultipartFile chunk, Integer chunkNumber, Integer totalChunks, String identifier, String filename) {
        String tempDir = System.getProperty("java.io.tmpdir") + java.io.File.separator + "upload_chunks" + java.io.File.separator + identifier;
        java.io.File folder = new java.io.File(tempDir);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        java.io.File dest = new java.io.File(folder, chunkNumber + ".part");
        try {
            chunk.transferTo(dest);
            return true;
        } catch (java.io.IOException e) {
            log.error("分片上传失败", e);
            throw new RuntimeException("分片上传失败", e);
        }
    }

    @Override
    public String mergeChunks(String identifier, String filename) {
        String tempDir = System.getProperty("java.io.tmpdir") + java.io.File.separator + "upload_chunks" + java.io.File.separator + identifier;
        java.io.File folder = new java.io.File(tempDir);
        if (!folder.exists() || !folder.isDirectory()) {
            throw new RuntimeException("分片文件不存在");
        }

        java.io.File[] chunks = folder.listFiles((dir, name) -> name.endsWith(".part"));
        if (chunks == null || chunks.length == 0) {
            throw new RuntimeException("未找到分片文件");
        }

        // 排序分片
        java.util.Arrays.sort(chunks, (o1, o2) -> {
            int n1 = Integer.parseInt(o1.getName().replace(".part", ""));
            int n2 = Integer.parseInt(o2.getName().replace(".part", ""));
            return Integer.compare(n1, n2);
        });

        // 合并文件
        java.io.File mergedFile = new java.io.File(folder, filename);
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(mergedFile, true)) {
            for (java.io.File chunk : chunks) {
                java.nio.file.Files.copy(chunk.toPath(), fos);
            }
        } catch (java.io.IOException e) {
            log.error("合并文件失败", e);
            throw new RuntimeException("合并文件失败", e);
        }

        // 上传合并后的文件
        String url;
        try (java.io.FileInputStream fis = new java.io.FileInputStream(mergedFile)) {
            // 识别文件类型并处理
            url = processFile(fis, filename, mergedFile.length());
        } catch (java.io.IOException e) {
            log.error("上传合并文件失败", e);
            throw new RuntimeException("上传合并文件失败", e);
        } finally {
            // 清理临时文件
            deleteFolder(folder);
        }

        return url;
    }

    private String processFile(java.io.InputStream inputStream, String originalFilename, long size) {
        // 判断文件类型
        String suffix = NamingUtils.getSuffix(originalFilename).toLowerCase();

        if (isCompressedFile(suffix)) {
            return processCompressedFile(inputStream, originalFilename, size);
        } else {
            return processNormalFile(inputStream, originalFilename, size);
        }
    }

    private boolean isCompressedFile(String suffix) {
        return java.util.Arrays.asList(".zip", ".rar", ".7z", ".tar", ".gz").contains(suffix);
    }

    private String processNormalFile(java.io.InputStream inputStream, String originalFilename, long size) {
        FileStorageStrategy strategy = fileStorageFactory.getStrategy();
        String fileName = NamingUtils.dateUuid(originalFilename);
        String url = strategy.upload(inputStream, fileName, fileName); // Fix: strategy upload signature needs check

        saveSysFile(originalFilename, fileName, url, size, strategy.getPlatform(), null, null);
        return url;
    }

    private String processCompressedFile(java.io.InputStream inputStream, String originalFilename, long size) {
        // 目前对于压缩包，暂时按普通文件处理，后续可扩展解压逻辑
        return processNormalFile(inputStream, originalFilename, size);
    }

    private void saveSysFile(String originalFilename, String filePath, String url, long size, String platform, Long businessId, String businessType) {
        SysFile sysFile = new SysFile();
        sysFile.setFileName(originalFilename);
        sysFile.setFilePath(filePath);
        sysFile.setFileUrl(url);
        sysFile.setFileSize(size);
        sysFile.setFileSuffix(NamingUtils.getSuffix(originalFilename));
        sysFile.setStorageType(platform);
        sysFile.setBusinessId(businessId);
        sysFile.setBusinessType(businessType);
        this.save(sysFile);
    }

    private void deleteFolder(java.io.File folder) {
        if (folder.isDirectory()) {
            java.io.File[] files = folder.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    deleteFolder(file);
                }
            }
        }
        folder.delete();
    }
}
