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

import ish.oncourse.cayenne.Taggable
import ish.oncourse.cayenne.TaggableClasses
import ish.oncourse.server.cayenne.glue._ApplicationTagRelation

import javax.annotation.Nonnull

class ApplicationTagRelation extends _ApplicationTagRelation {



	@Nonnull
	@Override
	TaggableClasses getTaggableClassesIdentifier() {
		return TaggableClasses.APPLICATION
	}

	@Nonnull
	@Override
	Taggable getTaggedRelation() {
		return super.getTaggedApplication()
	}

	@Override
	void setTaggedRelation(Taggable object) {
		super.setTaggedApplication((Application) object)
	}
}



