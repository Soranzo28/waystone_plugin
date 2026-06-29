package dev.soranzo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TeleportCostXpTest {

    @Test
    void scale20_500blocks() {
        assertEquals(25, WaystoneManager.calculateCostXp(500, 20));
    }

    @Test
    void scale20_1000blocks() {
        assertEquals(50, WaystoneManager.calculateCostXp(1000, 20));
    }

    @Test
    void scale20_2000blocks() {
        assertEquals(100, WaystoneManager.calculateCostXp(2000, 20));
    }

    @Test
    void scale20_3000blocks() {
        assertEquals(150, WaystoneManager.calculateCostXp(3000, 20));
    }

    @Test
    void scale20_5000blocks() {
        assertEquals(250, WaystoneManager.calculateCostXp(5000, 20));
    }

    @Test
    void zeroDistance_isFree() {
        assertEquals(0, WaystoneManager.calculateCostXp(0, 20));
    }

    @Test
    void customScale_affectsCost() {
        // scale=10 → 1000/10 = 100
        assertEquals(100, WaystoneManager.calculateCostXp(1000, 10));
    }

    @Test
    void costIsLinear_doublDistanceDoubleCost() {
        int cost1000 = WaystoneManager.calculateCostXp(1000, 20);
        int cost2000 = WaystoneManager.calculateCostXp(2000, 20);
        assertEquals(cost1000 * 2, cost2000);
    }
}
