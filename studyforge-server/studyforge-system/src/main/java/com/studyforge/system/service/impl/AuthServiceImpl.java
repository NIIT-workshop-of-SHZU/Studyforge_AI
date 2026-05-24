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
import com.studyforge.system.mapper.UserTokenMapper;
import com.studyforge.system.service.AuthService;
import com.studyforge.system.vo.LoginVO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {
    private static final String TOKEN_PREFIX = "Bearer ";

    private final UserMapper userMapper;
    private final UserTokenMapper userTokenMapper;

    public AuthServiceImpl(UserMapper userMapper, UserTokenMapper userTokenMapper) {
        this.userMapper = userMapper;
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

        return new LoginVO(token.getAccessToken(), user.getUserId(), user.getUsername(), user.getRole());
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
}
