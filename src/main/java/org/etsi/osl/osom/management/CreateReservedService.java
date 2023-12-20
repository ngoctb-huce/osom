package org.etsi.osl.osom.management;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.lcm.LCMRulesController;
import org.etsi.osl.osom.lcm.LCMRulesExecutorVariables;
import org.etsi.osl.tmf.common.model.Any;
import org.etsi.osl.tmf.common.model.EValueType;
import org.etsi.osl.tmf.common.model.UserPartRoleType;
import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.common.model.service.ServiceRef;
import org.etsi.osl.tmf.common.model.service.ServiceRelationship;
import org.etsi.osl.tmf.common.model.service.ServiceSpecificationRef;
import org.etsi.osl.tmf.common.model.service.ServiceStateType;
import org.etsi.osl.tmf.lcm.model.ELCMRulePhase;
import org.etsi.osl.tmf.prm669.model.RelatedParty;
import org.etsi.osl.tmf.scm633.model.ServiceSpecCharacteristic;
import org.etsi.osl.tmf.scm633.model.ServiceSpecCharacteristicValue;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;
import org.etsi.osl.tmf.sim638.model.EServiceStartMode;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceCreate;
import org.etsi.osl.tmf.sim638.model.ServiceOrderRef;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.etsi.osl.tmf.so641.model.ServiceOrderItem;
import org.etsi.osl.tmf.so641.model.ServiceOrderStateType;
import org.etsi.osl.tmf.so641.model.ServiceOrderUpdate;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.validation.Valid;

@Component(value = "createReservedService") // bean name
public class CreateReservedService implements JavaDelegate {
	private static final transient Log logger = LogFactory.getLog( CreateReservedService.class.getName());

	@Autowired
	private ServiceOrderManager serviceOrderManager;


	@Autowired
	private LCMRulesController lcmRulesController;
	
	
	@Value("${spring.application.name}")
	private String compname;
	
	public void execute(DelegateExecution execution) {

		logger.info("CreateReservedService:" + execution.getVariableNames().toString());


		String serviceSpecID = (String) execution.getVariable("serviceSpecID");		 //here we get the parent contextservice
		ServiceSpecification spec = serviceOrderManager.retrieveServiceSpec( serviceSpecID );
		
		if ( spec == null ) {
			return;
		}
		
		Service parentService = null;		
		String parentServiceId = (String) execution.getVariable("parentServiceId"); //here we get the parent contextservice
		if ( parentServiceId != null ) {
			parentService = serviceOrderManager.retrieveService(parentServiceId);
		}
		
		ServiceOrder sor = serviceOrderManager.retrieveServiceOrder((String) execution.getVariable("orderid"));

		logger.debug("Examine service items");
		String orderItemIdToProcess = (String) execution.getVariable("orderItemId");
		ServiceOrderItem soi = null;
		
		for (ServiceOrderItem i : sor.getOrderItem()) {
			if (i.getUuid().equals( orderItemIdToProcess )){
				soi = i;
				break;
			}
		}
		
		if ( soi == null ) {
			return;
		}
		
		

		// get service spec by id from model via bus, find if bundle and analyze its
		// related services	

		logger.debug("Retrieved Service ID:" + spec.getId());
		logger.debug("Retrieved Service Name:" + spec.getName());
		
		
		//this is a main underlying service for the requested service (restriction)					
		Service createdUnderlService = addServicesToVariables( spec, sor, soi,  parentService );
		
		soi.getService().setState( ServiceStateType.RESERVED );
		soi.setState(ServiceOrderStateType.INPROGRESS);
		
		/***
		 * we can update now the serviceorder element in catalog
		 * Update also the related service attributes
		 */
		
		ServiceOrderUpdate serviceOrderUpd = new ServiceOrderUpdate();
		for (ServiceOrderItem orderItemItem : sor.getOrderItem()) {
			orderItemItem.getService().setName( orderItemItem.getService().getServiceSpecification().getName() );
			orderItemItem.getService().setCategory( orderItemItem.getService().getServiceSpecification().getType() );
			//orderItemItem.getService().setState( ServiceStateType.RESERVED );
			//orderItemItem.setAction( ServiceOrderActionType.NOCHANGE ); //reset the action to NOCHANGE	
						
			serviceOrderUpd.addOrderItemItem(orderItemItem);
		}
		
		Note noteItem = new Note();
		noteItem.setText( String.format( "Create Reserved Service for %s " ,  spec.getName() ) );
		noteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
		noteItem.setAuthor( compname );
		serviceOrderUpd.addNoteItem( noteItem );

		execution.setVariable("contextServiceId", createdUnderlService.getId() );
		execution.setVariable("contextServiceSpecId", spec.getId() );
		serviceOrderManager.updateServiceOrderOrder( sor.getId(), serviceOrderUpd );
	}
	

	/**
	 * 
	 * This method decides the kind of Automation to be applied for a requested Service 
	 * e.g. Manual, Automated, Handled by NFVO, Handled by External partner.
	 * It creates an underlying service definition that needs to be managed next by various Activities/Processes 
	 * 
	 * 
	 * @param specrel
	 * @param sor
	 * @param soi
	 * @param servicesHandledByExternalSP
	 * @param servicesHandledManual
	 * @param servicesHandledByNFVOAutomated
	 * @param servicesLocallyAutomated
	 * @param parentService 
	 * @return 
	 */
	private Service addServicesToVariables(ServiceSpecification specrel, 
			ServiceOrder sor, ServiceOrderItem soi, 
			Service parentService) {
		
		logger.debug("\tService spec name :" + specrel.getName());
		logger.debug("\tService spec type :" + specrel.getType());
		
		Service createdServ = null;
		RelatedParty partnerOrg = fromPartnerOrganization( specrel );
		
		
		if ( partnerOrg != null  ) {
			createdServ = createServiceByServiceSpec(sor, soi, specrel, EServiceStartMode.AUTOMATICALLY_MANAGED, partnerOrg, parentService);
		}	
		else if (specrel.getType().equals("ResourceFacingServiceSpecification")) {
			createdServ = createServiceByServiceSpec(sor, soi, specrel, EServiceStartMode.AUTOMATICALLY_MANAGED, null, parentService);
			
		} else if ( specrel.getType().equals("CustomerFacingServiceSpecification") && (specrel.isIsBundle()!=null) && specrel.isIsBundle() ) {
			createdServ = createServiceByServiceSpec(sor, soi, specrel, EServiceStartMode.AUTOMATICALLY_MANAGED, null, parentService);			
			
		} else if ( specrel.getType().equals("CustomerFacingServiceSpecification") && (specrel.findSpecCharacteristicByName("OSAUTOMATED") != null )  ) {
			createdServ = createServiceByServiceSpec(sor, soi, specrel, EServiceStartMode.AUTOMATICALLY_MANAGED, null, parentService);			
			
		} else if ( specrel.getType().equals("CustomerFacingServiceSpecification") && (specrel.findSpecCharacteristicByName("testSpecRef") != null )  ) {
			createdServ = createServiceByServiceSpec(sor, soi, specrel, EServiceStartMode.AUTOMATICALLY_MANAGED, null, parentService);			
			
		}	
		else {
			createdServ = createServiceByServiceSpec(sor, soi, specrel, EServiceStartMode.MANUALLY_BY_SERVICE_PROVIDER, null, parentService);			
		}		
		
		//add now the serviceRef
		if ( createdServ!=null ) {
			ServiceRef supportingServiceRef = new ServiceRef();
			supportingServiceRef.setId( createdServ.getId() );
			supportingServiceRef.setReferredType( createdServ.getName() );
			supportingServiceRef.setName( createdServ.getName()  );
			soi.getService().addSupportingServiceItem(supportingServiceRef );			
			
			if ( parentService!= null) {
				addCreatedServiceAsSupportingServiceToParent( parentService, supportingServiceRef );	
			}
			
			
			return createdServ;
			
		} else {
			logger.error("Service was not created for spec: " + specrel.getName());
		}
		
		return null;
		
	}
	
	
	/**
	 * 
	 * Adds a supportingServiceRef to the Supporting Services of createdUnderlService
	 * 
	 * @param parentService
	 * @param supportingServiceRef
	 */
	private void addCreatedServiceAsSupportingServiceToParent(Service parentService,
			ServiceRef supportingServiceRef) {


		parentService.addSupportingServiceItem(supportingServiceRef);
		
		ServiceUpdate supd = new ServiceUpdate();
		
		for (ServiceRef existingSupportingServices : parentService.getSupportingService()  ) {
			supd.addSupportingServiceItem(existingSupportingServices);			
		}
		
		serviceOrderManager.updateService( parentService.getId() , supd, false);
		
		
	}
	
	
	/**
	 * @param sor
	 * @param soi 
	 * @param spec
	 * @return 
	 */
	private Service createServiceByServiceSpec(ServiceOrder sor, ServiceOrderItem soi,
			ServiceSpecification spec, EServiceStartMode startMode, 
			RelatedParty partnerOrg, Service parentService) {

		ServiceCreate serviceToCreate = new ServiceCreate();
		String servicename = spec.getName();
		serviceToCreate.setDescription("A Service for " + spec.getName());
		if ( partnerOrg!= null ) {
			servicename = partnerOrg.getName() + "::" +  servicename  + "::PROXY";
			serviceToCreate.setDescription("A Service for " + spec.getName() + " offered by external partner: " + partnerOrg.getName());
		}

		serviceToCreate.setName( servicename );
		serviceToCreate.setCategory(spec.getType().replace("Specification", "") );
		serviceToCreate.setType(spec.getType().replace("Specification", ""));
		serviceToCreate.setServiceDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
		serviceToCreate.setStartDate( OffsetDateTime.now(ZoneOffset.UTC).toString()  );
		serviceToCreate.setEndDate( sor.getExpectedCompletionDate()  );
		serviceToCreate.hasStarted(false);
		serviceToCreate.setIsServiceEnabled(false);
		serviceToCreate.setStartMode( startMode.getValue() );
		
		Note noteItem = new Note();
		noteItem.setText("Service Created by CreateReservedService");
		
		noteItem.setAuthor( compname );
		serviceToCreate.addNoteItem(noteItem);
		
		ServiceOrderRef serviceOrderref = new ServiceOrderRef();
		serviceOrderref.setId( sor.getId() );
		serviceOrderref.setServiceOrderItemId( soi.getId() );
		serviceToCreate.addServiceOrderItem(serviceOrderref );
		
		ServiceSpecificationRef serviceSpecificationRef = new ServiceSpecificationRef();
		serviceSpecificationRef.setId( spec.getId());
		serviceSpecificationRef.setName(spec.getName());
		serviceToCreate.setServiceSpecificationRef(serviceSpecificationRef );
		
		serviceToCreate.setServiceType( spec.getName());
		serviceToCreate.setState( ServiceStateType.RESERVED );
		
		
		if (spec.getRelatedParty()!=null) {
			for (RelatedParty rp : spec.getRelatedParty()) {
				rp.setUuid(null); 
				serviceToCreate.addRelatedPartyItem(rp);
			}			
		}
		
		//we need to be careful here with the bundle and the related Service Specs, to properly propagate the rules inside
		//first copy into the newly created service any characteristic values from the order
		for (ServiceSpecCharacteristic c : spec.getServiceSpecCharacteristic()) {
			
			boolean characteristicFound = false;
			for (Characteristic orderCharacteristic : soi.getService().getServiceCharacteristic()) {
				String specCharacteristicToSearch = spec.getName() + "::" +c.getName();
				 if ( orderCharacteristic.getName().equals( specCharacteristicToSearch )) { //copy only characteristics that are related from the order
					serviceToCreate.addServiceCharacteristicItem( addServiceCharacteristicItem(c, orderCharacteristic) );
					characteristicFound = true;
					break;
				}
			}
			
			if (!characteristicFound) { //fallback to find simple name (i.e. not starting with service spec name)
				for (Characteristic orderCharacteristic : soi.getService().getServiceCharacteristic()) {
					String specCharacteristicToSearch = c.getName();
					 if ( orderCharacteristic.getName().equals( specCharacteristicToSearch )) { //copy only characteristics that are related from the order							 
						
						serviceToCreate.addServiceCharacteristicItem( addServiceCharacteristicItem(c, orderCharacteristic) );
						characteristicFound = true;
						break;
					}
				}
				
			}
			
		}	
		
		if ( serviceToCreate.getServiceCharacteristic() == null ) {
			serviceToCreate.setServiceCharacteristic( new ArrayList<>() );			
		}
		copyRemainingSpecCharacteristicsToServiceCharacteristic(spec ,serviceToCreate.getServiceCharacteristic() );	//copy to service the rest of the characteristics that do not exists yet from the above search	
		
		
		if ( parentService != null ) { //if parentService is not Null, then we need the value of the corresponding characteristic from the parent into this service	
			for (Characteristic cchild : serviceToCreate.getServiceCharacteristic() ) {
				for (Characteristic c : parentService.getServiceCharacteristic() ) {
					String childCharacteristicToMatch = serviceToCreate.getName() + "::" +cchild.getName();
					if ( c.getName().equals( childCharacteristicToMatch )) { //assign only characteristics values that are related from the parent service							 
						cchild.getValue().setValue( c.getValue().getValue() );
						break;
					}
				}				
			}


			//also add parent service as relationship to parent
			ServiceRelationship srelationship = new ServiceRelationship();
			ServiceRef parentServiceRef = new ServiceRef();
			parentServiceRef.setId( parentService.getId() );
			parentServiceRef.setReferredType( parentService.getName() );
			parentServiceRef.setName( parentService.getName()  );
			srelationship.setRelationshipType("ChildService");
			srelationship.setService(parentServiceRef);
			
			serviceToCreate.addServiceRelationshipItem( srelationship );
			
		}	
		

//		:execute any LCM rules "PRE_PROVISION" phase for the SPEC;
		LCMRulesExecutorVariables vars = new LCMRulesExecutorVariables(spec, sor, soi, serviceToCreate, null , null, serviceOrderManager);
		
		logger.debug("===============BEFORE lcmRulesController.execPhase for spec:" + spec.getName() + " =============================");
		vars = lcmRulesController.execPhase( ELCMRulePhase.PRE_PROVISION, vars );

		//logger.debug("vars= " + vars );		
		logger.debug("===============AFTER lcmRulesController.execPhase =============================");
		
		if ( vars.getCompileDiagnosticErrors().size()>0 ) {
			noteItem = new Note();
			String msg = "LCM Rule execution error by AutomationCheck. ";
			for (String tmsg :  vars.getCompileDiagnosticErrors()) {
				msg = msg + "\n"+ tmsg;
			}
			noteItem.setText( msg );
			noteItem.setAuthor( compname );
			vars.getServiceToCreate().addNoteItem(noteItem);
		}
		
		Service createdService = serviceOrderManager.createService( 
				vars.getServiceToCreate() , 
				vars.getSorder(), 
				spec);
		return createdService;
	}
	


	private RelatedParty fromPartnerOrganization(ServiceSpecification specrel) {
		if ( specrel.getRelatedParty() != null ) {
			for (RelatedParty rp : specrel.getRelatedParty()) {
				if ( rp.getRole().equals( UserPartRoleType.ORGANIZATION.getValue() )) {
					return rp;					
				}				
			}			
		}
		return null;
	}
	
	private Characteristic addServiceCharacteristicItem(ServiceSpecCharacteristic c, Characteristic orderCharacteristic) {
		Characteristic serviceCharacteristicItem =  new Characteristic();
		serviceCharacteristicItem.setName( c.getName() );
		serviceCharacteristicItem.setValueType( c.getValueType() );
					
		Any val = new Any();
		val.setValue( orderCharacteristic.getValue().getValue() );
		val.setAlias( orderCharacteristic.getValue().getAlias() );
		
		serviceCharacteristicItem.setValue( val );
		
		return serviceCharacteristicItem;
	}
	
	/**
	 * 
	 * will copy any remaining service spec characteristics that where not included in the initial order
	 * 
	 * @param sourceSpecID
	 * @param destServiceCharacteristic
	 */
	private void copyRemainingSpecCharacteristicsToServiceCharacteristic(ServiceSpecification sourceSpec, @Valid List<Characteristic> destServiceCharacteristic) {
		
		
		for (ServiceSpecCharacteristic sourceCharacteristic : sourceSpec.getServiceSpecCharacteristic()) {
			if (  sourceCharacteristic.getValueType() != null ) {
				boolean charfound = false;
				for (Characteristic destchar : destServiceCharacteristic) {
					if ( destchar.getName().equals(sourceCharacteristic.getName())) {
						charfound = true;
						break;
					}
				}
				
				if (!charfound) {
				
					Characteristic newChar = new Characteristic();
					newChar.setName( sourceCharacteristic.getName() );
					newChar.setValueType( sourceCharacteristic.getValueType() );
					
					if (  sourceCharacteristic.getValueType() != null && sourceCharacteristic.getValueType().equals( EValueType.ARRAY.getValue() ) ||
							 sourceCharacteristic.getValueType() != null && sourceCharacteristic.getValueType().equals( EValueType.SET.getValue() ) ) {
						String valString = "";
						for (ServiceSpecCharacteristicValue specchar : sourceCharacteristic.getServiceSpecCharacteristicValue()) {
							if ( ( specchar.isIsDefault()!= null) && specchar.isIsDefault() ) {
								if ( !valString.equals("")) {
									valString = valString + ",";
								}
								valString = valString + "{\"value\":\"" + specchar.getValue().getValue() + "\",\"alias\":\"" + specchar.getValue().getAlias() + "\"}";
							}
							
						}
						
						newChar.setValue( new Any( "[" + valString + "]", "") );
						
						
					} else {
						for (ServiceSpecCharacteristicValue specchar : sourceCharacteristic.getServiceSpecCharacteristicValue()) {
							if ( ( specchar.isIsDefault()!= null) && specchar.isIsDefault() ) {
								newChar.setValue( new Any(
										specchar.getValue().getValue(), 
										specchar.getValue().getAlias()) );
								break;
							}else {
								if (specchar.isIsDefault()== null){

								logger.info("specchar is null value: " + sourceCharacteristic.getName() );
								}
							}

						}						
					}
					
					//sourceCharacteristic.getServiceSpecCharacteristicValue()
					
					if ( newChar.getValue() !=null) {
						destServiceCharacteristic.add(newChar );
					} else {
						newChar.setValue( new Any(
								"", 
								"") );
						destServiceCharacteristic.add(newChar );
					}
					
				}
				
			}
			
			
		}
		
	}
}
