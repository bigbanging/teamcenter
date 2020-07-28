package com.tc.demo.handlers;

import org.apache.log4j.Logger;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class CustomViewHandler extends AbstractHandler{
	
	private static Logger logger = Logger.getLogger(CustomViewHandler.class);

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		System.out.println("込込込込込込込込込込亜込込込");
		logger.debug("DEBUG");
		logger.info("INFO");
		logger.warn("WARN");
		logger.error("ERROR");
		return null;
	}

}
