package org.etsi.osl.osom.management;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.common.model.service.ResourceRef;
import org.etsi.osl.tmf.common.model.service.ServiceStateType;
import org.etsi.osl.tmf.common.model.Any;
import org.etsi.osl.tmf.pm628.model.*;
import org.etsi.osl.tmf.rcm634.model.ResourceSpecificationRef;
import org.etsi.osl.tmf.ri639.model.Resource;
import org.etsi.osl.tmf.ri639.model.ResourceCreate;
import org.etsi.osl.tmf.ri639.model.ResourceStatusType;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Component(value = "metricoOrchestrationService") //bean name
public class MetricoOrchestrationService implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(NFVOrchestrationService.class.getName());

	@Autowired
	private ProducerTemplate producerTemplate;
	@Value("{PM_MEASUREMENT_COLLECTION_JOB_ADD}")
	private String PM_MEASUREMENT_COLLECTION_JOB_ADD = "";

	@Value("${spring.application.name}")
	private String compname;
	
	
	@Autowired
	private ServiceOrderManager serviceOrderManager;
	

    @Override
    public void execute(DelegateExecution execution) {
		ServiceOrder sorder = serviceOrderManager.retrieveServiceOrder(execution.getVariable("orderid").toString());
		ServiceUpdate su = new ServiceUpdate();// the object to update the service
		Service aService = serviceOrderManager.retrieveService((String) execution.getVariable("contextServiceId"));
		logger.info("Service name:" + aService.getName());
		logger.info("Service state:" + aService.getState());
		logger.info("Request to TMF628 for Service: " + aService.getId());


		ServiceSpecification spec = serviceOrderManager.retrieveServiceSpec(aService.getServiceSpecificationRef().getId());

		if (spec != null) {

			Characteristic serviceCharacteristic;
			MeasurementCollectionJobFVO mcjFVO = new MeasurementCollectionJobFVO();
			mcjFVO.setProducingApplicationId(aService.getId());

			serviceCharacteristic = aService.getServiceCharacteristicByName("_MT_CHARACTERISTIC_NAME");
			String characteristicName = String.valueOf(serviceCharacteristic.getValue());
			mcjFVO.setOutputFormat(characteristicName);

			serviceCharacteristic = aService.getServiceCharacteristicByName("_MT_SERVICEUUID");
			String cfs_id = String.valueOf(serviceCharacteristic.getValue());
			mcjFVO.setConsumingApplicationId(cfs_id);

			serviceCharacteristic = aService.getServiceCharacteristicByName("_MT_END_TIME");
			String endTimeString = 	String.valueOf(serviceCharacteristic.getValue());
			ScheduleDefinitionFVO scheduleDefinition = new ScheduleDefinitionFVO();
			if (endTimeString != null) {
				OffsetDateTime endTime = convertStringToOffsetDateTime(endTimeString, DateTimeFormat.ISO.DATE_TIME );
				scheduleDefinition.setScheduleDefinitionEndTime(endTime);
			} else{
				OffsetDateTime endTime = OffsetDateTime.now().plusHours(1);
				scheduleDefinition.setScheduleDefinitionEndTime(endTime);
			}

			serviceCharacteristic = aService.getServiceCharacteristicByName("_MT_START_TIME");
			String startTimeString = String.valueOf(serviceCharacteristic.getValue());
			if (startTimeString != null) {
				OffsetDateTime startTime = convertStringToOffsetDateTime(startTimeString, DateTimeFormat.ISO.DATE_TIME );
				scheduleDefinition.setScheduleDefinitionStartTime(startTime);
			} else{
				OffsetDateTime startTime = OffsetDateTime.now();
				scheduleDefinition.setScheduleDefinitionStartTime(startTime);
			}
			List<ScheduleDefinitionFVO> scheduleDefinitions = new ArrayList<>();
			scheduleDefinitions.add(scheduleDefinition);
			mcjFVO.setScheduleDefinition(scheduleDefinitions);

			serviceCharacteristic = aService.getServiceCharacteristicByName("_MT_RECURRING_INTERVAL");
			String recurringIntervalString = String.valueOf(serviceCharacteristic.getValue());
			if (recurringIntervalString != null) {
				if (Granularity.contains(recurringIntervalString)){
					Granularity recurringInterval = Granularity.valueOf(recurringIntervalString);
					mcjFVO.setGranularity(recurringInterval);
				} else {
					logger.error("Invalid _MT_RECURRING_INTERVAL value. Valid values are:" + Granularity.getPossibleValues() + " It will be set to 1 minute.");
					Granularity recurringInterval = Granularity.G_1M;
					mcjFVO.setGranularity(recurringInterval);
				}
			} else {
				Granularity recurringInterval = Granularity.G_1M;
				mcjFVO.setGranularity(recurringInterval);
			}

			serviceCharacteristic = aService.getServiceCharacteristicByName("_MT_TYPE");
			String monitoringType = String.valueOf(serviceCharacteristic.getValue());
			DataAccessEndpointFVO dataAccessEndpoint = new DataAccessEndpointFVO();
			dataAccessEndpoint.setType(monitoringType);

			serviceCharacteristic = aService.getServiceCharacteristicByName("_MT_QUERY");
			String monitoringQuery = String.valueOf(serviceCharacteristic.getValue());
			serviceCharacteristic = aService.getServiceCharacteristicByName("_MT_URL");
			String monitoringURL = String.valueOf(serviceCharacteristic.getValue());
            try {
                URI monitoringURI = createUri(monitoringURL, monitoringQuery);
				dataAccessEndpoint.setUri(monitoringURI);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

			List<DataAccessEndpointFVO> dataAccessEndpoints = new ArrayList<>();
			dataAccessEndpoints.add(dataAccessEndpoint);
			mcjFVO.setDataAccessEndpoint(dataAccessEndpoints);

			MeasurementCollectionJob mcj = addMeasurementCollectionJob(mcjFVO);

			if  (mcj != null){

				ResourceSpecificationRef resourceSpecificationRef = spec.getResourceSpecification().stream().findFirst().get();
				Resource resourceMT = createRelatedResource( resourceSpecificationRef, sorder, aService, mcj );
				ResourceRef resourceRef = new ResourceRef();

				resourceRef.setId( resourceMT.getId() );
				resourceRef.setName( resourceMT.getName());
				resourceRef.setType( resourceMT.getType());
				su.addSupportingResourceItem( resourceRef );
				su.setState(ServiceStateType.RESERVED);
				Note successNoteItem = new Note();
				successNoteItem.setText(String.format("Requesting METRICO to create a new monitoring job"));
				successNoteItem.setDate(OffsetDateTime.now(ZoneOffset.UTC).toString());
				successNoteItem.setAuthor(compname);
				su.addNoteItem(successNoteItem);
				Service supd = serviceOrderManager.updateService(aService.getId(), su, false);

			} else {
				logger.error("Measurement Collection Job was not created.");
			}



		}
	}





//	Methods created to use in this class
	public static OffsetDateTime convertStringToOffsetDateTime(String dateTimeString, DateTimeFormat.ISO pattern) {
		DateTimeFormatter formatter;
		switch (pattern) {
			case DATE:
				formatter = DateTimeFormatter.ISO_DATE;
				break;
			case TIME:
				formatter = DateTimeFormatter.ISO_TIME;
				break;
			case DATE_TIME:
				formatter = DateTimeFormatter.ISO_DATE_TIME;
				break;
			default:
				throw new IllegalArgumentException("Unsupported DateTimeFormat.ISO pattern");
		}
		try {
			OffsetDateTime.parse(dateTimeString, formatter);
			return OffsetDateTime.parse(dateTimeString, formatter);
		} catch (DateTimeParseException e) {
			logger.error(e.getMessage());
			return null;
		}

	}

	public static URI createUri(String url, String query) throws URISyntaxException {
		return new URI(url + "?" + query);
	}

	public static <T> T toJsonObj(String content, Class<T> valueType)  throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		return mapper.readValue( content, valueType);
	}

	public static <T> String toJsonString(T object) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		try {
			return mapper.writeValueAsString(object);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		}
	}

	public MeasurementCollectionJob addMeasurementCollectionJob(MeasurementCollectionJobFVO mcjFVO) {

		logger.debug("Will create a new Measurement Collection Job");
		try {
			Object response = producerTemplate.
					requestBody( PM_MEASUREMENT_COLLECTION_JOB_ADD, mcjFVO);
			if ( !(response instanceof String)) {
				logger.error("Measurement Collection Job object is wrong.");
				return null;
			}
			logger.debug("retrieveMeasurementCollectionJobById response is: " + response);
			MeasurementCollectionJob mcj = toJsonObj( (String)response, MeasurementCollectionJob.class);
			return mcj;
		}catch (Exception e) {
			logger.error("Cannot create a new Measurement Collection Job. " + e.toString());
		}
		return null;
	}


	/**
	 *
	 * The resource maps the created MCJ
	 * @param rSpecRef
	 * @param sOrder
	 * @param aService
	 * @return
	 */
	private Resource createRelatedResource(ResourceSpecificationRef rSpecRef, ServiceOrder sOrder, Service aService, MeasurementCollectionJob mcj) {

		ResourceCreate resCreate = new ResourceCreate();
		resCreate.setName(   rSpecRef.getName() + "-" + aService.getId() );
		resCreate.setStartOperatingDate( aService.getStartDate() );
		resCreate.setEndOperatingDate(aService.getEndDate());
		resCreate.setResourceStatus (ResourceStatusType.RESERVED);

		ResourceSpecificationRef rSpecRefObj = new ResourceSpecificationRef() ;
		rSpecRefObj.id(rSpecRef.getId())
				.name( rSpecRef.getName())
				.setType(rSpecRef.getType());
		resCreate.setResourceSpecification(rSpecRefObj);

		org.etsi.osl.tmf.ri639.model.Characteristic resCharacteristicItem =  new org.etsi.osl.tmf.ri639.model.Characteristic();
		resCharacteristicItem.setName( "_MT_MCJ_REF" );
		resCharacteristicItem.setValueType( "TEXT" );
		Any val = new Any();
		val.setValue( mcj.getUuid() );
		val.setAlias( mcj.getUuid() );
		resCharacteristicItem.setValue( val );
		resCreate.addResourceCharacteristicItem(  resCharacteristicItem );


		// 1) need to copy the characteristics of the Resource Specification (use this instead of @param rSpecRef) and populate them with value from the aService (see GCOrchestrationService)
		// 2) also need to populate the characteristic _MT_MCJ_REF with the UUID of the created MCJ / pass it as @param mcj

		return serviceOrderManager.createResource( resCreate, sOrder, rSpecRef.getId() );
	}

}
