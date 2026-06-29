package dev.soranzo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class XpFractionalDisplayTest {

    private static final double DELTA = 0.01;

    // Fórmula Minecraft: XP por nível
    // Nível 0-15:  2*level + 7
    // Nível 16-30: 5*level - 38
    // Nível 31+:   9*level - 158

    @Test
    void fractionalLevels_lowLevel_higherFraction() {
        int costXp = 50;
        int xpPerLevel = 27; // nível 10: 2*10+7 = 27
        double fractional = (double) costXp / xpPerLevel;
        assertEquals(1.85, fractional, DELTA);
    }

    @Test
    void fractionalLevels_midLevel() {
        int costXp = 50;
        int xpPerLevel = 62; // nível 20: 5*20-38 = 62
        double fractional = (double) costXp / xpPerLevel;
        assertEquals(0.81, fractional, DELTA);
    }

    @Test
    void fractionalLevels_highLevel_lowerFraction() {
        int costXp = 50;
        int xpPerLevel = 112; // nível 30: 5*30-38 = 112
        double fractional = (double) costXp / xpPerLevel;
        assertEquals(0.45, fractional, DELTA);
    }

    @Test
    void fractionalLevels_zeroCost_isZero() {
        int costXp = 0;
        int xpPerLevel = 62;
        double fractional = (double) costXp / xpPerLevel;
        assertEquals(0.0, fractional, DELTA);
    }

    @Test
    void sameCostIsLowerFractionAtHigherLevels() {
        int costXp = 100;
        double fractionalLvl10 = (double) costXp / 27;  // nível 10
        double fractionalLvl30 = (double) costXp / 112; // nível 30

        // Jogador de nível alto paga proporcionalmente menos
        assertEquals(true, fractionalLvl10 > fractionalLvl30);
    }
}
