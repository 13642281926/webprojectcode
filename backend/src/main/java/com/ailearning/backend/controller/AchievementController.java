package com.ailearning.backend.controller;

import com.ailearning.backend.common.ApiResponse;
import com.ailearning.backend.common.AuthContext;
import com.ailearning.backend.entity.Achievement;
import com.ailearning.backend.service.AchievementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 成就系统控制器，提供成就列表查看、统计概览、成就解锁与初始化接口。
 * <p>
 * 成就系统是平台游戏化激励的核心模块，通过预设一系列学习成就（如"连续学习7天"、
 * "完成10门课程"等），在用户满足条件时解锁对应成就奖章，激发学习动力。
 * 每个用户首次使用时需要调用初始化接口生成个人成就记录；重置参数可用于重新生成所有成就。
 * </p>
 *
 * @author AI Learning Platform Team
 */
@RestController
@RequestMapping("/api/achievement")
public class AchievementController {
    private final AchievementService achievementService;

    /**
     * 构造函数，通过 Spring IoC 注入成就服务。
     *
     * @param achievementService 成就业务服务
     */
    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    /**
     * 获取当前用户的全部成就列表。
     * 返回所有成就及其解锁状态，已解锁的成就显示解锁时间，
     * 未解锁的成就显示进度条或达成条件说明。
     *
     * @return 用户所有成就的列表（含解锁状态）
     */
    @GetMapping("/list")
    public ApiResponse<List<Achievement>> list() {
        return ApiResponse.success(achievementService.list(AuthContext.getCurrentUserId()));
    }

    /**
     * 获取当前用户的成就统计概览。
     * 返回成就总览数据，包括总成就数、已解锁数、完成百分比等聚合指标，
     * 用于前端仪表盘的成就进度展示。
     *
     * @return 包含解锁数、总数、百分比等统计信息的结果
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.success(achievementService.stats(AuthContext.getCurrentUserId()));
    }

    /**
     * 手动解锁指定成就。
     * 当用户达成某个成就条件时，调用此接口将该成就标记为"已解锁"状态。
     * 服务层会再次校验解锁条件，防止前端绕过条件直接解锁。
     *
     * @param id 待解锁的成就ID
     * @return 解锁成功的空响应
     */
    @PostMapping("/unlock/{id}")
    public ApiResponse<Void> unlock(@PathVariable Long id) {
        achievementService.unlock(AuthContext.getCurrentUserId(), id);
        return ApiResponse.success("解锁成功", null);
    }

    /**
     * 初始化或重置当前用户的成就数据。
     * 新用户首次使用成就系统时调用此接口生成所有预设成就的初始记录（全部为未解锁状态）。
     * 传入 reset=true 可强制删除已有数据并重新初始化，通常用于调试或数据修复场景。
     *
     * @param reset 是否重置已有成就数据，默认为 false（仅在无数据时初始化）
     * @return 初始化成功的空响应
     */
    @PostMapping("/init")
    public ApiResponse<Void> init(@RequestParam(defaultValue = "false") boolean reset) {
        achievementService.initAchievements(AuthContext.getCurrentUserId(), reset);
        return ApiResponse.success("初始化成功", null);
    }
}
