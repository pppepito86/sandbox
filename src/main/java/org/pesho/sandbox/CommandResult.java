package org.pesho.sandbox;

public class CommandResult {

	protected final CommandStatus status;
	protected final String reason;
	protected final Double time;
	
	public CommandResult(CommandStatus status) {
		this(status, null);
	}
	
	public CommandResult(CommandStatus status, String reason) {
		this(status, reason, null);
	}
	
	public CommandResult(CommandStatus status, String reason, Double time) {
		this.status = status;
		this.reason = reason;
		this.time = time;
	}
	
	public CommandStatus getStatus() {
		return status;
	}
	
	public String getReason() {
		return reason;
	}
	
	public Double getTime() {
		return time;
	}
	
	@Override
	public String toString() {
		return status + ((reason == null) ? "": " " + reason);
	}
	
}
