/**
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 * <p>
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.ugandaemrsync.api.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Encounter;
import org.openmrs.EncounterType;
import org.openmrs.Concept;
import org.openmrs.Patient;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptSource;
import org.openmrs.api.APIException;
import org.openmrs.api.ObsService;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.ServiceContext;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.fhir2.api.FhirConceptSourceService;
import org.openmrs.module.fhir2.api.FhirObservationService;
import org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService;
import org.openmrs.module.ugandaemrsync.api.dao.UgandaEMRSyncDao;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfile;
import org.openmrs.module.ugandaemrsync.model.SyncFhirResource;
import org.openmrs.module.ugandaemrsync.model.SyncFhirProfileLog;
import org.openmrs.module.ugandaemrsync.model.SyncFhirCase;
import org.openmrs.module.ugandaemrsync.model.SyncTask;
import org.openmrs.module.ugandaemrsync.model.SyncTaskType;
import org.openmrs.module.ugandaemrsync.server.SyncGlobalProperties;
import org.openmrs.module.ugandaemrsync.util.UgandaEMRSyncUtil;
import org.openmrs.util.OpenmrsUtil;
import org.openmrs.scheduler.TaskDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.openmrs.module.ugandaemrsync.UgandaEMRSyncConfig.GP_DHIS2_ORGANIZATION_UUID;
import static org.openmrs.module.ugandaemrsync.UgandaEMRSyncConfig.SYNC_METRIC_DATA;

public class UgandaEMRSyncServiceImpl extends BaseOpenmrsService implements UgandaEMRSyncService {

    UgandaEMRSyncDao dao;
    Log log = LogFactory.getLog(UgandaEMRSyncServiceImpl.class);

    @Autowired
    UserService userService;

    /**
     * Injected in moduleApplicationContext.xml
     */
    public void setDao(UgandaEMRSyncDao dao) {
        this.dao = dao;
    }

    /**
     * Injected in moduleApplicationContext.xml
     */
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getAllSyncTaskType()
     */
    @Override
    public List<SyncTaskType> getAllSyncTaskType() throws APIException {
        return dao.getAllSyncTaskType();
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncTaskTypeByUUID(java.lang.String)
     */
    @Override
    public SyncTaskType getSyncTaskTypeByUUID(String uuid) throws APIException {
        return dao.getSyncTaskTypeByUUID(uuid);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveSyncTaskType(org.openmrs.module.ugandaemrsync.model.SyncTaskType)
     */
    @Override
    public SyncTaskType saveSyncTaskType(SyncTaskType syncTaskType) throws APIException {
        if (syncTaskType.getCreator() == null) {
            syncTaskType.setCreator(userService.getUser(1));
        }
        return dao.saveSyncTaskType(syncTaskType);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncTaskBySyncTaskId(java.lang.String)
     */
    @Override
    public SyncTask getSyncTaskBySyncTaskId(String syncTaskId) throws APIException {
        return dao.getSyncTask(syncTaskId);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getAllSyncTask()
     */
    @Override
    public List<SyncTask> getAllSyncTask() {
        return dao.getAllSyncTask();
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveSyncTask(org.openmrs.module.ugandaemrsync.model.SyncTask)
     */
    @Override
    public SyncTask saveSyncTask(SyncTask syncTask) throws APIException {
        if (syncTask.getCreator() == null) {
            syncTask.setCreator(userService.getUser(1));
        }
        return dao.saveSyncTask(syncTask);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getIncompleteActionSyncTask(java.lang.String)
     */
    @Override
    public List<SyncTask> getIncompleteActionSyncTask(String syncTaskTypeIdentifier) throws APIException {
        return dao.getIncompleteActionSyncTask(syncTaskTypeIdentifier);
    }

    /**
     * @param query
     * @return
     */
    @Override
    public List getDatabaseRecord(String query) {
        return dao.getDatabaseRecord(query);
    }

    /**
     * @param columns
     * @param finalQuery
     * @return
     */
    @Override
    public List getFinalList(List<String> columns, String finalQuery) {
        return dao.getFinalResults(columns, finalQuery);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#addVLToEncounter(java.lang.String, java.lang.String, java.lang.String, org.openmrs.Encounter, org.openmrs.Order)
     */
    public Encounter addVLToEncounter(String vlQualitative, String vlQuantitative, String vlDate, Encounter encounter, Order order) {
        if (!encounterHasVLDataAlreadySaved(encounter, order)) {
            Concept dateSampleTaken = Context.getConceptService().getConcept("163023");
            Concept viralLoadQualitative = Context.getConceptService().getConcept("1305");
            Concept viralLoadQuantitative = Context.getConceptService().getConcept("856");
            Concept valueCoded = null;

            String dateFormat = getDateFormat(vlDate);

            String vlQualitativeString = vlQualitative.replaceAll("\"", "");

            if (vlQualitativeString.contains("Target Not Detected") || vlQualitativeString.contains("Not detected")) {
                valueCoded = Context.getConceptService().getConcept("1306");
            } else if (vlQualitativeString.contains("FAILED")) {
                valueCoded = Context.getConceptService().getConcept("1304");
            } else {
                valueCoded = Context.getConceptService().getConcept("1301");
            }
            Concept viralLOadTestGroupConcept = null;
            if (order != null) {
                viralLOadTestGroupConcept = order.getConcept();
            } else {
                viralLOadTestGroupConcept = Context.getConceptService().getConcept(165412);
            }

            Obs dateSampleTakenObs = createObs(encounter, order, dateSampleTaken, null, convertStringToDate(vlDate, "00:00:00", dateFormat), null);
            Obs viralLoadQualitativeObs = createObs(encounter, order, viralLoadQualitative, valueCoded, null, null);
            Obs viralLoadQuantitativeObs = createObs(encounter, order, viralLoadQuantitative, null, null, Double.valueOf(vlQuantitative));

            Obs viralLoadTestGroupObs = createObs(encounter, order, viralLOadTestGroupConcept, null, null, null);
            viralLoadTestGroupObs.addGroupMember(dateSampleTakenObs);
            viralLoadTestGroupObs.addGroupMember(viralLoadQualitativeObs);
            viralLoadTestGroupObs.addGroupMember(viralLoadQuantitativeObs);

            //Void Similar observation
            voidObsFound(encounter, dateSampleTaken);
            voidObsFound(encounter, viralLoadQualitative);
            voidObsFound(encounter, viralLoadQuantitative);

            encounter.addObs(dateSampleTakenObs);
            encounter.addObs(viralLoadQualitativeObs);
            encounter.addObs(viralLoadQuantitativeObs);
            encounter.addObs(viralLoadTestGroupObs);

            try {
                if (order != null) {
                    Context.getOrderService().discontinueOrder(order, "Completed", new Date(), order.getOrderer(), order.getEncounter());
                }
            } catch (Exception e) {
                log.error("Failed to discontinue order", e);
            }
            Context.getObsService().saveObs(viralLoadTestGroupObs, "Adding Viral Load Data");
            return encounter;
        } else {
            if (order != null) {
                try {
                    Context.getOrderService().discontinueOrder(order, "Completed", new Date(), order.getOrderer(), order.getEncounter());
                } catch (Exception e) {
                    log.error("Failed to discontinue order", e);
                }
            }
            return encounter;
        }
    }

    public String getDateFormat(String date) {
        String dateFormat = "";
        if (date.contains("-")) {
            dateFormat = "yyyy-MM-dd";
        } else if (date.contains("/")) {
            dateFormat = "dd/MM/yyyy";
        }
        return dateFormat;
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getPatientByPatientIdentifier(java.lang.String)
     */
    public Patient getPatientByPatientIdentifier(String patientIdentifier) {
        try {
            return Context.getPatientService().getPatientIdentifiers(patientIdentifier, null, null, null, null).get(0).getPatient();
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#validateFacility(java.lang.String)
     */
    public boolean validateFacility(String facilityDHIS2UUID) {
        try {
            String globalProperty = Context.getAdministrationService().getGlobalProperty(GP_DHIS2_ORGANIZATION_UUID);
            return facilityDHIS2UUID.contentEquals(globalProperty);
        } catch (Exception e) {
            log.error("Failed to validate facility uuid", e);
            return false;
        }

    }

    public Collection<EncounterType> getEcounterTypes(String encounterTypesUUID) {
        Collection<EncounterType> encounterTypes = new ArrayList<>();
        encounterTypes.add(Context.getEncounterService().getEncounterTypeByUuid(encounterTypesUUID));
        return encounterTypes;
    }

    /**
     * Appends a time to a date
     *
     * @param dateString the date in string which will be
     * @param time
     * @param dateFormat
     * @return
     */
    public Date convertStringToDate(String dateString, String time, String dateFormat) {

        DateFormat format = new SimpleDateFormat(dateFormat, Locale.ENGLISH);
        Date date = null;
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        SimpleDateFormat formatterExt = new SimpleDateFormat("dd/MM/yyyy");

        try {
            date = format.parse(dateString);
            if (date != null && time != "") {
                date = formatter.parse(formatterExt.format(date) + " " + time);

            }
        } catch (ParseException e) {
            log.error("failed to convert date to string", e);
        }

        return date;
    }


    /**
     * This Method is used to generate an observation.
     *
     * @param encounter     the encounter which is has to be assigned to the observation
     * @param order         the order which has to be assigned to the observation
     * @param concept       the concept which is the question to the observation
     * @param valueCoded    concept which may be the answer to the question
     * @param valueDatetime the value date which may be the answer to the question.
     * @param valueNumeric  a numeric value which may be assigned to
     * @return
     */
    private Obs createObs(Encounter encounter, Order order, Concept concept, Concept valueCoded, Date valueDatetime, Double valueNumeric) {
        Obs newObs = new Obs();
        newObs.setConcept(concept);
        newObs.setValueCoded(valueCoded);
        newObs.setValueNumeric(valueNumeric);
        newObs.setValueDatetime(valueDatetime);
        newObs.setCreator(encounter.getCreator());
        newObs.setDateCreated(encounter.getDateCreated());
        newObs.setEncounter(encounter);
        newObs.setOrder(order);
        newObs.setPerson(encounter.getPatient());
        return newObs;
    }

    /**
     * This method is used to void any observation that is similar to what is being added
     *
     * @param encounter the encounter for the observation that will be voided
     * @param concept   the concept for the encounter that will be voided
     */
    private void voidObsFound(Encounter encounter, Concept concept) {
        ObsService obsService = Context.getObsService();
        List<Obs> obsListToVoid = obsService.getObservationsByPersonAndConcept(encounter.getPatient(), concept);
        for (Obs obsToVoid : obsListToVoid) {
            if (obsToVoid.getEncounter() == encounter) {
                obsService.voidObs(obsToVoid, "Observation has been replaced or updated.");
            }
        }
    }

    /**
     * /**
     *
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getHealthCenterCode()
     */
    public String getHealthCenterCode() {
        SyncGlobalProperties syncGlobalProperties = new SyncGlobalProperties();
        return syncGlobalProperties.getGlobalProperty(GP_DHIS2_ORGANIZATION_UUID);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getHealthCenterName()
     */
    public String getHealthCenterName() {
        SyncGlobalProperties syncGlobalProperties = new SyncGlobalProperties();
        return syncGlobalProperties.getGlobalProperty("aijar.healthCenterName");
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getPatientIdentifier(org.openmrs.Patient, java.lang.String)
     */
    public String getPatientIdentifier(Patient patient, String patientIdentifierTypeUUID) {
        String query = "select patient_identifier.identifier from patient_identifier inner join patient_identifier_type on(patient_identifier.identifier_type=patient_identifier_type.patient_identifier_type_id) where patient_identifier_type.uuid in ('" + patientIdentifierTypeUUID + "') AND patient_id=" + patient.getPatientId() + "";
        List list = Context.getAdministrationService().executeSQL(query, true);
        String patientARTNO = "";
        if (!list.isEmpty()) {
            patientARTNO = list.get(0).toString().replace("[", "").replace("]", "");
        }
        return patientARTNO;
    }

    public boolean encounterHasVLDataAlreadySaved(Encounter encounter, Order order) {

        if (encounter != null && order == null) {
            Set<Obs> obs = encounter.getAllObs(false);
            return obs.stream().map(Obs::getConcept).collect(Collectors.toSet()).contains(Context.getConceptService().getConcept(165412));
        } else {
            return testOrderHasResults(order);
        }
    }

    public Properties getUgandaEMRProperties() {
        Properties properties = new Properties();
        String appDataDir = OpenmrsUtil.getApplicationDataDirectory();
        String facilityDHIS2ID = Context.getAdministrationService().getGlobalProperty(GP_DHIS2_ORGANIZATION_UUID);
        if (!appDataDir.endsWith(System.getProperty("file.separator")))
            appDataDir = appDataDir + System.getProperty("file.separator");
        String filePath = appDataDir + "ugandaemr-setting.properties";

        try {
            File newUgandaEMRSettingFile = new File(filePath);
            if (!newUgandaEMRSettingFile.exists()) {
                newUgandaEMRSettingFile.createNewFile();

                FileInputStream fileInputStream = new FileInputStream(filePath);
                if (facilityDHIS2ID != null && !facilityDHIS2ID.equalsIgnoreCase("")) {
                    properties.setProperty(GP_DHIS2_ORGANIZATION_UUID, facilityDHIS2ID);
                    properties.setProperty(SYNC_METRIC_DATA, "true");
                } else {
                    properties.setProperty(GP_DHIS2_ORGANIZATION_UUID, "");
                    properties.setProperty(SYNC_METRIC_DATA, "false");
                }
                properties.load(fileInputStream);

                properties.store(new FileOutputStream(filePath), null);


            } else {
                FileReader reader = new FileReader(filePath);
                properties.load(reader);
            }
        } catch (FileNotFoundException e) {
            log.error("ugandaemr-setting.properties file Not found", e);
        } catch (IOException e) {
            log.error(e);
        }
        return properties;

    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveSyncFhirProfile(SyncFhirProfile)
     */
    @Override
    public SyncFhirProfile saveSyncFhirProfile(SyncFhirProfile syncFhirProfile) {
        return dao.saveSyncFhirProfile(syncFhirProfile);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirProfileById(java.lang.Integer)
     */
    @Override
    public SyncFhirProfile getSyncFhirProfileById(Integer id) {
        return dao.getSyncFhirProfileById(id);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirProfileByUUID(java.lang.String)
     */
    @Override
    public SyncFhirProfile getSyncFhirProfileByUUID(String uuid) {
        return dao.getSyncFhirProfileByUUID(uuid);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirProfileByScheduledTaskName(java.lang.String)
     */
    @Override
    public SyncFhirProfile getSyncFhirProfileByScheduledTaskName(String scheduledTaskName) {

        TaskDefinition taskDefinition = Context.getSchedulerService().getTaskByName(scheduledTaskName);

        if (taskDefinition != null) {
            String syncFhirProfileUUID = taskDefinition.getProperty("syncFhirProfileUUID");
            SyncFhirProfile syncFhirProfile = getSyncFhirProfileByUUID(syncFhirProfileUUID);
            if (syncFhirProfile != null) {
                return syncFhirProfile;
            }
        }
        return null;
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveFHIRResource(SyncFhirResource)
     */
    @Override
    public SyncFhirResource saveFHIRResource(SyncFhirResource syncFHIRResource) {
        return dao.saveSyncFHIRResource(syncFHIRResource);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveFHIRResource(SyncFhirResource)
     */
    @Override
    public List<SyncFhirResource> getSyncFHIRResourceBySyncFhirProfile(SyncFhirProfile syncFhirProfile, boolean includeSynced) {
        return dao.getSyncResourceBySyncFhirProfile(syncFhirProfile, includeSynced);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFHIRResourceById(java.lang.Integer)
     */
    @Override
    public SyncFhirResource getSyncFHIRResourceById(Integer id) {
        return dao.getSyncFHIRResourceById(id);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#markSyncFHIRResourceSynced(SyncFhirResource)
     */
    @Override
    public SyncFhirResource markSyncFHIRResourceSynced(SyncFhirResource syncFhirResources) {
        Date today = new Date();
        syncFhirResources.setSynced(true);
        syncFhirResources.setDateSynced(today);
        syncFhirResources.setExpiryDate(UgandaEMRSyncUtil.addDaysToDate(today, syncFhirResources.getGeneratorProfile().getDurationToKeepSyncedResources()));
        return dao.saveSyncFHIRResource(syncFhirResources);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getExpiredSyncFHIRResources(java.util.Date)
     */
    @Override
    public List<SyncFhirResource> getExpiredSyncFHIRResources(Date date) {
        return dao.getExpiredSyncFHIRResources(date);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getUnSyncedFHirResources(org.openmrs.module.ugandaemrsync.model.SyncFhirProfile)
     */
    @Override
    public List<SyncFhirResource> getUnSyncedFHirResources(SyncFhirProfile syncFhirProfile) {
        return dao.getUnSyncedFHirResources(syncFhirProfile);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#purgeExpiredFHIRResource(java.util.Date)
     */
    @Override
    public void purgeExpiredFHIRResource(Date date) {
        for (SyncFhirResource syncFHIRResource : getExpiredSyncFHIRResources(date)) {
            dao.purgeExpiredFHIRResource(syncFHIRResource);
        }
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveSyncFhirProfileLog(SyncFhirProfileLog)
     */
    @Override
    public SyncFhirProfileLog saveSyncFhirProfileLog(SyncFhirProfileLog syncFhirProfileLog) {
        return dao.saveSyncFhirProfileLog(syncFhirProfileLog);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirProfileLogByProfileAndResourceName(SyncFhirProfile, java.lang.String)
     */
    @Override
    public List<SyncFhirProfileLog> getSyncFhirProfileLogByProfileAndResourceName(SyncFhirProfile syncFhirProfile, String resourceType) {
        return dao.getSyncFhirProfileLogByProfileAndResourceName(syncFhirProfile, resourceType);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getLatestSyncFhirProfileLogByProfileAndResourceName(SyncFhirProfile, java.lang.String)
     */
    @Override
    public SyncFhirProfileLog getLatestSyncFhirProfileLogByProfileAndResourceName(SyncFhirProfile syncFhirProfile, String resourceType) {

        List<SyncFhirProfileLog> syncFhirProfileLogs = getSyncFhirProfileLogByProfileAndResourceName(syncFhirProfile, resourceType);

        if (syncFhirProfileLogs.size() > 0)
            return syncFhirProfileLogs.get(0);
        else
            return null;
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFHIRCaseBySyncFhirProfileAndPatient(SyncFhirProfile, org.openmrs.Patient, java.lang.String)
     */
    @Override
    public SyncFhirCase getSyncFHIRCaseBySyncFhirProfileAndPatient(SyncFhirProfile syncFhirProfile, Patient patient, String caseIdentifier) {
        return dao.getSyncFHIRCaseBySyncFhirProfileAndPatient(syncFhirProfile, patient, caseIdentifier);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#saveSyncFHIRCase(SyncFhirCase)
     */
    @Override
    public SyncFhirCase saveSyncFHIRCase(SyncFhirCase syncFHIRCase) {
        return dao.saveSyncFHIRCase(syncFHIRCase);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getAllSyncFhirProfile()
     */
    @Override
    public List<SyncFhirProfile> getAllSyncFhirProfile() {
        return dao.getAllSyncFhirProfile();
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirCasesByProfile(org.openmrs.module.ugandaemrsync.model.SyncFhirProfile)
     */
    @Override
    public List<SyncFhirCase> getSyncFhirCasesByProfile(SyncFhirProfile syncFhirProfile) {
        return dao.getSyncFhirCasesByProfile(syncFhirProfile);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#testOrderHasResults(org.openmrs.Order)
     */
    public boolean testOrderHasResults(Order order) {
        boolean hasOrder = false;

        List list = Context.getAdministrationService().executeSQL("select obs_id from obs where order_id=" + order.getOrderId() + "", true);

        if (!list.isEmpty()) {
            hasOrder = true;
        } else if (resultsEnteredOnEncounter(order)) {

            hasOrder = true;
        }
        return hasOrder;
    }



    /**
     * Checks if the test ordered already has detached results entered on separately on the encounter the encounter
     *
     * @param order order to be checked.
     * @return true when results have already been entered or false when results have not yet been entered
     */
    private boolean resultsEnteredOnEncounter(Order order) {

        boolean resultsEnteredOnEncounter = false;

        Set<Obs> allObs = order.getEncounter().getAllObs(false);
        for (Obs obs1 : allObs) {
            if (obs1.getConcept().getConceptId().equals(order.getConcept().getConceptId()) && (!obs1.getValueAsString(Locale.ENGLISH).equals("") || obs1.getValueAsString(Locale.ENGLISH) != null)) {
                resultsEnteredOnEncounter = true;
                return true;
            }
        }

        Set<Concept> conceptSet = allObs.stream().map(Obs::getConcept).collect(Collectors.toSet());
        List<Concept> members = order.getConcept().getSetMembers();

        if (members.size() > 0) {
            for (Concept concept : members) {
                if (conceptSet.contains(concept)) {
                    resultsEnteredOnEncounter = true;
                    return true;
                }
            }
        }

        return resultsEnteredOnEncounter;
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirProfileByName(java.lang.String)
     */
    public List<SyncFhirProfile> getSyncFhirProfileByName(String name) {
        return dao.getSyncFhirProfileByName(name);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirCaseByUUDI(java.lang.String)
     */
    @Override
    public SyncFhirCase getSyncFhirCaseByUUDI(String uuid) {
        return dao.getSyncFhirCaseByUUDI(uuid);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getAllSyncFhirCase()
     */
    @Override
    public List<SyncFhirCase> getAllSyncFhirCase() {
        return dao.getAllSyncFhirCase();
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirCaseById(java.lang.Integer)
     */
    @Override
    public SyncFhirCase getSyncFhirCaseById(Integer id) {
        return dao.getSyncFhirCaseById(id);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getAllSyncFhirProfileLog()
     */
    @Override
    public List<SyncFhirProfileLog> getAllSyncFhirProfileLog() {
        return dao.getAllSyncFhirProfileLog();
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirProfileLogByUUID(java.lang.String)
     */
    @Override
    public SyncFhirProfileLog getSyncFhirProfileLogByUUID(String uuid) {
        return dao.getSyncFhirProfileLogByUUID(uuid);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirProfileLogById(java.lang.Integer)
     */
    @Override
    public SyncFhirProfileLog getSyncFhirProfileLogById(Integer id) {
        return dao.getSyncFhirProfileLogById(id);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getAllFHirResources()
     */
    @Override
    public List<SyncFhirResource> getAllFHirResources() {
        return dao.getAllFHirResources();
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirResourceByUUID(java.lang.String)
     */
    @Override
    public SyncFhirResource getSyncFhirResourceByUUID(String uuid) {
        return dao.getSyncFhirResourceByUUID(uuid);
    }


    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncFhirProfileLogByProfile(org.openmrs.module.ugandaemrsync.model.SyncFhirProfile)
     */
    @Override
    public List<SyncFhirProfileLog> getSyncFhirProfileLogByProfile(SyncFhirProfile syncFhirProfile) {
        return dao.getSyncFhirProfileLogByProfile(syncFhirProfile);
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#addTestResultsToEncounter(org.json.JSONObject, org.openmrs.Order)
     */
    public Encounter addTestResultsToEncounter(JSONObject bundleResults, Order order) {
        Encounter encounter = order.getEncounter();
        Encounter returningEncounter = null;
        if (!resultsEnteredOnEncounter(order)) {
            JSONArray jsonArray = bundleResults.getJSONArray("entry");

            JSONArray filteredDiagnosticReportArray = searchForJSONOBJECTSByKey(jsonArray, "resourceType", "DiagnosticReport");

            JSONArray filteredObservationArray = searchForJSONOBJECTSByKey(jsonArray, "resourceType", "Observation");

            for (Object jsonObject : filteredDiagnosticReportArray) {
                JSONObject diagnosticReport = new JSONObject(jsonObject.toString());

                returningEncounter = processTestResults(diagnosticReport, encounter, filteredObservationArray, order);
            }

        }

        return returningEncounter;
    }


    private JSONArray searchForJSONOBJECTSByKey(JSONArray array, String key, String searchValue) {
        JSONArray filtedArray = new JSONArray();
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = null;
            try {
                obj = array.getJSONObject(i);
                if (obj.getJSONObject("resource").getString(key).equals(searchValue)) {
                    filtedArray.put(obj);
                }
            } catch (JSONException e) {
                log.error(e);
            }
        }

        return filtedArray;
    }

    private JSONObject searchForJSONOBJECTByKey(JSONArray array, String key, String searchValue) {

        JSONObject obj = null;
        for (int i = 0; i < array.length(); i++) {

            try {
                obj = array.getJSONObject(i);
                if (obj.getJSONObject("resource").getString(key).equals(searchValue)) {
                    return obj;
                }
            } catch (JSONException e) {
                log.error(e);
            }
        }

        return obj;
    }

    private Encounter processTestResults(JSONObject jsonObject, Encounter encounter, JSONArray observationArray, Order order) {
        JSONObject diagnosticReport = jsonObject.getJSONObject("resource");

        String orderCode = diagnosticReport.getJSONObject("code").getJSONArray("coding").getJSONObject(0).getString("code");
        String orderCodeSystem = diagnosticReport.getJSONObject("code").getJSONArray("coding").getJSONObject(0).getString("system");

        Concept orderConcept = Context.getConceptService().getConceptByMapping(orderCode, getConceptSourceBySystemURL(orderCodeSystem).getName());


        boolean orderConceptIsaSet = (orderConcept.getSetMembers().size() > 0);

        Obs groupingObservation = createObs(order.getEncounter(), order, orderConcept, null, null, null);

        JSONArray jsonArray = diagnosticReport.getJSONArray("result");
        for (Object resultReferenceObject : jsonArray) {
            JSONObject resultReference = new JSONObject(resultReferenceObject.toString());
            Concept concept = null;

            String reference = resultReference.getString("reference");

            JSONObject observation = searchForJSONOBJECTByKey(observationArray, "id", reference).getJSONObject("resource");

            String code = observation.getJSONObject("code").getJSONArray("coding").getJSONObject(0).getString("code");
            String system = observation.getJSONObject("code").getJSONArray("coding").getJSONObject(0).getString("system");
            ConceptSource conceptSource = getConceptSourceBySystemURL(system);

            if (conceptSource != null) {
                concept = Context.getConceptService().getConceptByMapping(code, conceptSource.getName());
            }

            if (concept == null) {
                continue;
            }


            Obs obs;

            if (orderConceptIsaSet) {
                obs = createObs(order.getEncounter(), order, concept, null, null, null);
                groupingObservation.addGroupMember(obs);
            } else {
                obs = groupingObservation;
            }

            assert obs != null;
            switch (concept.getDatatype().getUuid()) {
                case ConceptDatatype.CODED_UUID:
                    String valueCodedCode = observation.getJSONObject("valueCodeableConcept").getJSONArray("coding").getJSONObject(0).getString("code");
                    String valueCodedSystemURL = observation.getJSONObject("valueCodeableConcept").getJSONArray("coding").getJSONObject(0).getString("system");
                    Concept valueCodedConcept = Context.getConceptService().getConceptByMapping(valueCodedCode, getConceptSourceBySystemURL(valueCodedSystemURL).getName());
                    obs.setValueCoded(valueCodedConcept);
                    break;
                case ConceptDatatype.NUMERIC_UUID:
                    obs.setValueNumeric(observation.getJSONObject("valueQuantity").getDouble("value"));

                    break;
                case ConceptDatatype.BOOLEAN_UUID:
                    obs.setValueBoolean(observation.getBoolean("valueBoolean"));

                    break;
                case ConceptDatatype.TEXT_UUID:
                    obs.setValueText(observation.getString("valueString"));
                    break;
            }

            if (obs.getValueAsString(Locale.ENGLISH).isEmpty()) {
                continue;
            }

            encounter.addObs(obs);

        }
        if (orderConceptIsaSet) {
            encounter.addObs(groupingObservation);
        }

        Context.getEncounterService().saveEncounter(encounter);


        return encounter;
    }


    private ConceptSource getConceptSourceBySystemURL(String systemURL) {
        FhirConceptSourceService fhirConceptSourceService = null;
        ConceptSource conceptSource = null;
        try {
            Field serviceContextField = Context.class.getDeclaredField("serviceContext");
            serviceContextField.setAccessible(true);

            ApplicationContext applicationContext = ((ServiceContext) serviceContextField.get(null))
                    .getApplicationContext();
            fhirConceptSourceService = applicationContext.getBean(FhirConceptSourceService.class);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error(e);
        }
        assert fhirConceptSourceService != null;
        if (fhirConceptSourceService.getFhirConceptSourceByUrl(systemURL).isPresent()) {
            conceptSource = fhirConceptSourceService.getFhirConceptSourceByUrl(systemURL).get().getConceptSource();
        }

        return conceptSource;
    }

    /**
     * @see org.openmrs.module.ugandaemrsync.api.UgandaEMRSyncService#getSyncedFHirResources(org.openmrs.module.ugandaemrsync.model.SyncFhirProfile)
     */
    @Override
    public List<SyncFhirResource> getSyncedFHirResources(SyncFhirProfile syncFhirProfile) {
        return dao.getSyncedFHirResources(syncFhirProfile);
    }
}

