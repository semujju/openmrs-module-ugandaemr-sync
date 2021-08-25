package org.openmrs.module.ugandaemrsync.tasks;

import org.openmrs.api.context.Context;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.module.ugandaemrsync.server.SyncFHIRRecord;
import org.openmrs.scheduler.tasks.AbstractTask;

public class ARTAccessIntegrationTask extends AbstractTask {
    @Override
    public void execute() {
        UgandaEMRSyncService ugandaEMRSyncService = Context.getService(UgandaEMRSyncService.class);
        SyncFhirProfile syncFhirProfile = ugandaEMRSyncService.getSyncFhirProfileByScheduledTaskName("ART Access Integration");
        SyncFHIRRecord syncFHIRRecord = new SyncFHIRRecord();

        syncFHIRRecord.generateCaseBasedFHIRResourceBundles(syncFhirProfile);


    }
}
