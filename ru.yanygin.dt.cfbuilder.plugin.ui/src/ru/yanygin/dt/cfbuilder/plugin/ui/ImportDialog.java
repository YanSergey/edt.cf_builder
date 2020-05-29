package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.util.HashMap;

import org.eclipse.core.resources.ResourcesPlugin;
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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.wb.swt.SWTResourceManager;

public class ImportDialog extends Dialog {
	private Text projectNameTextBox;
	private Text cfPathTextBox;
	private Button buttonOK;
	private String projectName;
	private boolean projectIsExists;
	private java.util.List<Version> supportedVersion;
	private Version projectVersion;
	private Shell parentShell;
	private HashMap<String, String> cfNameInfo;
	
	private Label projectNameExistsLabel;
	
	public void setInitialProperties(String projectName, java.util.List<Version> supportedVersion) {
		this.projectName = projectName;
		this.supportedVersion = supportedVersion;
	}
	
	public Version getProjectSelectedVersion() {
		//return Version.V8_3_14;
		return projectVersion;
	}
	
	public HashMap<String, String> getProjectSelectedCfNameInfo() {
		return cfNameInfo;
	}
	
	public String getNewProjectName() {
		return projectName;
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
		
		Composite composite = new Composite(container, SWT.NONE);
		composite.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
		composite.setBounds(0, 0, 444, 64);
		
		Label lblNewLabel = new Label(composite, SWT.NONE);
		lblNewLabel.setFont(SWTResourceManager.getFont("Segoe UI", 12, SWT.BOLD));
		lblNewLabel.setLocation(10, 10);
		lblNewLabel.setSize(424, 28);
		lblNewLabel.setTouchEnabled(true);
		lblNewLabel.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
		lblNewLabel.setText(Messages.Dialog_ImportProjectFromCf);
		
		Label lblCf = new Label(composite, SWT.NONE);
		lblCf.setTouchEnabled(true);
		lblCf.setText(Messages.Actions_Set_CF_Location);
		lblCf.setFont(SWTResourceManager.getFont("Segoe UI", 10, SWT.NORMAL));
		lblCf.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
		lblCf.setBounds(20, 34, 402, 17);
		
		Label projectNameLabel = new Label(container, SWT.NONE);
		projectNameLabel.setBounds(10, 79, 73, 15);
		projectNameLabel.setText(Messages.Dialog_ProjectName);
		
		projectNameTextBox = new Text(container, SWT.BORDER);
		projectNameTextBox.setToolTipText("");
		projectNameTextBox.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				projectName = projectNameTextBox.getText().trim();
				
				if (projectName.isEmpty()) {
					projectIsExists = false;
				} else {
					projectIsExists = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName).exists();
				}
				projectNameExistsLabel.setVisible(projectIsExists);
				setButtonOKEnabled();
			}
		});
		projectNameTextBox.setBounds(89, 76, 260, 23);
		
		projectNameExistsLabel = new Label(container, SWT.NONE);
		projectNameExistsLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
		projectNameExistsLabel.setBounds(355, 79, 69, 15);
		projectNameExistsLabel.setText(Messages.Dialog_ProjectExists);
		projectNameExistsLabel.setVisible(false);
		
		Label projectVersionLabel = new Label(container, SWT.NONE);
		projectVersionLabel.setBounds(10, 109, 73, 15);
		projectVersionLabel.setText(Messages.Dialog_V8Version);
		
		Combo projectVersionList = new Combo(container, SWT.READ_ONLY);
		projectVersionList.setBounds(89, 106, 260, 23);
		
		supportedVersion.forEach(version -> {
			projectVersionList.add(version.toString());
			projectVersionList.setData(version.toString(), version);
		});

		projectVersionList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				projectVersion = (Version)projectVersionList.getData(projectVersionList.getText());
				setButtonOKEnabled();
			}
		});

		Label cfPathLabel = new Label(container, SWT.NONE);
		cfPathLabel.setBounds(10, 140, 73, 15);
		cfPathLabel.setText(Messages.Dialog_CfPath);
		
		cfPathTextBox = new Text(container, SWT.BORDER);
		cfPathTextBox.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (cfPathTextBox.getText().isEmpty()) {
					cfNameInfo = null;
				}
				setButtonOKEnabled();
			}
		});
		cfPathTextBox.setBounds(89, 137, 260, 23);
		
		Button selectCfButton = new Button(container, SWT.NONE);
		selectCfButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cfNameInfo = Actions.askCfLocationPath(parentShell, SWT.OPEN);
				if (cfNameInfo == null) {
					cfPathTextBox.clearSelection();
				} else {
					cfPathTextBox.setText(cfNameInfo.get("cfFullName"));
				}
				//setButtonOKEnabled();
			}
		});
		selectCfButton.setBounds(356, 136, 68, 23);
		selectCfButton.setText(Messages.Dialog_View);

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		buttonOK = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
		
		setButtonOKEnabled();
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(450, 250);
	}

	private void setButtonOKEnabled() {
		boolean enabled = !projectName.isEmpty() & !projectIsExists & projectVersion != null & cfNameInfo != null;
		buttonOK.setEnabled(enabled);
	}
}
