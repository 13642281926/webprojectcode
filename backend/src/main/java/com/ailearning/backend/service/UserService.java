package com.ailearning.backend.service;

import com.ailearning.backend.dto.LoginRequest;
import com.ailearning.backend.dto.ProfileUpdateRequest;
import com.ailearning.backend.dto.RegisterRequest;
import com.ailearning.backend.entity.User;
import com.ailearning.backend.exception.ApiException;
import com.ailearning.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务 —— 负责用户注册、登录、个人信息管理等核心业务逻辑。
 *
 * <p>主要功能：
 * <ol>
 *   <li><b>登录</b>：校验账号密码，调用 {@link AuthService} 签发 JWT，返回令牌与用户信息。</li>
 *   <li><b>注册</b>：校验密码一致性、账号唯一性，创建用户记录并自动生成默认头像和签名。</li>
 *   <li><b>个人信息</b>：查询、更新用户昵称、签名、头像等资料。</li>
 * </ol>
 *
 * <p>密码采用明文存储（演示项目性质），头像使用 DiceBear 在线服务自动生成。
 */
@Service
public class UserService {
    private final UserRepository userRepository;
    private final AuthService authService;

    /**
     * 构造用户服务，注入用户数据仓库与认证服务。
     *
     * @param userRepository 用户数据访问接口
     * @param authService    认证服务（用于签发 JWT）
     */
    public UserService(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    /**
     * 用户登录。
     *
     * <p>先按用户名查找用户（不存在则统一返回"账号或密码错误"以避免账号枚举），
     * 再比对明文密码，通过后签发 JWT 并组装用户信息返回。
     *
     * @param request 登录请求，含 username 和 password
     * @return 包含 token 和 userInfo 的 Map
     * @throws ApiException 401 如果用户名不存在或密码不匹配
     */
    @Transactional(readOnly = true)
    public Map<String, Object> login(LoginRequest request) {
        // 按用户名查找，不存在则统一提示错误（防止账号枚举攻击）
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ApiException(401, "账号或密码错误"));

        // 明文密码比对
        if (!user.getPassword().equals(request.getPassword())) {
            throw new ApiException(401, "账号或密码错误");
        }

        String token = authService.issueToken(user);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", toProfile(user));
        return data;
    }

    /**
     * 用户注册。
     *
     * <p>业务规则：
     * <ul>
     *   <li>两次密码输入必须一致</li>
     *   <li>用户名全局唯一（已存在则返回 409）</li>
     *   <li>昵称默认为用户名</li>
     *   <li>头像通过 DiceBear API 根据用户名种子自动生成</li>
     *   <li>默认角色为 "user"，默认签名为"专注学习，持续成长"</li>
     * </ul>
     *
     * @param request 注册请求，含 username、password、confirmPassword、nickname
     * @return 包含 token 和 userInfo 的 Map（注册即自动登录）
     * @throws ApiException 400 如果两次密码不一致
     * @throws ApiException 409 如果用户名已存在
     */
    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        // 校验两次密码一致性
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ApiException(400, "两次输入的密码不一致");
        }
        // 校验用户名唯一性
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ApiException(409, "该账号已存在");
        }

        // 构建新用户实体
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setNickname(StringUtils.hasText(request.getNickname())
                ? request.getNickname()
                : request.getUsername());  // 昵称默认取用户名
        user.setAvatar("https://api.dicebear.com/9.x/avataaars/svg?seed=" + request.getUsername());  // DiceBear 头像
        user.setRole("user");                      // 新用户默认普通角色
        user.setSignature("专注学习，持续成长");     // 默认个性签名
        user.setStudyDays(0);                      // 初始学习天数为 0
        user.setTotalHours(0);                     // 初始学习时长为 0
        userRepository.save(user);

        String token = authService.issueToken(user);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", toProfile(user));
        return data;
    }

    /**
     * 获取用户个人信息。
     *
     * @param userId 用户 ID
     * @return 用户信息的 Map 视图（ID、用户名、昵称、头像、签名、学习天数、学习时长、角色）
     * @throws ApiException 404 如果用户不存在
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getProfile(Long userId) {
        User user = getUser(userId);
        return toProfile(user);
    }

    /**
     * 更新用户个人信息（昵称、签名、头像）。
     *
     * @param userId  用户 ID
     * @param request 个人信息更新请求
     * @return 更新后的用户信息 Map 视图
     * @throws ApiException 404 如果用户不存在
     */
    @Transactional
    public Map<String, Object> updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = getUser(userId);
        user.setNickname(request.getNickname());
        user.setSignature(request.getSignature());
        user.setAvatar(request.getAvatar());
        userRepository.save(user);
        return toProfile(user);
    }

    /**
     * 按 ID 获取用户实体（内部/跨服务调用的统一入口）。
     *
     * @param userId 用户 ID
     * @return 用户实体
     * @throws ApiException 404 如果用户不存在
     */
    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "用户不存在"));
    }

    /**
     * 将用户实体转换为前端需要的个人信息 Map。
     *
     * <p>转换字段：id, username, nickname, avatar, signature, studyDays, totalHours, role。
     *
     * @param user 用户实体
     * @return 个人信息 Map
     */
    private Map<String, Object> toProfile(User user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("nickname", user.getNickname());
        profile.put("avatar", user.getAvatar());
        profile.put("signature", user.getSignature());
        profile.put("studyDays", user.getStudyDays());
        profile.put("totalHours", user.getTotalHours());
        profile.put("role", user.getRole() != null ? user.getRole() : "user");
        return profile;
    }
}
