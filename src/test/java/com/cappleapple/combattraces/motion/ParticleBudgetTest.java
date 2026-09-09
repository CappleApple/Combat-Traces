package com.cappleapple.combattraces.motion;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ParticleBudgetTest {
  @Test
  void frameCapAppliesAcrossMultipleSources() {
    var budget = new ParticleBudget();
    budget.beginFrame(10, 200, 24);
    assertEquals(20, budget.take(20));
    assertEquals(4, budget.take(20));
    assertEquals(0, budget.take(1));
  }

  @Test
  void rollingSecondCannotBeBypassedByChangingFrames() {
    var budget = new ParticleBudget();
    budget.beginFrame(10, 10, 10);
    assertEquals(10, budget.take(99));
    budget.beginFrame(10.99, 10, 10);
    assertEquals(0, budget.take(99));
    budget.beginFrame(11.01, 10, 10);
    assertEquals(10, budget.take(99));
  }

  @Test
  void reducingBudgetDoesNotSpendOldAllowance() {
    var budget = new ParticleBudget();
    budget.beginFrame(10, 100, 100);
    assertEquals(90, budget.take(90));
    budget.beginFrame(10.1, 20, 20);
    assertEquals(0, budget.take(20));
  }

  @Test
  void zeroBudgetAndResetAreSafe() {
    var budget = new ParticleBudget();
    budget.beginFrame(0, 0, 20);
    assertEquals(0, budget.take(20));
    budget.clear();
    budget.beginFrame(0, 20, 0);
    assertEquals(0, budget.take(20));
    budget.beginFrame(1, 20, 20);
    assertEquals(20, budget.take(20));
  }

  @Test
  void fixedRingHandlesLongRunningCombat() {
    var budget = new ParticleBudget();
    for (int frame = 0; frame < 5000; frame++) {
      budget.beginFrame(frame * 0.1, 2000, 128);
      assertTrue(budget.take(100000) <= 128);
    }
  }
}
