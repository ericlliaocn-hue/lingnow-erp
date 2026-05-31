package cc.lingnow.biz.notice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.lingnow.biz.notice.entity.SysNotice;
import cc.lingnow.biz.notice.mapper.SysNoticeMapper;
import cc.lingnow.biz.notice.service.SysNoticeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 通知公告Service业务层处理
 *
 * @author LingNow Team
 */
@Service
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements SysNoticeService {

    @Override
    public List<SysNotice> selectNoticeList(SysNotice notice) {
        LambdaQueryWrapper<SysNotice> lqw = new LambdaQueryWrapper<>();
        lqw.like(ObjectUtils.isNotEmpty(notice.getNoticeTitle()), SysNotice::getNoticeTitle, notice.getNoticeTitle())
                .eq(ObjectUtils.isNotEmpty(notice.getNoticeType()), SysNotice::getNoticeType, notice.getNoticeType())
                .like(ObjectUtils.isNotEmpty(notice.getCreateBy()), SysNotice::getCreateBy, notice.getCreateBy())
                .eq(notice.getStatus() != null, SysNotice::getStatus, notice.getStatus())
                .eq(SysNotice::getDelFlag, false)
                .orderByDesc(SysNotice::getCreateTime);
        return baseMapper.selectList(lqw);
    }
}
