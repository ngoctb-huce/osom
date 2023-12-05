package org.etsi.osl.osom.serviceactions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.management.ServiceOrderManager;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.etsi.osl.tmf.common.model.UserPartRoleType;
import org.etsi.osl.tmf.common.model.service.ResourceRef;
import org.etsi.osl.tmf.common.model.service.ServiceRef;
import org.etsi.osl.tmf.prm669.model.RelatedParty;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceActionQueueAction;
import org.etsi.osl.tmf.sim638.model.ServiceActionQueueItem;

@Component(value = "serviceActionCheck") //bean name
public class ServiceActionCheck implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( ServiceActionCheck.class.getName() );

	@Value("${spring.application.name}")
	private String compname;

    @Autowired
    private ServiceOrderManager serviceOrderManager;
    
	public void execute(DelegateExecution execution) {

		logger.info("ServiceActionCheck:" + execution.getVariableNames().toString() );
		
		
		
		if (execution.getVariable("serviceActionItem")!=null) {
			logger.debug("Will check status of serviceActionItem of ref:" + execution.getVariable("serviceActionItem") );
			ObjectMapper mapper = new ObjectMapper();
			
			ServiceActionQueueItem item;
			execution.setVariable("saction", "HandleManuallyAction");
			try {
				item = mapper.readValue( execution.getVariable("serviceActionItem").toString(), ServiceActionQueueItem.class);
			} catch (JsonMappingException e) {
				e.printStackTrace();
				return;
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				return;
			}
			
			
			Service aService = serviceOrderManager.retrieveService( item.getServiceRefId() );
			if ( aService == null ) {
				logger.error("aService is NULL!"  );	
				execution.setVariable("saction", "HandleManuallyAction");
				return;
			} else {
				if ( aService.getSupportingService()!=null) {
					for (ServiceRef aSupportingService : aService.getSupportingService() ) {
						logger.debug("aSupportingService:" + aSupportingService.getName() );				
					}
				}

				if ( aService.getSupportingResource()!=null) {
					for (ResourceRef aSupportingResource : aService.getSupportingResource() ) {
						logger.debug("aSupportingResource:" + aSupportingResource.getName() );				
					}				
					
				}				
			}
			
			
			
			if ( item.getAction().equals( ServiceActionQueueAction.EVALUATE_STATE_CHANGE_TOACTIVE  ) ) {
				execution.setVariable("saction", "HandleActiveStateChanged");
			} else if ( item.getAction().equals( ServiceActionQueueAction.EVALUATE_STATE_CHANGE_TOINACTIVE  ) ) {
				execution.setVariable("saction", "HandleInactiveStateChanged");
			} else if ( item.getAction().equals( ServiceActionQueueAction.EVALUATE_CHARACTERISTIC_CHANGED  ) || item.getAction().equals( ServiceActionQueueAction.EVALUATE_CHARACTERISTIC_CHANGED_MANODAY2  ) ) {
				
				execution.setVariable("saction", "HandleEvaluateService");// default
				
				if ( (aService.getServiceCharacteristicByName( "externalServiceOrderId" ) != null )){
					execution.setVariable("saction", "ExternalProviderServiceAction");					
					execution.setVariable("externalServiceOrderId", aService.getServiceCharacteristicByName( "externalServiceOrderId" ).getValue().getValue()  );	

					if ( (aService.getServiceCharacteristicByName( "externalPartnerServiceId" ) != null )){
						execution.setVariable("externalPartnerServiceId", aService.getServiceCharacteristicByName( "externalPartnerServiceId" ).getValue().getValue()  );							
					}
					
					RelatedParty rpOrg = null;
					if ( aService.getRelatedParty() != null ) {
						for (RelatedParty rp : aService.getRelatedParty()) {
							if ( rp.getRole().equals( UserPartRoleType.ORGANIZATION.getValue() )) {
								rpOrg =rp;
								break;
							}				
						}			
					}
					if ( rpOrg == null) {
						logger.error("Cannot retrieve partner organization, switch to HandleManuallyAction"  );
						execution.setVariable("saction", "HandleManuallyAction");
					} else {
						execution.setVariable("organizationId",  rpOrg.getId() );					
							
					}
				}else if ( aService.getCategory().equals( "ResourceFacingService") ) {
					
					if (aService.getServiceCharacteristicByName( "NSDID" ) != null  &&  item.getAction().equals( ServiceActionQueueAction.EVALUATE_CHARACTERISTIC_CHANGED_MANODAY2  ) ){						
						execution.setVariable("saction", "NFVODAY2config");
						 
					} else if (aService.getServiceCharacteristicByName("_CR_SPEC") != null) {
					  execution.setVariable("saction", "CRPatch");
					}
					else {
						execution.setVariable("saction", "AutomaticallyHandleAction");
					}					
					
				}
				
				
			}  else if ( !item.getAction().equals( ServiceActionQueueAction.NONE   ) ) {
					
					 if ( aService.getCategory().equals( "CustomerFacingService") ) {
						execution.setVariable("saction", "AutomaticallyHandleAction");
												
					} else if ( aService.getCategory().equals( "ResourceFacingService") ) {
						
						if (aService.getServiceCharacteristicByName( "NSDID" ) != null ){
							if ( item.getAction().equals( ServiceActionQueueAction.DEACTIVATE ) || item.getAction().equals( ServiceActionQueueAction.TERMINATE ) ) {
								execution.setVariable("saction", "NFVONSTerminate");
							} else if (  item.getAction().equals( ServiceActionQueueAction.MODIFY ) &&  item.getAction().equals( ServiceActionQueueAction.EVALUATE_CHARACTERISTIC_CHANGED_MANODAY2  )) {
								execution.setVariable("saction", "NFVODAY2config");
							}  else {
								execution.setVariable("saction", "HandleManuallyAction");
							} 
                          } else if (aService.getServiceCharacteristicByName("_CR_SPEC") != null) {
                            if (item.getAction().equals(ServiceActionQueueAction.DEACTIVATE) || item.getAction().equals(ServiceActionQueueAction.TERMINATE)) {
                              execution.setVariable("saction", "CRTerminate");
                            } else if (item.getAction().equals(ServiceActionQueueAction.MODIFY) ) {
                              execution.setVariable("saction", "CRPatch");
                            } else {
                              execution.setVariable("saction", "AutomaticallyHandleAction");
                            }
						  
						}
						else {
							execution.setVariable("saction", "AutomaticallyHandleAction");
						}					
						
					}
					
				
				
			} else {
				execution.setVariable("saction", "HandleManuallyAction");
			}

			


			try {
				String srv = mapper.writeValueAsString( aService );
				execution.setVariable("Service", srv);
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			
		}
	}

}
