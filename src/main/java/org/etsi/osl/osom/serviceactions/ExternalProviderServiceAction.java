package org.etsi.osl.osom.serviceactions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.management.ServiceOrderManager;
import org.etsi.osl.osom.partnerservices.PartnerOrganizationServicesManager;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.common.model.service.ServiceStateType;
import org.etsi.osl.tmf.pm632.model.Organization;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceActionQueueAction;
import org.etsi.osl.tmf.sim638.model.ServiceActionQueueItem;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.etsi.osl.tmf.so641.model.ServiceOrderActionType;
import org.etsi.osl.tmf.so641.model.ServiceOrderItem;
import org.etsi.osl.tmf.so641.model.ServiceOrderUpdate;

@Component(value = "ExternalProviderServiceAction") //bean name
public class ExternalProviderServiceAction  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( ExternalProviderServiceAction.class.getName() );

	@Value("${spring.application.name}")
	private String compname;

    @Autowired
    private ServiceOrderManager serviceOrderManager;

	@Autowired
	private PartnerOrganizationServicesManager partnerOrganizationServicesManager;
	

	@Value("${THIS_PARTNER_NAME}")
	private String THIS_PARTNER_NAME = "";
	
	public void execute(DelegateExecution execution) {
		
		logger.info("ExternalProviderServiceAction:" + execution.getVariableNames().toString() );
		String externalServiceOrderId = (String) execution.getVariable("externalServiceOrderId") ;
		String externalPartnerServiceId = (String) execution.getVariable("externalPartnerServiceId") ;
		String organizationId = (String) execution.getVariable("organizationId") ;
		
		

		ServiceActionQueueItem item;
		try {
			ObjectMapper mapper = new ObjectMapper();
			item = mapper.readValue( execution.getVariable("serviceActionItem").toString(), ServiceActionQueueItem.class);
		} catch (JsonMappingException e) {
			e.printStackTrace();
			return;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return;
		}
		
		
		logger.debug("Checking Order Status from partner with organizationId=" + organizationId + " of Order externalServiceOrderId= " + externalServiceOrderId );

		
		
		Organization orgz = serviceOrderManager.getExternalPartnerOrganization( organizationId );
		if ( orgz ==null ) {

			logger.debug("Organization is NULL");
		}

		Note noteItem = new Note();
		noteItem.setText("Service Action ExternalProviderServiceAction from " + THIS_PARTNER_NAME + " action " + item.getAction().toString());
		noteItem.author(compname);
		noteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
		
		if ( externalPartnerServiceId != null ) {
			//do nothing if the service contains the field externalPartnerServiceId
		} else {
			ServiceOrderUpdate servOrder = new ServiceOrderUpdate();
			
			servOrder.addNoteItem(noteItem);
			ServiceOrder externalSOrder = partnerOrganizationServicesManager.retrieveServiceOrder( orgz, externalServiceOrderId );
			if (externalSOrder != null ) {
				logger.info("External partner organization order state:" + externalSOrder.getState()  );
				for (ServiceOrderItem ext_soi : externalSOrder.getOrderItem()) {
					
					if ( item.getAction().equals( ServiceActionQueueAction.DEACTIVATE ) || item.getAction().equals( ServiceActionQueueAction.TERMINATE ) ) {
						ext_soi.getService().setState( ServiceStateType.TERMINATED );					
					}
					ext_soi.action(ServiceOrderActionType.MODIFY);
					
					servOrder.addOrderItemItem(ext_soi);
				}
			}
			
			partnerOrganizationServicesManager.updateExternalServiceOrder(externalServiceOrderId, servOrder, orgz);

		}
		

		Service aService = null;
		if (execution.getVariable("Service")!=null) {
			ObjectMapper mapper = new ObjectMapper();

			try {
				aService = mapper.readValue( execution.getVariable("Service").toString(), Service.class);
				item = mapper.readValue( execution.getVariable("serviceActionItem").toString(), ServiceActionQueueItem.class);
				ServiceUpdate localServiceUpd = new ServiceUpdate();
				Note n = new Note();
				n.setText("Service Action ExternalProviderServiceAction. Action: " + item.getAction() );
				n.setAuthor( compname );
				n.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
				localServiceUpd.addNoteItem( n );

				
				
				if ( externalPartnerServiceId != null ) {
					ServiceUpdate externServUpdate = new ServiceUpdate();
					externServUpdate.addNoteItem(noteItem);
					for (Characteristic serviceChar : aService.getServiceCharacteristic() ) {
						serviceChar.setUuid( null );
						if ( serviceChar.getName().equals("externalPartnerServiceId") || serviceChar.getName().equals("externalServiceOrderId")) {
							;//do nothing. Do not copy these two
						}else {
							externServUpdate.addServiceCharacteristicItem(serviceChar);
						}
					}
					Service responseService = partnerOrganizationServicesManager.updateExternalService(externalPartnerServiceId, externServUpdate, orgz);
					
					//reflect characteristics and status here from response					
					localServiceUpd.setState(responseService.getState() );
					for (Characteristic serviceChar : responseService.getServiceCharacteristic() ) {
						serviceChar.setUuid( null );
						localServiceUpd.addServiceCharacteristicItem(serviceChar);
					}
				}
				
				

				serviceOrderManager.deleteServiceActionQueueItem( item );			
				serviceOrderManager.updateService( aService.getId() , localServiceUpd, false);
				
				//need to clear any alarm if action was EXEC_ACTION
					
			} catch (JsonMappingException e1) {
				e1.printStackTrace();
			} catch (JsonProcessingException e1) {
				e1.printStackTrace();
			}
			
			
			
		}

		
		
	}

}
