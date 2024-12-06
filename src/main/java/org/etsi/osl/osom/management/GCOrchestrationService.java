/*-
 * ========================LICENSE_START=================================
 * org.etsi.osl.osom
 * %%
 * Copyright (C) 2019 openslice.io
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
package org.etsi.osl.osom.management;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.tmf.common.model.Any;
import org.etsi.osl.tmf.common.model.EValueType;
import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.common.model.service.ResourceRef;
import org.etsi.osl.tmf.common.model.service.ServiceStateType;
import org.etsi.osl.tmf.rcm634.model.ResourceSpecification;
import org.etsi.osl.tmf.rcm634.model.ResourceSpecificationCharacteristic;
import org.etsi.osl.tmf.rcm634.model.ResourceSpecificationCharacteristicValue;
import org.etsi.osl.tmf.rcm634.model.ResourceSpecificationRef;
import org.etsi.osl.tmf.ri639.model.Resource;
import org.etsi.osl.tmf.ri639.model.ResourceCreate;
import org.etsi.osl.tmf.scm633.model.ServiceSpecCharacteristic;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.validation.Valid;


@Component(value = "gcOrchestrationService") // bean name
public class GCOrchestrationService implements JavaDelegate {

  private static final transient Log logger =
      LogFactory.getLog(GCOrchestrationService.class.getName());


  @Value("${spring.application.name}")
  private String compname;


  @Autowired
  private ServiceOrderManager serviceOrderManager;


  public void execute(DelegateExecution execution) {

    logger.debug("Ceneric Controller OrchestrationService");
    logger.debug("VariableNames:" + execution.getVariableNames().toString());
    logger.debug("orderid:" + execution.getVariable("orderid").toString());
    logger.debug("contextServiceId:" + execution.getVariable("contextServiceId").toString());

    try {
      

    ServiceUpdate su = new ServiceUpdate();// the object to update the service
    Note noteItem = new Note();
    noteItem.setText("");

    if (execution.getVariable("contextServiceId") instanceof String contextServiceId)  {



      ServiceOrder sorder =
          serviceOrderManager.retrieveServiceOrder(execution.getVariable("orderid").toString());
      Service aService =
          serviceOrderManager.retrieveService( contextServiceId );
      logger.info("Service name:" + aService.getName());
      logger.info("Service state:" + aService.getState());
      logger.info("Request for a Custom Resource creation for Service: " + aService.getId());

      // we need to retrieve here the Service Spec of this service

      ServiceSpecification spec =
          serviceOrderManager.retrieveServiceSpec(aService.getServiceSpecificationRef().getId());

      if (spec != null) {

        
        
        
        //we need to get the equivalent resource spec. since ServiceSpec is an RFS
        ResourceSpecificationRef rSpecRef = spec.getResourceSpecification().stream().findFirst().get();
        
        ResourceSpecification rspec = serviceOrderManager.retrieveResourceSpec(rSpecRef.getId());
        
        //we will create a resource based on the values of resourcepsecificationRef
        ResourceCreate resourceCreate = createRelatedResource( rspec, sorder, aService );
        

        //save it to TMF API service inventory
        Resource resourceI = serviceOrderManager.createResource( resourceCreate, sorder, rspec.getId() );
        
        
        ResourceRef rr = new ResourceRef();
        rr.setId( resourceI.getId() );
        rr.setName( resourceI.getName());
        rr.setType( resourceI.getType());
        su.addSupportingResourceItem( rr );

        su.setState(ServiceStateType.RESERVED);
        Note successNoteItem = new Note();
        successNoteItem.setText(String.format("Requesting Controller of "+ rSpecRef.getName() +" to deploy resource"));
        successNoteItem.setDate(OffsetDateTime.now(ZoneOffset.UTC).toString());
        successNoteItem.setAuthor(compname);
        su.addNoteItem(successNoteItem);
        Service supd = serviceOrderManager.updateService(aService.getId(), su, false);
        
        Resource response = null;
        
        response = createNewResourceDeploymentRequest(aService, resourceI, resourceCreate,  sorder.getId() );
        
                
        
        if ( response==null  ) {
          su.setState(ServiceStateType.TERMINATED);
          Note errNoteItem = new Note();
          errNoteItem.setText(String.format("Requesting Controller to deploy resource failed "));
          errNoteItem.setDate(OffsetDateTime.now(ZoneOffset.UTC).toString());
          errNoteItem.setAuthor(compname);
          su.addNoteItem(errNoteItem);
          supd = serviceOrderManager.updateService(aService.getId(), su, false);
        }
        
        return;

      } else {

        logger.error("Cannot retrieve ServiceSpecification for service :"
            + (String) execution.getVariable("contextServiceId"));
      }
    } else {
      logger.error("Cannot retrieve variable contextServiceId");
    }

    // if we get here something is wrong so we need to terminate the service.

    
    
    }catch (Exception e) {
      e.printStackTrace();
    }
    
    
    try {
      Note noteItem = new Note();
      noteItem.setText("Request to CR FAILED." + noteItem.getText());
      noteItem.setAuthor(compname);
      noteItem.setDate(OffsetDateTime.now(ZoneOffset.UTC).toString());
      ServiceUpdate su = new ServiceUpdate();// the object to update the service
      su.addNoteItem(noteItem);
      su.setState(ServiceStateType.TERMINATED);
      serviceOrderManager.updateService(execution.getVariable("contextServiceId").toString(), su,
          false);
    }catch (Exception e) {
      e.printStackTrace();
    }

  }


  /**
   * 
   * THe resource has a temporary name.
   * later on the name and its characteristics are updated via the generic controller
   * 
   * 
   * @param rSpecRef
   * @param sOrder
   * @param aService
   * @return
   */
  private ResourceCreate createRelatedResource(ResourceSpecification rspec, ServiceOrder sOrder, Service aService) {
    
    /**
     * In future releases, it is better to create some helper function in the TMF model that
     * Creates a resource from a resource specification, (or a service from a servic spec). 
     * There are similar functions in other places, so it is better to move them in one place
     */
    ResourceCreate resCreate = new ResourceCreate();
    resCreate.setName(  rspec.getName()+"-" + aService.getId() );
    resCreate.setCategory( rspec.getCategory() );
    resCreate.resourceVersion( rspec.getVersion() );
    
    resCreate.setStartOperatingDate( aService.getStartDate() );
    resCreate.setEndOperatingDate(aService.getEndDate());
    ResourceSpecificationRef rSpecRefObj = new ResourceSpecificationRef() ;
    rSpecRefObj.id(rspec.getId())
      .version(rspec.getVersion())
      .name( rspec.getName())
      .setType(rspec.getType());
    resCreate.setResourceSpecification(rSpecRefObj); 
    
    
    for (ResourceSpecificationCharacteristic c : rspec.getResourceSpecCharacteristic()) {
      for (Characteristic orderCharacteristic : aService.getServiceCharacteristic() ) {
          String specCharacteristicToSearch = c.getName();
           if ( orderCharacteristic.getName().equals( specCharacteristicToSearch )) { //copy only characteristics that are related from the order     
              
             resCreate.addResourceCharacteristicItem(  addResourceCharacteristicItem(c, orderCharacteristic) );
             break;
          }
      }
    }
    
    //copy to resource the rest of the characteristics that do not exists yet from the above search
    resCreate = copyRemainingSpecCharacteristicsToResourceCharacteristic(rspec , resCreate );     
    
    return resCreate;

    
    
  }
  
  
  private org.etsi.osl.tmf.ri639.model.Characteristic addResourceCharacteristicItem(ResourceSpecificationCharacteristic c, Characteristic orderCharacteristic) {
    org.etsi.osl.tmf.ri639.model.Characteristic resCharacteristicItem =  new org.etsi.osl.tmf.ri639.model.Characteristic();
    resCharacteristicItem.setName( c.getName() );
    resCharacteristicItem.setValueType( c.getValueType() );
                
    Any val = new Any();
    val.setValue( orderCharacteristic.getValue().getValue() );
    val.setAlias( orderCharacteristic.getValue().getAlias() );
    
    resCharacteristicItem.setValue( val );
    
    return resCharacteristicItem;
  }
  
  
  /**
   * 
   * This helper function it is better in future to be moved in the TMF model.
   * There is similar in resource order
   * 
   * @param spec
   * @param list
   */
  private ResourceCreate copyRemainingSpecCharacteristicsToResourceCharacteristic(ResourceSpecification spec,
      @Valid ResourceCreate resCreate) {
    
    List<org.etsi.osl.tmf.ri639.model.Characteristic> list = resCreate.getResourceCharacteristic();
    
    for (ResourceSpecificationCharacteristic sourceCharacteristic : spec.getResourceSpecCharacteristic()) {
        if (  sourceCharacteristic.getValueType() != null ) {
            boolean charfound = false;
            for (org.etsi.osl.tmf.ri639.model.Characteristic destchar : list) {
                if ( destchar.getName().equals(sourceCharacteristic.getName())) {
                    charfound = true;
                    break;
                }
            }
            
            if (!charfound) {
            
                org.etsi.osl.tmf.ri639.model.Characteristic newChar = new org.etsi.osl.tmf.ri639.model.Characteristic();
                newChar.setName( sourceCharacteristic.getName() );
                newChar.setValueType( sourceCharacteristic.getValueType() );
                
                if (  sourceCharacteristic.getValueType() != null && sourceCharacteristic.getValueType().equals( EValueType.ARRAY.getValue() ) ||
                         sourceCharacteristic.getValueType() != null && sourceCharacteristic.getValueType().equals( EValueType.SET.getValue() ) ) {
                    String valString = "";
                    for (ResourceSpecificationCharacteristicValue specchar : sourceCharacteristic.getResourceSpecCharacteristicValue()) {
                        if ( ( specchar.isIsDefault()!= null) && specchar.isIsDefault() ) {
                            if ( !valString.equals("")) {
                                valString = valString + ",";
                            }
                            valString = valString + "{\"value\":\"" + specchar.getValue().getValue() + "\",\"alias\":\"" + specchar.getValue().getAlias() + "\"}";
                        }
                        
                    }
                    
                    newChar.setValue( new Any( "[" + valString + "]", "") );
                    
                    
                } else {
                    for (ResourceSpecificationCharacteristicValue specchar : sourceCharacteristic.getResourceSpecCharacteristicValue()) {
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
                
                
                if ( newChar.getValue() !=null) {
                    list.add(newChar );
                } else {
                    newChar.setValue( new Any(
                            "", 
                            "") );
                    list.add(newChar );
                }
                
            }
            
        }
        
        
    }
    
    return resCreate;
    
  }


  /**
   * 
   * This function makes a new deployment resource request for a resource  specification.
   * The request is performed via the message queue.
   * The function sends also some headers that maybe are needed for deployment
   * These are the headers
   * <br>
   * <br><b>org.etsi.osl.serviceId:</b> This is the related service id that the created resource has a reference
   * <br><b>org.etsi.osl.resourceId:</b> This is the related resource id that the created CR will wrap and reference. There
   * <br><b>org.etsi.osl.prefixName:</b> we need to add a short prefix (default is cr) to various places. For example in K8s cannot start with a number
   * <br><b>org.etsi.osl.serviceOrderId:</b> the related service order id of this deployment request
   * <br>
   * 
   * @param aService reference to the service that the resource and the CR belongs to
   * @param resourceI reference the equivalent resource in TMF repo of the target CR. One to one mapping
   * @param orderId related service order ID
   * @return a Resource as updated. It might return "OK" if everything is ok. 
   * "SEE OTHER" if there are multiple CRIDGEs then some other cridge will handle the request for the equivalent cluster. 
   * Any other response is handled as error
   * return Resource object from the controller
   */
  private Resource createNewResourceDeploymentRequest( Service aService, Resource resourceI,
      ResourceCreate aResourceCreate, 
      String orderId) {

    try {
      Map<String, Object> map = new HashMap<>();
      map.put("org.etsi.osl.serviceId", aService.getId() );
      map.put("org.etsi.osl.resourceId", resourceI.getId() );
      map.put("org.etsi.osl.prefixName", "gr" + resourceI.getId().substring(0, 8) );
      map.put("org.etsi.osl.serviceOrderId", orderId );
      

      logger.debug("createNewResourceDeploymentRequest ");
      
      String queueName = "jms:queue:CREATE/"+ aResourceCreate.getCategory() + "/" + aResourceCreate.getResourceVersion() ;
      Resource response  = serviceOrderManager.gcGenericResourceDeploymentRequest(queueName , map, aResourceCreate);
      

      return response;
      
    } catch (Exception e) {
      logger.error("cridgeDeploymentRequest failed");
      e.printStackTrace();
    }

    return null;

  }


  private Object getServiceCharacteristic(Service aService, String val) {
    if (aService.getServiceCharacteristicByName( val ) !=null ) {
      return aService.getServiceCharacteristicByName( val ).getValue().getValue();
    }
    return "";
  }


}
