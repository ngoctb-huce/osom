package org.etsi.osl.osom.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component(value = "initializeServiceTestOrchestration") //bean name
public class InitializeServiceTestOrchestration  implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog( CheckServiceTestDeployment.class.getName());
	
	@Autowired
	private ServiceOrderManager serviceOrderManager;

	@Value("${spring.application.name}")
	private String compname;
	
	@Override
	public void execute(DelegateExecution execution) {
		
		
		logger.info( "initializeServiceTestOrchestration" );
		logger.info( "VariableNames:" + execution.getVariableNames().toString() );
		logger.info("orderid:" + execution.getVariable("orderid").toString() );
		logger.info("contextServiceId:" + execution.getVariable("contextServiceId").toString() );
				

		if (execution.getVariableLocal("contextServiceId") instanceof String) {



		} else {
			logger.error( "Cannot retrieve variable contextServiceId"  );
		}

	}
}
