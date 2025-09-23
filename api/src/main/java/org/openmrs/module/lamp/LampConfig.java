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

/**
 * Contains module's config.
 */
@Component("lamp.LampConfig")
public class LampConfig {
	
	// EncounterType used by the Child Nutrition form
	public static final String CHILD_NUTRITION_ENCOUNTER_TYPE_UUID = "a46c50d1-f8f2-4b73-9940-7e77c64bcffc";
	
	public static final String CONCEPT_CHILD_NUTRITION_PROGRAM_STATUS_UUID = "9c01f218-1f2e-4f1a-9c5e-b9a92f123456";
	
	public static final String CONCEPT_CHILD_NUTRITION_REASON_FOR_DISCHARGE_UUID = "a7781567-2c1e-4bfd-ad3a-182915722916";
	
	public static final String CONCEPT_CHILD_NUTRITION_MALNUTRITION_STATUS_UUID = "0ae3326d-592b-4ce0-a523-6e03bbe99b69";
	
	public static final String PROGRAM_CHILD_NUTRITION_UUID = "828ce80d-1de0-4798-a9a9-0e89f37d0aaa";
	
	public static final String WORKFLOW_CHILD_NUTRITION_UUID = "ffe59a80-e9fa-4403-aa00-999ee812f602";
}
