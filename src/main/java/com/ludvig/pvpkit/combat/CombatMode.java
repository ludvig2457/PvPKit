package com.ludvig.pvpkit.combat;

import java.util.Locale;

public enum CombatMode {

    LEGACY_1_8_9("1.8.9", "&e1.8.9", false, false, false),
    LEGACY_1_12("1.12", "&61.12", true, false, true),
    MODERN_1_21_11("1.21.11", "&b1.21.11", true, true, true);

    private final String id;
    private final String coloredName;
    private final boolean attackCooldown;
    private final boolean sweepAttack;
    private final boolean shields;

    CombatMode(String id, String coloredName, boolean attackCooldown, boolean sweepAttack, boolean shields) {
        this.id = id;
        this.coloredName = coloredName;
        this.attackCooldown = attackCooldown;
        this.sweepAttack = sweepAttack;
        this.shields = shields;
    }

    public String id() {
        return id;
    }

    public String coloredName() {
        return coloredName;
    }

    public boolean attackCooldown() {
        return attackCooldown;
    }

    public boolean sweepAttack() {
        return sweepAttack;
    }

    public boolean shields() {
        return shields;
    }

    public static CombatMode parse(String input) {
        if (input == null) {
            return MODERN_1_21_11;
        }

        String value = input.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(".", "");
        return switch (value) {
            case "18", "189", "legacy18", "legacy189", "legacy" -> LEGACY_1_8_9;
            case "112", "1122", "legacy112" -> LEGACY_1_12;
            case "121", "12111", "modern", "current" -> MODERN_1_21_11;
            default -> MODERN_1_21_11;
        };
    }

    public static boolean isValid(String input) {
        if (input == null) {
            return false;
        }
        String value = input.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(".", "");
        return value.equals("18") || value.equals("189") || value.equals("legacy18")
                || value.equals("legacy189") || value.equals("legacy")
                || value.equals("112") || value.equals("1122") || value.equals("legacy112")
                || value.equals("121") || value.equals("12111") || value.equals("modern")
                || value.equals("current");
    }
}
