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
import org.etsi.osl.tmf.common.model.Any;
import org.etsi.osl.tmf.common.model.service.Characteristic;
import org.etsi.osl.tmf.common.model.service.Note;
import org.etsi.osl.tmf.lcm.model.ELCMRulePhase;
import org.etsi.osl.tmf.scm633.model.ServiceSpecification;
import org.etsi.osl.tmf.sim638.model.Service;
import org.etsi.osl.tmf.sim638.model.ServiceActionQueueItem;
import org.etsi.osl.tmf.sim638.model.ServiceUpdate;
import org.etsi.osl.tmf.so641.model.ServiceOrder;
import org.etsi.osl.tmf.so641.model.ServiceOrderItem;
import org.etsi.osl.tmf.stm653.model.ServiceTest;
import org.etsi.osl.tmf.stm653.model.ServiceTestUpdate;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component(value = "ServiceEvaluateAction") //bean name
public class ServiceEvaluateAction  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( ServiceEvaluateAction.class.getName() );

	@Value("${spring.application.name}")
	private String compname;

    @Autowired
    private ServiceOrderManager serviceOrderManager;

	@Autowired
	private LCMRulesController lcmRulesController;
    
	public void execute(DelegateExecution execution) {
		
		logger.info("ServiceEvaluateAction:" + execution.getVariableNames().toString() );
		
		
		ObjectMapper mapper = new ObjectMapper();
		
		try {
			ServiceActionQueueItem item;
			Service aService;
			item = mapper.readValue( execution.getVariable("serviceActionItem").toString(), ServiceActionQueueItem.class);
			aService = mapper.readValue( execution.getVariable("Service").toString(), Service.class);
			
			ServiceUpdate supd = new ServiceUpdate();
			Note n = new Note();
			n.setText("Service Action ServiceEvaluateAction. Action: " + item.getAction() );
			n.setAuthor( compname );
			n.setDate( OffsetDateTime.now(ZoneOffset.UTC).toString() );
			supd.addNoteItem( n );
			serviceOrderManager.deleteServiceActionQueueItem( item );
			

			ServiceSpecification spec = serviceOrderManager.retrieveServiceSpec( aService.getServiceSpecificationRef().getId() );//fetch the equivalent spec;			
			ServiceOrder sor = null;
			ServiceOrderItem soi = null;
			
			if ( aService.getServiceOrder().size() >0  ) {
				sor = serviceOrderManager.retrieveServiceOrder( aService.getServiceOrder().stream().findFirst().get().getId() ) ;
				if ( sor == null ) {
					logger.error("ServiceOrder is NULL. will return!");
					return;
				}
				if ( sor.getOrderItem().size()>0) {
					soi = sor.getOrderItem().stream().findFirst().get();
				}
			}
			
			
			if ( spec!= null ) {
				//execute any LCM rules "SUPERVISION" phase for the SPEC;
				LCMRulesExecutorVariables vars = new LCMRulesExecutorVariables(spec, sor, soi, null, supd,aService, serviceOrderManager);
				logger.debug("===============DEBUG lcmRulesController.execPhas SUPERVISION for spec:" + spec.getName() + " =============================");
				
				Characteristic schart = aService.getServiceCharacteristicByName("long_text");
				if ( schart!= null ) {
					String teest = schart.getValue().getValue();
					logger.debug("schart size = " + teest.length() );
					
					logger.debug("schart " + teest );
					logger.debug("======================================================================================================");					
				}
				
				logger.debug("===============BEFORE lcmRulesController.execPhas SUPERVISION for spec:" + spec.getName() + " =============================");
				vars = lcmRulesController.execPhase( ELCMRulePhase.SUPERVISION, vars );

				//logger.debug("vars= " + vars );		
				logger.debug("===============AFTER lcmRulesController.execPhas =============================");

				Note noteItem = new Note();
				if ( vars.getCompileDiagnosticErrors().size()>0 ) {
					String msg = "LCM Rule execution error by ServiceEvaluateAction. ";
					for (String tmsg :  vars.getCompileDiagnosticErrors()) {
						msg = msg + "\n"+ tmsg;
					}
					noteItem.setText( msg );
				} else {
					String msg = "LCM SUPERVISION Rules executed. ";
					noteItem.setText( msg );
					
				}
				noteItem.setAuthor( compname );
				supd.addNoteItem(noteItem);
			}
			
			serviceOrderManager.updateService( aService.getId() , supd, false);
			
			//if references ServiceTest we need to update it
			
			if (  ( aService.getServiceCharacteristicByName( "testSpecRef" ) != null ) && ( aService.getServiceCharacteristicByName( "testInstanceRef" ) != null )  ) {
				String sTestId = aService.getServiceCharacteristicByName( "testInstanceRef" ).getValue().getValue();
				ServiceTest serviceTest = serviceOrderManager.retrieveServiceTest(sTestId);
				ServiceTestUpdate stupd = new ServiceTestUpdate();
				for (org.etsi.osl.tmf.stm653.model.Characteristic c : serviceTest.getCharacteristic()) {						
					stupd.addCharacteristicItem( c );		
					String newvalue = aService.getServiceCharacteristicByName( c.getName() ).getValue().getValue();
					c.setValue( new Any(newvalue) ) ; 
				}	
				
				serviceOrderManager.updateServiceTest(sTestId, stupd);				
				
			}
			
		} catch (JsonMappingException e) {
			e.printStackTrace();
			return;
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return;
		}
	}

}
