package org.etsi.osl.osom.lcm;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.etsi.osl.tmf.lcm.model.LCMRuleSpecification;

/**
 * @author ctranoris
 *
 */
public class LCMRulesExecutor {

	private static final transient Log logger = LogFactory.getLog( LCMRulesExecutor.class.getName() );
	
	public LCMRulesExecutorVariables executeLCMRuleCode(LCMRuleSpecification lcmspec, LCMRulesExecutorVariables vars) {

		logger.info("executeLCMRuleCode lcmspecId =" + lcmspec.getId() + "lcmspec =" + lcmspec.getName()   );
		
		//Prepare code
		final String className =  "ExecRule_"+lcmspec.getId().replace("-", "_");
		
		String code=
				//"package " + className + "; \n\n" +	
				"import org.etsi.osl.osom.lcm.LcmBaseExecutor;\n\n" 
				+ "public class "+className+" extends LcmBaseExecutor{\n\n" +	
				"""
						@Override
						public void exec() {						
						//SNIP STARTS
						"""
				+ preprocess( lcmspec.getCode() )
				+"""
					//SNIP ENDS
					}
				}
			
				""";
		
		logger.debug("code dump:");
		logger.debug( code );
		try {
			vars = execudeCode(className, code, vars, lcmspec);		
		}catch (Exception e) {
			e.printStackTrace();
		}
		
		
		
		
		return vars;
	}
	
	
	/**
	 * 
	 * Any code preprocessing before compilation
	 * @param aCode
	 * @return
	 */
	private String preprocess(final String aCode) {
		String newCode = aCode;
		newCode = preprocess_ClearAllEVALVariables(aCode );
		return newCode;
	}
	
	
	/**
	 * 
	 * inside Strings we may have parameters from the code. 
	 * The parameters are in the form $EVAL_param3_EVAL$
	 * They should be properly replaced inside the String
	 * @param newValue
	 * @return
	 */
	private String preprocess_ClearAllEVALVariables(String newValue) {
		String anewValue = newValue;
		logger.debug("clearEVALVariables before " + newValue);
				
		anewValue = anewValue.replace("$QUOTESTR$", "\"");
//		anewValue = anewValue.replace("$$XVALS_", " \"\"\" +");
//		anewValue = anewValue.replace("_XVALE$$", "+ \"\"\"\n");
		anewValue = anewValue.replace("$$XVALS_", "\"+");
		anewValue = anewValue.replace("_XVALE$$", "+\"");
		logger.debug("clearEVALVariables after " + anewValue);
		return anewValue;
	}

	/**
	 * @param className
	 * @param code
	 * @param vars
	 * @param lcmspec 
	 * @return
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws NoSuchMethodException 
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	private LCMRulesExecutorVariables execudeCode(String className, String code, LCMRulesExecutorVariables vars, LCMRuleSpecification lcmspec) throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		 // A temporary directory where the java code and class will be located
        Path temp = Paths.get(System.getProperty("java.io.tmpdir"),  "openslice", className);
        Files.createDirectories(temp);
		
        // Creation of the java source file
        // You could also extends the SimpleJavaFileObject object as shown in the doc.
        // See SimpleJavaFileObject at https://docs.oracle.com/javase/8/docs/api/javax/tools/JavaCompiler.html
        Path javaSourceFile = Paths.get(temp.normalize().toAbsolutePath().toString(), className + ".java");
        Files.write(javaSourceFile, code.getBytes());

        logger.debug("For lcmspec " + lcmspec.getName() + " The java source file is located at "+javaSourceFile);
        // Verification of the presence of the compilation tool archive
        final String toolsJarFileName = "tools.jar";
        final String javaHome = System.getProperty("java.home");
        Path toolsJarFilePath = Paths.get(javaHome, "lib", toolsJarFileName);
        if (!Files.exists(toolsJarFilePath)){
        	logger.warn( "The tools jar file ("+toolsJarFileName+") could not be found at ("+toolsJarFilePath+").");
        }
		
        // The compile part
        // Definition of the files to compile
        File[] files1 = {javaSourceFile.toFile()};
        
        // Get the compiler
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        logger.debug("compiler =  "+ compiler);
        // Get the file system manager of the compiler
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
        // Create a compilation unit (files)
        Iterable<? extends JavaFileObject> compilationUnits =
                fileManager.getJavaFileObjectsFromFiles(Arrays.asList(files1));
        // A feedback object (diagnostic) to get errors
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
        // Compilation unit can be created and called only once
        
        List<String> optionList = new ArrayList<String>();
        // set compiler's classpath to be same as the runtime's
        /**
         * this is a current solution found. 
         * We need here to add to classpath the jar with the classes, so that the compile can be performed in runtime. 
         * This is the location of the jar inside the running container 
         */
        
        File classesJar = new File("/opt/openslice/lib/org.etsi.osl.osom-1.2.0-SNAPSHOT.jar");        
        if ( classesJar.exists()  ) {
            optionList.addAll(Arrays.asList("-classpath", classesJar.getAbsoluteFile().toString() ));
        } 
        logger.debug("optionList =  "+ optionList.toString());
        

     
     
        JavaCompiler.CompilationTask task = compiler.getTask(
                null,
                fileManager,
                diagnostics,
                optionList,
                null,
                compilationUnits
        );
        logger.debug("task1 =  "+ task);
        // The compile task is called
        task.call();
        logger.debug("task2 =  "+ task);
        // Printing of any compile problems
        
        for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
        	String err =  String.format("LCMRule: %s, Phase: %s -> Error on line %d in %s, %s, %s %n",
        			lcmspec.getName(),
        			lcmspec.getLcmrulephase() ,
                    diagnostic.getLineNumber(),
                    diagnostic.getSource(),
                    diagnostic.getCode(),
                    diagnostic.getMessage(null) );
        	logger.error( err );
        	vars.getCompileDiagnosticErrors().add(err);
        	
        }
        

        logger.debug("task3 =  "+ task);
        if ( diagnostics.getDiagnostics().size()>0 ) {
            logger.error("LCMRule:" + lcmspec.getName() + "execudeCode compile error. Will just return");
        	return vars;
        }
        
        // Close the compile resources
        fileManager.close();

        URL[] classpath = new URL[] { temp.toUri().toURL()  };
            
        if ( classesJar.exists()  ) {
        	classpath = new URL[] { temp.toUri().toURL(), classesJar.toURI().toURL()  };
        } 
        logger.debug("classpath =  "+ classpath.toString());


        // Now that the class was created, we will load it and run it
        ClassLoader classLoader = LCMRulesExecutor.class.getClassLoader();
        logger.debug("classLoader =  "+ classLoader);
        @SuppressWarnings("resource")
		URLClassLoader urlClassLoader = new URLClassLoader(
				classpath,
                classLoader);
        Class javaDemoClass = urlClassLoader.loadClass(className);
        logger.debug("javaDemoClass =  "+ javaDemoClass);
        Object obj = javaDemoClass.getDeclaredConstructor().newInstance();
        logger.debug("obj =  "+ obj);
        
        Method method = javaDemoClass.getMethod("run",  LCMRulesExecutorVariables.class,  LCMRuleSpecification.class);
        logger.debug("method =  "+ method);
        ArrayList<Object> methodArgs = new ArrayList<Object>();
        methodArgs.add( vars );
        methodArgs.add( lcmspec );
        
        Object response = method.invoke(obj, methodArgs.toArray());
        if ( response instanceof LCMRulesExecutorVariables ) {
        	return (LCMRulesExecutorVariables) response;
        }
        
		return vars;
	}

}
