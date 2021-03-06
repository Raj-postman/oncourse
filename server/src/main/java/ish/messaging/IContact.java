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
package ish.messaging;

import ish.oncourse.cayenne.ContactInterface;
import ish.oncourse.cayenne.PersistentObjectI;

import java.time.LocalDate;

/**
 */
public interface IContact extends ContactInterface, PersistentObjectI {

	String FULL_NAME_KEY = "fullName";
	String IS_MALE_KEY = "isMale";
	String PHONES_PROP = "phones";
	String MESSAGE_KEY = "message";

    String getFullName();

	String getMiddleName();

	LocalDate getBirthDate();

	String getHomePhone();

	String getWorkPhone();

	String getState();

	String getMessage();

	String getAbn();

	String getTfn();
}
