package org.openmrs.module.lamp.scheduler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.scheduler.tasks.AbstractTask;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class CloseStaleVisitsTask extends AbstractTask {
	
	private static final Log log = LogFactory.getLog(CloseStaleVisitsTask.class);
	
	static final int INACTIVITY_THRESHOLD_HOURS = 12;
	
	@Override
	public void execute() {
		log.debug("Executing CloseStaleVisits Task");
		VisitService visitService = Context.getVisitService();
		
		Date thresholdDate = hoursBefore(now(), INACTIVITY_THRESHOLD_HOURS);
		for (Visit visit : getActiveVisits(visitService)) {
			if (!hasRecentActivity(visit, thresholdDate)) {
				closeVisit(visitService, visit, now());
			}
		}
	}
	
	@Override
	public void shutdown() {
		log.debug("Shutting down CloseStaleVisits Task");
		stopExecuting();
	}
	
	private List<Visit> getActiveVisits(VisitService visitService) {
		List<Visit> visits = visitService.getVisits(null, null, null, null, null, null, null, null, null, false, false);
		if (visits == null) {
			return Collections.emptyList();
		}
		return visits;
	}
	
	private void closeVisit(VisitService visitService, Visit visit, Date stopDatetime) {
		visit.setStopDatetime(stopDatetime);
		visitService.saveVisit(visit);
		log.info("Auto-closed stale visit " + visit.getUuid());
	}
	
	protected Date now() {
		return new Date();
	}
	
	private Date hoursBefore(Date date, int hours) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.HOUR_OF_DAY, -hours);
		return cal.getTime();
	}
	
	private boolean hasRecentActivity(Visit visit, Date thresholdDate) {
		if (isOnOrAfter(visit.getStartDatetime(), thresholdDate)) {
			return true;
		}
		
		Set<Encounter> encounters = visit.getEncounters();
		if (encounters != null) {
			for (Encounter encounter : encounters) {
				if (encounter == null || Boolean.TRUE.equals(encounter.getVoided())) {
					continue;
				}
				if (isOnOrAfter(encounter.getEncounterDatetime(), thresholdDate)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private static boolean isOnOrAfter(Date date, Date thresholdDate) {
		return date != null && !date.before(thresholdDate);
	}
}
