package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobFunction;
import org.eclipse.core.runtime.jobs.Job;
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
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.wizards.IWizardDescriptor;
import com._1c.g5.v8.dt.export.IExportServiceRegistry;
import com._1c.g5.v8.dt.platform.services.core.infobases.IInfobaseManager;
import com._1c.g5.v8.dt.platform.services.core.infobases.InfobaseReferences;
import com._1c.g5.v8.dt.platform.services.model.InfobaseReference;
import com._1c.g5.v8.dt.platform.services.ui.infobases.IInfobaseWizard;
import com._1c.g5.v8.dt.platform.services.ui.infobases.InfobaseSelectDialog;
import com._1c.g5.v8.dt.platform.version.Version;
import com.google.common.base.Strings;
import com.google.common.io.Files;

import ru.yanygin.dt.cfbuilder.plugin.ui.ImportProjectWorker.SupportChangeMode;
import ru.yanygin.dt.cfbuilder.plugin.ui.BaseProjectWorker.ProjectType;

import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.wb.swt.ResourceManager;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.DisposeEvent;

public class JobDialog extends Dialog {
	
	public enum DialogType {
		IMPORT, EXPORT
	}
	
	private DialogType dialogType = DialogType.EXPORT;
	
	private Shell parentShell;
	
	private Label projectExistsInfoLabel;
	private Combo comboProjectsList;
	private Text txtFilePath;
	private Button buttonOK;
	private Button btnUseTempIB;
	private Button btnCreateDistributionCffile;
	private Composite compositeInfoBases;
	private Combo comboInfoBasesList;
	private Button btnShowOnlyAssociatedIB;
	private Button btnAssociateAfterDeploy;
	private Combo supportModeList;
	private Combo runtimeV8VersionList;
	private String projectName;
	private IProject projectRef = null;
	
	private boolean deployInTempIB = true;
	private InfobaseReference selectedInfobase = null;
	private boolean loadFullConfiguration = false;
	private boolean associateAfterDeploy = false;
	private boolean createDistributionFile = false;
	private String selectedFileName;
	private String taskName;
	
	private IExportServiceRegistry exportServiceRegistry;
	
	private Version projectVersion;
	private SupportChangeMode supportChangeMode = SupportChangeMode.DEFAULT;
	
	public JobDialog(Shell parentShell, DialogType dialogType, IProject project,
			IExportServiceRegistry exportServiceRegistry) throws Exception {
		
		super(parentShell);
		if (dialogType != DialogType.EXPORT)
			throw new Exception(Messages.Status_IncorrectDialogType);
		
		this.parentShell = parentShell;
		this.dialogType = dialogType;
		this.projectRef = project;
		this.exportServiceRegistry = exportServiceRegistry;
		
		this.taskName = Messages.Task_BuildCfFromProject;
	}
	
	/**
	 * @wbp.parser.constructor
	 */
	public JobDialog(Shell parentShell, DialogType dialogType, String projectName) throws Exception {
		
		super(parentShell);
		if (dialogType != DialogType.IMPORT)
			throw new Exception(Messages.Status_IncorrectDialogType);
		
		this.parentShell = parentShell;
		this.dialogType = dialogType;
		this.projectName = projectName;
		if (!projectName.isBlank()) {
			projectRef = BaseProjectWorker.getProjectReferenceFromWorkspace(projectName);
		}
		
		this.taskName = Messages.Task_ImportProjectFromCf;
	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent.addDisposeListener(new DisposeListener() { // NOSONAR
			public void widgetDisposed(DisposeEvent e) {
				deployInTempIB = btnUseTempIB.getSelection();
				associateAfterDeploy = btnAssociateAfterDeploy.getSelection();
			}
		});
		
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(null);
		
		addHeaderContainer(container);
		addProjectsControl(container);
		addFileChoiceControl(container);
		addButtonUseTempIB(container);
		addInfobasesPropertiesContainer(container);
		addAdditionalProperties(container);
		
		fillProjectsList();
		selectCheckboxUseTempIB();
		selectCheckboxesAssociatedIB();
		
		addEventHandlers();
		
		return container;
	}
	
	private void addEventHandlers() {
		
		comboProjectsList.addModifyListener(new ModifyListener() { // NOSONAR
			public void modifyText(ModifyEvent e) {
				
				selectProjectFromProjectList();
				changeSelectedFileNameExtension();
				setV8RuntimeCurrentVersion();
				
				selectCheckboxUseTempIB();
				selectCheckboxesAssociatedIB();
				setEnabledCheckboxCreateDistributionCffile();
				
				fillInfobasesList();
				
				setButtonOKEnabled();
			}
			
		});
		
		txtFilePath.addModifyListener(new ModifyListener() { // NOSONAR
			public void modifyText(ModifyEvent e) {
				if (txtFilePath.getText().isEmpty()) {
					selectedFileName = null;
				} else {
					selectedFileName = txtFilePath.getText().trim();
					generateProjectNameFromSelectedFilename();
				}
				setButtonOKEnabled();
			}
		});
		
		btnUseTempIB.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				enableInfobaseSubControl();
				
				if (btnUseTempIB.getSelection()) {
					selectedInfobase = null;
				} else if (dialogType == DialogType.IMPORT && projectName.isBlank()) {
					btnShowOnlyAssociatedIB.setEnabled(true);
					btnShowOnlyAssociatedIB.setSelection(false);
					btnAssociateAfterDeploy.setEnabled(false);
					btnAssociateAfterDeploy.setSelection(false);
				}
				
				fillInfobasesList();
				setButtonOKEnabled();
			}
		});
		
		comboInfoBasesList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectInfobaseFromInfobaseList();
				enableAndSelectAssociateCheckboxesAfterSelectInfobase();
				setButtonOKEnabled();
			}
		});
		
		btnShowOnlyAssociatedIB.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				btnAssociateAfterDeploy.setEnabled(false);
				btnAssociateAfterDeploy.setSelection(false);
				fillInfobasesList();
			}
		});
		
	}
	
	private void addHeaderContainer(Composite container) {
		Composite composite = new Composite(container, SWT.NONE);
		composite.setBackground(getColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
		composite.setBounds(0, 0, 474, 81);
		
		Label lblHeader = new Label(composite, SWT.NONE);
		lblHeader.setFont(getFont("Segoe UI", 12, SWT.BOLD));
		lblHeader.setLocation(10, 10);
		lblHeader.setSize(424, 28);
		lblHeader.setTouchEnabled(true);
		lblHeader.setBackground(getColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
		
		Label lblSubHeader = new Label(composite, SWT.NONE);
		lblSubHeader.setTouchEnabled(true);
		lblSubHeader.setFont(getFont("Segoe UI", 10, SWT.NORMAL));
		lblSubHeader.setBackground(getColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
		lblSubHeader.setBounds(30, 34, 262, 17);
		
		projectExistsInfoLabel = new Label(composite, SWT.NONE);
		projectExistsInfoLabel.setBounds(30, 57, 415, 15);
		projectExistsInfoLabel.setForeground(getColor(SWT.COLOR_RED));
		projectExistsInfoLabel.setText(Messages.Dialog_ProjectExists);
		projectExistsInfoLabel.setVisible(false);
		projectExistsInfoLabel.setBackground(getColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));
		
		lblHeader.setText(Messages.Dialog_ExportProjectToCf);
		lblSubHeader.setText(Messages.Dialog_SetExportProperties);
		if (dialogType == DialogType.IMPORT) {
			lblHeader.setText(Messages.Dialog_ImportProjectFromCf);
			lblSubHeader.setText(Messages.Dialog_SetNewProjectProperties);
		} else if (dialogType == DialogType.EXPORT) {
			lblHeader.setText(Messages.Dialog_ExportProjectToCf);
			lblSubHeader.setText(Messages.Dialog_SetExportProperties);
		}
		
	}
	
	private void addProjectsControl(Composite container) {
		Label lblProject = new Label(container, SWT.NONE);
		lblProject.setBounds(10, 99, 73, 15);
		
		int style = SWT.READ_ONLY;
		if (dialogType == DialogType.IMPORT) {
			style = SWT.NONE;
		}
		
		comboProjectsList = new Combo(container, style);
		comboProjectsList.setBounds(89, 96, 360, 23);
		if (dialogType == DialogType.IMPORT) {
			comboProjectsList.setText(projectName);
		}
		
		lblProject.setText(Messages.Dialog_ProjectReference);
		if (dialogType == DialogType.IMPORT) {
			lblProject.setText(Messages.Dialog_ProjectReference);
		} else if (dialogType == DialogType.EXPORT) {
			lblProject.setText(Messages.Dialog_ProjectName);
		}
		
	}
	
	private void addFileChoiceControl(Composite container) {
		Label lblFilePath = new Label(container, SWT.NONE);
		lblFilePath.setBounds(10, 129, 73, 15);
		lblFilePath.setText(Messages.Dialog_CfPath);
		
		txtFilePath = new Text(container, SWT.BORDER);
		txtFilePath.setBounds(89, 126, 286, 23);
		
		Button btnSelectCf = new Button(container, SWT.NONE);
		btnSelectCf.setBounds(381, 125, 68, 24);
		btnSelectCf.setText(Messages.Dialog_View);
		btnSelectCf.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				String fileName = BaseProjectWorker.askCfLocationPath(parentShell, dialogType, projectRef);
				if (fileName != null) {
					txtFilePath.setText(fileName);
				}
			}
		});
	}
	
	private void addButtonUseTempIB(Composite container) {
		btnUseTempIB = new Button(container, SWT.CHECK);
		btnUseTempIB.setSelection(true);
		btnUseTempIB.setBounds(10, 155, 235, 16);
		btnUseTempIB.setText(Messages.Dialog_UseTempIB);
		
//		if (dialogType == DialogType.EXPORT) { //NOSONAR
		btnCreateDistributionCffile = new Button(container, SWT.CHECK);
		btnCreateDistributionCffile.setBounds(251, 155, 198, 16);
		btnCreateDistributionCffile.setText(Messages.Dialog_CreateDistributionCffile);
		btnCreateDistributionCffile.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				createDistributionFile = btnCreateDistributionCffile.getSelection();
			}
		});
		setEnabledCheckboxCreateDistributionCffile();
//		} //NOSONAR
		if (dialogType == DialogType.IMPORT) {
			btnCreateDistributionCffile.setVisible(false);
		}
	}
	
	private void addInfobasesPropertiesContainer(Composite container) {
		compositeInfoBases = new Composite(container, SWT.NONE);
		compositeInfoBases.setBounds(10, 177, 439, 89);
		
		Label lblInfoBase = new Label(compositeInfoBases, SWT.NONE);
		lblInfoBase.setAlignment(SWT.RIGHT);
		lblInfoBase.setBounds(10, 5, 61, 15);
		lblInfoBase.setText(Messages.Dialog_InfoBase);
		
		comboInfoBasesList = new Combo(compositeInfoBases, SWT.READ_ONLY);
		comboInfoBasesList.setBounds(79, 0, 285, 23);
		
		Button btnSelectInfobase = new Button(compositeInfoBases, SWT.NONE);
		btnSelectInfobase.setBounds(370, 0, 26, 25);
		btnSelectInfobase.setText(Messages.Dialog_SelectInfobase);
		btnSelectInfobase.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				InfobaseReference ib = showInfobaseSelectDialog(parentShell);
				
				if (ib != null) {
					selectedInfobase = ib;
					enableAndSelectAssociateCheckboxesAfterSelectInfobase();
					fillInfobasesList();
					setButtonOKEnabled();
				}
				
			}
		});
		
		Button btnAddInfobase = new Button(compositeInfoBases, SWT.NONE);
		btnAddInfobase.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				IWizardDescriptor wizardDescriptor = PlatformUI.getWorkbench().getNewWizardRegistry()
						.findWizard("com._1c.g5.v8.dt.platform.services.ui.InfobaseWizard");
				
				try {
					IInfobaseWizard wizard = (IInfobaseWizard) wizardDescriptor.createWizard();
					WizardDialog dialog = new WizardDialog(parentShell, wizard);
					
					if (dialog.open() == 0) {
						
						InfobaseReference created = (InfobaseReference) wizard.getResultSection();
						if (created != null) {
							selectedInfobase = created;
							btnShowOnlyAssociatedIB.setSelection(false);
							btnAssociateAfterDeploy.setEnabled(true);
							btnAssociateAfterDeploy.setSelection(true);
							fillInfobasesList();
							comboInfoBasesList.setText(selectedInfobase.getName());
						}
						
					}
				} catch (CoreException ex) {
					String errorMessage = MessageFormat.format(Messages.Status_Error, ex.getLocalizedMessage());
					Activator.log(Activator.createErrorStatus(errorMessage, ex));
					Messages.showPostBuildMessage(parentShell, taskName, errorMessage);
				}
				
				setButtonOKEnabled();
			}
		});
		btnAddInfobase.setImage(ResourceManager.getPluginImage("com._1c.g5.v8.dt.platform.services.ui",
				"/icons/etool16/infobase_new.png"));
		btnAddInfobase.setBounds(402, 0, 26, 25);
		
		btnShowOnlyAssociatedIB = new Button(compositeInfoBases, SWT.CHECK);
		btnShowOnlyAssociatedIB.setBounds(79, 26, 285, 16);
		btnShowOnlyAssociatedIB.setSelection(true);
		btnShowOnlyAssociatedIB.setText(Messages.Dialog_ShowOnlyAssociatedIB);
		
		btnAssociateAfterDeploy = new Button(compositeInfoBases, SWT.CHECK);
		btnAssociateAfterDeploy.setBounds(79, 48, 285, 16);
		btnAssociateAfterDeploy.setText(Messages.Dialog_AssociateAfterDeploy);
		btnAssociateAfterDeploy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				associateAfterDeploy = btnAssociateAfterDeploy.getSelection();
			}
		});
		
//		if (dialogType == DialogType.EXPORT) { //NOSONAR
		Button btnLoadFullConfiguration = new Button(compositeInfoBases, SWT.CHECK);
		btnLoadFullConfiguration.setBounds(10, 70, 429, 16);
		btnLoadFullConfiguration.setText(Messages.Dialog_LoadFullConfiguration);
		btnLoadFullConfiguration.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				loadFullConfiguration = btnLoadFullConfiguration.getSelection();
			}
		});
//		} //NOSONAR
		if (dialogType == DialogType.IMPORT) {
			btnLoadFullConfiguration.setVisible(false);
		}
		
	}
	
	private void addAdditionalProperties(Composite container) {
		
//		if (dialogType == DialogType.IMPORT) { //NOSONAR
		
		Label projectVersionLabel = new Label(container, SWT.NONE);
		projectVersionLabel.setAlignment(SWT.RIGHT);
		projectVersionLabel.setBounds(10, 270, 73, 15);
		projectVersionLabel.setText(Messages.Dialog_V8Version);
		
		runtimeV8VersionList = new Combo(container, SWT.READ_ONLY);
		runtimeV8VersionList.setBounds(89, 267, 58, 23);
		
		List<Version> supportedVersion = BaseProjectWorker.getRuntimeV8SupportedVersions();
		
		supportedVersion.forEach(version -> {
			runtimeV8VersionList.add(version.toString());
			runtimeV8VersionList.setData(version.toString(), version);
		});
		setV8RuntimeCurrentVersion();
		
		runtimeV8VersionList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setSelectedProjectVersionFromV8VersionList();
				setButtonOKEnabled();
			}
		});
		
		Label supportModeLabel = new Label(container, SWT.NONE);
		supportModeLabel.setAlignment(SWT.RIGHT);
		supportModeLabel.setBounds(153, 270, 128, 15);
		supportModeLabel.setText(Messages.Dialog_SupportlabelText);
		
		supportModeList = new Combo(container, SWT.READ_ONLY);
		supportModeList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				supportChangeMode = (SupportChangeMode) supportModeList.getData(supportModeList.getText());
			}
		});
		supportModeList.setBounds(287, 267, 162, 23);
		
		supportModeList.add(Messages.Dialog_SupportModeDefault);
		supportModeList.setData(Messages.Dialog_SupportModeDefault, SupportChangeMode.DEFAULT);
		supportModeList.add(Messages.Dialog_SupportModeDisable);
		supportModeList.setData(Messages.Dialog_SupportModeDisable, SupportChangeMode.DISABLESUPPORT);
		supportModeList.select(0);
//		} //NOSONAR
		if (dialogType == DialogType.EXPORT) {
			supportModeLabel.setVisible(false);
			supportModeList.setVisible(false);
			
		}
		
	}

	private void selectProjectFromProjectList() {
		
		if (dialogType == DialogType.IMPORT) {
			
			projectName = comboProjectsList.getText().trim();
			if (projectName.isBlank()) {
				projectRef = null;
			} else {
				projectRef = BaseProjectWorker.getProjectReferenceFromWorkspace(projectName);
				
			}
			setVisibleProjectExistsInfoLabel();
			
		} else if (dialogType == DialogType.EXPORT) {
			projectRef = (IProject) comboProjectsList.getData(comboProjectsList.getText());
		}
		
	}
	
	private void setVisibleProjectExistsInfoLabel() {
		if (projectRef != null && projectRef.exists()) {
			projectExistsInfoLabel.setVisible(true);
			projectExistsInfoLabel.setText(Messages.Dialog_ProjectExists);
			
			if (BaseProjectWorker.projectIsExtension(projectRef)
					&& BaseProjectWorker.checkParentProjectFromExtensionIsNull(projectRef)) {
				projectExistsInfoLabel.setText(Messages.Dialog_ParentProjectNotSet);
			}
		} else {
			projectExistsInfoLabel.setVisible(false);
		}
	}
	
	private void changeSelectedFileNameExtension() {
		
		if (Strings.isNullOrEmpty(selectedFileName))
			return;
		
		switch (dialogType) {
			case IMPORT:
				if (projectName.isBlank())
					break;
				if (!ProjectInfo.checkFileExtensionOfProject(selectedFileName,
						BaseProjectWorker.getProjectTypeFromProject(projectRef))) {
					txtFilePath.setText("");
				}
				
				break;
			case EXPORT:
				String newFileName = ProjectInfo.changeFileExtension(selectedFileName,
						BaseProjectWorker.getProjectTypeFromProject(projectRef));
				txtFilePath.setText(newFileName);
				break;
			default:
				break;
		}
		
	}
	
	private void setV8RuntimeCurrentVersion() {
		
		switch (dialogType) {
			case IMPORT:
				if (projectName.isBlank() || !projectRef.exists()) {
					runtimeV8VersionList.setEnabled(true);
					runtimeV8VersionList.select(runtimeV8VersionList.getItemCount()-1);
					break;
				}
				IProject project = projectRef;
				boolean projectExist = project.exists();
				runtimeV8VersionList.setEnabled(!projectExist);
				if (projectExist) {
					selectVersionInV8VersionListFromProject(project);
				}
				break;
			case EXPORT:
				runtimeV8VersionList.setEnabled(false);
				selectVersionInV8VersionListFromProject(projectRef);
				break;
			default:
				break;
		}
		
		setSelectedProjectVersionFromV8VersionList();
	}
	
	private void selectVersionInV8VersionListFromProject(IProject project) {
		Version projectV8Version = BaseProjectWorker.getV8VersionFromProject(project);
		String[] v8Items = runtimeV8VersionList.getItems();
		for (int i = 0; i < v8Items.length; i++) {
			if (v8Items[i].equals(projectV8Version.toString())) {
				runtimeV8VersionList.select(i);
				break;
			}
		}
	}
	
	private void setSelectedProjectVersionFromV8VersionList() {
		projectVersion = (Version) runtimeV8VersionList.getData(runtimeV8VersionList.getText());
	}
	
	private void setEnabledCheckboxCreateDistributionCffile() {
		
		if (dialogType == DialogType.EXPORT) {
			ProjectType projectType = BaseProjectWorker.getProjectTypeFromProject(projectRef);
			btnCreateDistributionCffile.setEnabled(projectType == ProjectType.CONFIGURATION);
		}
	}
	
	private void fillProjectsList() {
		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			comboProjectsList.add(project.getName());
			comboProjectsList.setData(project.getName(), project);
			
			if (projectRef != null && project == projectRef) {
				comboProjectsList.select(i);
				
				if (dialogType == DialogType.IMPORT)
					setVisibleProjectExistsInfoLabel();
			}
		}
	}
	
	private void fillInfobasesList() {
		comboInfoBasesList.removeAll();
		
		if (btnUseTempIB.getSelection()) {
			return;
		}
		
		IProject project = null;
		if (dialogType == DialogType.IMPORT) {
			if (projectName.isBlank()) {
				return;
			} else {
				project = projectRef;
			}
		} else if (dialogType == DialogType.EXPORT) {
			project = projectRef;
		} else {
			return;
		}
		
		List<InfobaseReference> infobases = BaseProjectWorker.getInfobases(project,
				btnShowOnlyAssociatedIB.getSelection());
		
		for (int i = 0; i < infobases.size(); i++) {
			InfobaseReference ib = infobases.get(i);
			
			comboInfoBasesList.add(ib.getName());
			comboInfoBasesList.setData(ib.getName(), ib);
			
			if (selectedInfobase != null && ib.getName().equals(selectedInfobase.getName())) {
				comboInfoBasesList.select(i);
			}
		}
		if (comboInfoBasesList.getItemCount() == 1 && btnShowOnlyAssociatedIB.getSelection()) {
			comboInfoBasesList.select(0);
		}
		if (comboInfoBasesList.getItemCount() > 1 && btnShowOnlyAssociatedIB.getSelection()
				&& selectedInfobase == null) {
			comboInfoBasesList.select(0);
		}
		
		// если selectedInfobase != null, то можно не вызывать (проверить на всех
		// сценариях)
		selectInfobaseFromInfobaseList();
		
	}
	
	private void selectInfobaseFromInfobaseList() {
		if (comboInfoBasesList.getText().isEmpty()) {
			selectedInfobase = null;
		} else {
			selectedInfobase = (InfobaseReference) comboInfoBasesList.getData(comboInfoBasesList.getText());
		}
	}
	
	public InfobaseReference showInfobaseSelectDialog(Shell parentShell) {
		
		IInfobaseManager infobaseManager = BaseProjectWorker.getInfobaseManager();
		
		InfobaseSelectDialog ibSelectDialog = new InfobaseSelectDialog(parentShell, infobaseManager, selectedInfobase);
		int isSelected = ibSelectDialog.open();
		InfobaseReference ibSelected = null;
		
		if (isSelected == 0)
			ibSelected = ibSelectDialog.getLastSelectedInfobase();
		
		return ibSelected;
	}
	
	private void enableInfobaseSubControl() {
		
		boolean useTempIB = btnUseTempIB.getSelection();
		
		Control[] controls = compositeInfoBases.getChildren();
		for (int j = 0; j < controls.length; j++) {
			controls[j].setEnabled(!useTempIB);
		}
	}
	
	private void generateProjectNameFromSelectedFilename() {
		
		if (dialogType == DialogType.IMPORT && comboProjectsList.getText().isBlank()
				&& !Strings.isNullOrEmpty(selectedFileName)) {
			
			String nameWithoutExtension = Files.getNameWithoutExtension(selectedFileName);
			comboProjectsList.setText(nameWithoutExtension);
		}
	}
	
	/**
	 * Ставит флаг btnUseTempIB в зависимости от выбранного проекта и наличия
	 * привязанных инфобаз
	 */
	private void selectCheckboxUseTempIB() {
		if (dialogType == DialogType.IMPORT) {
			
			if (projectName.isBlank()) {
				projectRef = null;
				
				btnUseTempIB.setEnabled(true);
				btnUseTempIB.setSelection(true);
				enableInfobaseSubControl();
				
				return;
			}
			
			btnUseTempIB.setEnabled(!projectRef.exists());
			btnUseTempIB.setSelection(!projectRef.exists());
			enableInfobaseSubControl();
			
		} else if (dialogType == DialogType.EXPORT) {
			
			List<InfobaseReference> associatedInfobases = BaseProjectWorker.getInfobases(projectRef, true);
			btnUseTempIB.setSelection(associatedInfobases.isEmpty());
			enableInfobaseSubControl();
		}
		
	}
	
	private void selectCheckboxesAssociatedIB() {
		
		if (btnUseTempIB.getSelection()) {
			btnShowOnlyAssociatedIB.setSelection(false);
			btnAssociateAfterDeploy.setSelection(false);
			return;
		}
		
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
//			 Для импорта
//				В существующий проект - только через существующую базу
//										временную использовать нельзя, потому что из нее нельзя импортировать изменения
//					Если выбрали связанную базу - галку "Связать с проектом" снимаем и ставить пользователю не даем
//					Если выбрали несвязанную базу
//						(она привяжется, самый медленный вариант, потому что нужно сначала ее синхронизировать):
//						- галку "Показывать только привязанные ИБ" снимаем
//						- галку "Связать с проектом" ставим и снять пользователю не даем
//			
//				В новый проект - через любую базу (связанных быть не может)
//					По умолчанию через временную
//					Если выбрали существующую базу:
//						- не давать ставить галку "Показывать только привязанные ИБ"
//						- дать ставить галку "Связать с проектом" (ставим ее автоматом)
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
//			 Для экспорта
//				при экспорте мы можем использовать любую базу
//				но через существующую привязанную быстрее
//					поэтому:
//						если есть привязанная база, то нужно автоматически выбрать ее
//						если нет - выбрать временную
//				Через существующую базу - даем ставить галку "Показывать только привязанные ИБ"
//					Если база привязана - не даем ставить галку "Связать с проектом" и снимаем ее
//					Если база непривязана - даем ставить галку "Связать с проектом" и ставим ее автоматом
///////////////////////////////////////////////////////////////////////////////////////////////////////////////
		
		if (dialogType == DialogType.IMPORT) {
			if (projectRef.exists()) {
				
				List<InfobaseReference> associatedInfobases = BaseProjectWorker.getInfobases(projectRef, true);
				
				if (associatedInfobases.isEmpty()) {
					selectedInfobase = null;
					btnShowOnlyAssociatedIB.setSelection(false);
					btnAssociateAfterDeploy.setEnabled(false);
					btnAssociateAfterDeploy.setSelection(true);
				} else {
					btnShowOnlyAssociatedIB.setSelection(true);
					btnAssociateAfterDeploy.setEnabled(false);
					btnAssociateAfterDeploy.setSelection(false);
				}
				
			} else {
				btnShowOnlyAssociatedIB.setEnabled(false);
				btnShowOnlyAssociatedIB.setSelection(false);
				btnAssociateAfterDeploy.setEnabled(true);
				btnAssociateAfterDeploy.setSelection(true);
			}
		} else if (dialogType == DialogType.EXPORT) {
			if (btnUseTempIB.getSelection()) {
				btnShowOnlyAssociatedIB.setSelection(false);
				btnAssociateAfterDeploy.setSelection(false);
				return;
			} else {
				List<InfobaseReference> associatedInfobases = BaseProjectWorker.getInfobases(projectRef, true);
				if (associatedInfobases.isEmpty()) {
					selectedInfobase = null;
					btnShowOnlyAssociatedIB.setSelection(false);
					
					btnAssociateAfterDeploy.setEnabled(false);
					btnAssociateAfterDeploy.setSelection(true);
				} else {
					btnShowOnlyAssociatedIB.setSelection(true);
					
					btnAssociateAfterDeploy.setEnabled(false);
					btnAssociateAfterDeploy.setSelection(false);
				}
			}
		}
		fillInfobasesList();
	}
	
	private void enableAndSelectAssociateCheckboxesAfterSelectInfobase() {
		boolean isAssociated = projectRef != null
				&& InfobaseReferences.contains(BaseProjectWorker.getInfobases(projectRef, true), selectedInfobase);
		
		if (isAssociated) {
			btnShowOnlyAssociatedIB.setSelection(true);
			btnAssociateAfterDeploy.setEnabled(false);
			btnAssociateAfterDeploy.setSelection(false);
		} else {
			btnShowOnlyAssociatedIB.setSelection(false);
			btnAssociateAfterDeploy.setEnabled(
					dialogType == DialogType.EXPORT || (dialogType == DialogType.IMPORT && !projectRef.exists()));
			btnAssociateAfterDeploy.setSelection(true);
		}
	}
	
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		
		buttonOK = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		setButtonOKEnabled();
		buttonOK.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				
				ProjectInfo fileInfo = new ProjectInfo(selectedFileName, deployInTempIB, selectedInfobase,
						associateAfterDeploy, createDistributionFile);
				
				BaseProjectWorker projectWorker;
				try {
					if (dialogType == DialogType.IMPORT) {
						projectWorker = new ImportProjectWorker(parentShell, fileInfo, projectName, projectVersion,
								supportChangeMode);
						
						proceedJobInBackground(projectWorker, taskName);
						
					} else if (dialogType == DialogType.EXPORT) {
						projectWorker = new ExportProjectWorker(parentShell, fileInfo, projectRef,
								loadFullConfiguration, exportServiceRegistry);
						
						ProgressMonitorDialog progressDialog = new ProgressMonitorDialog(parentShell);
						
						Display.getDefault().asyncExec(() -> {
							try {
								progressDialog.run(true, true, new IRunnableWithProgress() {
									@Override
									public void run(IProgressMonitor monitor) throws InterruptedException {
										if (deployInTempIB) {
											((ExportProjectWorker) projectWorker).exportProjectToXml(monitor);
											proceedJobInBackground(projectWorker, taskName);
										} else {
											projectWorker.proceedJob(monitor);
										}
									}
									
								});
							} catch (InvocationTargetException | InterruptedException e) { // NOSONAR
								String errorMessage = MessageFormat.format(Messages.Status_Error,
										e.getLocalizedMessage());
								Activator.log(Activator.createErrorStatus(errorMessage, e));
								Messages.showPostBuildMessage(parentShell, taskName, errorMessage);
							}
							
						});
					}
					
				} catch (Exception e) {
					String errorMessage = MessageFormat.format(Messages.Status_Error, e.getLocalizedMessage());
					Activator.log(Activator.createErrorStatus(errorMessage, e));
					Messages.showPostBuildMessage(parentShell, taskName, errorMessage);
				}
				
			}
		});
		
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	private void setButtonOKEnabled() {
		boolean enabled = false;
		
		boolean projectOK = false;
		if (dialogType == DialogType.IMPORT) {
			
			if (projectName.isBlank()) {
				projectOK = false;
			} else if (BaseProjectWorker.projectIsExtension(projectRef)
					&& BaseProjectWorker.checkParentProjectFromExtensionIsNull(projectRef)) {
				projectOK = false;
			} else {
				projectOK = true;
			}
			
		} else if (dialogType == DialogType.EXPORT) {
			projectOK = true;
		}
		
		if (btnUseTempIB.getSelection()) {
			enabled = projectOK && selectedFileName != null;
		} else {
			enabled = projectOK && selectedFileName != null && selectedInfobase != null;
		}
		
		buttonOK.setEnabled(enabled);
	}
	
	private void proceedJobInBackground(BaseProjectWorker projectWorker, String taskName) {
		IJobFunction runnable = new IJobFunction() { // NOSONAR
			
			@Override
			public IStatus run(IProgressMonitor monitor) {
				return projectWorker.proceedJob(monitor);
			}
		};
		
		String jobName = MessageFormat.format(taskName, projectWorker.getProjectName());
		
		Job job = Job.create(jobName, runnable);
		job.schedule();
	}
	
	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(480, 380);
	}

	public static Color getColor(int systemColorID) {
		return Display.getCurrent().getSystemColor(systemColorID);
	}
	
	public static Font getFont(String name, int size, int style) {
		FontData fontData = new FontData(name, size, style);
		return new Font(Display.getCurrent(), fontData);
	}
	
}
