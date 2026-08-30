package com.campus.visit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.visit.common.BusinessException;
import com.campus.visit.common.ResultCode;
import com.campus.visit.entity.ChatMessage;
import com.campus.visit.entity.VisitReservation;
import com.campus.visit.mapper.ChatMessageMapper;
import com.campus.visit.mapper.VisitorUserMapper;
import com.campus.visit.mapper.VisitReservationMapper;
import com.campus.visit.service.ReservationService;
import com.campus.visit.service.StatsService;
import com.campus.visit.vo.reservation.ReservationListVO;
import com.campus.visit.vo.stats.ChatLogVO;
import com.campus.visit.vo.stats.DashboardVO;
import com.campus.visit.vo.stats.HotKeywordsVO;
import com.campus.visit.vo.stats.KeywordCount;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 统计与问答日志 Service 实现（对标 architecture.md 模块 10）
 *
 * 三个接口全部只读：只用 SELECT 聚合，不改任何业务数据，
 * 因此不加 @Transactional（没有事务可言，答辩可讲这个取舍）。
 */
@Slf4j
@Service
public class StatsServiceImpl implements StatsService {

    /** 预约状态：待审核（状态字典 D4，ReservationServiceImpl 内常量是 private，这里按字典值写死） */
    private static final int STATUS_PENDING = 0;

    /**
     * 中文高频虚词停用字表（简化分词的配套过滤）。
     * 这些字单独出现不构成有效语义，统计前剔除，热词才有可读性。
     * 答辩话术：这是设计文档钦定的零依赖简化方案，生产建议升级 IK/HanLP 分词器。
     */
    private static final Set<Character> STOP_WORDS = Set.of(
            '的', '了', '是', '我', '在', '有', '和', '就', '不', '人', '都', '一', '个',
            '上', '也', '很', '到', '说', '要', '去', '你', '会', '着', '没', '看', '好',
            '自', '里', '后', '他', '这', '那', '中', '大', '为', '与', '及', '或', '等',
            '对', '吗', '呢', '吧', '啊', '呀', '哦', '把', '被', '让', '给', '向', '从',
            '以', '之', '其', '此', '什', '么', '怎', '样', '如', '何', '请', '问', '想',
            '能', '可', '需', '还', '又', '再', '才', '只', '更', '最');

    @Resource
    private VisitReservationMapper visitReservationMapper;

    @Resource
    private VisitorUserMapper visitorUserMapper;

    @Resource
    private ChatMessageMapper chatMessageMapper;

    /** 复用模块 5 的管理员分页查"最近 5 条待审核"，避免重复写一遍 VO 组装逻辑 */
    @Resource
    private ReservationService reservationService;

    @Override
    public DashboardVO dashboard() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        // ① 今日新增预约数：submit_time >= 今天 0 点（含全部状态：新提交/已审/已取消都算"今日新增"）
        Long todayReservation = visitReservationMapper.selectCount(
                new LambdaQueryWrapper<VisitReservation>()
                        .ge(VisitReservation::getSubmitTime, todayStart));

        // ② 待审核订单数：status=0
        Long pendingAudit = visitReservationMapper.selectCount(
                new LambdaQueryWrapper<VisitReservation>()
                        .eq(VisitReservation::getStatus, STATUS_PENDING));

        // ③ 访客总数：visitor_user 全表（该表无逻辑删除，直接 count）
        Long visitorTotal = visitorUserMapper.selectCount(null);

        // ④ AI 问答总次数：assistant 消息数（一次 AI 回答 = 一次有效问答，与日志口径一致）
        Long chatTotal = chatMessageMapper.selectCount(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getRole, "assistant"));

        // ⑤ 近 7 天预约趋势（缺数据的天补 0）
        List<DashboardVO.DayCount> weeklyTrend = buildWeeklyTrend();

        // ⑥ 最近 5 条待审核订单：复用模块 5 管理员分页（status=0，size=5），零重复代码
        List<ReservationListVO> recentPending = reservationService
                .pageForAdmin(null, STATUS_PENDING, null, null, 1, 5)
                .getRecords();

        return DashboardVO.builder()
                .todayReservationCount(todayReservation.intValue())
                .pendingAuditCount(pendingAudit.intValue())
                .visitorTotal(visitorTotal.intValue())
                .chatTotalCount(chatTotal.intValue())
                .weeklyTrend(weeklyTrend)
                .recentPending(recentPending)
                .build();
    }

    @Override
    public IPage<ChatLogVO> pageChatLogs(Long visitorId, String keyword,
            LocalDateTime startDate, LocalDateTime endDate,
            Integer current, Integer size) {
        // 三表关联 SQL 在 Mapper 里（见 pageChatLogs 注释），这里只负责传参
        return chatMessageMapper.pageChatLogs(
                new Page<>(current, size), visitorId, keyword, startDate, endDate);
    }

    @Override
    public HotKeywordsVO hotKeywords(Integer days) {
        // 参数校验：1~365，防全表扫描被滥用（365 天以上的统计没业务意义）
        if (days == null || days < 1 || days > 365) {
            throw new BusinessException(ResultCode.PARAM_INVALID, "days 取值范围 1~365");
        }

        // 近 N 天：含今天往前推 days-1 天（days=1 就是"只看今天"）
        LocalDateTime since = LocalDate.now().minusDays(days - 1L).atStartOfDay();

        // 只查 content 一列（问题原文），减少内存占用
        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .select(ChatMessage::getContent)
                        .eq(ChatMessage::getRole, "user")
                        .ge(ChatMessage::getCreateTime, since));

        // 简化分词：按字符切分 + 停用字过滤 → 频次聚合（设计文档钦定方案）
        Map<String, Integer> freq = new HashMap<>();
        for (ChatMessage msg : messages) {
            String content = msg.getContent();
            if (content == null || content.isEmpty()) {
                continue;
            }
            for (int i = 0; i < content.length(); i++) {
                char c = content.charAt(i);
                // 只统计汉字（CJK 统一表意文字 \u4E00-\u9FA5）：
                // 标点（？，。）、字母、数字、空白对"高频问题"无语义，首轮实测标点霸榜（？26 次），故先过滤
                if (c < 0x4E00 || c > 0x9FA5) {
                    continue;
                }
                // 再剔除高频虚词停用字
                if (STOP_WORDS.contains(c)) {
                    continue;
                }
                freq.merge(String.valueOf(c), 1, Integer::sum);
            }
        }

        // 按出现次数降序排（次数相同的字顺序不稳定，对统计场景无影响）
        List<KeywordCount> sorted = freq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> new KeywordCount(e.getKey(), e.getValue()))
                .toList();

        // Top10 与 Top100 是"前 N 个"的包含关系，一次排序两份切片
        return HotKeywordsVO.builder()
                .topKeywords(sorted.subList(0, Math.min(10, sorted.size())))
                .wordCloud(sorted.subList(0, Math.min(100, sorted.size())))
                .build();
    }

    /**
     * 近 7 天预约趋势：一次 GROUP BY 查出有数据的日期，应用层补零。
     *
     * 为什么不在 SQL 里补零？MySQL 没有日期维表，要补零得派生表 + 日期自增，复杂度远超收益；
     * 应用层 7 次循环 getOrDefault 一行一个，简单直白——统计类需求优先可读性。
     */
    private List<DashboardVO.DayCount> buildWeeklyTrend() {
        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);

        // 一次分组查询：按提交日期统计预约数（只扫近 7 天范围，走 submit_time 范围过滤）
        QueryWrapper<VisitReservation> qw = new QueryWrapper<>();
        qw.select("DATE(submit_time) AS stat_date", "COUNT(*) AS cnt")
                .ge("submit_time", weekAgo.atStartOfDay())
                .groupBy("DATE(submit_time)");
        List<Map<String, Object>> rows = visitReservationMapper.selectMaps(qw);

        // Map<日期, 数量>：GROUP BY 返回的 DATE() 是 java.sql.Date，转 LocalDate 做键
        Map<LocalDate, Long> countMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object d = row.get("stat_date");
            if (d == null) {
                continue; // submit_time 理论上非空，防御一下
            }
            LocalDate date = (d instanceof java.sql.Date sqlDate)
                    ? sqlDate.toLocalDate()
                    : LocalDate.parse(d.toString());
            Object c = row.get("cnt");
            long cnt = (c instanceof Number n) ? n.longValue() : 0L;
            countMap.put(date, cnt);
        }

        // 从 6 天前倒推到今天，缺数据的日期补 0，保证图表 7 个点连续
        List<DashboardVO.DayCount> trend = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            trend.add(new DashboardVO.DayCount(day, countMap.getOrDefault(day, 0L).intValue()));
        }
        return trend;
    }
}
