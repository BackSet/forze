package com.backset.forze.module.budgeting.budget.application;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centralized rounding policy for the budgeting financial pipeline. Every money
 * amount, unit cost/price and ratio (margin, yield divides) routes through here
 * so scale and rounding mode are defined in one place instead of being repeated
 * as magic numbers across the calculation services.
 *
 * <ul>
 *   <li>{@code MONEY_SCALE} (2): totals and line amounts in currency.</li>
 *   <li>{@code UNIT_SCALE} (4): unit cost/price, yields and ratios (e.g. margin).</li>
 * </ul>
 * Mode is always {@link RoundingMode#HALF_UP}.
 */
public final class BudgetRounding {

	public static final int MONEY_SCALE = 2;
	public static final int UNIT_SCALE = 4;
	public static final RoundingMode MODE = RoundingMode.HALF_UP;

	private BudgetRounding() {
	}

	/** Rounds a currency amount to {@link #MONEY_SCALE} (2) decimals. */
	public static BigDecimal money(BigDecimal value) {
		return value.setScale(MONEY_SCALE, MODE);
	}

	/** Rounds a unit cost/price or yield to {@link #UNIT_SCALE} (4) decimals. */
	public static BigDecimal unit(BigDecimal value) {
		return value.setScale(UNIT_SCALE, MODE);
	}

	/** Divides at {@link #UNIT_SCALE} (4) decimals — used for yield divides and ratios such as margin. */
	public static BigDecimal divideUnit(BigDecimal dividend, BigDecimal divisor) {
		return dividend.divide(divisor, UNIT_SCALE, MODE);
	}
}
