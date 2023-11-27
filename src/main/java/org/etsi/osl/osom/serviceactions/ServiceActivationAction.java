package org.etsi.osl.osom.serviceactions;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.lcm.LCMRulesController;
import org.etsi.osl.osom.lcm.LCMRulesExecutorVariables;
import org.etsi.osl.osom.management.ServiceOrderManager;
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.lcm.model.ELCMRulePhase;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceActionQueueItem;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.etsi.osl.tmf.so641.model.ServiceOrderItem;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component(value = "ServiceActivationAction") //bean name
public class ServiceActivationAction  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( ServiceActivationAction.class.getName() );

	@Value("${spring.application.name}")
	private String compname;

    @Autowired
    private ServiceOrderManager serviceOrderManager;

	@Autowired
	private LCMRulesController lcmRulesController;
	
	public void execute(DelegateExecution execution) {
		
		logger.info("ServiceActivationAction:" + execution.getVariableNames().toString() );
		
		
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			ServiceActionQueueItem item;
			Service aService;
			item = mapper.readValue( execution.getVariable("serviceActionItem").toString(), ServiceActionQueueItem.class);
			aService = mapper.readValue( execution.getVariable("Service").toString(), Service.class);
			
			ServiceUpdate supd = new ServiceUpdate();
			Note n = new Note();
			n.setText("Service Action ServiceActivationAction. Action: " + item.getAction() );
			n.setAuthor( compname );
			n.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
			supd.addNoteItem( n );
			serviceOrderManager.deleteServiceActionQueueItem( item );			
			

			
			
			ServiceSpecification spec = serviceOrderManager.retrieveServiceSpec( aService.getServiceSpecificationRef().getId() );//fetch the equivalent spec;			
			ServiceOrder sor = null;
			ServiceOrderItem soi = null;
			
			if ( aService.getServiceOrder().size() >0  ) {
				sor = serviceOrderManager.retrieveServiceOrder( aService.getServiceOrder().stream().findFirst().get().getId() ) ;
				if ( sor.getOrderItem().size()>0) {
					soi = sor.getOrderItem().stream().findFirst().get();
				}
			}
			
			
			if ( spec!= null ) {
				//execute any LCM rules "AFTER_ACTIVATION" phase for the SPEC;
				LCMRulesExecutorVariables vars = new LCMRulesExecutorVariables(spec, sor, soi, null, supd, aService, serviceOrderManager);
				
				logger.debug("===============BEFORE lcmRulesController.execPhas AFTER_ACTIVATION for spec:" + spec.getName() + " =============================");
				vars = lcmRulesController.execPhase( ELCMRulePhase.AFTER_ACTIVATION, vars );

				//logger.debug("vars= " + vars );		
				logger.debug("===============AFTER lcmRulesController.execPhas =============================");

				Note noteItem = new Note();
				
				if ( vars.getCompileDiagnosticErrors().size()>0 ) {
					String msg = "LCM Rule execution error by ServiceActivationAction. ";
					for (String tmsg :  vars.getCompileDiagnosticErrors()) {
						msg = msg + "\n"+ tmsg;
					}
					noteItem.setText( msg );
				} else {
					String msg = "LCM AFTER_ACTIVATION Rules executed. ";
					noteItem.setText( msg );
					
				}
				noteItem.setAuthor( compname );
				noteItem.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
				supd.addNoteItem(noteItem);
			}
				
			

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
