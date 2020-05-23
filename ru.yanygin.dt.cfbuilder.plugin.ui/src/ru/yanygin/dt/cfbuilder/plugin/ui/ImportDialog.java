package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.util.HashMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com._1c.g5.v8.dt.platform.version.Version;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.List;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class ImportDialog extends Dialog {
	private String projectName;
	private Text projectNameText;
	private Text cfPathText;
	private java.util.List<Version> supportedVersion;
	private Shell parentShell;
	private HashMap<String, String> cfNameInfo;
	
	public void setInitialProperties(String projectName, java.util.List<Version> supportedVersion) {
		this.projectName = projectName;
		this.supportedVersion = supportedVersion;
	}
	
	public Version getProjectSelectedVersion() {
		return Version.V8_3_14;
	}
	
	public HashMap<String, String> getProjectSelectedcfNameInfo() {
		return cfNameInfo;
	}

	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public ImportDialog(Shell parentShell) {
		super(parentShell);
		this.parentShell = parentShell;
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(null);
		
		Label projectNameLabel = new Label(container, SWT.NONE);
		projectNameLabel.setBounds(10, 26, 73, 15);
		projectNameLabel.setText("Project name:");
		
		projectNameText = new Text(container, SWT.BORDER);
		projectNameText.setBounds(89, 23, 271, 21);
		projectNameText.setText(projectName);
		
		Label projectVersionLabel = new Label(container, SWT.NONE);
		projectVersionLabel.setBounds(10, 56, 73, 15);
		projectVersionLabel.setText("v8 version:");
		
		List projectVersionList = new List(container, SWT.BORDER);
		projectVersionList.setBounds(89, 52, 271, 22);
		
		Label cfPathLabel = new Label(container, SWT.NONE);
		cfPathLabel.setBounds(10, 87, 73, 15);
		cfPathLabel.setText("cf path:");
		
		cfPathText = new Text(container, SWT.BORDER);
		cfPathText.setBounds(89, 84, 271, 21);
		
		Button selectCfButton = new Button(container, SWT.NONE);
		selectCfButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				cfNameInfo = Actions.askCfLocationPath(parentShell, SWT.OPEN);
				if (cfNameInfo.get("cfFullName") != null) {
//					Activator.log(Activator.createErrorStatus(Messages.CfBuild_Set_CF_Error));
//				} else {
					cfPathText.setText(cfNameInfo.get("cfFullName"));
				}

			}
		});
		selectCfButton.setBounds(366, 82, 68, 25);
		selectCfButton.setText("Выбрать...");

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 197);
	}
}
