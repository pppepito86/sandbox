package org.pesho.sandbox;

import static org.pesho.sandbox.CommandStatus.OOM;
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
import org.apache.commons.math3.util.Precision;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;

public class SandboxResult {

	protected final ProcessResult processResult;
	protected final File outputDir;
	protected final CommandResult commandResult;
	protected final Map<String, Object> metadata;

	public SandboxResult(ProcessResult processResult, File outputDir, double timeout, int memory, File errorFile, boolean hasExtraMetadata) {
		this.processResult = processResult;
		this.outputDir = outputDir;
		this.metadata = new HashMap<>();
		this.metadata.putAll(getMetadata());
		this.metadata.putAll(getExtraMetadata(hasExtraMetadata));
		System.out.println(metadata);
		this.commandResult = parseResult(timeout, memory, errorFile);
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
		return Precision.round((double) metadata.get("time") - (double) metadata.get("io-time"), 3);
	}

	public Long getMemory() {
		return (Long) metadata.get("cg-mem");
	}

	public Integer getExitcode() {
		return (Integer) metadata.get("exitcode");
	}
	
	public String getError(File errorFile) throws IOException {
		if (errorFile != null && errorFile.exists()) return readError(errorFile);
		return (String) metadata.get("message");
	}
	
	protected CommandResult parseResult(double timeout, int memory, File errorFile) {
//		if (processResult.getExitValue() == 127) return new CommandResult(SYSTEM_ERROR, "sandbox.sh not found");
//		else if (processResult.getExitValue() != 0) return new CommandResult(SYSTEM_ERROR, "docker failed with exitcode (" + processResult.getExitValue() + ")");

		Integer exitCode = getExitcode();
		Long memoryToShow = getMemory() == null ? null : (getMemory() >= (memory+4)*1024 ? -1024*(memory+4) : getMemory());
		if (memoryToShow == null) System.out.println("No memory to show");
		
		try {
			// Sandbox error: this isn't a user error, the administrator needs to check the environment.
			if ("XX".equals(metadata.get("status"))) {
				return new CommandResult(SYSTEM_ERROR);
			}
		    // Timeout: returning the error to the user.
			if ("TO".equals(metadata.get("status"))) {
				String error = getError(errorFile);
				if (error != null && error.contains("wall clock")) {
					return new CommandResult(TIMEOUT, Messages.WALL_CLOCK_TIMEOUT, exitCode, Precision.round(-getTime(), 3), memoryToShow);
				}
			}

			double extraTime = Precision.round(timeout+Math.min(timeout/2, 0.5), 3);
			if (getTime() >= extraTime) {
				return new CommandResult(TIMEOUT, Messages.EXTRA_TIME_LIMIT_EXCEEDED, exitCode, -extraTime, memoryToShow);
			}

			if (getTime() > timeout) {
				return new CommandResult(TIMEOUT, Messages.TIME_LIMIT_EXCEEDED, exitCode, getTime(), memoryToShow);
			}
				
			// OOM
			if (getMemory() >= memory*1024) {
				return new CommandResult(OOM, Messages.MEMORY_LIMIT_EXCEEDED, exitCode, getTime(), memoryToShow);
			}
			// Suicide with signal (memory limit, segfault, abort): returning the error to the user.
			if ("SG".equals(metadata.get("status"))) {
				return new CommandResult(PROGRAM_ERROR, getError(errorFile), exitCode, getTime(), memoryToShow);
			}
			if (getExitcode() != 0) {
				return new CommandResult(PROGRAM_ERROR, getError(errorFile), exitCode, getTime(), memoryToShow);
			}
			
			return new CommandResult(SUCCESS, getError(errorFile), exitCode, getTime(), memoryToShow);
		} catch (Exception e) {
			e.printStackTrace();
			return new CommandResult(SYSTEM_ERROR, e.getMessage(), exitCode, getTime(), memoryToShow);
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
					if ("cg-mem".equals(split[0])) {
						long maxMemory = Long.valueOf(split[1].trim());
						map.put("cg-mem", maxMemory);
					}
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

	protected Map<String, Object> getExtraMetadata(boolean hasExtraMetadata) {
		Map<String, Object> map = new HashMap<>();
		map.put("io-time", 0.0);
		File metadataFile = new File(outputDir, "extra_metadata");
		if (!hasExtraMetadata || !metadataFile.exists()) return map;

		try {
			((List<String>) FileUtils.readLines(metadataFile)).stream().forEach(line -> {
				if (line.contains(":")) {
					String[] split = line.split(":");
					if ("io-time".equals(split[0])) map.put("io-time", Double.valueOf(split[1].trim()));
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}

}
