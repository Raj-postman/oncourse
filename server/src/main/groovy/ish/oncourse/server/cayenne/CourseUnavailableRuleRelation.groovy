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

package ish.oncourse.server.cayenne

import ish.oncourse.server.cayenne.glue.CayenneDataObject
import ish.oncourse.server.cayenne.glue._CourseUnavailableRuleRelation

/**
 * A persistent class mapped as "CourseUnavailableRuleRelation" Cayenne entity.
 */
class CourseUnavailableRuleRelation extends _CourseUnavailableRuleRelation {



	@Override
	void setRelatedObject(CayenneDataObject relatedObject) {
		setCourse((Course) relatedObject)
	}
}
