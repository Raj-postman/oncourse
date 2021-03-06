/*
 * Copyright ish group pty ltd 2020.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 */
package ish.oncourse.server.payroll;

import ish.math.Money;
import ish.oncourse.server.cayenne.ClassCost;
import ish.oncourse.server.cayenne.PayLine;
import ish.util.LocalDateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CreatePaylinesFixed extends AbstractPaylinesCreator {
	private static final Logger logger = LogManager.getLogger(CreatePaylinesFixed.class);


	private CreatePaylinesFixed(ClassCost classCost) {
		super(classCost);
	}

	public static CreatePaylinesFixed valueOf(ClassCost classCost) {
		return new CreatePaylinesFixed(classCost);
	}

	/**
	 * Create single payline for classCost with FIXED repetition type. Budgeted quantity for such line is always ONE.
	 * Only perform if no existing lines and pay rate amount is greater than zero.
	 * @return
	 */
	@Override
	public List<PayLine> createLines() {
		List<PayLine> payLines = new ArrayList<>();

		var eligibleRate = getPerUnitAmountExTax(classCost.getCourseClass().getStartDateTime());

		logger.entry();
		if (classCost.getPaylines().size() == 0 && eligibleRate != null
				&& eligibleRate.isGreaterThan(Money.ZERO)) {

			var pl = classCost.getObjectContext().newObject(PayLine.class);
			logger.debug("fixed payline {} ... {}", pl.hashCode(), eligibleRate);
			pl.setCreatedOn(new Date());
			pl.setModifiedOn(new Date());

			pl.setClassCost(classCost);
			pl.setDescription(classCost.getDescription());
			pl.setQuantity(BigDecimal.ONE);
			pl.setBudgetedQuantity(BigDecimal.ONE);

			pl.setValue(eligibleRate);
			pl.setBudgetedValue(eligibleRate);

			var dateFor = classCost.getCourseClass().getStartDateTime();
			if (dateFor == null) {
				dateFor = classCost.getCreatedOn();
			}
			pl.setDateFor(LocalDateUtils.dateToValue(dateFor));
			payLines.add(pl);
		}
		return payLines;
	}
}
