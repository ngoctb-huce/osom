package org.etsi.osl.osom.serviceactions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.management.ServiceOrderManager;
import org.etsi.osl.tmf.common.model.AttachmentRefOrValue;
import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.common.model.service.ResourceRef;
import org.etsi.osl.tmf.prm669.model.RelatedParty;
import org.etsi.osl.tmf.ri639.model.Feature;
import org.etsi.osl.tmf.ri639.model.Resource;
import org.etsi.osl.tmf.ri639.model.ResourceCreate;
import org.etsi.osl.tmf.ri639.model.ResourceRelationship;
import org.etsi.osl.tmf.ri639.model.ResourceUpdate;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceActionQueueAction;
import org.etsi.osl.tmf.sim638.model.ServiceActionQueueItem;
import org.etsi.osl.tmf.sim638.model.ServiceOrderRef;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.validation.Valid;

@Component(value = "AutomaticallyHandleAction") //bean name
public class AutomaticallyHandleAction  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( AutomaticallyHandleAction.class.getName() );

	@Value("${spring.application.name}")
	private String compname;

    @Autowired
    private ServiceOrderManager serviceOrderManager;
    
	public void execute(DelegateExecution execution) {
		
		logger.debug("AutomaticallyHandleAction:" + execution.getVariableNames().toString() );
	    logger.debug("Ceneric Controller OrchestrationService");
	    logger.debug("VariableNames:" + execution.getVariableNames().toString());
		
		
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			ServiceActionQueueItem item;
			Service aService;
			item = mapper.readValue( execution.getVariable("serviceActionItem").toString(), ServiceActionQueueItem.class);
			aService = mapper.readValue( execution.getVariable("Service").toString(), Service.class);
			
			if ( aService.getSupportingResource().stream().findFirst().isPresent() ) { //there is a resourceRef			  

	            ResourceRef rr = aService.getSupportingResource().stream().findFirst().get();
	            
	            Resource relatedResource = serviceOrderManager.retrieveResource( rr.getId() );
	            
	            ResourceUpdate aResourceUpdate = resourceToResourceUpdate(relatedResource);
	            
	            //pass characteristic values from services to resource
	            Optional<ServiceOrderRef> sref = aService.getServiceOrder().stream().findFirst();
	            String serviceOrderID="";
	            if ( sref.isPresent()) {
	              serviceOrderID = sref.get().getId();
	            }

                Boolean allServiceAndResourceCharacteristicsAreTheSame = true;
                Boolean onlyStatusCharacteristicsChanged = true;
	            for (org.etsi.osl.tmf.ri639.model.Characteristic resCharacteristic : aResourceUpdate.getResourceCharacteristic()) {
	               Characteristic servChar = aService.getServiceCharacteristicByName( resCharacteristic.getName() );
	              if ( servChar !=null) {      
	                //we need to check if the characteristics that changed are only from the resources (e.g. start with status. )
	                //if there are only from resources then we will not snd back an update request, otherwise we get into an infinite loop
	                if ( resCharacteristic.getValue().getValue() != null && 
	                    servChar.getValue().getValue()  != null &&
	                    !resCharacteristic.getValue().getValue().equals( servChar.getValue().getValue() ) ) {
	                  allServiceAndResourceCharacteristicsAreTheSame = false;
                      if ( !servChar.getValue().getValue().startsWith("status.") ) {
                        onlyStatusCharacteristicsChanged = false;
                      } else {

                        logger.debug("Only Status Characteristic Changed. Will not update the resource!" );
                      }
                    }
	                

                    resCharacteristic.getValue().setValue( servChar.getValue().getValue() );
	                
	              }
	            } 
	            
	            
	            try{
	              
	                Resource response = null;
	                
	                if ( item.getAction().equals( ServiceActionQueueAction.EVALUATE_CHARACTERISTIC_CHANGED  ) 
	                    || item.getAction().equals( ServiceActionQueueAction.EVALUATE_CHILD_CHARACTERISTIC_CHANGED  ) 
	                    || item.getAction().equals( ServiceActionQueueAction.EVALUATE_CHARACTERISTIC_CHANGED_MANODAY2  ) ) {
	                    
	                  if ( !onlyStatusCharacteristicsChanged && !allServiceAndResourceCharacteristicsAreTheSame) {
	                      response = createNewResourceDeploymentRequest(aService, 
	                            relatedResource, aResourceUpdate,  
	                            serviceOrderID );     
	                    
	                  }            
	                } else {

	                  response = createNewResourceDeleteRequest(aService, 
	                        relatedResource, aResourceUpdate,  
	                        serviceOrderID );  
	                }
	                
	              
	            }catch (Exception e) {
	              e.printStackTrace();
	            }
			  
			  
			}
			
			
			
			ServiceUpdate supd = new ServiceUpdate();
			Note n = new Note();
			n.setText("Service Action AutomaticallyHandleAction. Action: " + item.getAction() );
			n.setAuthor( compname );
			n.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
			supd.addNoteItem( n );
			
			serviceOrderManager.deleteServiceActionQueueItem( item );			
			serviceOrderManager.updateService( aService.getId() , supd, false);
			
			
		} catch (JsonMappingException e) {
			e.printStackTrace();
			return;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return;
		}
	}
	
	
    private ResourceUpdate resourceToResourceUpdate(Resource source) {
      if ( source == null ) {
          return null;
      }

      ResourceUpdate resourceUpdate = new ResourceUpdate();

      resourceUpdate.setCategory( source.getCategory() );
      resourceUpdate.setDescription( source.getDescription() );
      resourceUpdate.setEndOperatingDate( source.getEndOperatingDate() );
      resourceUpdate.setName( source.getName() );
      resourceUpdate.setResourceVersion( source.getResourceVersion() );
      resourceUpdate.setStartOperatingDate( source.getStartOperatingDate() );
      Set<Feature> set = source.getActivationFeature();
      if ( set != null ) {
          resourceUpdate.setActivationFeature( new ArrayList<Feature>( set ) );
      }
      resourceUpdate.setAdministrativeState( source.getAdministrativeState() );
      Set<AttachmentRefOrValue> set1 = source.getAttachment();
      if ( set1 != null ) {
          resourceUpdate.setAttachment( new ArrayList<AttachmentRefOrValue>( set1 ) );
      }
      Set<Note> set2 = source.getNote();
      if ( set2 != null ) {
          resourceUpdate.setNote( new ArrayList<Note>( set2 ) );
      }
      resourceUpdate.setOperationalState( source.getOperationalState() );
      resourceUpdate.setPlace( source.getPlace() );
      Set<RelatedParty> set3 = source.getRelatedParty();
      if ( set3 != null ) {
          resourceUpdate.setRelatedParty( new ArrayList<RelatedParty>( set3 ) );
      }
      @Valid Set<org.etsi.osl.tmf.ri639.model.Characteristic> set4 = source.getResourceCharacteristic();
      if ( set4 != null ) {
          resourceUpdate.setResourceCharacteristic( new ArrayList<org.etsi.osl.tmf.ri639.model.Characteristic>( set4 ) );
      }
      Set<ResourceRelationship> set5 = source.getResourceRelationship();
      if ( set5 != null ) {
          resourceUpdate.setResourceRelationship( new ArrayList<ResourceRelationship>( set5 ) );
      }
      resourceUpdate.setResourceSpecification( source.getResourceSpecification() );
      resourceUpdate.setResourceStatus( source.getResourceStatus() );
      resourceUpdate.setUsageState( source.getUsageState() );

      return resourceUpdate;
  }
    
    private Resource createNewResourceDeploymentRequest( Service aService, 
        Resource resourceI,
        ResourceUpdate aResourceUpdate, 
        String orderId) {

      try {
        Map<String, Object> map = new HashMap<>();
        map.put("org.etsi.osl.serviceId", aService.getId() );
        map.put("org.etsi.osl.resourceId", resourceI.getId() );
        map.put("org.etsi.osl.prefixName", "gr" + resourceI.getId().substring(0, 8) );
        map.put("org.etsi.osl.serviceOrderId", orderId );
        

        logger.debug("createNewResourceDeploymentRequest ");
        
        String queueName = "jms:queue:UPDATE/"+ aResourceUpdate.getCategory() + "/" + aResourceUpdate.getResourceVersion() ;
        Resource response  = serviceOrderManager.gcGenericResourceDeploymentRequest(queueName , map, aResourceUpdate);
        

        return response;
        
      } catch (Exception e) {
        logger.error("cridgeDeploymentRequest failed");
        e.printStackTrace();
      }

      return null;

    }
    
    private Resource createNewResourceDeleteRequest( Service aService, 
        Resource resourceI,
        ResourceUpdate aResourceUpdate, 
        String orderId) {

      try {
        Map<String, Object> map = new HashMap<>();
        map.put("org.etsi.osl.serviceId", aService.getId() );
        map.put("org.etsi.osl.resourceId", resourceI.getId() );
        map.put("org.etsi.osl.prefixName", "gr" + resourceI.getId().substring(0, 8) );
        map.put("org.etsi.osl.serviceOrderId", orderId );
        

        logger.debug("createNewResourceDeploymentRequest ");
        
        String queueName = "jms:queue:DELETE/"+ aResourceUpdate.getCategory() + "/" + aResourceUpdate.getResourceVersion() ;
        Resource response  = serviceOrderManager.gcGenericResourceDeploymentRequest(queueName , map, aResourceUpdate);
        

        return response;
        
      } catch (Exception e) {
        logger.error("cridgeDeploymentRequest failed");
        e.printStackTrace();
      }

      return null;

    }

}
