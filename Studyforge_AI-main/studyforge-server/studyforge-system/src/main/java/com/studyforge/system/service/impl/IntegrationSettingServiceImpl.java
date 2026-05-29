package com.studyforge.system.service.impl;

import com.studyforge.common.exception.BizException;
import com.studyforge.common.exception.ErrorCode;
import com.studyforge.system.dto.IntegrationSettingUpdateRequest;
import com.studyforge.system.entity.IntegrationSetting;
import com.studyforge.system.mapper.IntegrationSettingMapper;
import com.studyforge.system.service.IntegrationSettingService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class IntegrationSettingServiceImpl implements IntegrationSettingService {
    private final IntegrationSettingMapper integrationSettingMapper;

    public IntegrationSettingServiceImpl(IntegrationSettingMapper integrationSettingMapper) {
        this.integrationSettingMapper = integrationSettingMapper;
    }

    @Override
    public String getValue(String settingKey, String defaultValue) {
        IntegrationSetting setting = integrationSettingMapper.selectByKey(settingKey);
        if (setting == null || setting.getSettingValue() == null || setting.getSettingValue().isBlank()) {
            return defaultValue;
        }
        return setting.getSettingValue();
    }

    @Override
    public Map<String, String> getValues(String prefix) {
        Map<String, String> values = new LinkedHashMap<>();
        for (IntegrationSetting setting : integrationSettingMapper.selectAll()) {
            if (prefix == null || setting.getSettingKey().startsWith(prefix)) {
                values.put(setting.getSettingKey(), setting.getSettingValue());
            }
        }
        return values;
    }

    @Override
    public List<IntegrationSetting> list(boolean maskSecrets) {
        return integrationSettingMapper.selectAll()
                .stream()
                .map(setting -> maskSecrets && Integer.valueOf(1).equals(setting.getSecretFlag()) ? masked(setting) : setting)
                .toList();
    }

    @Override
    public void save(IntegrationSettingUpdateRequest request, Long adminId) {
        if (request == null || isBlank(request.settingKey())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "settingKey is required");
        }

        IntegrationSetting setting = new IntegrationSetting();
        setting.setSettingKey(request.settingKey().trim());
        setting.setSecretFlag(request.secretFlag() == null ? 0 : request.secretFlag());
        setting.setSettingValue(resolveSettingValue(setting.getSettingKey(), request.settingValue(), setting.getSecretFlag()));
        setting.setUpdatedBy(adminId);
        integrationSettingMapper.upsert(setting);
    }

    private String resolveSettingValue(String settingKey, String requestedValue, Integer secretFlag) {
        String nextValue = requestedValue == null ? "" : requestedValue.trim();
        if (!Integer.valueOf(1).equals(secretFlag) || !looksMasked(nextValue)) {
            return nextValue;
        }

        IntegrationSetting existing = integrationSettingMapper.selectByKey(settingKey);
        return existing == null ? "" : existing.getSettingValue();
    }

    private IntegrationSetting masked(IntegrationSetting source) {
        IntegrationSetting setting = new IntegrationSetting();
        setting.setSettingKey(source.getSettingKey());
        setting.setSettingValue(mask(source.getSettingValue()));
        setting.setSecretFlag(source.getSecretFlag());
        setting.setUpdatedBy(source.getUpdatedBy());
        setting.setCreatedTime(source.getCreatedTime());
        setting.setUpdatedTime(source.getUpdatedTime());
        return setting;
    }

    private String mask(String value) {
        if (value == null || value.length() <= 8) {
            return "********";
        }
        return value.substring(0, 4) + "..." + value.substring(value.length() - 4);
    }

    private boolean looksMasked(String value) {
        return value.contains("...") || value.chars().allMatch(character -> character == '*');
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
