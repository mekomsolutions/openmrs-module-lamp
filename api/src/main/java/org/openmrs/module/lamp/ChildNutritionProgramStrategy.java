package org.openmrs.module.lamp;

import java.util.Date;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.PatientProgram;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.User;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;

public class ChildNutritionProgramStrategy implements ProgramStrategy {
	
	public void execute(Encounter encounter, User currentUser, Date currentDate, String reason) {
		if (!isChildNutritionEncounter(encounter)) {
			return;
		}
		ProgramWorkflowService programWorkflowService = Context.getProgramWorkflowService();
		Program program = programWorkflowService.getProgramByUuid(LampConfig.PROGRAM_CHILD_NUTRITION_UUID);
		if (program == null) {
			return;
		}
		
		PatientProgram patientProgram = Utils.getOrCreateActiveProgramEnrollment(programWorkflowService,
		    encounter.getPatient(), program, encounter.getEncounterDatetime());
		Concept malnutritionStatusConcept = Context.getConceptService().getConceptByUuid(
		    LampConfig.CONCEPT_CHILD_NUTRITION_MALNUTRITION_STATUS_UUID);
		if (malnutritionStatusConcept == null) {
			return;
		}
		
		Concept reasonForDischargeConcept = Context.getConceptService().getConceptByUuid(
		    LampConfig.CONCEPT_CHILD_NUTRITION_REASON_FOR_DISCHARGE_UUID);
		if (reasonForDischargeConcept == null) {
			return;
		}
		
		Concept malnutritionStatusValue = Utils.findLatestCodedObsValue(encounter, malnutritionStatusConcept);
		Concept reasonForDischargeValue = Utils.findLatestCodedObsValue(encounter, reasonForDischargeConcept);
		if (malnutritionStatusValue == null && reasonForDischargeValue == null) {
			return;
		}
		
		ProgramWorkflow programWorkflow = Utils.getWorkflowByUuid(program, LampConfig.WORKFLOW_CHILD_NUTRITION_UUID);
		if (programWorkflow == null) {
			return;
		}
		
		ProgramWorkflowState targetState = null;
		boolean isReachedTargetGoalWeightStateFromMalnutritionStatusValue = false;
		
		if (malnutritionStatusValue != null) {
			targetState = Utils.getStateByConcept(programWorkflow, malnutritionStatusValue);
			if (targetState == null) {
				return;
			}
			if (targetState.getConcept().getUuid().equalsIgnoreCase(LampConfig.CONCEPT_REACHED_TARGET_GOAL_WEIGHT_UUID)) {
				isReachedTargetGoalWeightStateFromMalnutritionStatusValue = true;
			}
		}
		
		if (reasonForDischargeValue != null) {
			targetState = Utils.getStateByConcept(programWorkflow, reasonForDischargeValue);
			if (targetState == null) {
				return;
			}
			if (targetState.getConcept().getUuid().equalsIgnoreCase(LampConfig.CONCEPT_REACHED_TARGET_GOAL_WEIGHT_UUID)) {
				isReachedTargetGoalWeightStateFromMalnutritionStatusValue = false;
			}
		}
		
		if (isReachedTargetGoalWeightStateFromMalnutritionStatusValue) {
			targetState.setTerminal(false);
		}
		
		Utils.updateProgram(patientProgram, encounter, currentDate, targetState);
		patientProgram.setLocation(encounter.getLocation());
		programWorkflowService.savePatientProgram(patientProgram);
	}
	
	private boolean isChildNutritionEncounter(Encounter encounter) {
		return encounter != null && encounter.getEncounterType() != null
		        && LampConfig.CHILD_NUTRITION_ENCOUNTER_TYPE_UUID.equals(encounter.getEncounterType().getUuid());
	}
}
