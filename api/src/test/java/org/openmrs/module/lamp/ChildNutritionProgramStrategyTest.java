package org.openmrs.module.lamp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, Utils.class })
@PowerMockIgnore({ "javax.management.*", "javax.script.*" })
public class ChildNutritionProgramStrategyTest {
	
	@Mock
	private ProgramWorkflowService mockProgramWorkflowService;
	
	@Mock
	private ConceptService mockConceptService;
	
	private ChildNutritionProgramStrategy childNutritionProgramStrategy;
	
	private Encounter buildEncounter(boolean childNutritionType) {
		EncounterType type = new EncounterType();
		type.setUuid(childNutritionType ? LampConfig.CHILD_NUTRITION_ENCOUNTER_TYPE_UUID : "some-other-type");
		
		Patient patient = new Patient(999);
		Location location = new Location(7);
		location.setName("Clinic");
		
		Encounter encounter = new Encounter();
		encounter.setEncounterType(type);
		encounter.setPatient(patient);
		encounter.setEncounterDatetime(new Date());
		encounter.setLocation(location);
		return encounter;
	}
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(Utils.class);
		
		when(Context.getProgramWorkflowService()).thenReturn(mockProgramWorkflowService);
		when(Context.getConceptService()).thenReturn(mockConceptService);
		
		childNutritionProgramStrategy = new ChildNutritionProgramStrategy();
	}
	
	@Test
	public void shouldExitWhenEncounterTypeIsNotChildNutrition() {
		Encounter encounter = buildEncounter(false);
		childNutritionProgramStrategy.execute(encounter, new User(), new Date(), "reason");
		verify(mockProgramWorkflowService, never()).getProgramByUuid(anyString());
		verify(mockProgramWorkflowService, never()).savePatientProgram(any(PatientProgram.class));
	}
	
	@Test
	public void shouldExitWhenProgramIsNull() {
		Encounter encounter = buildEncounter(true);
		when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_CHILD_NUTRITION_UUID)).thenReturn(null);
		
		childNutritionProgramStrategy.execute(encounter, new User(), new Date(), "reason");
		
		verify(mockProgramWorkflowService, times(1)).getProgramByUuid(LampConfig.PROGRAM_CHILD_NUTRITION_UUID);
		verify(mockProgramWorkflowService, never()).savePatientProgram(any(PatientProgram.class));
		verify(mockConceptService, never()).getConceptByUuid(anyString());
	}
	
	@Test
	public void shouldExitWhenMalnutritionConceptIsNull() {
		Encounter encounter = buildEncounter(true);
		when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_CHILD_NUTRITION_UUID)).thenReturn(new Program());
		when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_CHILD_NUTRITION_MALNUTRITION_STATUS_UUID)).thenReturn(
		    null);
		
		childNutritionProgramStrategy.execute(encounter, new User(), new Date(), "reason");
		
		verify(mockProgramWorkflowService, never()).savePatientProgram(any(PatientProgram.class));
		PowerMockito.verifyStatic(Utils.class, never());
		Utils.getOrCreateActiveProgramEnrollment(eq(mockProgramWorkflowService), any(Patient.class), any(Program.class),
		    any(Date.class));
	}
	
	@Test
	public void shouldExitWhenReasonForDischargeConceptIsNull() {
		Encounter encounter = buildEncounter(true);
		Program program = new Program();
		when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_CHILD_NUTRITION_UUID)).thenReturn(program);
		when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_CHILD_NUTRITION_MALNUTRITION_STATUS_UUID)).thenReturn(
		    new Concept(101));
		when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_CHILD_NUTRITION_REASON_FOR_DISCHARGE_UUID)).thenReturn(
		    null);
		
		childNutritionProgramStrategy.execute(encounter, new User(), new Date(), "reason");
		
		verify(mockProgramWorkflowService, never()).savePatientProgram(any(PatientProgram.class));
	}
	
	@Test
	public void shouldExitWhenBothValuesAreNull() {
		Encounter encounter = buildEncounter(true);
		Program program = new Program();
		when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_CHILD_NUTRITION_UUID)).thenReturn(program);
		Concept malC = new Concept(101);
		Concept reasonC = new Concept(102);
		when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_CHILD_NUTRITION_MALNUTRITION_STATUS_UUID)).thenReturn(
		    malC);
		when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_CHILD_NUTRITION_REASON_FOR_DISCHARGE_UUID)).thenReturn(
		    reasonC);
		
		PatientProgram patientProgram = new PatientProgram();
		PowerMockito.when(
		    Utils.getOrCreateActiveProgramEnrollment(eq(mockProgramWorkflowService), eq(encounter.getPatient()),
		        eq(program), any(Date.class))).thenReturn(patientProgram);
		
		PowerMockito.when(Utils.findLatestCodedObsValue(encounter, malC)).thenReturn(null);
		PowerMockito.when(Utils.findLatestCodedObsValue(encounter, reasonC)).thenReturn(null);
		
		childNutritionProgramStrategy.execute(encounter, new User(), new Date(), "reason");
		
		verify(mockProgramWorkflowService, never()).savePatientProgram(any(PatientProgram.class));
	}
	
	@Test
	public void shouldExitWhenWorkflowIsNull() {
		Encounter encounter = buildEncounter(true);
		Program program = new Program();
		when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_CHILD_NUTRITION_UUID)).thenReturn(program);
		
		Concept malC = new Concept(101);
		Concept reasonC = new Concept(102);
		when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_CHILD_NUTRITION_MALNUTRITION_STATUS_UUID)).thenReturn(
		    malC);
		when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_CHILD_NUTRITION_REASON_FOR_DISCHARGE_UUID)).thenReturn(
		    reasonC);
		
		PatientProgram patientProgram = new PatientProgram();
		PowerMockito.when(
		    Utils.getOrCreateActiveProgramEnrollment(eq(mockProgramWorkflowService), eq(encounter.getPatient()),
		        eq(program), any(Date.class))).thenReturn(patientProgram);
		
		PowerMockito.when(Utils.findLatestCodedObsValue(encounter, malC)).thenReturn(new Concept(201));
		PowerMockito.when(Utils.findLatestCodedObsValue(encounter, reasonC)).thenReturn(null);
		
		PowerMockito.when(Utils.getWorkflowByUuid(program, LampConfig.WORKFLOW_CHILD_NUTRITION_UUID)).thenReturn(null);
		
		childNutritionProgramStrategy.execute(encounter, new User(), new Date(), "reason");
		
		verify(mockProgramWorkflowService, never()).savePatientProgram(any(PatientProgram.class));
	}
	
	@Test
	public void shouldExitWhenTargetStateFromMalnutritionStatusIsNull() {
		Encounter encounter = buildEncounter(true);
		Program program = new Program();
		when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_CHILD_NUTRITION_UUID)).thenReturn(program);
		
		Concept malC = new Concept(101);
		Concept reasonC = new Concept(102);
		when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_CHILD_NUTRITION_MALNUTRITION_STATUS_UUID)).thenReturn(
		    malC);
		when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_CHILD_NUTRITION_REASON_FOR_DISCHARGE_UUID)).thenReturn(
		    reasonC);
		
		PatientProgram pp = new PatientProgram();
		PowerMockito.when(
		    Utils.getOrCreateActiveProgramEnrollment(eq(mockProgramWorkflowService), eq(encounter.getPatient()),
		        eq(program), any(Date.class))).thenReturn(pp);
		
		Concept malValue = new Concept(201);
		PowerMockito.when(Utils.findLatestCodedObsValue(encounter, malC)).thenReturn(malValue);
		PowerMockito.when(Utils.findLatestCodedObsValue(encounter, reasonC)).thenReturn(null);
		
		ProgramWorkflow wf = new ProgramWorkflow();
		PowerMockito.when(Utils.getWorkflowByUuid(program, LampConfig.WORKFLOW_CHILD_NUTRITION_UUID)).thenReturn(wf);
		
		PowerMockito.when(Utils.getStateByConcept(wf, malValue)).thenReturn(null);
		
		childNutritionProgramStrategy.execute(encounter, new User(), new Date(), "reason");
		
		verify(mockProgramWorkflowService, never()).savePatientProgram(any(PatientProgram.class));
	}
	
	@Test
	public void shouldExitWhenTargetStateFromReasonForDischargeValueIsNull() {
		Encounter encounter = buildEncounter(true);
		Program program = new Program();
		when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_CHILD_NUTRITION_UUID)).thenReturn(program);
		
		Concept malC = new Concept(101);
		Concept reasonC = new Concept(102);
		when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_CHILD_NUTRITION_MALNUTRITION_STATUS_UUID)).thenReturn(
		    malC);
		when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_CHILD_NUTRITION_REASON_FOR_DISCHARGE_UUID)).thenReturn(
		    reasonC);
		
		PatientProgram pp = new PatientProgram();
		PowerMockito.when(
		    Utils.getOrCreateActiveProgramEnrollment(eq(mockProgramWorkflowService), eq(encounter.getPatient()),
		        eq(program), any(Date.class))).thenReturn(pp);
		
		// malnutrition value null, reason value present
		PowerMockito.when(Utils.findLatestCodedObsValue(encounter, malC)).thenReturn(null);
		Concept reasonValue = new Concept(301);
		PowerMockito.when(Utils.findLatestCodedObsValue(encounter, reasonC)).thenReturn(reasonValue);
		
		ProgramWorkflow wf = new ProgramWorkflow();
		PowerMockito.when(Utils.getWorkflowByUuid(program, LampConfig.WORKFLOW_CHILD_NUTRITION_UUID)).thenReturn(wf);
		
		PowerMockito.when(Utils.getStateByConcept(wf, reasonValue)).thenReturn(null);
		
		childNutritionProgramStrategy.execute(encounter, new User(), new Date(), "reason");
		
		verify(mockProgramWorkflowService, never()).savePatientProgram(any(PatientProgram.class));
	}
	
	@Test
	public void shouldNotSetDateCompletedWhenReachedTargetFromMalnutritionStatus() {
		Encounter encounter = buildEncounter(true);
		Date now = new Date();
		Program program = new Program();
		when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_CHILD_NUTRITION_UUID)).thenReturn(program);
		
		Concept malC = new Concept(101);
		Concept reasonC = new Concept(102);
		when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_CHILD_NUTRITION_MALNUTRITION_STATUS_UUID)).thenReturn(
		    malC);
		when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_CHILD_NUTRITION_REASON_FOR_DISCHARGE_UUID)).thenReturn(
		    reasonC);
		
		PatientProgram pp = new PatientProgram();
		PowerMockito.when(
		    Utils.getOrCreateActiveProgramEnrollment(eq(mockProgramWorkflowService), eq(encounter.getPatient()),
		        eq(program), any(Date.class))).thenReturn(pp);
		
		Concept malValue = new Concept(201);
		malValue.setUuid(LampConfig.CONCEPT_REACHED_TARGET_GOAL_WEIGHT_UUID);
		PowerMockito.when(Utils.findLatestCodedObsValue(encounter, malC)).thenReturn(malValue);
		PowerMockito.when(Utils.findLatestCodedObsValue(encounter, reasonC)).thenReturn(null);
		
		ProgramWorkflow wf = new ProgramWorkflow();
		PowerMockito.when(Utils.getWorkflowByUuid(program, LampConfig.WORKFLOW_CHILD_NUTRITION_UUID)).thenReturn(wf);
		
		ProgramWorkflowState state = new ProgramWorkflowState();
		state.setConcept(malValue);
		PowerMockito.when(Utils.getStateByConcept(wf, malValue)).thenReturn(state);
		
		childNutritionProgramStrategy.execute(encounter, new User(), now, "reason");
		
		// dateCompleted should NOT be set because it came from malnutrition path
		assertNull(pp.getDateCompleted());
		assertFalse(state.getTerminal());
		
		// then
		PowerMockito.verifyStatic(Utils.class, times(1));
		Utils.updateProgram(eq(pp), eq(encounter), eq(now), eq(state));
		
		// transition & save
		assertEquals(encounter.getLocation(), pp.getLocation());
		verify(mockProgramWorkflowService, times(1)).savePatientProgram(pp);
	}
	
	@Test
	public void shouldSetDateCompletedWhenReachedTargetFromReasonForDischarge() {
		Encounter encounter = buildEncounter(true);
		Date now = new Date();
		Program program = new Program();
		when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_CHILD_NUTRITION_UUID)).thenReturn(program);
		
		Concept malC = new Concept(101);
		Concept reasonC = new Concept(102);
		when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_CHILD_NUTRITION_MALNUTRITION_STATUS_UUID)).thenReturn(
		    malC);
		when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_CHILD_NUTRITION_REASON_FOR_DISCHARGE_UUID)).thenReturn(
		    reasonC);
		
		PatientProgram pp = new PatientProgram();
		PowerMockito.when(
		    Utils.getOrCreateActiveProgramEnrollment(eq(mockProgramWorkflowService), eq(encounter.getPatient()),
		        eq(program), any(Date.class))).thenReturn(pp);
		
		// malnutrition null; reason == REACHED_TARGET_GOAL_WEIGHT
		PowerMockito.when(Utils.findLatestCodedObsValue(encounter, malC)).thenReturn(null);
		
		Concept reasonValue = new Concept(301);
		reasonValue.setUuid(LampConfig.CONCEPT_REACHED_TARGET_GOAL_WEIGHT_UUID);
		PowerMockito.when(Utils.findLatestCodedObsValue(encounter, reasonC)).thenReturn(reasonValue);
		
		ProgramWorkflow wf = new ProgramWorkflow();
		PowerMockito.when(Utils.getWorkflowByUuid(program, LampConfig.WORKFLOW_CHILD_NUTRITION_UUID)).thenReturn(wf);
		
		ProgramWorkflowState state = new ProgramWorkflowState();
		state.setConcept(reasonValue);
		state.setTerminal(true);
		PowerMockito.when(Utils.getStateByConcept(wf, reasonValue)).thenReturn(state);
		
		childNutritionProgramStrategy.execute(encounter, new User(), now, "reason");
		
		// dateCompleted should NOT be set because it came from malnutrition path
		assertNull(pp.getDateCompleted());
		assertTrue(state.getTerminal());
		
		// then
		PowerMockito.verifyStatic(Utils.class, times(1));
		Utils.updateProgram(eq(pp), eq(encounter), eq(now), eq(state));
		
		// transition & save
		assertEquals(encounter.getLocation(), pp.getLocation());
		verify(mockProgramWorkflowService, times(1)).savePatientProgram(pp);
	}
}
