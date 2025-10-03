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
import org.openmrs.api.ProgramWorkflowService;

public class Utils {
	
	public static PatientProgram getOrCreateActiveProgramEnrollment(ProgramWorkflowService programWorkflowService,
	        Patient patient, Program program, Date enrolledOn) {
		PatientProgram activePatientProgram = getActiveProgramEnrollment(programWorkflowService, patient, program);
		if (activePatientProgram != null) {
			return activePatientProgram;
		}
		PatientProgram patientProgram = new PatientProgram();
		patientProgram.setPatient(patient);
		patientProgram.setProgram(program);
		patientProgram.setDateEnrolled(enrolledOn);
		return programWorkflowService.savePatientProgram(patientProgram);
	}
	
	public static PatientProgram getActiveProgramEnrollment(ProgramWorkflowService programWorkflowService, Patient patient,
	        Program program) {
		for (PatientProgram patientProgram : programWorkflowService.getPatientPrograms(patient, program, null, null, null,
		    null, false)) {
			if (patientProgram.getDateCompleted() == null && patientProgram.getVoided().equals(Boolean.FALSE)) {
				return patientProgram;
			}
		}
		return null;
	}
	
	public static Concept findLatestCodedObsValue(Encounter encounter, Concept questionConcept) {
		Concept result = null;
		Date latest = null;
		for (Obs obs : encounter.getAllObs(false)) {
			if (questionConcept.equals(obs.getConcept()) && obs.getValueCoded() != null) {
				Date obsDate = obs.getObsDatetime();
				if (latest == null || (obsDate != null && obsDate.after(latest))) {
					latest = obsDate;
					result = obs.getValueCoded();
				}
			}
		}
		return result;
	}
	
	public static ProgramWorkflow getWorkflowByUuid(Program program, String workflowUuid) {
		for (ProgramWorkflow programWorkflow : program.getWorkflows()) {
			if (programWorkflow != null && workflowUuid.equals(programWorkflow.getUuid())) {
				return programWorkflow;
			}
		}
		return null;
	}
	
	public static ProgramWorkflowState getStateByConcept(ProgramWorkflow programWorkflow, Concept concept) {
		for (ProgramWorkflowState programWorkflowState : programWorkflow.getStates()) {
			if (concept.equals(programWorkflowState.getConcept())) {
				return programWorkflowState;
			}
		}
		return null;
	}
	
	public static Date getProgramStatusUpdateDate(PatientProgram patientProgram, Encounter encounter, Date currentDate) {
		Date programStatusUpdateDate;
		Date enrolled = patientProgram.getDateEnrolled();
		if (enrolled != null && encounter.getEncounterDatetime() != null
		        && encounter.getEncounterDatetime().before(enrolled)) {
			programStatusUpdateDate = enrolled;
		} else if (encounter.getEncounterDatetime() != null) {
			programStatusUpdateDate = encounter.getEncounterDatetime();
		} else {
			programStatusUpdateDate = currentDate;
		}
		return programStatusUpdateDate;
	}
	
	public static void updateProgram(PatientProgram patientProgram, Encounter encounter, Date currentDate,
	        ProgramWorkflowState targetState) {
		Date programStatusUpdateDate = getProgramStatusUpdateDate(patientProgram, encounter, currentDate);
		
		if (targetState.getTerminal()) {
			patientProgram.setDateCompleted(new Date());
		}
		patientProgram.transitionToState(targetState, programStatusUpdateDate);
	}
}
