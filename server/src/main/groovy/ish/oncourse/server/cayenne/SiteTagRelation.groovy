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
import ish.oncourse.server.cayenne.glue._SiteTagRelation

import javax.annotation.Nonnull

/**
 * A persistent class mapped as "SiteTagRelation" Cayenne entity.
 */
class SiteTagRelation extends _SiteTagRelation {



	/**
	 * @see TagRelation#getTaggableClassesIdentifier()
	 */
	@Nonnull
	@Override
	TaggableClasses getTaggableClassesIdentifier() {
		return TaggableClasses.SITE
	}

	/**
	 * @see TagRelation#getTaggedRelation()
	 */
	@Nonnull
	@Override
	Taggable getTaggedRelation() {
		return super.getTaggedSite()
	}

	/**
	 * @see TagRelation#setTaggedRelation(Taggable)
	 */
	@Override
	void setTaggedRelation(Taggable object) {
		setTaggedSite((Site) object)
	}
}
