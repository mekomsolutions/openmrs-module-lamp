/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmrs.module.lamp;

import java.util.Date;

import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.User;
import org.openmrs.annotation.Handler;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.api.handler.SaveHandler;

@Handler(supports = Encounter.class)
public class LampEncounterSaveHandler implements SaveHandler<Encounter> {
	
	@Override
	public void handle(Encounter encounter, User currentUser, Date currentDate, String reason) {
		ProgramStrategy childNutritionProgramStrategy = new ChildNutritionProgramStrategy();
		ProgramStrategy prenatalProgramStrategy = new PrenatalProgramStrategy();
		
		childNutritionProgramStrategy.execute(encounter, currentUser, currentDate, reason);
		prenatalProgramStrategy.execute(encounter, currentUser, currentDate, reason);
	}
}
