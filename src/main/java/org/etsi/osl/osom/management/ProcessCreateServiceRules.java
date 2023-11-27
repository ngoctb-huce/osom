package org.etsi.osl.osom.management;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.lcm.LCMRulesController;
import org.etsi.osl.osom.lcm.LCMRulesExecutorVariables;
import org.etsi.osl.tmf.common.model.service.ServiceRef;
import org.etsi.osl.tmf.common.model.service.ServiceStateType;
import org.etsi.osl.tmf.lcm.model.ELCMRulePhase;
import org.etsi.osl.tmf.scm633.model.ServiceSpecRelationship;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.etsi.osl.tmf.so641.model.ServiceOrderItem;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component(value = "processCreateServiceRules") 
public class ProcessCreateServiceRules implements JavaDelegate {
	private static final transient Log logger = LogFactory.getLog( ProcessCreateServiceRules.class.getName());

	@Autowired
	private ServiceOrderManager serviceOrderManager;


	@Autowired
	private LCMRulesController lcmRulesController;
	
	
	@Value("${spring.application.name}")
	private String compname;
	
	public void execute(DelegateExecution execution) {

		logger.info("processCreateServiceRules:" + execution.getVariableNames().toString());
		Boolean allSupportingServicesCreatedAndActive = Boolean.TRUE;
		Boolean allSupportingServicesCreated = Boolean.TRUE;
		
		execution.setVariable("allSupportingServicesCreatedAndActive", allSupportingServicesCreatedAndActive ); //by default
		execution.setVariable("allSupportingServicesCreated", allSupportingServicesCreated ); //by default
		

		Service contextService = null;
		String contextServiceId = (String) execution.getVariable("contextServiceId"); 
		if ( contextServiceId != null ) {
			contextService = serviceOrderManager.retrieveService(contextServiceId);
		}else {
			return;
		}
		
		ServiceSpecification spec = null;
		String contextServiceSpecId = (String) execution.getVariable("contextServiceSpecId");
		if ( contextServiceSpecId != null ) {
			spec = serviceOrderManager.retrieveServiceSpec(contextServiceSpecId);
		} else {
			return;
		}
		

		/*
		 * first find all referenced ServiceSpecs of a ServiceSpec to be created
		 */
		boolean foundCreatedButNOTACTIVEServices = false;
		Map<String, Boolean> tobeCreated = new HashMap<>();
		for (ServiceSpecRelationship specRels : spec.getServiceSpecRelationship()) {
			logger.debug("\tService specRelsId:" + specRels.getId());
			tobeCreated.put(specRels.getId(), true);
		}
		
		
		for ( ServiceRef serviceRef: contextService.getSupportingService()  ) {
			
			Service theServiceReferenced = serviceOrderManager.retrieveService( serviceRef.getId() );
			
			if ( tobeCreated.get(theServiceReferenced.getServiceSpecificationRef().getId() ) != null ) {	
				tobeCreated.put( theServiceReferenced.getServiceSpecificationRef().getId(), false);
			}
			

			if ( theServiceReferenced != null ) {
				if ( theServiceReferenced.getState().equals( ServiceStateType.RESERVED) ) {
					foundCreatedButNOTACTIVEServices = true;
				}
			}
			
		}
		
		
		/**
		 * decisions for CREATE dependencies
		 * 
		 */
		
		//execute any LCM rules "SUPERVISION" phase for the SPEC;
		ServiceOrder sor = serviceOrderManager.retrieveServiceOrder((String) execution.getVariable("orderid"));
		String orderItemIdToProcess = (String) execution.getVariable("orderItemId");
		ServiceOrderItem soi = null;
		
		for (ServiceOrderItem i : sor.getOrderItem()) {
			if (i.getUuid().equals( orderItemIdToProcess )){
				soi = i;
				break;
			}
		}
		
		ServiceUpdate supd = new ServiceUpdate();
		LCMRulesExecutorVariables vars = new LCMRulesExecutorVariables(spec, sor, soi, null, supd , contextService, serviceOrderManager);
		
		logger.debug("===============BEFORE lcmRulesController.exec Phase CREATION for spec:" + spec.getName() + " =============================");
		vars = lcmRulesController.execPhase( ELCMRulePhase.CREATION, vars );

		//logger.debug("vars= " + vars );		
		logger.debug("===============AFTER lcmRulesController.exec Phase =============================");


		for (String serviceId : vars.getOutParams().keySet()) {
			if (  vars.getOutParams().get(serviceId) !=null) {
				if (  vars.getOutParams().get(serviceId).equals( "true")  ) {	
					tobeCreated.put( serviceId, true && tobeCreated.get(serviceId) );				
				} else {
					tobeCreated.put( serviceId, false);
					allSupportingServicesCreated = false;	
				}				
			}
		}

		serviceOrderManager.updateService( contextService.getId() , supd, false); //update context service
		
		List<String> servicesToCreate = new ArrayList<>();
		for (String specid : tobeCreated.keySet()) {
			if ( tobeCreated.get(specid) ) {
				servicesToCreate.add(specid);
				allSupportingServicesCreated = false;				
			}
		}
		
		if ( foundCreatedButNOTACTIVEServices ) {
			allSupportingServicesCreatedAndActive = false;
		}

		
		//we need to put here cases to avoid deadlock on waiting too much
		for ( ServiceRef serviceRef: contextService.getSupportingService()  ) {
			
			Service theServiceReferenced = serviceOrderManager.retrieveService( serviceRef.getId() );	
			if ( theServiceReferenced != null ) {
				if ( theServiceReferenced.getState().equals( ServiceStateType.INACTIVE ) || theServiceReferenced.getState().equals( ServiceStateType.TERMINATED ) ) {
					allSupportingServicesCreatedAndActive = true;
					allSupportingServicesCreated = true;	
					break;// this will help us to avoid a deadlock if a failure occurs
				}
			}
			
		}
		

		execution.setVariable("allSupportingServicesCreated", allSupportingServicesCreated ); 
		execution.setVariable("allSupportingServicesCreatedAndActive", allSupportingServicesCreatedAndActive && allSupportingServicesCreated ); //by default
		execution.setVariable("parentServiceId", contextServiceId);
		execution.setVariable("serviceSpecsToCreate", servicesToCreate);
	}

	
	

}
