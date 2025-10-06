package org.openmrs.module.lamp;

import java.util.Date;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.User;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Context.class, Utils.class})
@PowerMockIgnore({"javax.management.*", "javax.script.*"})
public class PrenatalProgramStrategyTest {

    @Mock
    private ProgramWorkflowService mockProgramWorkflowService;

    @Mock
    private ConceptService mockConceptService;

    private PrenatalProgramStrategy prenatalProgramStrategy;

    @Before
    public void setup() {
        PowerMockito.mockStatic(Context.class);
        Mockito.when(Context.getProgramWorkflowService()).thenReturn(mockProgramWorkflowService);
        Mockito.when(Context.getConceptService()).thenReturn(mockConceptService);
        prenatalProgramStrategy = new PrenatalProgramStrategy();
    }

    private Encounter buildEncounter(boolean prenatalType) {
        EncounterType encounterType = new EncounterType();
        encounterType.setUuid(prenatalType ? LampConfig.PRENATAL_ENCOUNTER_TYPE_UUID : "some-other-type");

        Patient patient = new Patient(123);
        Location location = new Location(10);
        location.setName("ANC Clinic");

        Encounter e = new Encounter();
        e.setPatient(patient);
        e.setEncounterType(encounterType);
        e.setEncounterDatetime(new Date());
        e.setLocation(location);
        return e;
    }

    @Test
    public void shouldExitWhenEncounterTypeIsNotPrenatalEncounter() {
        // given
        Encounter encounter = buildEncounter(false);
        User user = new User();

        // when
        prenatalProgramStrategy.execute(encounter, user, new Date(), "reason");

        // then
        verify(mockProgramWorkflowService, never()).getProgramByUuid(anyString());
        verify(mockProgramWorkflowService, never()).savePatientProgram(any(PatientProgram.class));
        PowerMockito.verifyStatic(Utils.class, never());
        Utils.updateProgram(any(PatientProgram.class), any(Encounter.class), any(Date.class),
                any(ProgramWorkflowState.class));
    }

    @Test
    public void shouldExitWhenProgramIsNull() {
        // given
        Encounter encounter = buildEncounter(true);
        when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_PRENATAL_UUID)).thenReturn(null);
        User user = new User();

        // when
        prenatalProgramStrategy.execute(encounter, user, new Date(), "reason");

        // then
        verify(mockProgramWorkflowService, times(1)).getProgramByUuid(LampConfig.PROGRAM_PRENATAL_UUID);
        verify(mockConceptService, never()).getConceptByUuid(anyString());
        verify(mockProgramWorkflowService, never()).savePatientProgram(any(PatientProgram.class));
        PowerMockito.verifyStatic(Utils.class, never());
        Utils.getOrCreateActiveProgramEnrollment(eq(mockProgramWorkflowService), any(Patient.class),any(Program.class), any(Date.class));
    }

    @Test
    public void shouldExitWhenPregnancyStatusConceptIsNull() {
        // given
        Encounter encounter = buildEncounter(true);
        Program program = new Program();
        when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_PRENATAL_UUID)).thenReturn(program);
        when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_PRENATAL_PREGNANCY_STATUS_UUID)).thenReturn(null);
        User user = new User();

        // when
        prenatalProgramStrategy.execute(encounter, user, new Date(), "reason");

        // then
        verify(mockProgramWorkflowService, never()).savePatientProgram(any(PatientProgram.class));
        PowerMockito.verifyStatic(Utils.class, never());
        Utils.findLatestCodedObsValue(any(Encounter.class), any(Concept.class));
    }

    @Test
    public void shouldExitWhenWorkflowIsNull() {
        // given
        Encounter encounter = buildEncounter(true);
        Program program = new Program();
        when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_PRENATAL_UUID)).thenReturn(program);

        Concept pregnancyStatusConcept = new Concept(1000);
        when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_PRENATAL_PREGNANCY_STATUS_UUID)).thenReturn(
                pregnancyStatusConcept);

        PatientProgram pp = new PatientProgram();
        PowerMockito.when(
                Utils.getOrCreateActiveProgramEnrollment(eq(mockProgramWorkflowService), eq(encounter.getPatient()),
                        eq(program), any(Date.class))).thenReturn(pp);

        Concept prenatalStatusValue = new Concept(2000);
        PowerMockito.when(Utils.findLatestCodedObsValue(encounter, pregnancyStatusConcept)).thenReturn(prenatalStatusValue);

        PowerMockito.when(Utils.getWorkflowByUuid(program, LampConfig.WORKFLOW_PRENATAL_UUID)).thenReturn(null);

        User user = new User();

        // when
        prenatalProgramStrategy.execute(encounter, user, new Date(), "reason");

        // then
        verify(mockProgramWorkflowService, never()).savePatientProgram(any(PatientProgram.class));
        PowerMockito.verifyStatic(Utils.class, never());
        Utils.getStateByConcept(any(ProgramWorkflow.class), any(Concept.class));
    }

    @Test
    public void shouldExitWhenTargetStateIsNull() {
        // given
        Encounter encounter = buildEncounter(true);
        Program program = new Program();
        when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_PRENATAL_UUID)).thenReturn(program);

        Concept pregnancyStatusConcept = new Concept(1000);
        when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_PRENATAL_PREGNANCY_STATUS_UUID)).thenReturn(
                pregnancyStatusConcept);

        PatientProgram pp = new PatientProgram();
        PowerMockito.when(
                Utils.getOrCreateActiveProgramEnrollment(eq(mockProgramWorkflowService), eq(encounter.getPatient()),
                        eq(program), any(Date.class))).thenReturn(pp);

        Concept prenatalStatusValue = new Concept(2000);
        PowerMockito.when(Utils.findLatestCodedObsValue(encounter, pregnancyStatusConcept)).thenReturn(prenatalStatusValue);

        ProgramWorkflow wf = new ProgramWorkflow();
        PowerMockito.when(Utils.getWorkflowByUuid(program, LampConfig.WORKFLOW_PRENATAL_UUID)).thenReturn(wf);

        PowerMockito.when(Utils.getStateByConcept(wf, prenatalStatusValue)).thenReturn(null);

        User user = new User();

        // when
        prenatalProgramStrategy.execute(encounter, user, new Date(), "reason");

        // then
        verify(mockProgramWorkflowService, never()).savePatientProgram(any(PatientProgram.class));
        PowerMockito.verifyStatic(Utils.class, never());
        Utils.updateProgram(any(PatientProgram.class), any(Encounter.class), any(Date.class),
                any(ProgramWorkflowState.class));
    }

    @Test
    public void shouldSaveProgram() {
        // given
        Encounter encounter = buildEncounter(true);
        Date now = new Date();
        User user = new User();

        Program program = new Program();
        when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_PRENATAL_UUID)).thenReturn(program);

        Concept pregnancyStatusConcept = new Concept(1000);
        when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_PRENATAL_PREGNANCY_STATUS_UUID)).thenReturn(
                pregnancyStatusConcept);

        PatientProgram pp = new PatientProgram();
        PowerMockito.when(
                Utils.getOrCreateActiveProgramEnrollment(eq(mockProgramWorkflowService), eq(encounter.getPatient()),
                        eq(program), any(Date.class))).thenReturn(pp);

        Concept prenatalStatusValue = new Concept(2000);
        PowerMockito.when(Utils.findLatestCodedObsValue(encounter, pregnancyStatusConcept)).thenReturn(prenatalStatusValue);

        ProgramWorkflow wf = new ProgramWorkflow();
        PowerMockito.when(Utils.getWorkflowByUuid(program, LampConfig.WORKFLOW_PRENATAL_UUID)).thenReturn(wf);

        ProgramWorkflowState targetState = new ProgramWorkflowState();
        PowerMockito.when(Utils.getStateByConcept(wf, prenatalStatusValue)).thenReturn(targetState);

        // when
        prenatalProgramStrategy.execute(encounter, user, now, "reason");

        // then
        PowerMockito.verifyStatic(Utils.class, times(1));
        Utils.updateProgram(eq(pp), eq(encounter), eq(now), eq(targetState));

        assertEquals(encounter.getLocation(), pp.getLocation());
        verify(mockProgramWorkflowService, times(1)).savePatientProgram(pp);
    }
}
