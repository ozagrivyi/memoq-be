package com.memoq.backend.repository;

import com.memoq.backend.entity.Settings;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingsRepository extends JpaRepository<Settings, UUID> {}
