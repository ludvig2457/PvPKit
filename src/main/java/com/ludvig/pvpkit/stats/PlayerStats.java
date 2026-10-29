package com.ludvig.pvpkit.stats;

import java.util.UUID;

public final class PlayerStats {

    private final UUID uuid;
    private String name;
    private int kills;
    private int deaths;
    private int streak;
    private int bestStreak;

    public PlayerStats(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
    }

    public int kills() {
        return kills;
    }

    public void kills(int kills) {
        this.kills = Math.max(0, kills);
    }

    public int deaths() {
        return deaths;
    }

    public void deaths(int deaths) {
        this.deaths = Math.max(0, deaths);
    }

    public int streak() {
        return streak;
    }

    public void streak(int streak) {
        this.streak = Math.max(0, streak);
        if (this.streak > bestStreak) {
            bestStreak = this.streak;
        }
    }

    public int bestStreak() {
        return bestStreak;
    }

    public void bestStreak(int bestStreak) {
        this.bestStreak = Math.max(0, bestStreak);
    }

    public double kdr() {
        return deaths == 0 ? kills : (double) kills / deaths;
    }

    public void addKill() {
        kills++;
        streak(streak + 1);
    }

    public void addDeath() {
        deaths++;
        streak = 0;
    }
}
