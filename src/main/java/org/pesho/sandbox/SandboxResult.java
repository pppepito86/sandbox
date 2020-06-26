package org.pesho.sandbox;

import static org.pesho.sandbox.CommandStatus.PROGRAM_ERROR;
import static org.pesho.sandbox.CommandStatus.SUCCESS;
import static org.pesho.sandbox.CommandStatus.SYSTEM_ERROR;
import static org.pesho.sandbox.CommandStatus.TIMEOUT;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;

public class SandboxResult {

	protected final ProcessResult processResult;
	protected final File outputDir;
	protected final CommandResult commandResult;
	protected final Map<String, Object> metadata;

	public SandboxResult(ProcessResult processResult, File outputDir, double timeout, File errorFile) {
		this.processResult = processResult;
		this.outputDir = outputDir;
		this.metadata = getMetadata();
		System.out.println(metadata);
		this.commandResult = parseResult(timeout, errorFile);
	}

	public SandboxResult(Exception e) {
		this.commandResult = new CommandResult(SYSTEM_ERROR, e.getMessage());
		this.processResult = null;
		this.outputDir = null;
		this.metadata = getMetadata();
	}

	public ProcessOutput getOutput() {
		return processResult.getOutput();
	}

	public CommandResult getResult() {
		return commandResult;
	}

	public CommandStatus getStatus() {
		return commandResult.getStatus();
	}
	
	public Double getTime() {
		return (Double) metadata.get("time");
	}

	public Long getMemory() {
		return (Long) metadata.get("max-rss");
	}

	public Integer getExitcode() {
		return (Integer) metadata.get("exitcode");
	}
	
	protected CommandResult parseResult(double timeout, File errorFile) {
//		if (processResult.getExitValue() == 127) return new CommandResult(SYSTEM_ERROR, "sandbox.sh not found");
//		else if (processResult.getExitValue() != 0) return new CommandResult(SYSTEM_ERROR, "docker failed with exitcode (" + processResult.getExitValue() + ")");

		Integer exitCode = getExitcode();
		try {
		    // Timeout: returning the error to the user.
			if ("TO".equals(metadata.get("status"))) {
				return new CommandResult(TIMEOUT);
			}
			// Suicide with signal (memory limit, segfault, abort): returning the error to the user.
			if ("SG".equals(metadata.get("status"))) {
				return new CommandResult(PROGRAM_ERROR, readError(errorFile), exitCode, getTime(), getMemory());
			}
			// Sandbox error: this isn't a user error, the administrator needs to check the environment.
			if ("XX".equals(metadata.get("status"))) {
				return new CommandResult(SYSTEM_ERROR);
			}
			if (getExitcode() != 0) {
				return new CommandResult(PROGRAM_ERROR, (String) metadata.get("message"), exitCode, getTime(), getMemory());
			}
			return new CommandResult(SUCCESS, readError(errorFile), exitCode, getTime(), getMemory());
		} catch (Exception e) {
			e.printStackTrace();
			return new CommandResult(SYSTEM_ERROR, e.getMessage());
		}
	}

	private String readError(File errorFile) throws IOException {
		if (!errorFile.exists()) return null;
		return FileUtils.readFileToString(errorFile);
	}

	protected Map<String, Object> getMetadata() {
		Map<String, Object> map = new HashMap<>();
		File metadataFile = new File(outputDir, "metadata");
		if (!metadataFile.exists()) return map;

		try {
			((List<String>) FileUtils.readLines(metadataFile)).stream().forEach(line -> {
				if (line.contains(":")) {
					String[] split = line.split(":");
					if ("time".equals(split[0])) map.put("time", Double.valueOf(split[1].trim()));
					if ("time-wall".equals(split[0])) map.put("time-wall", Double.valueOf(split[1].trim()));
					if ("max-rss".equals(split[0])) map.put("max-rss", Long.valueOf(split[1].trim()));
					if ("exitcode".equals(split[0])) map.put("exitcode", Integer.valueOf(split[1].trim()));
					if ("status".equals(split[0])) map.put("status", split[1].trim());
					if ("message".equals(split[0])) map.put("message", split[1].trim());
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

}
