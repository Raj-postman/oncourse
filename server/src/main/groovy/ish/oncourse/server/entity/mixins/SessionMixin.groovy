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

package ish.oncourse.server.entity.mixins

import ish.oncourse.API
import ish.oncourse.server.cayenne.Session

import java.time.LocalDateTime

class SessionMixin  {


	/**
	 * @return JSR-310 compatible start date time of the session
	 */
	@API
	static LocalDateTime getStart(Session self) {
		return self.getStartDatetime().toInstant().atZone(self.getTimeZone().toZoneId()).toLocalDateTime()
	}

	/**
	 * @return JSR-310 compatible end date time of the session
	 */
	@API
	static LocalDateTime getEnd(Session self) {
		return self.getEndDatetime().toInstant().atZone(self.getTimeZone().toZoneId()).toLocalDateTime()
	}

}
