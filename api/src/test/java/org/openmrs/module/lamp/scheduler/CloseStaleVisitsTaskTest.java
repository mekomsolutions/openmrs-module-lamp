package org.openmrs.module.lamp.scheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.openmrs.Encounter;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Context.class })
@PowerMockIgnore({ "javax.management.*", "javax.script.*", "javax.xml.*", "org.xml.*", "org.w3c.*",
        "com.sun.org.apache.xerces.*" })
public class CloseStaleVisitsTaskTest {
	
	private static final Date NOW = date(2026, Calendar.MAY, 20, 12);
	
	@Mock
	private VisitService visitService;
	
	private CloseStaleVisitsTask task;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(Context.class);
		when(Context.getVisitService()).thenReturn(visitService);
		
		task = new CloseStaleVisitsTask() {
			
			@Override
			protected Date now() {
				return NOW;
			}
		};
	}
	
	@Test
	public void shouldCloseVisitInactiveLongerThanTwelveHours() {
		Visit visit = visitStarted(hoursAgo(24));
		addEncounter(visit, hoursAgo(13), false);
		when(activeVisits()).thenReturn(Collections.singletonList(visit));
		
		task.execute();
		
		assertEquals(NOW, visit.getStopDatetime());
		verify(visitService).saveVisit(visit);
	}
	
	@Test
	public void shouldLeaveRecentlyActiveVisitOpen() {
		Visit visit = visitStarted(hoursAgo(24));
		addEncounter(visit, hoursAgo(1), false);
		when(activeVisits()).thenReturn(Collections.singletonList(visit));
		
		task.execute();
		
		assertNull(visit.getStopDatetime());
		verify(visitService, never()).saveVisit(any(Visit.class));
	}
	
	@Test
	public void shouldUseVisitStartWhenThereAreNoEncounters() {
		Date startDatetime = hoursAgo(24);
		Visit visit = visitStarted(startDatetime);
		when(activeVisits()).thenReturn(Collections.singletonList(visit));
		
		task.execute();
		
		assertEquals(NOW, visit.getStopDatetime());
		verify(visitService).saveVisit(visit);
	}
	
	@Test
	public void shouldIgnoreVoidedEncountersWhenCheckingActivity() {
		Date startDatetime = hoursAgo(24);
		Visit visit = visitStarted(startDatetime);
		addEncounter(visit, hoursAgo(1), true);
		when(activeVisits()).thenReturn(Collections.singletonList(visit));
		
		task.execute();
		
		assertEquals(NOW, visit.getStopDatetime());
		verify(visitService).saveVisit(visit);
	}
	
	@Test
	public void shouldHandleNullActiveVisitList() {
		when(activeVisits()).thenReturn(null);
		
		task.execute();
		
		verify(visitService, never()).saveVisit(any(Visit.class));
	}
	
	private List<Visit> activeVisits() {
		return visitService.getVisits(null, null, null, null, null, null, null, null, null, false, false);
	}
	
	private static Visit visitStarted(Date startDatetime) {
		Visit visit = new Visit();
		visit.setStartDatetime(startDatetime);
		visit.setDateCreated(startDatetime);
		return visit;
	}
	
	private static void addEncounter(Visit visit, Date encounterDatetime, boolean voided) {
		Encounter encounter = new Encounter();
		encounter.setEncounterDatetime(encounterDatetime);
		encounter.setVoided(voided);
		visit.setEncounters(new HashSet<Encounter>(Collections.singletonList(encounter)));
	}
	
	private static Date hoursAgo(int hours) {
		Calendar c = Calendar.getInstance();
		c.setTime(NOW);
		c.add(Calendar.HOUR_OF_DAY, -hours);
		return c.getTime();
	}
	
	private static Date date(int year, int month, int day, int hour) {
		Calendar c = Calendar.getInstance();
		c.clear();
		c.set(year, month, day, hour, 0, 0);
		return c.getTime();
	}
}
