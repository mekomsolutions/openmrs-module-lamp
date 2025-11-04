package org.openmrs.module.lamp.scheduler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.PatientProgram;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.module.lamp.LampConfig;
import static org.openmrs.module.lamp.LampConfig.PROGRAM_CHILD_NUTRITION_UUID;
import static org.openmrs.module.lamp.LampConfig.PROGRAM_PRENATAL_UUID;
import org.openmrs.module.lamp.Utils;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component
public class CompleteProgramsTask extends AbstractTask {
	
	private static Log log = LogFactory.getLog(CompleteProgramsTask.class);
	
	@Override
	public void execute() {
		log.info("Executing CompletePrograms Task");
		try {
			ProgramWorkflowService programWorkflowService = Context.getProgramWorkflowService();
			
			Program childNutrition = programWorkflowService.getProgramByUuid(PROGRAM_CHILD_NUTRITION_UUID);
			Program prenatal = programWorkflowService.getProgramByUuid(LampConfig.PROGRAM_PRENATAL_UUID);
			
			if (childNutrition != null) {
				completeProgramsStartedBefore(programWorkflowService, childNutrition, getThresholdDateWeeksAgo(18));
			}
			if (prenatal != null) {
				completeProgramsStartedBefore(programWorkflowService, prenatal, getThresholdDateWeeksAgo(44));
			}
		}
		catch (Exception e) {
			log.error("Error while completing old program enrollments", e);
		}
	}
	
	@Override
	public void shutdown() {
		log.debug("Shutting down CompleteProgramsTask Task");
		
		this.stopExecuting();
	}
	
	private Date getThresholdDateWeeksAgo(int weeks) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.WEEK_OF_YEAR, -weeks);
		return cal.getTime();
	}
	
	private void completeProgramsStartedBefore(ProgramWorkflowService programWorkflowService, Program program,
	        Date thresholdDate) {
		List<PatientProgram> patientPrograms = programWorkflowService.getPatientPrograms(null, program, null, null, null,
		    null, false);
		for (PatientProgram pp : patientPrograms) {
			if (pp == null) {
				continue;
			}
			if (pp.getVoided() != null && pp.getVoided()) {
				continue;
			}
			if (pp.getDateCompleted() != null) {
				continue;
			}
			Date enrolled = pp.getDateEnrolled();
			if (enrolled == null) {
				continue;
			}
			if (enrolled.before(thresholdDate)) {
				try {
					if (pp.getProgram().getUuid().equals(PROGRAM_CHILD_NUTRITION_UUID)) {
						pp.setDateCompleted(new Date());
						ProgramWorkflow programWorkflow = Utils.getWorkflowByUuid(program,
						    LampConfig.WORKFLOW_CHILD_NUTRITION_UUID);
						if (programWorkflow == null) {
							return;
						}
						pp.transitionToState(
						    Utils.getStateByConcept(
						        programWorkflow,
						        Context.getConceptService().getConceptByUuid(
						            LampConfig.CONCEPT_18_WEEKS_IN_CHILD_NUTRITION_PROGRAM)), new Date());
						log.info("Auto-completed Child Nutrition program '" + program.getName() + "' for patient "
						        + (pp.getPatient() != null ? pp.getPatient().getId() : "unknown") + " (enrolled: "
						        + enrolled + ")");
					}
					
					if (pp.getProgram().getUuid().equals(PROGRAM_PRENATAL_UUID)) {
						pp.setDateCompleted(new Date());
						ProgramWorkflow programWorkflow = Utils
						        .getWorkflowByUuid(program, LampConfig.WORKFLOW_PRENATAL_UUID);
						if (programWorkflow == null) {
							return;
						}
						pp.transitionToState(
						    Utils.getStateByConcept(
						        programWorkflow,
						        Context.getConceptService().getConceptByUuid(
						            LampConfig.CONCEPT_10_MONTHS_IN_PRENATAL_PROGRAM)), new Date());
						log.info("Auto-completed Prenatal program '" + program.getName() + "' for patient "
						        + (pp.getPatient() != null ? pp.getPatient().getId() : "unknown") + " (enrolled: "
						        + enrolled + ")");
					}
					
				}
				catch (Exception e) {
					log.error("Failed to auto-complete program '" + program.getName() + "' for a patient", e);
				}
			}
		}
	}
}
