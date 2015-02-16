package com.wildex999.utils;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class FileChooser implements Runnable {
	public static final int returnCanceled = JFileChooser.CANCEL_OPTION;
	public static final int returnOk = JFileChooser.APPROVE_OPTION;
	
	private AtomicBoolean fileChoosen;
	private String filePath;
	private int returnVal;
	
	private boolean save;
	private String dialogTitle;
	private String startDir;
	private FileNameExtensionFilter fileFilter;
	
	public FileChooser() {
		fileChoosen = new AtomicBoolean(false);
	}
	
	public boolean isFileChoosen() {
		return fileChoosen.get();
	}
	
	public int getReturnValue() {
		if(!isFileChoosen())
			return JFileChooser.ERROR_OPTION;
		
		return returnVal;
	}
	
	public String getFileChoosen() {
		if(!isFileChoosen())
			return null;
		
		return filePath;
	}
	
	//Open File Chooser in another thread
	public static FileChooser getFile(boolean save) {
		return getFile(save, "", "", null);
	}
	
	//Open File Chooser in another thread
	public static FileChooser getFile(boolean save, String title, String startPath, FileNameExtensionFilter filter) {
		FileChooser chooser = new FileChooser();
		chooser.save = save;
		chooser.dialogTitle = title;
		chooser.startDir = startPath;
		chooser.fileFilter = filter;
		(new Thread(chooser)).start();
		
		return chooser;
	}

	@Override
	public void run() {
		//Note, this is not thread safe, throw the dice
		//TODO: Find a thread-safe way to do this
		JFileChooser chooser;
		
		if(startDir.length() > 0)
			chooser = new JFileChooser(startDir);
		else
			chooser = new JFileChooser();
		
		if(dialogTitle.length() > 0)
			chooser.setDialogTitle(dialogTitle);
		if(fileFilter != null)
			chooser.setFileFilter(fileFilter);
		
		if(save)
			returnVal = chooser.showSaveDialog(null);
		else
			returnVal = chooser.showOpenDialog(null);
		
		if(returnVal == JFileChooser.APPROVE_OPTION)
			filePath = chooser.getSelectedFile().getAbsolutePath();
		else
			filePath = "";
		
		fileChoosen.set(true);
	}
}
