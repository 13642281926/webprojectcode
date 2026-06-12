package com.ailearning.backend.service;

import com.ailearning.backend.entity.Achievement;
import com.ailearning.backend.entity.User;
import com.ailearning.backend.exception.ApiException;
import com.ailearning.backend.repository.AchievementRepository;
import com.ailearning.backend.repository.NoteRepository;
import com.ailearning.backend.repository.ResourceRepository;
import com.ailearning.backend.repository.UserRepository;
import com.ailearning.backend.repository.WrongQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 成就服务 —— 负责用户成就体系的完整生命周期管理。
 *
 * <p>核心机制：
 * <ol>
 *   <li><b>成就初始化</b>：为新用户创建 8 个预设成就（学习新手、知识探索者、笔记达人、错题收集者、持续学习、资源大师、学习狂人、传奇学者）。</li>
 *   <li><b>进度同步（sync）</b>：每次查询成就时自动从各业务模块拉取最新指标（学习天数、笔记数、错题数、资源数等），
 *       与成就目标值比对，更新进度条和解锁状态。这是成就系统的核心引擎。</li>
 *   <li><b>解锁判定</b>：当成就进度 >= 目标值（target）时自动解锁，记录解锁时间。</li>
 *   <li><b>排行榜/统计</b>：计算已解锁数量、总积分、连续学习天数、最近解锁记录。</li>
 * </ol>
 *
 * <p>稀有度等级：common（普通）、uncommon（稀有）、rare（罕见）、epic（史诗）、legendary（传说）。
 * "传奇学者" 是最高成就，需要解锁其他所有成就后自动触发。
 */
@Service
public class AchievementService {
    private final AchievementRepository achievementRepository;
    private final UserRepository userRepository;
    private final NoteRepository noteRepository;
    private final ResourceRepository resourceRepository;
    private final WrongQuestionRepository wrongQuestionRepository;

    /**
     * 构造成就服务，注入成就仓库及所有需要统计的业务仓库。
     *
     * @param achievementRepository  成就数据访问接口
     * @param userRepository         用户数据访问接口（用于获取学习天数等）
     * @param noteRepository         笔记仓库（用于统计笔记数量）
     * @param resourceRepository     资源仓库（用于统计资源数量）
     * @param wrongQuestionRepository 错题仓库（用于统计错题数量）
     */
    public AchievementService(AchievementRepository achievementRepository,
                              UserRepository userRepository,
                              NoteRepository noteRepository,
                              ResourceRepository resourceRepository,
                              WrongQuestionRepository wrongQuestionRepository) {
        this.achievementRepository = achievementRepository;
        this.userRepository = userRepository;
        this.noteRepository = noteRepository;
        this.resourceRepository = resourceRepository;
        this.wrongQuestionRepository = wrongQuestionRepository;
    }

    /**
     * 获取用户成就列表（自动同步进度后返回）。
     *
     * @param userId 用户 ID
     * @return 成就列表（进度已实时同步）
     */
    @Transactional
    public List<Achievement> list(Long userId) {
        return syncAchievements(userId);
    }

    /**
     * 成就统计概览（供前端成就页面/仪表盘使用）。
     *
     * <p>返回指标包括：总成就数、已解锁数、总积分、连续学习天数、
     * 最长连续天数、最近解锁的 5 个成就。
     *
     * @param userId 用户 ID
     * @return 统计指标 Map
     */
    @Transactional
    public Map<String, Object> stats(Long userId) {
        List<Achievement> list = syncAchievements(userId);
        User user = getUser(userId);
        // 统计已解锁数量和总积分
        long unlockedCount = list.stream().filter(Achievement::isUnlocked).count();
        int totalPoints = list.stream().filter(Achievement::isUnlocked).mapToInt(Achievement::getPoints).sum();

        List<Map<String, Object>> recentUnlocked = list.stream()
                .filter(Achievement::isUnlocked)
                .sorted(Comparator.comparing(Achievement::getUnlockedAt).reversed())
                .limit(5)
                .map(a -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", a.getId());
                    m.put("name", a.getTitle());
                    m.put("icon", a.getIcon());
                    m.put("points", a.getPoints());
                    m.put("unlockedAt", a.getUnlockedAt() != null ? a.getUnlockedAt().toString() : null);
                    return m;
                })
                .toList();

        Map<String, Object> data = new HashMap<>();
        data.put("total", list.size());
        data.put("unlocked", unlockedCount);
        data.put("totalPoints", totalPoints);
        data.put("unlockedCount", unlockedCount);
        data.put("streak", user.getStudyDays());
        data.put("longestStreak", user.getStudyDays());
        data.put("recentUnlocked", recentUnlocked);
        return data;
    }

    /**
     * 手动解锁指定成就（管理员或前端直接触发）。
     *
     * <p>如果成就已解锁则不做任何操作（幂等）。
     *
     * @param userId 用户 ID
     * @param id     成就 ID
     * @throws ApiException 404 如果成就不存在或不属于该用户
     */
    @Transactional
    public void unlock(Long userId, Long id) {
        Achievement a = achievementRepository.findById(id)
                .filter(ach -> ach.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(404, "成就不存在"));
        if (!a.isUnlocked()) {
            a.setUnlocked(true);
            a.setUnlockedAt(LocalDateTime.now());
            a.setProgress(a.getTarget());
            achievementRepository.save(a);
        }
    }

    /**
     * 初始化用户成就（不重置已有成就）。
     *
     * <p>如果用户已有成就记录，则仅做同步而非重新创建。
     *
     * @param userId 用户 ID
     */
    @Transactional
    public void initAchievements(Long userId) {
        initAchievements(userId, false);
    }

    /**
     * 初始化用户成就，可选择是否重置已有成就。
     *
     * <p>流程：
     * <ol>
     *   <li>查询用户现有成就列表</li>
     *   <li>如果非重置模式且已有记录，仅做同步</li>
     *   <li>如果重置模式，先删除全部旧记录再重新创建</li>
     *   <li>创建完成后立即同步进度</li>
     * </ol>
     *
     * @param userId 用户 ID
     * @param reset  是否重置已有成就（true = 删除重建，false = 保留并同步）
     */
    @Transactional
    public void initAchievements(Long userId, boolean reset) {
        List<Achievement> existing = achievementRepository.findByUserId(userId);
        if (!existing.isEmpty()) {
            if (!reset) {
                // 非重置模式：已有记录则只同步
                syncAchievements(userId, existing);
                return;
            }
            // 重置模式：清空旧记录
            achievementRepository.deleteAll(existing);
        }
        // 创建 8 个预设成就
        achievementRepository.saveAll(buildAchievements(userId));
        syncAchievements(userId);
    }

    /**
     * 创建单个成就实体的工厂方法。
     *
     * @param userId      所属用户 ID
     * @param title       成就名称
     * @param description 成就描述
     * @param icon        前端图标名称
     * @param category    成就类别（成长/学习/创作/坚持/任务/特殊）
     * @param rarity      稀有度（common/uncommon/rare/epic/legendary）
     * @param points      成就积分
     * @param target      解锁所需的目标值
     * @param progress    当前进度
     * @return 初始化好的成就实体
     */
    private Achievement createAchievement(Long userId, String title, String description, String icon, String category, String rarity, int points, int target, int progress) {
        Achievement a = new Achievement();
        a.setUserId(userId);
        a.setTitle(title);
        a.setDescription(description);
        a.setIcon(icon);
        a.setCategory(category);
        a.setRarity(rarity);
        a.setPoints(points);
        a.setTarget(target);
        a.setProgress(progress);
        a.setUnlocked(false);
        return a;
    }

    /**
     * 批量更新指定类别下所有未解锁成就的进度。
     *
     * <p>当用户在某个业务模块（如创建笔记、添加错题）中执行操作后调用此方法，
     * 自动推进相关成就的进度。进度达到目标值时自动解锁。
     *
     * @param userId    用户 ID
     * @param category  成就类别（如"学习"、"创作"、"坚持"等）
     * @param increment 进度增量（通常为 1）
     */
    @Transactional
    public void updateProgress(Long userId, String category, int increment) {
        List<Achievement> list = syncAchievements(userId).stream()
                .filter(a -> !a.isUnlocked() && category.equals(a.getCategory()))
                .toList();

        for (Achievement a : list) {
            int newProgress = a.getProgress() + increment;
            a.setProgress(Math.min(newProgress, a.getTarget()));
            if (a.getProgress() >= a.getTarget()) {
                a.setUnlocked(true);
                a.setUnlockedAt(LocalDateTime.now());
            }
            achievementRepository.save(a);
        }
    }

    /**
     * 同步用户成就进度（公开入口，自动查询成就列表）。
     *
     * <p>该方法是成就系统的核心引擎：每次查询/展示成就时调用，
     * 从各业务模块拉取最新数据并与成就目标比对。
     *
     * @param userId 用户 ID
     * @return 同步后的成就列表
     */
    @Transactional
    public List<Achievement> syncAchievements(Long userId) {
        return syncAchievements(userId, achievementRepository.findByUserId(userId));
    }

    /**
     * 同步用户成就进度的核心逻辑。
     *
     * <p>执行步骤：
     * <ol>
     *   <li>若成就列表为空，先创建预设成就</li>
     *   <li>从各业务模块统计实际数据：学习天数、笔记数、错题数、资源数</li>
     *   <li>构建"成就名称 -> 实际进度"映射表</li>
     *   <li>遍历每个成就，用实际进度与目标值比对，更新进度和解锁状态</li>
     *   <li>特殊处理"传奇学者"：统计其他已解锁成就数作为其进度</li>
     *   <li>若有变更则持久化，无变更则直接返回</li>
     * </ol>
     *
     * @param userId       用户 ID
     * @param achievements 用户的现有成就列表
     * @return 同步后的成就列表
     */
    private List<Achievement> syncAchievements(Long userId, List<Achievement> achievements) {
        List<Achievement> current = achievements;
        // 如果没有成就记录，先创建预设成就
        if (current.isEmpty()) {
            current = achievementRepository.saveAll(buildAchievements(userId));
        }

        User user = getUser(userId);
        // 从各业务模块统计实际数据
        int studyCount = Math.max(user.getStudyDays(), 0);
        int noteCount = noteRepository.findByUserIdOrderByUpdatedAtDesc(userId).size();
        int wrongQuestionCount = wrongQuestionRepository.findByUserIdOrderByUpdatedAtDesc(userId).size();
        int resourceCount = resourceRepository.findByUserIdOrderByCreatedAtDesc(userId).size();

        // 构建"成就名称 -> 实际进度"映射表
        Map<String, Integer> progressMap = new HashMap<>();
        progressMap.put("学习新手", Math.min(studyCount, 1));
        progressMap.put("知识探索者", Math.min(studyCount, 3));
        progressMap.put("笔记达人", noteCount);
        progressMap.put("错题收集者", wrongQuestionCount);
        progressMap.put("持续学习", Math.min(studyCount, 7));
        progressMap.put("资源大师", resourceCount);
        progressMap.put("学习狂人", studyCount);

        boolean changed = false;
        // 遍历每个成就，比对实际进度与目标值
        for (Achievement achievement : current) {
            Integer actualProgress = progressMap.get(achievement.getTitle());
            if (actualProgress == null) {
                continue;  // 此成就不在进度映射中，跳过
            }

            // 进度不能超过目标值
            int nextProgress = Math.min(actualProgress, achievement.getTarget());
            if (achievement.getProgress() != nextProgress) {
                achievement.setProgress(nextProgress);
                changed = true;
            }

            // 判断是否应解锁
            boolean shouldUnlock = nextProgress >= achievement.getTarget();
            if (achievement.isUnlocked() != shouldUnlock) {
                achievement.setUnlocked(shouldUnlock);
                achievement.setUnlockedAt(shouldUnlock ? LocalDateTime.now() : null);
                changed = true;
            } else if (!shouldUnlock && achievement.getUnlockedAt() != null) {
                achievement.setUnlockedAt(null);
                changed = true;
            }
        }

        // 特殊处理"传奇学者"：以其他成就中已解锁的数量作为进度
        long unlockedWithoutLegend = current.stream()
                .filter(achievement -> !"传奇学者".equals(achievement.getTitle()))
                .filter(Achievement::isUnlocked)
                .count();
        for (Achievement achievement : current) {
            if (!"传奇学者".equals(achievement.getTitle())) {
                continue;
            }

            int nextProgress = (int) Math.min(unlockedWithoutLegend, achievement.getTarget());
            if (achievement.getProgress() != nextProgress) {
                achievement.setProgress(nextProgress);
                changed = true;
            }

            boolean shouldUnlock = unlockedWithoutLegend >= current.size() - 1;
            if (achievement.isUnlocked() != shouldUnlock) {
                achievement.setUnlocked(shouldUnlock);
                achievement.setUnlockedAt(shouldUnlock ? LocalDateTime.now() : null);
                changed = true;
            } else if (!shouldUnlock && achievement.getUnlockedAt() != null) {
                achievement.setUnlockedAt(null);
                changed = true;
            }
            break;
        }

        // 只有发生变更时才写库，未变更直接返回
        if (changed) {
            return achievementRepository.saveAll(current);
        }
        return current;
    }

    /**
     * 构建用户的 8 个预设成就列表。
     *
     * <p>成就设计：
     * <table>
     *   <tr><th>名称</th><th>类别</th><th>稀有度</th><th>积分</th><th>目标</th></tr>
     *   <tr><td>学习新手</td><td>成长</td><td>common</td><td>10</td><td>1天</td></tr>
     *   <tr><td>知识探索者</td><td>学习</td><td>uncommon</td><td>20</td><td>3天</td></tr>
     *   <tr><td>笔记达人</td><td>创作</td><td>uncommon</td><td>25</td><td>5条</td></tr>
     *   <tr><td>错题收集者</td><td>学习</td><td>rare</td><td>30</td><td>10道</td></tr>
     *   <tr><td>持续学习</td><td>坚持</td><td>epic</td><td>50</td><td>7天</td></tr>
     *   <tr><td>资源大师</td><td>任务</td><td>rare</td><td>35</td><td>20个</td></tr>
     *   <tr><td>学习狂人</td><td>学习</td><td>epic</td><td>60</td><td>50天</td></tr>
     *   <tr><td>传奇学者</td><td>特殊</td><td>legendary</td><td>100</td><td>7个</td></tr>
     * </table>
     *
     * @param userId 用户 ID
     * @return 8 个初始化的成就实体
     */
    private List<Achievement> buildAchievements(Long userId) {
        return Arrays.asList(
                createAchievement(userId, "学习新手", "完成第一次学习", "CircleCheck", "成长", "common", 10, 1, 0),
                createAchievement(userId, "知识探索者", "完成3次学习", "List", "学习", "uncommon", 20, 3, 0),
                createAchievement(userId, "笔记达人", "创建5条笔记", "Document", "创作", "uncommon", 25, 5, 0),
                createAchievement(userId, "错题收集者", "添加10道错题", "Warning", "学习", "rare", 30, 10, 0),
                createAchievement(userId, "持续学习", "连续7天学习", "Timer", "坚持", "epic", 50, 7, 0),
                createAchievement(userId, "资源大师", "上传20个资源", "Folder", "任务", "rare", 35, 20, 0),
                createAchievement(userId, "学习狂人", "完成50次学习", "Timer", "学习", "epic", 60, 50, 0),
                createAchievement(userId, "传奇学者", "解锁所有成就", "Trophy", "特殊", "legendary", 100, 7, 0)
        );
    }

    /**
     * 按 ID 获取用户实体（内部调用的统一入口）。
     *
     * @param userId 用户 ID
     * @return 用户实体
     * @throws ApiException 404 如果用户不存在
     */
    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "用户不存在"));
    }
}
