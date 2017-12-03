package org.pesho.judge;

import static org.pesho.judge.CommandStatus.OOM;
import static org.pesho.judge.CommandStatus.PROGRAM_ERROR;
import static org.pesho.judge.CommandStatus.SUCCESS;
import static org.pesho.judge.CommandStatus.SYSTEM_ERROR;
import static org.pesho.judge.CommandStatus.TIMEOUT;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;

public class SandboxResult {

	protected final ProcessResult processResult;
	protected final File outputDir;
	protected final CommandResult commandResult;
	protected final Double time;

	public SandboxResult(ProcessResult processResult, File outputDir, double timeout) {
		this.processResult = processResult;
		this.outputDir = outputDir;
		this.time = parseTime();
		this.commandResult = parseResult(timeout);
	}

	public SandboxResult(Exception e) {
		this.commandResult = new CommandResult(SYSTEM_ERROR, e.getMessage());
		this.processResult = null;
		this.outputDir = null;
		this.time = null;
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
		return time;
	}

	protected CommandResult parseResult(double timeout) {
		if (processResult.getExitValue() == 127)
			return new CommandResult(SYSTEM_ERROR, "sandbox.sh not found");
		else if (processResult.getExitValue() != 0)
			return new CommandResult(SYSTEM_ERROR,
					"docker failed with exitcode (" + processResult.getExitValue() + ")");

		try {
			File exitCodeFile = new File(outputDir, "exitcode");
			if (exitCodeFile.exists()) {
				int exitCode = Integer.valueOf(FileUtils.readFileToString(exitCodeFile).trim());
				if (exitCode == 0)
					return new CommandResult(SUCCESS);
				if (exitCode == 127)
					return new CommandResult(CommandStatus.PROGRAM_NOT_FOUND);
				if (exitCode == 137 && time > timeout)
					return new CommandResult(TIMEOUT);
				if (exitCode == 137 && time <= timeout)
					return new CommandResult(OOM);
				return new CommandResult(PROGRAM_ERROR);
			}

			return new CommandResult(SYSTEM_ERROR, "result files do not exist");
		} catch (Exception e) {
			// TODO log e.printStackTrace();
			return new CommandResult(SYSTEM_ERROR, e.getMessage());
		}
	}

	protected Double parseTime() {
		File timeFile = new File(outputDir, "time");
		if (!timeFile.exists())
			return null;

		try {
			String timeAsString = FileUtils.readFileToString(timeFile).trim();
			return Double.valueOf(timeAsString);
		} catch (Exception e) {
			// TODO log e
			return null;
		}
	}

}
