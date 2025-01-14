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
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.common.model.service.ResourceRef;
import org.etsi.osl.tmf.common.model.service.ServiceStateType;
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


@Component(value = "crOrchestrationService") // bean name
public class CROrchestrationService implements JavaDelegate {

  private static final transient Log logger =
      LogFactory.getLog(CROrchestrationService.class.getName());


  @Value("${spring.application.name}")
  private String compname;


  @Autowired
  private ServiceOrderManager serviceOrderManager;


  public void execute(DelegateExecution execution) {

    logger.info("CROrchestrationService");
    logger.info("VariableNames:" + execution.getVariableNames().toString());
    logger.info("orderid:" + execution.getVariable("orderid").toString());
    logger.info("contextServiceId:" + execution.getVariable("contextServiceId").toString());

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

        ServiceSpecCharacteristic c = spec.getServiceSpecCharacteristicByName("_CR_SPEC");
        String crspec = c.getDefaultValue();
        Characteristic servicecrspec = aService.getServiceCharacteristicByName("_CR_SPEC");
        if (servicecrspec != null) {
          crspec = servicecrspec.getValue().getValue();
        }
        
        //we need to get the equivalent resource spec. since ServiceSpec is an RFS
        ResourceSpecificationRef rSpecRef = spec.getResourceSpecification().stream().findFirst().get();
        //we will create a resource based on the values of resourcepsecificationRef
        Resource resourceCR = createRelatedResource( rSpecRef, sorder, aService );
        ResourceRef rr = new ResourceRef();
        rr.setId( resourceCR.getId() );
        rr.setName( resourceCR.getName());
        rr.setType( resourceCR.getType());
        su.addSupportingResourceItem( rr );
        su.setState(ServiceStateType.RESERVED);
        Note successNoteItem = new Note();
        successNoteItem.setText(String.format("Requesting CRIDGE to deploy crspec"));
        successNoteItem.setDate(OffsetDateTime.now(ZoneOffset.UTC).toString());
        successNoteItem.setAuthor(compname);
        su.addNoteItem(successNoteItem);        
        Service supd = serviceOrderManager.updateService(aService.getId(), su, false);
        
        

        String response = null;
        if (crspec != null) {
          response = createNewDeploymentRequest(aService, resourceCR, sorder.getId(), sorder.getStartDate(),
              sorder.getExpectedCompletionDate(), crspec);
        }
        

        Characteristic servicecrspecLast = aService.getServiceCharacteristicByName("_CR_SPEC_LASTSEND");
        servicecrspecLast.getValue().setValue( crspec );
        su.addServiceCharacteristicItem(servicecrspecLast);
        
        if ( response==null || !response.equals("OK")) {
          su = new ServiceUpdate();
          su.setState(ServiceStateType.TERMINATED);
          Note errNoteItem = new Note();
          errNoteItem.setText(String.format("Requesting CRIDGE to deploy crspec failed with message: " + response));
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
   * later on the name and its characteristics are updated via cridge 
   * @param rSpecRef
   * @param sOrder
   * @param aService
   * @return
   */
  private Resource createRelatedResource(ResourceSpecificationRef rSpecRef, ServiceOrder sOrder, Service aService) {
    
    ResourceCreate resCreate = new ResourceCreate();
    resCreate.setName(   "_cr_tmpname_service_" + aService.getId() );
    resCreate.setStartOperatingDate( aService.getStartDate() );
    resCreate.setEndOperatingDate(aService.getEndDate());
    ResourceSpecificationRef rSpecRefObj = new ResourceSpecificationRef() ;
    rSpecRefObj.id(rSpecRef.getId())
      .name( rSpecRef.getName())
      .setType(rSpecRef.getType());
    resCreate.setResourceSpecification(rSpecRefObj); 
    return serviceOrderManager.createResource( resCreate, sOrder, rSpecRef.getId() );

    
    
  }


  /**
   * 
   * This function makes a new deployment request for a custom resource  specification.
   * The request is performed via the message queue.
   * The function sends also some headers that are related and needed for deployment
   * These are the headers, that some of them are also added as metadata labels in CR:
   * <br>
   * <br><b>currentContextCluster:</b> current context of cluster
   * <br><b>clusterMasterURL:</b> current master url of the cluster
   * <br><b>org.etsi.osl.serviceId:</b> This is the related service id that the created resource has a reference
   * <br><b>org.etsi.osl.resourceId:</b> This is the related resource id that the created CR will wrap and reference. There
   * <br><b>org.etsi.osl.prefixName:</b> we need to add a short prefix (default is cr) to various places. For example in K8s cannot start with a number
   * <br><b>org.etsi.osl.serviceOrderId:</b> the related service order id of this deployment request
   * <br><b>org.etsi.osl.namespace:</b> requested namespace name
   * <br><b>org.etsi.osl.statusCheckFieldName:</b> The name of the field that is needed to be monitored in order to monitor the status of the service and translate it to TMF resource statys (RESERVED AVAILABLE, etc)
   * <br><b>org.etsi.osl.statusCheckValueStandby:</b> The CR specific value (of the CheckFieldName) that needs to me mapped to the TMF resource state STANDBY (see org.etsi.osl.tmf.ri639.model.ResourceStatusType)
   * <br><b>org.etsi.osl.statusCheckValueAlarm:</b> The CR specific value (of the CheckFieldName) that needs to me mapped to the TMF resource state ALARMS (see org.etsi.osl.tmf.ri639.model.ResourceStatusType)
   * <br><b>org.etsi.osl.statusCheckValueAvailable:</b> The CR specific value (of the CheckFieldName) that needs to me mapped to the TMF resource state AVAILABLE (see org.etsi.osl.tmf.ri639.model.ResourceStatusType)
   * <br><b>org.etsi.osl.statusCheckValueReserved:</b> The CR specific value (of the CheckFieldName) that needs to me mapped to the TMF resource state RESERVED (see org.etsi.osl.tmf.ri639.model.ResourceStatusType)
   * <br><b>org.etsi.osl.statusCheckValueUnknown:</b> The CR specific value (of the CheckFieldName) that needs to me mapped to the TMF resource state UNKNOWN (see org.etsi.osl.tmf.ri639.model.ResourceStatusType)
   * <br><b>org.etsi.osl.statusCheckValueSuspended:</b> The CR specific value (of the CheckFieldName) that needs to me mapped to the TMF resource state SUSPENDED (see org.etsi.osl.tmf.ri639.model.ResourceStatusType)
   * <br>
   * 
   * @param aService reference to the service that the resource and the CR belongs to
   * @param resourceCR reference the equivalent resource in TMF repo of the target CR. One to one mapping
   * @param orderId related service order ID
   * @param startDate start date of the deployment  (not used currently)
   * @param endDate end date of the deployment (not used currently)
   * @param _CR_SPEC the spec that is sent to cridge (in json)
   * @return a string respons from cridge. It might return "OK" if everything is ok. "SEE OTHER" if there are multiple CRIDGEs then some other cridge will handle the request for the equivalent cluster. Any other response is handled as error
   */
  private String createNewDeploymentRequest(Service aService, Resource resourceCR, String orderId,
      OffsetDateTime startDate,
      OffsetDateTime endDate, String _CR_SPEC) {

    try {
      Map<String, Object> map = new HashMap<>();
      map.put("currentContextCluster",getServiceCharacteristic(aService, "currentContextCluster")    );
      map.put("clusterMasterURL",getServiceCharacteristic(aService, "clusterMasterURL")    );
      map.put("org.etsi.osl.serviceId", aService.getId() );
      map.put("org.etsi.osl.resourceId", resourceCR.getId() );
      map.put("org.etsi.osl.prefixName", "cr" + resourceCR.getId().substring(0, 8) );
      map.put("org.etsi.osl.serviceOrderId", orderId );
      map.put("org.etsi.osl.namespace", orderId );
      map.put("org.etsi.osl.statusCheckFieldName",  getServiceCharacteristic(aService, "_CR_CHECK_FIELD")    );
      map.put("org.etsi.osl.statusCheckValueStandby", getServiceCharacteristic(aService, "_CR_CHECKVAL_STANDBY")  );
      map.put("org.etsi.osl.statusCheckValueAlarm", getServiceCharacteristic(aService, "_CR_CHECKVAL_ALARM")  );
      map.put("org.etsi.osl.statusCheckValueAvailable", getServiceCharacteristic(aService, "_CR_CHECKVAL_AVAILABLE")  );
      map.put("org.etsi.osl.statusCheckValueReserved", getServiceCharacteristic(aService, "_CR_CHECKVAL_RESERVED")  );
      map.put("org.etsi.osl.statusCheckValueUnknown", getServiceCharacteristic(aService, "_CR_CHECKVAL_UNKNOWN")  );
      map.put("org.etsi.osl.statusCheckValueSuspended", getServiceCharacteristic(aService, "_CR_CHECKVAL_SUSPENDED")  );
      

      logger.debug("createNewDeploymentRequest _CR_SPEC = " + _CR_SPEC);
      
      String response  = serviceOrderManager.cridgeDeploymentRequest( map, _CR_SPEC);
      int retries = 0;
      while ( response.equals("SEE OTHER")) {
        response  = serviceOrderManager.cridgeDeploymentRequest( map, _CR_SPEC);
        Thread.sleep(1000);
        retries++;
        if (retries>100) { //will support maximum 100 registered CRIDGE in queue
          break;
        }
      }
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
