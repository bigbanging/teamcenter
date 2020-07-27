package com.tc.demo.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.tc.demo.command.BOMCheckCommand;
import com.teamcenter.rac.aif.AbstractAIFCommand;
import com.teamcenter.rac.aif.AbstractAIFUIApplication;
import com.teamcenter.rac.aifrcp.AIFUtility;

public class BOMCheckHandler extends AbstractHandler {
	private AbstractAIFUIApplication app;
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		 app = AIFUtility.getCurrentApplication();
		new Thread(new Runnable() {
			

			@Override
			public void run() {
				AbstractAIFCommand abstractAIFCommand = new BOMCheckCommand(app);
				
				try {
					abstractAIFCommand.executeModal();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
		return null;
	}
}
