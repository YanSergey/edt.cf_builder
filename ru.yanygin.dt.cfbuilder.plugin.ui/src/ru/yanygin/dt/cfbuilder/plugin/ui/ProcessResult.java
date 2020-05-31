package ru.yanygin.dt.cfbuilder.plugin.ui;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class ProcessResult {

	private IStatus status;
	private String output;

	public ProcessResult(IStatus status) {
		this.status = status;
		this.output = "";
	}

	public ProcessResult(IStatus status, String output) {
		this.status = status;
		this.output = output;
	}

	public IStatus getStatus() {
		return status;
	}

	public String getOutput() {
		return output;
	}

	public boolean statusIsOK() {
		return status.isOK();
	}

	public boolean statusIsCancel() {
		return status == Status.CANCEL_STATUS;
	}

	public void setResult(IStatus status) {
		this.status = status;
		this.output = "";
	}

	public void setResult(IStatus status, String output) {
		this.status = status;
		this.output = output;
	}
}
