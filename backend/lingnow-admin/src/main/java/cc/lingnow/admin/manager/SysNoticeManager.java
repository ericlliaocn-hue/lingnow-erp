package cc.lingnow.admin.manager;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cc.lingnow.admin.model.bo.NoticeQueryBO;
import cc.lingnow.admin.model.bo.NoticeSaveBO;
import cc.lingnow.admin.model.vo.NoticeVO;
import cc.lingnow.biz.notice.entity.SysNotice;
import cc.lingnow.biz.notice.service.SysNoticeService;
import cc.lingnow.common.enums.ErrorCode;
import cc.lingnow.common.exception.BusinessException;
import cc.lingnow.common.vo.PageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知公告管理业务逻辑
 *
 * @author LingNow Team
 */
@Service
@RequiredArgsConstructor
public class SysNoticeManager {

    private final SysNoticeService noticeService;

    /**
     * 查询通知公告列表
     */
    public PageResult<NoticeVO> listNotices(NoticeQueryBO query) {
        SysNotice notice = BeanUtil.copyProperties(query, SysNotice.class);
        List<SysNotice> list = noticeService.selectNoticeList(notice);

        long total = list.size(); // Placeholder
        List<NoticeVO> voList = list.stream()
                .map(item -> BeanUtil.copyProperties(item, NoticeVO.class))
                .collect(Collectors.toList());

        return PageResult.of(query.getCurrent(), query.getSize(), total, voList);
    }

    /**
     * 查询通知公告详情
     */
    public NoticeVO getNotice(Long noticeId) {
        SysNotice notice = noticeService.getById(noticeId);
        if (notice == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXIST);
        }
        return BeanUtil.copyProperties(notice, NoticeVO.class);
    }

    /**
     * 新增通知公告
     */
    public void addNotice(NoticeSaveBO bo) {
        SysNotice notice = BeanUtil.copyProperties(bo, SysNotice.class);
        noticeService.save(notice);
    }

    /**
     * 修改通知公告
     */
    public void updateNotice(NoticeSaveBO bo) {
        if (ObjectUtil.isNull(bo.getNoticeId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        SysNotice notice = BeanUtil.copyProperties(bo, SysNotice.class);
        noticeService.updateById(notice);
    }

    /**
     * 删除通知公告
     */
    public void removeNotice(List<Long> noticeIds) {
        noticeService.removeByIds(noticeIds);
    }
}
