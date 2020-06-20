package org.pesho.sandbox;

public class CommandResult {

	protected final CommandStatus status;
	protected final String reason;
	protected final Double time;
	protected final Long memory;
	
	public CommandResult(CommandStatus status) {
		this(status, null);
	}
	
	public CommandResult(CommandStatus status, String reason) {
		this(status, reason, null, null);
	}
	
	public CommandResult(CommandStatus status, String reason, Double time, Long memory) {
		this.status = status;
		this.reason = reason;
		this.time = time;
		this.memory = memory;
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
	
	public Long getMemory() {
		return memory;
	}
	
	@Override
	public String toString() {
		return status + ((reason == null) ? "": " " + reason);
	}
	
}
