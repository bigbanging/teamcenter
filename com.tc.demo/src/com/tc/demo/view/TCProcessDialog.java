package com.tc.demo.view;

import java.awt.BorderLayout;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;

import com.tc.demo.util.TCUtils;

public class TCProcessDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	JLabel info;
	/**
	 * 创建信息框，实时输出当前程序运行情况
	 */
	public TCProcessDialog() {
		setBounds(100, 100, 500, 160);
		setAlwaysOnTop(true);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setVisible(true);
		ImageIcon iconTitle = new ImageIcon(TCUtils.class.getResource("icon.png"));
		setIconImage(iconTitle.getImage()); //设置标题图标
		getContentPane().setLayout(new BorderLayout());
		JPanel panel = new JPanel();
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(new BorderLayout());
		ImageIcon iconInfo = new ImageIcon(TCUtils.class.getResource("info.png"));
		info = new JLabel(iconInfo);
		info.setHorizontalAlignment(SwingConstants.LEFT);
		panel.add(info,BorderLayout.NORTH);
	}
	/**
	 * 设置对话框提示信息
	 * @param msg
	 */
	public void setInfo(String msg) {
		info.setText(msg);
	}

}
