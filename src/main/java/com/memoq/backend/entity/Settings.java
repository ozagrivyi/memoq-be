package com.memoq.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Single-admin tool (see CLAUDE.md) — one global row, always addressed by {@link #SINGLETON_ID}. */
@Entity
@Table(name = "settings")
@Getter
@Setter
@NoArgsConstructor
public class Settings {

    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Id
    private UUID id;

    @Column(name = "timer_enabled", nullable = false)
    private boolean timerEnabled;

    @Column(name = "timer_seconds", nullable = false)
    private int timerSeconds;

    @Column(name = "rephrase_enabled", nullable = false)
    private boolean rephraseEnabled;
}
