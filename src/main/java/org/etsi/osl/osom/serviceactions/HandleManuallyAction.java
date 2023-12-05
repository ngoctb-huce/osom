package org.etsi.osl.osom.serviceactions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceActionQueueItem;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;

@Component(value = "HandleManuallyAction") //bean name
public class HandleManuallyAction  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( HandleManuallyAction.class.getName() );


	@Value("${spring.application.name}")
	private String compname;
	
    @Autowired
    private ServiceOrderManager serviceOrderManager;
    
	public void execute(DelegateExecution execution) {

		logger.info("HandleManuallyAction:" + execution.getVariableNames().toString() );
		logger.info("Action will be logged and deleted" );
		Service aService = null;
		ServiceActionQueueItem item;
		if (execution.getVariable("Service")!=null) {
			ObjectMapper mapper = new ObjectMapper();

			try {
				aService = mapper.readValue( execution.getVariable("Service").toString(), Service.class);
				item = mapper.readValue( execution.getVariable("serviceActionItem").toString(), ServiceActionQueueItem.class);
				ServiceUpdate supd = new ServiceUpdate();
				Note n = new Note();
				n.setText("Service Action HandleManuallyAction." + item.getAction() );
				n.setAuthor( compname );
				n.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
				supd.addNoteItem( n );

				
				serviceOrderManager.deleteServiceActionQueueItem( item );			
				serviceOrderManager.updateService( aService.getId() , supd, false);
			} catch (JsonMappingException e1) {
				e1.printStackTrace();
			} catch (JsonProcessingException e1) {
				e1.printStackTrace();
			}
			
			
			
		}
	}

}
