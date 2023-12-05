package org.etsi.osl.osom.serviceactions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.management.AlarmsService;
import org.etsi.osl.osom.management.ServiceOrderManager;
import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.common.model.service.ResourceRef;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceActionQueueItem;
import org.etsi.osl.tmf.sim638.model.ServiceOrderRef;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component(value = "CRPatchTask") // bean name
public class CRPatchTask implements JavaDelegate {

  private static final transient Log logger = LogFactory.getLog(CRPatchTask.class.getName());

  @Value("${spring.application.name}")
  private String compname;

  @Autowired
  private ServiceOrderManager serviceOrderManager;

  @Autowired
  AlarmsService alarmsService;

  public void execute(DelegateExecution execution) {

    logger.debug("CRPatchTask:" + execution.getVariableNames().toString());

    ObjectMapper mapper = new ObjectMapper();
    ServiceActionQueueItem item;
    Service aService;
    Service originalService;
    try {
      item = mapper.readValue(execution.getVariable("serviceActionItem").toString(),
          ServiceActionQueueItem.class);
      aService = mapper.readValue(execution.getVariable("Service").toString(), Service.class);
      // extract the original service from the Item

      originalService = mapper.readValue(item.getOriginalServiceInJSON(), Service.class);;
    } catch (JsonProcessingException e1) {
      e1.printStackTrace();
      return;
    }



    List<Characteristic> changeCharacteristics = new ArrayList<>();
    // identify here the characteristics that changed
    if (aService.getServiceCharacteristic() != null) {
      for (Characteristic srcChar : aService.getServiceCharacteristic()) {

        if (originalService.getServiceCharacteristicByName(srcChar.getName()) != null) {

          Characteristic origChar = originalService.getServiceCharacteristicByName(srcChar.getName());
          if ((origChar != null) && (origChar.getValue() != null) && (srcChar.getValue() != null) && (origChar.getValue().getValue() != null)) {
            if (!origChar.getValue().getValue().equals(srcChar.getValue().getValue())) {
              changeCharacteristics.add(srcChar);
            }
          }
        }
      }
    }



    



    ServiceUpdate supd = new ServiceUpdate();

    try {

      String response = null;
      Characteristic servicecrspec = aService.getServiceCharacteristicByName("_CR_SPEC");
      String crspec = servicecrspec.getValue().getValue();

        response = createNewDeploymentUpdateRequest(aService, crspec);
        Note n = new Note();
        n.setAuthor(compname);
        n.setDate(OffsetDateTime.now(ZoneOffset.UTC).toString());

        if ( response!=null && response.equals("OK")) {

          n.setText("Service Action CRPatchTask successful . Action: " + item.getAction() + ". ");
        } else {
          n.setText("Service Action CRPatchTask failed . Action: " + item.getAction() + ". Response = " + response);
        }

        supd.addNoteItem(n);
        serviceOrderManager.deleteServiceActionQueueItem(item);
        serviceOrderManager.updateService(aService.getId(), supd, false);
        

        return;

    }catch (Exception e) {
      e.printStackTrace();
    }
    
    
    
    
    Note n = new Note();
    n.setText("Service Action CRPatchTask FAILED. Action: " + item.getAction() + ". ");
    n.setAuthor(compname);
    n.setDate(OffsetDateTime.now(ZoneOffset.UTC).toString());
    supd.addNoteItem(n);
    serviceOrderManager.deleteServiceActionQueueItem(item);
    serviceOrderManager.updateService(aService.getId(), supd, false);

    logger.debug("CRPatchTask:" + n.getText());



  }

  private String createNewDeploymentUpdateRequest(Service aService, String crspec) {
    try {
      Map<String, Object> map = new HashMap<>();
      map.put("currentContextCluster",getServiceCharacteristic(aService, "currentContextCluster")    );
      map.put("clusterMasterURL",getServiceCharacteristic(aService, "clusterMasterURL")    );
      map.put("org.etsi.osl.serviceId", aService.getId() );
      map.put("org.etsi.osl.prefixName",getServiceCharacteristic(aService, "org.etsi.osl.prefixName")    );
      map.put("org.etsi.osl.resourceId",getServiceCharacteristic(aService, "org.etsi.osl.resourceId")    );
      map.put("org.etsi.osl.serviceOrderId",getServiceCharacteristic(aService, "org.etsi.osl.serviceOrderId")    );
      map.put("org.etsi.osl.namespace",getServiceCharacteristic(aService, "org.etsi.osl.namespace")    );
      map.put("org.etsi.osl.statusCheckFieldName",getServiceCharacteristic(aService, "org.etsi.osl.statusCheckFieldName")    );
      map.put("org.etsi.osl.statusCheckValueStandby",getServiceCharacteristic(aService, "org.etsi.osl.statusCheckValueStandby")    );
      map.put("org.etsi.osl.statusCheckValueAlarm",getServiceCharacteristic(aService, "org.etsi.osl.statusCheckValueAlarm")    );
      map.put("org.etsi.osl.statusCheckValueAvailable",getServiceCharacteristic(aService, "org.etsi.osl.statusCheckValueAvailable")    );
      map.put("org.etsi.osl.statusCheckValueReserved",getServiceCharacteristic(aService, "org.etsi.osl.statusCheckValueReserved")    );
      map.put("org.etsi.osl.statusCheckValueUnknown",getServiceCharacteristic(aService, "org.etsi.osl.statusCheckValueUnknown")    );
      map.put("org.etsi.osl.statusCheckValueSuspended",getServiceCharacteristic(aService, "org.etsi.osl.statusCheckValueSuspended")    );
      
      

      
      String response  = serviceOrderManager.cridgeDeploymentUpdateRequest( map, crspec);
      int retries = 0;
      while ( response.equals("SEE OTHER")) {
        response  = serviceOrderManager.cridgeDeploymentUpdateRequest( map, crspec);
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
