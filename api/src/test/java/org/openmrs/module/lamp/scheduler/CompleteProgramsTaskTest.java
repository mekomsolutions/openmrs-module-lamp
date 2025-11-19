package org.openmrs.module.lamp.scheduler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import static org.junit.Assert.assertNotNull;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.PatientProgram;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.ConceptService;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.module.lamp.LampConfig;
import org.openmrs.module.lamp.Utils;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class, Utils.class })
@PowerMockIgnore({ "javax.management.*", "javax.script.*" })
public class CompleteProgramsTaskTest {
	
	@Mock
	private ProgramWorkflowService mockProgramWorkflowService;
	
	@Mock
	private ConceptService mockConceptService;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		PowerMockito.mockStatic(Utils.class);
		when(Context.getProgramWorkflowService()).thenReturn(mockProgramWorkflowService);
		when(Context.getConceptService()).thenReturn(mockConceptService);
	}
	
	@Test
	public void shouldCompleteChildNutritionProgramsOlderThan18Weeks() {
		Program childProgram = new Program();
		childProgram.setUuid(LampConfig.PROGRAM_CHILD_NUTRITION_UUID);
		childProgram.setName("Child Nutrition");
		when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_CHILD_NUTRITION_UUID)).thenReturn(childProgram);
		
		Program prenatalProgram = new Program();
		prenatalProgram.setUuid(LampConfig.PROGRAM_PRENATAL_UUID);
		prenatalProgram.setName("Prenatal");
		when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_PRENATAL_UUID)).thenReturn(prenatalProgram);
		
		PatientProgram eligible = spy(new PatientProgram());
		eligible.setProgram(childProgram);
		eligible.setDateEnrolled(weeksAgo(30));
		
		PatientProgram ineligible = spy(new PatientProgram());
		ineligible.setProgram(childProgram);
		ineligible.setDateEnrolled(weeksAgo(5));
		
		List<PatientProgram> childPrograms = new ArrayList<PatientProgram>(Arrays.asList(eligible, ineligible));
		when(mockProgramWorkflowService.getPatientPrograms(null, childProgram, null, null, null, null, false)).thenReturn(
		    childPrograms);
		when(mockProgramWorkflowService.getPatientPrograms(null, prenatalProgram, null, null, null, null, false))
		        .thenReturn(new ArrayList<PatientProgram>());
		
		ProgramWorkflow mockWorkflow = Mockito.mock(ProgramWorkflow.class);
		ProgramWorkflowState mockState = Mockito.mock(ProgramWorkflowState.class);
		Concept mockConcept = new Concept();
		when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_18_WEEKS_IN_CHILD_NUTRITION_PROGRAM)).thenReturn(
		    mockConcept);
		PowerMockito.when(Utils.getWorkflowByUuid(childProgram, LampConfig.WORKFLOW_CHILD_NUTRITION_UUID)).thenReturn(
		    mockWorkflow);
		PowerMockito.when(Utils.getStateByConcept(mockWorkflow, mockConcept)).thenReturn(mockState);
		
		AbstractTask task = new CompleteProgramsTask();
		task.execute();
		
		verify(eligible, times(1)).transitionToState(eq(mockState), any(Date.class));
		verify(ineligible, never()).transitionToState(any(ProgramWorkflowState.class), any(Date.class));
	}
	
	@Test
	public void shouldCompletePrenatalProgramsOlderThan44Weeks() {
		Program prenatalProgram = new Program();
		prenatalProgram.setUuid(LampConfig.PROGRAM_PRENATAL_UUID);
		prenatalProgram.setName("Prenatal");
		when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_PRENATAL_UUID)).thenReturn(prenatalProgram);
		
		Program childProgram = new Program();
		childProgram.setUuid(LampConfig.PROGRAM_CHILD_NUTRITION_UUID);
		childProgram.setName("Child Nutrition");
		when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_CHILD_NUTRITION_UUID)).thenReturn(childProgram);
		when(mockProgramWorkflowService.getPatientPrograms(null, childProgram, null, null, null, null, false)).thenReturn(
		    new ArrayList<PatientProgram>());
		
		PatientProgram eligible = spy(new PatientProgram());
		eligible.setProgram(prenatalProgram);
		eligible.setDateEnrolled(weeksAgo(60));
		
		PatientProgram ineligible = spy(new PatientProgram());
		ineligible.setProgram(prenatalProgram);
		ineligible.setDateEnrolled(weeksAgo(5));
		
		List<PatientProgram> prenatalPrograms = new ArrayList<PatientProgram>(Arrays.asList(eligible, ineligible));
		when(mockProgramWorkflowService.getPatientPrograms(null, prenatalProgram, null, null, null, null, false))
		        .thenReturn(prenatalPrograms);
		
		ProgramWorkflow mockWorkflow = Mockito.mock(ProgramWorkflow.class);
		ProgramWorkflowState mockState = Mockito.mock(ProgramWorkflowState.class);
		
		Concept mockConcept = new Concept();
		when(mockConceptService.getConceptByUuid(LampConfig.CONCEPT_10_MONTHS_IN_PRENATAL_PROGRAM)).thenReturn(mockConcept);
		PowerMockito.when(Utils.getWorkflowByUuid(prenatalProgram, LampConfig.WORKFLOW_PRENATAL_UUID)).thenReturn(
		    mockWorkflow);
		PowerMockito.when(Utils.getStateByConcept(mockWorkflow, mockConcept)).thenReturn(mockState);
		
		AbstractTask task = new CompleteProgramsTask();
		task.execute();
		
		verify(eligible, times(1)).transitionToState(eq(mockState), any(Date.class));
		verify(ineligible, never()).transitionToState(any(ProgramWorkflowState.class), any(Date.class));
	}
	
	@Test
	public void shouldNotFailWhenProgramMissing() {
		when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_CHILD_NUTRITION_UUID)).thenReturn(null);
		when(mockProgramWorkflowService.getProgramByUuid(LampConfig.PROGRAM_PRENATAL_UUID)).thenReturn(null);
		
		new CompleteProgramsTask().execute();
		
		verify(mockProgramWorkflowService, never()).getPatientPrograms(any(Patient.class), any(Program.class),
		    any(Date.class), any(Date.class), any(Date.class), any(Date.class), any(Boolean.class));
	}
	
	private static Date weeksAgo(int weeks) {
		Calendar c = Calendar.getInstance();
		c.add(Calendar.WEEK_OF_YEAR, -weeks);
		return c.getTime();
	}
}
