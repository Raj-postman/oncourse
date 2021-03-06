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

package ish.util;

import ish.common.types.ExpiryType;
import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.Date;

public class ProductUtil {

	public static Date calculateExpiryDate(Date purchaseDate, ExpiryType expiryType, Integer days) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(purchaseDate);
		cal = DateUtils.truncate(cal, Calendar.DATE);

		if (ExpiryType.DAYS.equals(expiryType)) {
			cal.add(Calendar.DATE, days);
		} else if (ExpiryType.FIRST_JANUARY.equals(expiryType)) {
			cal.set(Calendar.MONTH, Calendar.JANUARY);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			cal.add(Calendar.YEAR, 1);
		} else if (ExpiryType.FIRST_JULY.equals(expiryType)) {
			cal.set(Calendar.MONTH, Calendar.JULY);
			cal.set(Calendar.DAY_OF_MONTH, 1);
			if (cal.get(Calendar.MONTH) >= Calendar.JULY) {
				cal.add(Calendar.YEAR, 1);
			}
		} else if (ExpiryType.LIFETIME.equals(expiryType)) {
			cal.add(Calendar.YEAR, 100);
		}

		return cal.getTime();
	}

	public static Date getToday() {
		return DateTimeUtil.trancateToMidnight(Calendar.getInstance().getTime());
	}

}
