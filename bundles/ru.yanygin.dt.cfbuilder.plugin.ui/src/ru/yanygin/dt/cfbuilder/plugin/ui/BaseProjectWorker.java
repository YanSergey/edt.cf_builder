package ru.yanygin.dt.cfbuilder.plugin.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com._1c.g5.v8.dt.common.FileUtil;
import com._1c.g5.v8.dt.common.Pair;
import com._1c.g5.v8.dt.compare.ui.editor.IDtComparisonEditorInputFactory;
import com._1c.g5.v8.dt.core.platform.IConfigurationProvider;
import com._1c.g5.v8.dt.core.platform.IDependentProject;
import com._1c.g5.v8.dt.core.platform.IV8Project;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.import_.IImportOperationFactory;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.platform.services.core.infobases.IInfobaseAccessManager;
import com._1c.g5.v8.dt.platform.services.core.infobases.IInfobaseAccessSettings;
import com._1c.g5.v8.dt.platform.services.core.infobases.IInfobaseAssociation;
import com._1c.g5.v8.dt.platform.services.core.infobases.IInfobaseAssociationContextProvider;
import com._1c.g5.v8.dt.platform.services.core.infobases.IInfobaseAssociationManager;
import com._1c.g5.v8.dt.platform.services.core.infobases.IInfobaseManager;
import com._1c.g5.v8.dt.platform.services.core.infobases.InfobaseAssociationSettings;
import com._1c.g5.v8.dt.platform.services.core.infobases.InfobaseReferences;
import com._1c.g5.v8.dt.platform.services.core.infobases.sync.IInfobaseSynchronizationManager;
import com._1c.g5.v8.dt.platform.services.core.infobases.sync.InfobaseSynchronizationException;
import com._1c.g5.v8.dt.platform.services.core.runtimes.RuntimeInstallations;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallation;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.IResolvableRuntimeInstallationManager;
import com._1c.g5.v8.dt.platform.services.core.runtimes.environments.MatchingRuntimeNotFound;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.ILaunchableRuntimeComponent;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.IRuntimeComponentManager;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.IRuntimeComponentTypes;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.IThickClientLauncher;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.RuntimeExecutionArguments;
import com._1c.g5.v8.dt.platform.services.core.runtimes.execution.RuntimeExecutionException;
import com._1c.g5.v8.dt.platform.services.model.CreateInfobaseArguments;
import com._1c.g5.v8.dt.platform.services.model.InfobaseReference;
import com._1c.g5.v8.dt.platform.services.model.InfobaseType;
import com._1c.g5.v8.dt.platform.services.model.ModelFactory;
import com._1c.g5.v8.dt.platform.services.model.Section;
import com._1c.g5.v8.dt.platform.services.ui.infobases.sync.InfobaseUpdateDialogBasedCallback;
import com._1c.g5.v8.dt.platform.version.IRuntimeVersionSupport;
import com._1c.g5.v8.dt.platform.version.UnsupportedVersionException;
import com._1c.g5.v8.dt.platform.version.Version;
import com._1c.g5.wiring.ServiceAccess;
import com._1c.g5.wiring.ServiceSupplier;
import com.google.common.base.Strings;
import com.google.common.io.Files;

import ru.yanygin.dt.cfbuilder.plugin.ui.JobDialog.DialogType;

public abstract class BaseProjectWorker {
	
	protected String JOB_TITLE;
	protected String JOB_IS_DONE_MESSAGE;
	protected String JOB_IS_DONE_STATUS;
	protected String JOB_IS_ABORT_MESSAGE;
	
	protected enum ProjectType {
		CONFIGURATION, EXTENSION, ALL
	}
	
	protected String projectName;
	protected String extName;
	
	protected ProjectInfo projectInfo;
	
	protected Version projectV8version;
	
	protected Shell parentShell;
	
	protected Path temptXMLPath;
	protected String tempInfobaseDirPath;
	
	protected ProjectType projectType;
	
	public String getProjectName() {
		return projectName;
	}
	
	protected Version getProjectV8Version() {
		return projectV8version;
	}
	
	protected Pair<ILaunchableRuntimeComponent, IThickClientLauncher> v8Launcher;
	protected IStatus jobStatus = Status.OK_STATUS;
	
	public BaseProjectWorker(Shell parentShell, ProjectInfo projectInfo, String projectName) {
		this.parentShell = parentShell;
		this.projectInfo = projectInfo;
		this.projectName = projectName;
	}
	
	protected boolean checkJobStatus(String taskName, IProgressMonitor buildMonitor) {
		if (jobStatus.isOK() && buildMonitor.isCanceled()) {
			
			String cancelStatus = Messages.Status_OperationAbortByUser;
			
			jobStatus = Activator.createCancelStatus(cancelStatus);
			Activator.log(Activator.createInfoStatus(cancelStatus));
			buildMonitor.setTaskName(cancelStatus);
			
			return false;
		}
		
		if (jobStatus.isOK()) {
			buildMonitor.beginTask(taskName, IProgressMonitor.UNKNOWN);
			Activator.log(Activator.createInfoStatus(taskName));
			return true;
		}
		
		return false;
	}
	
	protected void createThickClientLauncher() {
		
		IRuntimeComponentManager runtimeComponentManager = getRuntimeComponentManager();
		
		IResolvableRuntimeInstallationManager resolvableRuntimeInstallationManager = getResolvableRuntimeInstallationManager();
		
		IResolvableRuntimeInstallation resolvableRuntimeInstallation = resolvableRuntimeInstallationManager
				.getDefault(RuntimeInstallations.ENTERPRISE_PLATFORM, getProjectV8Version().toString());
		
		try {
			v8Launcher = runtimeComponentManager.getComponentAndExecutor(
					resolvableRuntimeInstallation.get(IRuntimeComponentTypes.THICK_CLIENT),
					IRuntimeComponentTypes.THICK_CLIENT);
			
		} catch (MatchingRuntimeNotFound e) {
			jobStatus = Activator.createErrorStatus(e);
			Activator.log(jobStatus);
		}
		
	}
	
	public String[] getParentConfigurationsFiles(Path xmlTempPath) {
		
		String tempDir = xmlTempPath.toString();
		
		String[] parentConfigurationsFiles = new String[2];
		
		parentConfigurationsFiles[0] = tempDir.concat("\\Ext\\ParentConfigurations.bin");
		parentConfigurationsFiles[1] = tempDir.concat("\\Ext\\ParentConfigurations");
		
		return parentConfigurationsFiles;
	}
	
	protected RuntimeExecutionArguments buildArguments(InfobaseReference infobase) {
		RuntimeExecutionArguments arguments = new RuntimeExecutionArguments();
		
		IInfobaseAccessManager infobaseAccessManager = getInfobaseAccessManager();
		
		try {
			IInfobaseAccessSettings settings = infobaseAccessManager.getSettings(infobase);
			
			arguments.setAccess(settings.access());
			arguments.setUsername(settings.userName());
			arguments.setPassword(settings.password());
			
		} catch (CoreException e) {
			jobStatus = Activator.createErrorStatus(e);
			Activator.log(jobStatus);
		}
		
		return arguments;
	}
	
	protected void createTempInfobase(IProgressMonitor monitor) {
		
		if (!checkJobStatus(Messages.Actions_CreateTempBase, monitor)) {
			return;
		}
		
		CreateInfobaseArguments args = ModelFactory.eINSTANCE.createCreateInfobaseArguments();
		
		try {
			tempInfobaseDirPath = createTempDirectory();
		} catch (IOException e) {
			jobStatus = Activator.createErrorStatus(e);
			Activator.log(jobStatus);
		}
		
		InfobaseReference deploymentInfobase = InfobaseReferences.newFileInfobaseReference(tempInfobaseDirPath);
		UUID baseUUID = UUID.randomUUID();
		deploymentInfobase.setUuid(baseUUID);
		deploymentInfobase.setName("TempIB-".concat(baseUUID.toString()));
		
		try {
			v8Launcher.second.createInfobase(v8Launcher.first, deploymentInfobase, args, false);// false = не
																								// регистрировать базу
		} catch (RuntimeExecutionException e) {
			jobStatus = Activator.createErrorStatus(e);
			Activator.log(jobStatus);
		}
		projectInfo.setDeploymentInfobase(deploymentInfobase);
	}
	
	protected void deleteTempInfobase(IProgressMonitor monitor) {
		
		if (Strings.isNullOrEmpty(tempInfobaseDirPath)) {
			return;
		}
		
		File tempBasePath = new File(tempInfobaseDirPath);
		if (tempBasePath.exists()) {
			monitor.beginTask(Messages.Actions_DeleteTempBase, IProgressMonitor.UNKNOWN);
			Activator.log(Activator.createInfoStatus(Messages.Actions_DeleteTempBase));
			
			recursiveDelete(new File(tempInfobaseDirPath));
		}
		
	}
	
	protected void createTempXMLDir(IProgressMonitor monitor) {
		
		if (!checkJobStatus(Messages.Actions_CreateTempDir, monitor)) {
			return;
		}
		
		try {
			temptXMLPath = Paths.get(createTempDirectory());
		} catch (IOException e) {
			jobStatus = Activator.createErrorStatus(e);
			Activator.log(jobStatus);
		}
		
	}
	
	protected void deleteTempDirs(IProgressMonitor monitor) {
		
		if (temptXMLPath == null) {
			return;
		}
		
		monitor.beginTask(Messages.Actions_ClearingTemp, IProgressMonitor.UNKNOWN);
		recursiveDelete(temptXMLPath.toFile());
		monitor.done();
	}
	
	protected void recursiveDelete(File file) {
		
		if (!file.exists())
			return;
		
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				recursiveDelete(f);
			}
		}
		
		if (!file.delete())
			Activator.log(Activator
					.createInfoStatus(MessageFormat.format(Messages.Info_ErrorDeleteFile, file.getAbsolutePath())));
	}
	
	private String createTempDirectory() throws IOException {
		return FileUtil.createTempDirectory("cfbldr-").getAbsolutePath();
	}
	
	public abstract IStatus proceedJob(IProgressMonitor monitor);
	
	public static String askCfLocationPath(Shell parentShell, DialogType dialogType, IProject projectRef) {
		
		int style = dialogType == DialogType.IMPORT ? SWT.OPEN : SWT.SAVE;
		
		ProjectType projectType = (projectRef == null || !projectRef.exists()) ? ProjectType.ALL
				: BaseProjectWorker.getProjectTypeFromProject(projectRef);
		
		FileDialog fileDialog = new FileDialog(parentShell, style);
		fileDialog.setText(Messages.Dialog_SetCfLocation);
		
		String ext;
		String names;
		switch (projectType) {
			case CONFIGURATION:
				ext = "*.cf";
				names = Messages.Filter_CF_Files;
				break;
			case EXTENSION:
				ext = "*.cfe";
				names = Messages.Filter_CFE_Files;
				break;
			case ALL:
			default:
				ext = "*.cf;*.cfe";
				names = Messages.Filter_CF_CFE_Files;
				break;
		}
		String[] filterExt = { ext };
		String[] filterNames = { names };
		
		fileDialog.setFilterExtensions(filterExt);
		fileDialog.setFilterNames(filterNames);
		
		return fileDialog.open();
	}
	
	public static List<InfobaseReference> getInfobases(IProject project, boolean onlyAssociated) {
		
		List<InfobaseReference> infoBases = new ArrayList<>();
		
		if (!project.exists() && onlyAssociated)
			return infoBases;
		
		if (onlyAssociated) {
			
			if (projectIsExtension(project)) {
				project = getParentProjectFromExtensionProject(project);
				if (project == null)
					return infoBases;
			}
			
			IInfobaseAssociationManager infobaseAssociationManager = getInfobaseAssociationManager();
			
			Optional<IInfobaseAssociation> infobaseAssociations = infobaseAssociationManager.getAssociation(project);
			
			infobaseAssociations.stream().forEach(i -> {
				Collection<InfobaseReference> ibs = i.getInfobases();
				ibs.forEach(ib -> {
					if (ib.getInfobaseType() != InfobaseType.WEB)
						infoBases.add(ib);
				});
			});
			
		} else {
			
			List<Section> infoBasesSection = getInfobaseManager().getAll(true);
			
			infoBasesSection.forEach(ib -> {
				if (ib instanceof InfobaseReference && ((InfobaseReference) ib).getInfobaseType() != InfobaseType.WEB)
					infoBases.add((InfobaseReference) ib);
			});
			
		}
		
		return infoBases;
	}
	
	protected void deployProjectToExistingInfobase(IProject deployProject, InfobaseReference infobase, boolean fullLoad,
			boolean linkIBToProject, IProgressMonitor monitor) {
		
		if (linkIBToProject) {
			associateInfobaseToProject(infobase, deployProject, monitor);
		}
		
		if (!checkJobStatus(Messages.Actions_DeployProjectToInfobase, monitor)) {
			return;
		}
		
		IV8ProjectManager v8projectManager = getV8projectManager();
		IDtComparisonEditorInputFactory compareEditorInputFactory = getCompareEditorInputFactory();
		IInfobaseSynchronizationManager infobaseSynchroManager = getInfobaseSynchronizationManager();
		
		try {
			InfobaseUpdateDialogBasedCallback confirm = new InfobaseUpdateDialogBasedCallback(parentShell,
					v8projectManager, compareEditorInputFactory);
			confirm.setAllowOverrideConflict(true);
			
			boolean progressIsOk = true;
			
			if (fullLoad || linkIBToProject) {
				progressIsOk = infobaseSynchroManager.reloadInfobase(deployProject, infobase, confirm, true, monitor);
			} else {
				progressIsOk = infobaseSynchroManager.updateInfobase(deployProject, infobase, confirm, true, monitor);
			}
			monitor.subTask("");
			if (!progressIsOk || monitor.isCanceled()) {
				jobStatus = Activator.createCancelStatus(Messages.Status_OperationAbortByUser);
				Activator.log(jobStatus);
			}
			
		} catch (UnsupportedVersionException | InfobaseSynchronizationException e) {
			jobStatus = Activator.createErrorStatus(e);
			Activator.log(jobStatus);
		}
		
	}
	
	protected void associateInfobaseToProject(InfobaseReference infobase, IProject project, IProgressMonitor monitor) {
		if (!checkJobStatus(Messages.Actions_SetAssociateInfobaseToProject, monitor)) {
			return;
		}
		
		IInfobaseAssociationManager infobaseAssociationManager = getInfobaseAssociationManager();
		IInfobaseAssociationContextProvider infobaseAssociationContextProvider = getInfobaseAssociationContextProvider();
		
		infobaseAssociationManager.associate(project, infobase,
				new InfobaseAssociationSettings(true, true, infobaseAssociationContextProvider.get(project)));
	}
	
	public static List<Version> getRuntimeV8SupportedVersions() {
		return getRuntimeVersionSupport().getSupportedVersions();
	}
	
	public static Version getV8VersionFromProject(IProject project) {
		return getRuntimeVersionSupport().getRuntimeVersion(project);
	}
	
	public static IProject getProjectReferenceFromWorkspace(String projectName) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
	}
	
	public static boolean projectExistInWorkspace(String projectName) {
		return getProjectReferenceFromWorkspace(projectName).exists();
	}
	
	public static ProjectType getProjectTypeFromProject(IProject project) {
		
		try {
			if (project.hasNature("com._1c.g5.v8.dt.core.V8ConfigurationNature")) {
				return ProjectType.CONFIGURATION;
			} else if (project.hasNature("com._1c.g5.v8.dt.core.V8ExtensionNature")) {
				return ProjectType.EXTENSION;
			}
		} catch (CoreException e) {
			return ProjectType.ALL;
		}
		return ProjectType.ALL;
	}
	
	public static ProjectType getProjectTypeFromFileName(String filename) {
		
		String fileExt = Files.getFileExtension(filename);
		
		if (fileExt.equalsIgnoreCase(ProjectInfo.FILEEXTENSION_CONFIGURATION)) {
			return ProjectType.CONFIGURATION;
		} else if (fileExt.equalsIgnoreCase(ProjectInfo.FILEEXTENSION_EXTENSION)) {
			return ProjectType.EXTENSION;
		} else {
			return ProjectType.CONFIGURATION;
		}
	}
	
	protected String getExtensionNameFromProject(IProject project, ProjectType projectType) {
		
		if (projectType == ProjectType.EXTENSION && project.exists()) {
			
			IConfigurationProvider configurationProvider = getConfigurationProvider();
			
			Configuration configuration = configurationProvider.getConfiguration(project);
			return configuration.getName();
		} else {
			return project.getName();
		}
		
	}
	
	protected void showOperationResult() {
		
		String buildMessage;
		
		if (jobStatus.isOK()) {
			buildMessage = JOB_IS_DONE_MESSAGE;
			Activator.log(Activator.createInfoStatus(JOB_IS_DONE_STATUS));
			
		} else if (jobStatus.getSeverity() == IStatus.CANCEL) {
			buildMessage = jobStatus.getMessage();
			
		} else {
			String errorMessage = jobStatus.getException() == null ? jobStatus.getMessage()
					: jobStatus.getException().getMessage();
			buildMessage = MessageFormat.format(Messages.Status_Error, errorMessage);
		}
		Messages.showPostBuildMessage(parentShell, JOB_TITLE, buildMessage);
	}
	
	public static IProject getParentProjectFromExtensionProject(IProject project) {
		if (projectIsExtension(project)) {
			
			IV8ProjectManager v8projectManager = getV8projectManager();
			
			IV8Project v8project = v8projectManager.getProject(project);
			if (v8project instanceof IDependentProject) {
				project = ((IDependentProject) v8project).getParentProject();
			}
		}
		return project;
	}
	
	public static boolean projectIsExtension(IProject project) {
		boolean isExtension = false;
		try {
			if (project.exists() && project.hasNature("com._1c.g5.v8.dt.core.V8ExtensionNature")) {
				isExtension = true;
			}
		} catch (CoreException e) {
			Activator.log(Activator.createInfoStatus(Messages.Info_ErrorIdentifyingProjectType));
		}
		return isExtension;
	}
	
	public static boolean checkParentProjectFromExtensionIsNull(IProject project) {
		if (project.exists() && projectIsExtension(project)) {
			return getParentProjectFromExtensionProject(project) == null;
		}
		return false;
	}
	
	protected static IInfobaseManager getInfobaseManager() {
		ServiceSupplier<IInfobaseManager> infobaseManagerSupplier = ServiceAccess.supplier(IInfobaseManager.class,
				Activator.getDefault());
		IInfobaseManager infobaseManager = infobaseManagerSupplier.get();
		infobaseManagerSupplier.close();
		return infobaseManager;
	}
	
	protected IInfobaseSynchronizationManager getInfobaseSynchronizationManager() {
		ServiceSupplier<IInfobaseSynchronizationManager> infobaseSynchronizationManagerSupplier = ServiceAccess
				.supplier(IInfobaseSynchronizationManager.class, Activator.getDefault());
		IInfobaseSynchronizationManager infobaseSynchronizationManager = infobaseSynchronizationManagerSupplier.get();
		infobaseSynchronizationManagerSupplier.close();
		return infobaseSynchronizationManager;
	}
	
	private IInfobaseAccessManager getInfobaseAccessManager() {
		ServiceSupplier<IInfobaseAccessManager> infobaseAccessManagerSupplier = ServiceAccess
				.supplier(IInfobaseAccessManager.class, Activator.getDefault());
		IInfobaseAccessManager infobaseAccessManager = infobaseAccessManagerSupplier.get();
		infobaseAccessManagerSupplier.close();
		return infobaseAccessManager;
	}
	
	private static IInfobaseAssociationManager getInfobaseAssociationManager() {
		ServiceSupplier<IInfobaseAssociationManager> infobaseAssociationManagerSupplier = ServiceAccess
				.supplier(IInfobaseAssociationManager.class, Activator.getDefault());
		IInfobaseAssociationManager infobaseAssociationManager = infobaseAssociationManagerSupplier.get();
		infobaseAssociationManagerSupplier.close();
		return infobaseAssociationManager;
	}
	
	private IInfobaseAssociationContextProvider getInfobaseAssociationContextProvider() {
		ServiceSupplier<IInfobaseAssociationContextProvider> infobaseAssociationSupplier = ServiceAccess
				.supplier(IInfobaseAssociationContextProvider.class, Activator.getDefault());
		IInfobaseAssociationContextProvider infobaseAssociationContextProvider = infobaseAssociationSupplier.get();
		infobaseAssociationSupplier.close();
		return infobaseAssociationContextProvider;
	}
	
	protected static IV8ProjectManager getV8projectManager() {
		ServiceSupplier<IV8ProjectManager> v8projectManagerSupplier = ServiceAccess.supplier(IV8ProjectManager.class,
				Activator.getDefault());
		IV8ProjectManager v8projectManager = v8projectManagerSupplier.get();
		v8projectManagerSupplier.close();
		return v8projectManager;
	}
	
	protected IDtComparisonEditorInputFactory getCompareEditorInputFactory() {
		ServiceSupplier<IDtComparisonEditorInputFactory> compareEditorInputFactorySupplier = ServiceAccess
				.supplier(IDtComparisonEditorInputFactory.class, Activator.getDefault());
		IDtComparisonEditorInputFactory compareEditorInputFactory = compareEditorInputFactorySupplier.get();
		compareEditorInputFactorySupplier.close();
		return compareEditorInputFactory;
	}
	
	protected IImportOperationFactory getImportOperationFactory() {
		ServiceSupplier<IImportOperationFactory> importOperationFactorySupplier = ServiceAccess
				.supplier(IImportOperationFactory.class, Activator.getDefault());
		IImportOperationFactory importOperationFactory = importOperationFactorySupplier.get();
		importOperationFactorySupplier.close();
		return importOperationFactory;
	}
	
	private IResolvableRuntimeInstallationManager getResolvableRuntimeInstallationManager() {
		ServiceSupplier<IResolvableRuntimeInstallationManager> managerSupplier = ServiceAccess
				.supplier(IResolvableRuntimeInstallationManager.class, Activator.getDefault());
		IResolvableRuntimeInstallationManager resolvableRuntimeInstallationManager = managerSupplier.get();
		managerSupplier.close();
		return resolvableRuntimeInstallationManager;
	}
	
	private IRuntimeComponentManager getRuntimeComponentManager() {
		ServiceSupplier<IRuntimeComponentManager> runtimeComponentManagerSupplier = ServiceAccess
				.supplier(IRuntimeComponentManager.class, Activator.getDefault());
		IRuntimeComponentManager runtimeComponentManager = runtimeComponentManagerSupplier.get();
		runtimeComponentManagerSupplier.close();
		return runtimeComponentManager;
	}
	
	protected static IRuntimeVersionSupport getRuntimeVersionSupport() {
		ServiceSupplier<IRuntimeVersionSupport> runtimeVersionSupportSupplier = ServiceAccess
				.supplier(IRuntimeVersionSupport.class, Activator.getDefault());
		IRuntimeVersionSupport runtimeVersionSupport = runtimeVersionSupportSupplier.get();
		runtimeVersionSupportSupplier.close();
		return runtimeVersionSupport;
	}
	
	protected static IConfigurationProvider getConfigurationProvider() {
		ServiceSupplier<IConfigurationProvider> configurationProviderSupplier = ServiceAccess
				.supplier(IConfigurationProvider.class, Activator.getDefault());
		IConfigurationProvider configurationProvider = configurationProviderSupplier.get();
		configurationProviderSupplier.close();
		return configurationProvider;
	}
	
}
