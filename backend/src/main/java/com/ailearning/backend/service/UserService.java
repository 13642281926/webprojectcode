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

@Service
public class UserService {
    private final UserRepository userRepository;
    private final AuthService authService;

    public UserService(UserRepository userRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ApiException(401, "账号或密码错误"));

        if (!user.getPassword().equals(request.getPassword())) {
            throw new ApiException(401, "账号或密码错误");
        }

        String token = authService.issueToken(user);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", toProfile(user));
        return data;
    }

    @Transactional
    public Map<String, Object> register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new ApiException(400, "两次输入的密码不一致");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new ApiException(409, "该账号已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        user.setNickname(StringUtils.hasText(request.getNickname())
                ? request.getNickname()
                : request.getUsername());
        user.setAvatar("https://api.dicebear.com/9.x/avataaars/svg?seed=" + request.getUsername());
        user.setSignature("专注学习，持续成长");
        user.setStudyDays(0);
        user.setTotalHours(0);
        userRepository.save(user);

        String token = authService.issueToken(user);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("userInfo", toProfile(user));
        return data;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProfile(Long userId) {
        User user = getUser(userId);
        return toProfile(user);
    }

    @Transactional
    public Map<String, Object> updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = getUser(userId);
        user.setNickname(request.getNickname());
        user.setSignature(request.getSignature());
        user.setAvatar(request.getAvatar());
        userRepository.save(user);
        return toProfile(user);
    }

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(404, "用户不存在"));
    }

    private Map<String, Object> toProfile(User user) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUsername());
        profile.put("nickname", user.getNickname());
        profile.put("avatar", user.getAvatar());
        profile.put("signature", user.getSignature());
        profile.put("studyDays", user.getStudyDays());
        profile.put("totalHours", user.getTotalHours());
        return profile;
    }
}
