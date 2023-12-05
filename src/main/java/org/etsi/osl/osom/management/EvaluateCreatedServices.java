package org.etsi.osl.osom.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.osom.lcm.LCMRulesController;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component(value = "evaluateCreatedServices") //bean name
public class EvaluateCreatedServices implements JavaDelegate {

	private static final transient Log logger = LogFactory.getLog(EvaluateCreatedServices.class.getName());

	@Autowired
	private ServiceOrderManager serviceOrderManager;


	@Autowired
	private LCMRulesController lcmRulesController;
	
	
	@Value("${spring.application.name}")
	private String compname;
	
	public void execute(DelegateExecution execution) {

		logger.info("EvaluateCreatedServices:" + execution.getVariableNames().toString());

		execution.setVariable(
				"finishedServiceID", 
				(String) execution.getVariable("contextServiceId")  ); 
		
		
	}
	
}
