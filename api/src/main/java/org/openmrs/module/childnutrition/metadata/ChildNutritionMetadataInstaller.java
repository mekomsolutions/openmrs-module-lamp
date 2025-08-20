/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.openmrs.module.childnutrition.metadata;

import org.openmrs.Concept;
import org.openmrs.Program;
import org.openmrs.ProgramWorkflow;
import org.openmrs.ProgramWorkflowState;
import org.openmrs.api.ProgramWorkflowService;
import org.openmrs.api.context.Context;
import org.openmrs.module.childnutrition.ChildnutritionConfig;

/**
 * Installs Child Nutrition program metadata (Program and Workflow) at module startup if missing.
 */
public class ChildNutritionMetadataInstaller {
	
	public void installOrUpdateProgramAndWorkflow() {
		ProgramWorkflowService pws = Context.getProgramWorkflowService();
		
		// Ensure backing concepts exist (should be provided via CSV or prior metadata)
		Concept programConcept = Context.getConceptService().getConceptByUuid(
		    ChildnutritionConfig.CONCEPT_CHILD_NUTRITION_PROGRAM_UUID);
		Concept statusConcept = Context.getConceptService().getConceptByUuid(
		    ChildnutritionConfig.CONCEPT_MALNUTRITION_STATUS_UUID);
		if (programConcept == null || statusConcept == null) {
			// Do not create concepts here; rely on CSV import. If missing, skip installation gracefully.
			return;
		}
		
		Program program = pws.getProgramByUuid(ChildnutritionConfig.PROGRAM_CHILD_NUTRITION_UUID);
		if (program == null) {
			program = new Program();
			program.setUuid(ChildnutritionConfig.PROGRAM_CHILD_NUTRITION_UUID);
			program.setConcept(programConcept);
			program.setName(programConcept.getName(Context.getLocale()).getName());
			program.setDescription(programConcept.getDescription(Context.getLocale()) != null ? programConcept
			        .getDescription(Context.getLocale()).getDescription() : program.getName());
			pws.saveProgram(program);
		}
		
		ProgramWorkflow workflow = null;
		for (ProgramWorkflow wf : program.getWorkflows()) {
			if (wf != null && ChildnutritionConfig.WORKFLOW_MALNUTRITION_STATUS_UUID.equals(wf.getUuid())) {
				workflow = wf;
				break;
			}
		}
		if (workflow == null) {
			workflow = new ProgramWorkflow();
			workflow.setUuid(ChildnutritionConfig.WORKFLOW_MALNUTRITION_STATUS_UUID);
			workflow.setConcept(statusConcept);
			workflow.setProgram(program);
			program.addWorkflow(workflow);
			pws.saveProgram(program);
		}
		
		// Ensure states for each status answer concept exist
		ensureStateForAnswer(workflow, ChildnutritionConfig.CONCEPT_STATUS_SAM_UUID, false);
		ensureStateForAnswer(workflow, ChildnutritionConfig.CONCEPT_STATUS_MAM_UUID, false);
		ensureStateForAnswer(workflow, ChildnutritionConfig.CONCEPT_STATUS_REACHED_TARGET_WEIGHT_UUID, true);
		ensureStateForAnswer(workflow, ChildnutritionConfig.CONCEPT_STATUS_UNDEFINED_UUID, false);
		
		pws.saveProgram(program);
	}
	
	private void ensureStateForAnswer(ProgramWorkflow workflow, String answerConceptUuid, boolean terminal) {
		Concept answerConcept = Context.getConceptService().getConceptByUuid(answerConceptUuid);
		if (answerConcept == null) {
			return;
		}
		
		for (ProgramWorkflowState existing : workflow.getStates()) {
			if (answerConcept.equals(existing.getConcept())) {
				existing.setTerminal(terminal);
				return;
			}
		}
		
		ProgramWorkflowState state = new ProgramWorkflowState();
		state.setConcept(answerConcept);
		state.setInitial(false);
		state.setTerminal(terminal);
		workflow.addState(state);
	}
}
