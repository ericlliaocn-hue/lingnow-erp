package cc.lingnow.biz.notice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import cc.lingnow.biz.notice.entity.SysNotice;

import java.util.List;

/**
 * 通知公告Service接口
 *
 * @author LingNow Team
 */
public interface SysNoticeService extends IService<SysNotice> {

    /**
     * 查询通知公告列表
     *
     * @param notice 通知公告信息
     * @return 通知公告集合
     */
    List<SysNotice> selectNoticeList(SysNotice notice);
}
