/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.lamp;

import org.springframework.stereotype.Component;

@Component("lamp.LampConfig")
public class LampConfig {
	
	public static final String CHILD_NUTRITION_ENCOUNTER_TYPE_UUID = "a46c50d1-f8f2-4b73-9940-7e77c64bcffc";
	
	public static final String CONCEPT_REACHED_TARGET_GOAL_WEIGHT_UUID = "0a947c3b-ac0a-42af-9299-674620dc7a6d";
	
	public static final String CONCEPT_CHILD_NUTRITION_REASON_FOR_DISCHARGE_UUID = "a7781567-2c1e-4bfd-ad3a-182915722916";
	
	public static final String CONCEPT_CHILD_NUTRITION_MALNUTRITION_STATUS_UUID = "0ae3326d-592b-4ce0-a523-6e03bbe99b69";
	
	public static final String PROGRAM_CHILD_NUTRITION_UUID = "828ce80d-1de0-4798-a9a9-0e89f37d0aaa";
	
	public static final String WORKFLOW_CHILD_NUTRITION_UUID = "ffe59a80-e9fa-4403-aa00-999ee812f602";
	
	public static final String PRENATAL_ENCOUNTER_TYPE_UUID = "919115d3-206c-456e-a74d-00c0669a83eb";
	
	public static final String CONCEPT_PRENATAL_PREGNANCY_STATUS_UUID = "a203471c-47bc-4706-a288-6f74ecec6932";
	
	public static final String PROGRAM_PRENATAL_UUID = "3531501f-bbdf-4e49-be19-6c87220f71ee";
	
	public static final String WORKFLOW_PRENATAL_UUID = "3009b582-1745-46bc-8886-7ea20f4675f2";
	
	public static final String CONCEPT_18_WEEKS_IN_CHILD_NUTRITION_PROGRAM = "74f45a8a-4128-4eb6-b8ca-f4b641c6de3a";
	
	public static final String CONCEPT_10_MONTHS_IN_PRENATAL_PROGRAM = "20cfecf2-d01f-4bd8-b71e-ad112ce0d7ce";
}
