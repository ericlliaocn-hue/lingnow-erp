package cc.lingnow.core.handler;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import cc.lingnow.common.constant.CommonConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器
 * 自动填充创建时间、更新时间、创建人、更新人等字段
 *
 * @author LingNow Team
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("开始插入填充...");

        // 填充创建时间和更新时间
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // 填充删除标记
        this.strictInsertFill(metaObject, "delFlag", Integer.class, CommonConstants.DEL_FLAG_NORMAL);

        // 填充创建人和更新人（从 Sa-Token 获取当前登录用户ID）
        try {
            if (StpUtil.isLogin()) {
                String userId = StpUtil.getLoginIdAsString();
                this.strictInsertFill(metaObject, "createBy", String.class, userId);
                this.strictInsertFill(metaObject, "updateBy", String.class, userId);
            }
        } catch (Exception e) {
            log.debug("获取当前登录用户失败，跳过 createBy 和 updateBy 填充");
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        log.debug("开始更新填充...");

        // 填充更新时间
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());

        // 填充更新人（从 Sa-Token 获取当前登录用户ID）
        try {
            if (StpUtil.isLogin()) {
                String userId = StpUtil.getLoginIdAsString();
                this.strictUpdateFill(metaObject, "updateBy", String.class, userId);
            }
        } catch (Exception e) {
            log.debug("获取当前登录用户失败，跳过 updateBy 填充");
        }
    }

}
