package org.etsi.osl.osom.serviceactions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

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

import org.etsi.osl.model.DeploymentDescriptor;
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceActionQueueItem;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;

@Component(value = "NFVONSTerminateTask") //bean name
public class NFVONSTerminateTask  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( NFVONSTerminateTask.class.getName() );

	@Value("${spring.application.name}")
	private String compname;

    @Autowired
    private ServiceOrderManager serviceOrderManager;
    
	public void execute(DelegateExecution execution) {
		
		logger.info("NFVONSTerminateTask:" + execution.getVariableNames().toString() );

		Service aService = null;
		if (execution.getVariable("Service")!=null) {
			ObjectMapper mapper = new ObjectMapper();
			
			try {
				aService = mapper.readValue( execution.getVariable("Service").toString(), Service.class);
			} catch (JsonMappingException e) {
				e.printStackTrace();
				return;
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				return;
			}
		}

		logger.info("Will terminate Service with id:" + aService.getId() );


		
		if (aService.getServiceCharacteristicByName( "DeploymentRequestID" ) != null ){
			String deploymentRequestID = aService.getServiceCharacteristicByName( "DeploymentRequestID" ).getValue().getValue();
			logger.info("Will terminate DeploymentRequestID:" + deploymentRequestID );
			DeploymentDescriptor dd =serviceOrderManager.retrieveNFVODeploymentRequestById( Long.parseLong( deploymentRequestID ) );
			if ( dd != null ) {
				dd.setEndDate( new Date() ); // it will terminate it now
				serviceOrderManager.nfvoDeploymentRequestByNSDid(dd);
				
			}
		}
		
		
		try {
			ServiceActionQueueItem item;

			ObjectMapper mapper = new ObjectMapper();
			item = mapper.readValue( execution.getVariable("serviceActionItem").toString(), ServiceActionQueueItem.class);
			aService = mapper.readValue( execution.getVariable("Service").toString(), Service.class);
			
			ServiceUpdate supd = new ServiceUpdate();
			Note n = new Note();
			n.setText("Service Action NFVONSTerminateTask. Action: " + item.getAction() );
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

	

}
