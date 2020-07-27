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
		dialog.setTitle("���ѡ��BOM...");
		dialog.setInfo("���ڼ���汾����ͬ...");
		try {
			//ѡ�ж���
			InterfaceAIFComponent targetComponent = application.getTargetComponent();
			if (targetComponent == null) {
				if (dialog != null) {
					dialog.dispose();
				}
				MessageBox.post("���ڽṹ��������ѡ��Ҫ��ǱȽϵĶ���BOMLine", "��ʾ",MessageBox.INFORMATION);
				return;
			}
			if (targetComponent instanceof TCComponentBOMLine) {
				TCComponentBOMLine tcComponentBOMLine = (TCComponentBOMLine) targetComponent;
				List<TCComponentItem> allItems = getAllItems(tcComponentBOMLine);
				outInfo(allItems);
				
				if (dialog != null) {
					dialog.dispose();
				}
				MessageBox.post("BOM�汾��ͬ�Ƚ����,�ֶ���·�� "+createFile().getAbsolutePath(), "��ʾ", 2);
				Runtime.getRuntime().exec("C:/WINDOWS/system32/notepad.exe " + createFile().getAbsoluteFile());
			}
			
		}catch(TCException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/**
	 * ���ݶ���BomLine�ݹ����е�BOMLine
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
	 * ͨ��BomLine��ȡItem �����뼯���У�ȥ�ظ�
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
		//ͨ���汾��ϵ��ȡItem�µ����а汾 "revision_list" 
		TCComponent[] relatedComponents = item.getRelatedComponents("revision_list");
		int length = relatedComponents.length;
		//Item�°汾��������1ʱ �жϰ汾�Ƿ���BOM ����оͽ��бȽ�
		if (relatedComponents != null && length > 1) {
			compareResult(relatedComponents[length-1], relatedComponents[length-2], fileWriter);
		}
	}
	/**
	 * �Ƚ϶�汾��BOM����
	 * @param last
	 * @param before
	 * @param fileWriter
	 * @throws Exception
	 */
	private void compareResult(TCComponent last, TCComponent before, FileWriter fileWriter) throws Exception {
		TCComponentItemRevision lastRevision = (TCComponentItemRevision) last;
		TCComponentItemRevision beforeRevision = (TCComponentItemRevision) before;
		//ͨ���԰汾��������BOM
		TCComponentBOMLine lastBomLine = constructBomLine(lastRevision);
		TCComponentBOMLine beforeBomLine = constructBomLine(beforeRevision);
		//��ȡ����BOM���ӣ�ֻ��һ�㼴��
		AIFComponentContext[] lastChildren = lastBomLine.getChildren();
		AIFComponentContext[] beforeChildren = beforeBomLine.getChildren();
		List<AIFComponentContext> flags = new ArrayList<AIFComponentContext>();
		if (lastChildren == null || lastChildren.length<=0 || beforeChildren == null || beforeChildren.length <= 0) {
			return;
		}
		dialog.setInfo("�Ƚ�Item��"+last.getProperty("item_id")+"�İ汾��"+last.toString()+", "+before.toString()+" BOM ����");
		//�Ƚ���ͬItem��ͬ�İ汾�µ�BOM���ȣ��������ͬ��˵�������汾��BOM�в���ͬ����¼
		if (lastChildren.length != beforeChildren.length) {
			//System.out.println("�������������汾��BOMLine��������ͬ������������");
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
		//��BOM����ͬʱ����Ƚ�ÿһ�����Ƿ�һ����Ӧ������һһ��Ӧ��˵�����ɲ���ͬ��һһ��Ӧ��Ƚ�����
		if (flags.size() != lastChildren.length) {
			//System.out.println("�������������汾��BOMLine������ͬ�����ɲ�ͬ������������");
			fileWriter.write("ItemRevision's has same BomLine Quantity different content :\t" 
					+ lastBomLine.getItem().getProperty("object_string")+"\r\n");
		}else {
			//BOMLine������ͬ����һһ��Ӧ ��Ƚ�����
			out:
				for (int i = 0; i < beforeChildren.length; i++) {
					for (int j = 0; j < lastChildren.length; j++) {
						if (beforeChildren[i].getComponent().getProperty("item_id").equals(lastChildren[j].getComponent().getProperty("item_id"))) {

							if (!beforeChildren[i].getComponent().getProperty("bl_quantity").equals(lastChildren[j].getComponent().getProperty("bl_quantity"))) {
								//System.out.println("�������������汾��BOMLine������ͬ��������ͬ�������������ͬ������������");
								fileWriter.write("ItemRevision's has different BomLine part number :\t\t"+lastBomLine.getItem().getProperty("object_string")+"\r\n");
							}
							break out;
						}
					}
				}
		dialog.setInfo("��ɱȽϣ�����д��txt�У�д����ɽ��Զ���...");
		}
	}
	/**
	 * ������Ϣ������ļ�
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
	 * ͨ����������Item�����ڷ�PSE�ṹ�������Ļ����У�����һ��BOM�ṹ.
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
