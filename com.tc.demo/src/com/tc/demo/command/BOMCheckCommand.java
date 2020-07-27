package com.tc.demo.command;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.tc.demo.view.TCProcessDialog;
import com.teamcenter.rac.aif.AbstractAIFCommand;
import com.teamcenter.rac.aif.AbstractAIFUIApplication;
import com.teamcenter.rac.aif.kernel.AIFComponentContext;
import com.teamcenter.rac.aif.kernel.InterfaceAIFComponent;
import com.teamcenter.rac.kernel.TCComponent;
import com.teamcenter.rac.kernel.TCComponentBOMLine;
import com.teamcenter.rac.kernel.TCComponentBOMWindow;
import com.teamcenter.rac.kernel.TCComponentBOMWindowType;
import com.teamcenter.rac.kernel.TCComponentItem;
import com.teamcenter.rac.kernel.TCComponentItemRevision;
import com.teamcenter.rac.kernel.TCException;
import com.teamcenter.rac.kernel.TCSession;
import com.teamcenter.rac.util.MessageBox;

public class BOMCheckCommand extends AbstractAIFCommand{

	private AbstractAIFUIApplication application = null;
	private TCSession session = null;
	private TCProcessDialog dialog = null;

	public BOMCheckCommand(AbstractAIFUIApplication app) {
		this.application = app;
		session = (TCSession) application.getSession();

		dialog = new TCProcessDialog();
		dialog.setTitle("检查选中BOM...");
		dialog.setInfo("正在检擦版本的异同...");
		try {
			//选中对象
			InterfaceAIFComponent targetComponent = application.getTargetComponent();
			if (targetComponent == null) {
				if (dialog != null) {
					dialog.dispose();
				}
				MessageBox.post("请在结构管理器中选择要标记比较的顶层BOMLine", "提示",MessageBox.INFORMATION);
				return;
			}
			if (targetComponent instanceof TCComponentBOMLine) {
				TCComponentBOMLine tcComponentBOMLine = (TCComponentBOMLine) targetComponent;
				List<TCComponentItem> allItems = getAllItems(tcComponentBOMLine);
				outInfo(allItems);
				
				if (dialog != null) {
					dialog.dispose();
				}
				MessageBox.post("BOM版本异同比较完成,手动打开路径 "+createFile().getAbsolutePath(), "提示", 2);
				Runtime.getRuntime().exec("C:/WINDOWS/system32/notepad.exe " + createFile().getAbsoluteFile());
			}
			
		}catch(TCException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * 根据顶层BomLine递归活动所有的BOMLine
	 * @param bomline
	 * @param parent
	 * @return
	 * @throws TCException
	 */
	private List<TCComponentBOMLine> getAllBomline(TCComponentBOMLine bomline, TCComponentBOMLine parent) throws TCException {
		List<TCComponentBOMLine> bomlist = new ArrayList<TCComponentBOMLine>();
		if (!bomlist.contains(bomline)) {
			bomlist.add(bomline);
		}
		AIFComponentContext[] children = bomline.getChildren();
		if(children == null) {
			return bomlist;
		}
		for (int i = 0; i < children.length; i++) {
			InterfaceAIFComponent tcComp = children[i].getComponent();	
			TCComponentBOMLine childBomline = (TCComponentBOMLine) tcComp;
			List<TCComponentBOMLine> temp = getAllBomline(childBomline, bomline);
			for (int j = 0; j < temp.size(); j++) {
				TCComponentBOMLine tempBomList = temp.get(j);
				if (!bomlist.contains(tempBomList)) {
					bomlist.add(tempBomList);
				}
			}
		}
		return bomlist;
	}
	/**
	 * 通过BomLine获取Item 并存入集合中，去重复
	 * @param bomline
	 * @return
	 * @throws TCException
	 */
	private List<TCComponentItem> getAllItems(TCComponentBOMLine bomline) throws TCException{
		List<TCComponentItem> items = new ArrayList<TCComponentItem>();
		List<TCComponentBOMLine> allBomline = getAllBomline(bomline, null);
		if (allBomline != null && allBomline.size() > 0) {
			for (TCComponentBOMLine tcComponentBOMLine : allBomline) {
				TCComponentItem item = tcComponentBOMLine.getItem();
				if(item == null) continue;
				if(items.contains(item)) continue;
				items.add(item);
			}
		}
		return items;
	}

	private void outInfo(List<TCComponentItem> items) {
		FileWriter fileWriter = null;
		try {
			File file = createFile();
			fileWriter = new FileWriter(file);
			fileWriter.write("=======================Show Different between ItemRevision=======================\r\n");
			fileWriter.write("*********************************************************************************\r\n");
			for (TCComponentItem tcComponentItem : items) {
				getItemRevision(tcComponentItem, fileWriter);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TCException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			fileWriter.flush();
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void getItemRevision(TCComponentItem item, FileWriter fileWriter) throws Exception {
		//通过版本关系获取Item下的所有版本 "revision_list" 
		TCComponent[] relatedComponents = item.getRelatedComponents("revision_list");
		int length = relatedComponents.length;
		//Item下版本数量大于1时 判断版本是否有BOM 如果有就进行比较
		if (relatedComponents != null && length > 1) {
			compareResult(relatedComponents[length-1], relatedComponents[length-2], fileWriter);
		}
	}
	/**
	 * 比较多版本的BOM差异
	 * @param last
	 * @param before
	 * @param fileWriter
	 * @throws Exception
	 */
	private void compareResult(TCComponent last, TCComponent before, FileWriter fileWriter) throws Exception {
		TCComponentItemRevision lastRevision = (TCComponentItemRevision) last;
		TCComponentItemRevision beforeRevision = (TCComponentItemRevision) before;
		//通过对版本构建虚拟BOM
		TCComponentBOMLine lastBomLine = constructBomLine(lastRevision);
		TCComponentBOMLine beforeBomLine = constructBomLine(beforeRevision);
		//获取虚拟BOM的子，只需一层即可
		AIFComponentContext[] lastChildren = lastBomLine.getChildren();
		AIFComponentContext[] beforeChildren = beforeBomLine.getChildren();
		List<AIFComponentContext> flags = new ArrayList<AIFComponentContext>();
		if (lastChildren == null || lastChildren.length<=0 || beforeChildren == null || beforeChildren.length <= 0) {
			return;
		}
		dialog.setInfo("比较Item："+last.getProperty("item_id")+"的版本："+last.toString()+", "+before.toString()+" BOM 差异");
		//比较相同Item不同的版本下的BOM长度，如果不相同，说明两个版本的BOM行不相同，记录
		if (lastChildren.length != beforeChildren.length) {
			//System.out.println("——————版本的BOMLine数量不相同——————");
			fileWriter.write("ItemRevision's has different BomLine Quantity :\t\t\t" + lastBomLine.getItem().getProperty("object_string")+"\r\n");

		}
		for (int i = 0; i < beforeChildren.length; i++) {
			for (int j = 0; j < lastChildren.length; j++) {
				if (beforeChildren[i].getComponent().getProperty("item_id").equals(lastChildren[j].getComponent().getProperty("item_id"))) {
					if (!flags.contains(beforeChildren[i])) {
						flags.add(beforeChildren[i]);
					}
				}
			}
		}
//		System.out.println("========flags=======: "+flags);
		//当BOM行相同时则需比较每一个子是否一样对应，不能一一对应则说明构成不相同，一一对应则比较数量
		if (flags.size() != lastChildren.length) {
			//System.out.println("——————版本的BOMLine数量相同，构成不同——————");
			fileWriter.write("ItemRevision's has same BomLine Quantity different content :\t" 
					+ lastBomLine.getItem().getProperty("object_string")+"\r\n");
		}else {
			//BOMLine行数相同并且一一对应 则比较数量
			out:
				for (int i = 0; i < beforeChildren.length; i++) {
					for (int j = 0; j < lastChildren.length; j++) {
						if (beforeChildren[i].getComponent().getProperty("item_id").equals(lastChildren[j].getComponent().getProperty("item_id"))) {

							if (!beforeChildren[i].getComponent().getProperty("bl_quantity").equals(lastChildren[j].getComponent().getProperty("bl_quantity"))) {
								//System.out.println("——————版本的BOMLine数量相同，构成相同，零组件数量不同——————");
								fileWriter.write("ItemRevision's has different BomLine part number :\t\t"+lastBomLine.getItem().getProperty("object_string")+"\r\n");
							}
							break out;
						}
					}
				}
		dialog.setInfo("完成比较，正在写入txt中，写入完成将自动打开...");
		}
	}
	/**
	 * 创建信息输出的文件
	 * @return
	 */
	private File createFile() {
		File fileFolder = new File("d:/ZK/Check/");
		if (!fileFolder.exists()) {
			fileFolder.mkdirs();
		}
		File file = new File(fileFolder, "result.txt");
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return file;
	}
	/**
	 * 通过给定顶层Item对象，在非PSE结构管理器的环境中，构建一个BOM结构.
	 * @param revision
	 * @throws TCException
	 */
	private TCComponentBOMLine constructBomLine(TCComponentItemRevision revision) throws TCException {
		TCComponentBOMWindowType type = (TCComponentBOMWindowType) session.getTypeComponent("BOMWindow");
		TCComponentBOMWindow bomWindow = type.create(null);
		TCComponentBOMLine topBomline = bomWindow.setWindowTopLine(revision.getItem(), revision, null, null);
		topBomline.pack();
		return topBomline;
	}
}
