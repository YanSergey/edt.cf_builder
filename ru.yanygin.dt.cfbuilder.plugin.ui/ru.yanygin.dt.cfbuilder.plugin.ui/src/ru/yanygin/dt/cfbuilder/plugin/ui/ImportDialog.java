package ru.yanygin.dt.cfbuilder.plugin.ui;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com._1c.g5.v8.dt.platform.version.Version;

import ru.yanygin.dt.cfbuilder.plugin.ui.ImportJob.SupportMode;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;

public class ImportDialog extends Dialog {
	private Text projectNameTextBox;
	private Text cfPathTextBox;
	private Button buttonOK;
	private String projectName;
	private boolean projectIsExists;
	private java.util.List<Version> supportedVersion;
	private Version projectVersion;
	private Shell parentShell;
	private CfFileInfo cfFileInfo;
	private SupportMode supportMode = SupportMode.DEFAULT;

	private Label projectNameExistsLabel;
	private Combo supportModeList;

	public void setInitialProperties(String projectName, java.util.List<Version> supportedVersion) {
		this.projectName = projectName;
		this.supportedVersion = supportedVersion;
	}

	public Version getProjectSelectedVersion() {
		return projectVersion;
	}

	public CfFileInfo getProjectSelectedCfNameInfo() {
		return cfFileInfo;
	}

	public String getNewProjectName() {
		return projectName;
	}

	public SupportMode getSupportMode() {
		return supportMode;
	}

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public ImportDialog(Shell parentShell) {
		super(parentShell);
		this.parentShell = parentShell;
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(null);

		Composite composite = new Composite(container, SWT.NONE);
		composite.setBackground(getColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
		composite.setBounds(0, 0, 474, 81);

		Label lblNewLabel = new Label(composite, SWT.NONE);
		lblNewLabel.setFont(getFont("Segoe UI", 12, SWT.BOLD));
		lblNewLabel.setLocation(10, 10);
		lblNewLabel.setSize(424, 28);
		lblNewLabel.setTouchEnabled(true);
		lblNewLabel.setBackground(getColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
		lblNewLabel.setText(Messages.Dialog_ImportProjectFromCf);

		Label lblCf = new Label(composite, SWT.NONE);
		lblCf.setTouchEnabled(true);
		lblCf.setText(Messages.Dialog_SetNewProjectProperties);
		lblCf.setFont(getFont("Segoe UI", 10, SWT.NORMAL));
		lblCf.setBackground(getColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
		lblCf.setBounds(20, 34, 262, 17);

		projectNameExistsLabel = new Label(composite, SWT.NONE);
		projectNameExistsLabel.setBounds(30, 57, 262, 15);
		projectNameExistsLabel.setForeground(getColor(SWT.COLOR_RED));
		projectNameExistsLabel.setText(Messages.Dialog_ProjectExists);
		projectNameExistsLabel.setVisible(false);
		projectNameExistsLabel.setBackground(getColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));

		Label projectNameLabel = new Label(container, SWT.NONE);
		projectNameLabel.setBounds(10, 99, 73, 15);
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
		projectNameTextBox.setBounds(89, 96, 360, 23);

		Label projectVersionLabel = new Label(container, SWT.NONE);
		projectVersionLabel.setBounds(10, 129, 73, 15);
		projectVersionLabel.setText(Messages.Dialog_V8Version);

		Combo projectVersionList = new Combo(container, SWT.READ_ONLY);
		projectVersionList.setBounds(89, 126, 58, 23);

		supportedVersion.forEach(version -> {
			projectVersionList.add(version.toString());
			projectVersionList.setData(version.toString(), version);
		});

		projectVersionList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				projectVersion = (Version) projectVersionList.getData(projectVersionList.getText());
				setButtonOKEnabled();
			}
		});
		
		Label supportModeLabel = new Label(container, SWT.NONE);
		supportModeLabel.setBounds(153, 129, 128, 15);
		supportModeLabel.setText(Messages.Dialog_SupportlabelText);
		
		supportModeList = new Combo(container, SWT.READ_ONLY);
		supportModeList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				supportMode = (SupportMode) supportModeList.getData(supportModeList.getText());
			}
		});
		supportModeList.setBounds(287, 125, 162, 23);
		
		supportModeList.add(Messages.Dialog_SupportModeDefault);
		supportModeList.setData(Messages.Dialog_SupportModeDefault, SupportMode.DEFAULT);
		supportModeList.add(Messages.Dialog_SupportModeDisable);
		supportModeList.setData(Messages.Dialog_SupportModeDisable, SupportMode.DISABLESUPPORT);
		supportModeList.select(0);

		Label cfPathLabel = new Label(container, SWT.NONE);
		cfPathLabel.setBounds(10, 160, 73, 15);
		cfPathLabel.setText(Messages.Dialog_CfPath);

		cfPathTextBox = new Text(container, SWT.BORDER);
		cfPathTextBox.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (cfPathTextBox.getText().isEmpty()) {
					cfFileInfo = null;
				}
				setButtonOKEnabled();
			}
		});
		cfPathTextBox.setBounds(89, 157, 286, 23);

		Button selectCfButton = new Button(container, SWT.NONE);
		selectCfButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				cfFileInfo = Actions.askCfLocationPath(parentShell, SWT.OPEN);
				if (cfFileInfo == null) {
					cfPathTextBox.clearSelection();
				} else {
					cfPathTextBox.setText(cfFileInfo.FULLNAME);
					projectNameTextBox.setText(cfFileInfo.NAMEWITHOUTEXTENSION);
				}
			}
		});
		selectCfButton.setBounds(381, 156, 68, 24);
		selectCfButton.setText(Messages.Dialog_View);

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * 
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
		return new Point(480, 270);
	}

	private void setButtonOKEnabled() {
		boolean enabled = !projectName.isEmpty() & !projectIsExists & projectVersion != null & cfFileInfo != null;
		buttonOK.setEnabled(enabled);
	}

	public static Color getColor(int systemColorID) {
		return Display.getCurrent().getSystemColor(systemColorID);
	}

	public static Font getFont(String name, int size, int style) {
		FontData fontData = new FontData(name, size, style);
		return new Font(Display.getCurrent(), fontData);
	}
}
