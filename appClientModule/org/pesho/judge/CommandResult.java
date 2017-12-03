package org.pesho.judge;

public class CommandResult {

	protected final CommandStatus status;
	protected final String reason;
	
	public CommandResult(CommandStatus status) {
		this(status, null);
	}
	
	public CommandResult(CommandStatus status, String reason) {
		this.status = status;
		this.reason = reason;
	}
	
	public CommandStatus getStatus() {
		return status;
	}
	
	public String getReason() {
		return reason;
	}
	
	@Override
	public String toString() {
		return status + ((reason == null) ? "": " " + reason);
	}
	
}
