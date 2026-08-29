package com.memoq.backend.controller;

import com.memoq.backend.dto.SettingsDto;
import com.memoq.backend.dto.SettingsUpdateRequest;
import com.memoq.backend.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    public SettingsDto get() {
        return settingsService.get();
    }

    /** Full replace — the client always sends every field together. */
    @PutMapping
    public SettingsDto update(@Valid @RequestBody SettingsUpdateRequest request) {
        return settingsService.update(request);
    }
}
