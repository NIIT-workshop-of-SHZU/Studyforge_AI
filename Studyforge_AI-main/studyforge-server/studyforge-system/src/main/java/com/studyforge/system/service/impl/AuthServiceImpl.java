package com.studyforge.system.service.impl;

import com.studyforge.common.enums.UserStatus;
import com.studyforge.common.enums.RoleType;
import com.studyforge.common.exception.BizException;
import com.studyforge.common.exception.ErrorCode;
import com.studyforge.system.dto.LoginRequest;
import com.studyforge.system.dto.RegisterRequest;
import com.studyforge.system.entity.User;
import com.studyforge.system.entity.UserToken;
import com.studyforge.system.mapper.UserMapper;
import com.studyforge.system.mapper.UserExperienceMapper;
import com.studyforge.system.mapper.UserTokenMapper;
import com.studyforge.system.service.AuthService;
import com.studyforge.system.vo.LoginVO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final int DAILY_LOGIN_EXPERIENCE = 15;

    private final UserMapper userMapper;
    private final UserExperienceMapper userExperienceMapper;
    private final UserTokenMapper userTokenMapper;

    public AuthServiceImpl(UserMapper userMapper, UserExperienceMapper userExperienceMapper, UserTokenMapper userTokenMapper) {
        this.userMapper = userMapper;
        this.userExperienceMapper = userExperienceMapper;
        this.userTokenMapper = userTokenMapper;
    }

    @Override
    @Transactional
    public LoginVO login(LoginRequest request) {
        if (request == null || isBlank(request.account()) || isBlank(request.password())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "account and password are required");
        }

        User user = userMapper.selectByAccount(request.account().trim());
        if (user == null || !UserStatus.ACTIVE.equals(user.getStatus()) || !verifyPassword(request.password(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "account or password is incorrect");
        }

        UserToken token = new UserToken();
        token.setUserId(user.getUserId());
        token.setAccessToken(newAccessToken());
        token.setExpireTime(LocalDateTime.now().plusDays(7));
        token.setStatus("ACTIVE");
        userTokenMapper.insert(token);

        boolean rewarded = rewardDailyLoginIfNeeded(user);
        User refreshed = userMapper.selectById(user.getUserId());
        User loginUser = refreshed == null ? user : refreshed;

        return new LoginVO(
                token.getAccessToken(),
                loginUser.getUserId(),
                loginUser.getUsername(),
                displayName(loginUser),
                loginUser.getRole(),
                safeInt(loginUser.getCommunityLevel(), 1),
                safeInt(loginUser.getExperiencePoints(), 0),
                rewarded,
                rewarded ? DAILY_LOGIN_EXPERIENCE : 0
        );
    }

    @Override
    @Transactional
    public Long register(RegisterRequest request) {
        if (request == null || isBlank(request.username()) || isBlank(request.email()) || isBlank(request.password())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "username, email and password are required");
        }
        if (userMapper.selectByAccount(request.username().trim()) != null || userMapper.selectByAccount(request.email().trim()) != null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "account already exists");
        }

        User user = new User();
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim());
        user.setPasswordHash(hashPassword(request.password()));
        user.setRole(RoleType.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setDisplayName(request.username().trim());
        user.setBio("刚加入 StudyForge AI，正在整理自己的学习路径。");
        user.setCommunityLevel(1);
        user.setExperiencePoints(0);
        user.setReputationScore(0);
        userMapper.insert(user);
        return user.getUserId();
    }

    @Override
    @Transactional
    public void logout(String accessToken) {
        String token = normalizeToken(accessToken);
        if (!token.isBlank()) {
            userTokenMapper.updateStatusByToken(token, "REVOKED");
        }
    }

    @Override
    public User requireUser(String authorization) {
        String tokenValue = normalizeToken(authorization);
        if (tokenValue.isBlank()) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }

        UserToken token = userTokenMapper.selectByToken(tokenValue);
        if (token == null || !"ACTIVE".equals(token.getStatus()) || token.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "login has expired");
        }

        User user = userMapper.selectById(token.getUserId());
        if (user == null || !UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    @Override
    public Long currentUserId(String authorization) {
        String tokenValue = normalizeToken(authorization);
        if (tokenValue.isBlank()) {
            return null;
        }

        UserToken token = userTokenMapper.selectByToken(tokenValue);
        if (token == null || !"ACTIVE".equals(token.getStatus()) || token.getExpireTime().isBefore(LocalDateTime.now())) {
            return null;
        }

        User user = userMapper.selectById(token.getUserId());
        if (user == null || !UserStatus.ACTIVE.equals(user.getStatus())) {
            return null;
        }
        return user.getUserId();
    }

    @Override
    public Long requireUserId(String authorization) {
        return requireUser(authorization).getUserId();
    }

    @Override
    public void requireAdmin(String authorization) {
        User user = requireUser(authorization);
        if (!RoleType.ADMIN.equals(user.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "admin permission is required");
        }
    }

    private String normalizeToken(String authorization) {
        if (authorization == null) {
            return "";
        }
        String token = authorization.trim();
        if (token.regionMatches(true, 0, TOKEN_PREFIX, 0, TOKEN_PREFIX.length())) {
            return token.substring(TOKEN_PREFIX.length()).trim();
        }
        return token;
    }

    private boolean rewardDailyLoginIfNeeded(User user) {
        LocalDate today = LocalDate.now();
        if (today.equals(user.getLastLoginRewardDate())) {
            return false;
        }

        boolean updated = userMapper.rewardDailyLogin(user.getUserId(), today, DAILY_LOGIN_EXPERIENCE) > 0;
        if (updated) {
            userExperienceMapper.insertIgnore(user.getUserId(), "DAILY_LOGIN", DAILY_LOGIN_EXPERIENCE, null, today);
        }
        return updated;
    }

    private String newAccessToken() {
        return "sf_" + HexFormat.of().formatHex(secureBytes(LocalDateTime.now().toString() + Math.random()));
    }

    private byte[] secureBytes(String seed) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest((seed + System.nanoTime()).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private boolean verifyPassword(String rawPassword, String storedHash) {
        return hashPassword(rawPassword).equals(storedHash);
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return "sha256:" + HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String displayName(User user) {
        return isBlank(user.getDisplayName()) ? user.getUsername() : user.getDisplayName();
    }

    private int safeInt(Integer value, int fallback) {
        return value == null ? fallback : value;
    }
}
