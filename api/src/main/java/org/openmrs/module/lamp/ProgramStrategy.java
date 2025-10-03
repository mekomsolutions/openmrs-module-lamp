package org.openmrs.module.lamp;

import java.util.Date;
import org.openmrs.Encounter;
import org.openmrs.User;

public interface ProgramStrategy {
	
	void execute(Encounter encounter, User currentUser, Date currentDate, String reason);
}
