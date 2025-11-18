package org.openmrs.module.lamp;

import java.util.Date;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.PatientProgram;
import org.openmrs.PatientState;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.User;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;

public class PrenatalProgramStrategy implements ProgramStrategy {
	
	public void execute(Encounter encounter, User currentUser, Date currentDate, String reason) {
		if (!isPrenatalEncounter(encounter)) {
			return;
		}
		ProgramWorkflowService programWorkflowService = Context.getProgramWorkflowService();
		Program program = programWorkflowService.getProgramByUuid(LampConfig.PROGRAM_PRENATAL_UUID);
		if (program == null) {
			return;
		}
		
		PatientProgram patientProgram = Utils.getOrCreateActiveProgramEnrollment(programWorkflowService,
		    encounter.getPatient(), program, encounter.getEncounterDatetime());
		Concept pregnancyStatusConcept = Context.getConceptService().getConceptByUuid(
		    LampConfig.CONCEPT_PRENATAL_PREGNANCY_STATUS_UUID);
		if (pregnancyStatusConcept == null) {
			return;
		}
		
		ProgramWorkflow programWorkflow = Utils.getWorkflowByUuid(program, LampConfig.WORKFLOW_PRENATAL_UUID);
		if (programWorkflow == null) {
			return;
		}
		
		Concept prenatalStatusValue = Utils.findLatestCodedObsValue(encounter, pregnancyStatusConcept);
		if (prenatalStatusValue == null) {
			return;
		}
		
		ProgramWorkflowState targetState = Utils.getStateByConcept(programWorkflow, prenatalStatusValue);
		if (targetState == null) {
			return;
		}
		
		PatientState patientState = patientProgram.getCurrentState(programWorkflow);
		if (patientState != null && patientState.getState() != null
		        && patientState.getState().getConcept().getUuid().equals(targetState.getConcept().getUuid())) {
			return;
		}
		
		Utils.updateProgram(patientProgram, encounter, currentDate, targetState);
		patientProgram.setLocation(encounter.getLocation());
		programWorkflowService.savePatientProgram(patientProgram);
	}
	
	private boolean isPrenatalEncounter(Encounter encounter) {
		return encounter != null && encounter.getEncounterType() != null
		        && LampConfig.PRENATAL_ENCOUNTER_TYPE_UUID.equals(encounter.getEncounterType().getUuid());
	}
}
