package org.openmrs.module.lamp.scheduler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.PatientProgram;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.module.lamp.LampConfig;
import org.openmrs.module.lamp.Utils;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Component
public class CompleteProgramsTask extends AbstractTask {

    private static final Log log = LogFactory.getLog(CompleteProgramsTask.class);

    @Override
    public void execute() {
        log.debug("Executing CompletePrograms Task");
        ProgramWorkflowService service = Context.getProgramWorkflowService();

        completeProgramIfExists(service, LampConfig.PROGRAM_CHILD_NUTRITION_UUID, 18);
        completeProgramIfExists(service, LampConfig.PROGRAM_PRENATAL_UUID, 44);
    }

    @Override
    public void shutdown() {
        log.debug("Shutting down CompletePrograms Task");
        stopExecuting();
    }

    private void completeProgramIfExists(ProgramWorkflowService service, String programUuid, int weeksThreshold) {
        Program program = service.getProgramByUuid(programUuid);
        if (program != null) {
            completeProgramsStartedBefore(service, program, getThresholdDateWeeksAgo(weeksThreshold));
        }
    }

    private Date getThresholdDateWeeksAgo(int weeks) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, -weeks);
        return cal.getTime();
    }

    private void completeProgramsStartedBefore(ProgramWorkflowService service, Program program, Date thresholdDate) {
        List<PatientProgram> patientPrograms = service.getPatientPrograms(null, program, null, null, null, null, false);

        for (PatientProgram pp : patientPrograms) {
            if (pp == null || Boolean.TRUE.equals(pp.getVoided()) || pp.getDateCompleted() != null
                    || pp.getDateEnrolled() == null) {
                continue;
            }

            if (pp.getDateEnrolled().before(thresholdDate)) {
                completePatientProgram(pp, program);
            }
        }
    }

    private void completePatientProgram(PatientProgram pp, Program program) {
        String programUuid = pp.getProgram().getUuid();

        if (LampConfig.PROGRAM_CHILD_NUTRITION_UUID.equals(programUuid)) {
            transitionProgramState(pp, program, LampConfig.WORKFLOW_CHILD_NUTRITION_UUID,
                    LampConfig.CONCEPT_18_WEEKS_IN_CHILD_NUTRITION_PROGRAM, pp.getProgram().getName());
        } else if (LampConfig.PROGRAM_PRENATAL_UUID.equals(programUuid)) {
            transitionProgramState(pp, program, LampConfig.WORKFLOW_PRENATAL_UUID,
                    LampConfig.CONCEPT_10_MONTHS_IN_PRENATAL_PROGRAM, pp.getProgram().getName());
        }
    }

    private void transitionProgramState(PatientProgram pp, Program program, String workflowUuid, String conceptUuid,
                                        String programName) {
        ProgramWorkflow workflow = Utils.getWorkflowByUuid(program, workflowUuid);
        if (workflow == null) {
            return;
        }

        pp.setDateCompleted(new Date());
        pp.transitionToState(Utils.getStateByConcept(workflow, Context.getConceptService().getConceptByUuid(conceptUuid)),
                new Date());

        log.info("Auto-completed " + programName + " program");
    }
}
