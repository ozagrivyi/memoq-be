package com.memoq.backend.service;

import com.memoq.backend.dto.DtoMapper;
import com.memoq.backend.dto.SettingsDto;
import com.memoq.backend.dto.SettingsUpdateRequest;
import com.memoq.backend.entity.Settings;
import com.memoq.backend.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SettingsService {

    private final SettingsRepository settingsRepository;

    public SettingsDto get() {
        return DtoMapper.toDto(getSingleton());
    }

    @Transactional
    public SettingsDto update(SettingsUpdateRequest request) {
        Settings settings = getSingleton();
        settings.setTimerEnabled(request.timerEnabled());
        settings.setTimerSeconds(request.timerSeconds());
        settings.setRephraseEnabled(request.rephraseEnabled());
        return DtoMapper.toDto(settingsRepository.save(settings));
    }

    /** The V5 migration seeds this row; it should always be present. */
    private Settings getSingleton() {
        return settingsRepository
                .findById(Settings.SINGLETON_ID)
                .orElseThrow(() -> new IllegalStateException("Settings row missing — check V5 migration."));
    }
}
