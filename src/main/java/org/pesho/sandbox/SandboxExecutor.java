package org.pesho.sandbox;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

public class SandboxExecutor {

	protected static AtomicInteger processCounter = new AtomicInteger(0);
	
	protected ProcessExecutor processExecutor = new ProcessExecutor();

	protected File sandboxDir = new File(".").getAbsoluteFile();
	protected List<String> userCommand = new ArrayList<>();
	protected double timeoutInSeconds = 5.0;
	protected double extraTimeoutInSeconds = 10.0;
	protected int memoryInMB = 256;
	protected String input = "input";
	protected String output = "output";
	protected String error = "error";
	protected boolean clean = false;
	protected String containerName = null;
	
	public SandboxExecutor directory(File directory) {
		sandboxDir = directory.getAbsoluteFile();
		return this;
	}
	
	public SandboxExecutor command(String command) {
		return command(command.split(" "));
	}
	
	
	public SandboxExecutor command(String... command) {
		Arrays.stream(command).forEach(userCommand::add);
		return this;
	}
	
	public SandboxExecutor timeout(double time) {
		timeoutInSeconds = time;
		return this;
	}
	
	public SandboxExecutor memory(int memory) {
		memoryInMB = memory;
		return this;
	}

	public SandboxExecutor input(String input) {
		this.input = input;
		return this;
	}
	
	public SandboxExecutor output(String output) {
		this.output = output;
		return this;
	}
	
	public SandboxExecutor error(String error) {
		this.error = error;
		return this;
	}

	public SandboxExecutor clean(boolean clean) {
		this.clean = clean;
		return this;
	}
	
	public SandboxExecutor name(String containerName) {
		this.containerName = containerName;
		return this;
	}
	
	public SandboxResult execute() {
		if (!sandboxDir.exists()) sandboxDir.mkdirs();
		processExecutor.command(buildCommand());
		long sandboxTimeout = (long) (((timeoutInSeconds + extraTimeoutInSeconds))*1000);
		processExecutor.timeout(sandboxTimeout, TimeUnit.MILLISECONDS);
		
		System.out.println("command: " + printCommand());
		try {
			ProcessResult processResult = processExecutor.execute();
			return new SandboxResult(processResult, sandboxDir, timeoutInSeconds, new File(sandboxDir, error));
		} catch (TimeoutException e) {
			return new SandboxResult(e);
		} catch (Exception e) {
			e.printStackTrace();
			return new SandboxResult(e);
		} finally {
			System.out.println("Clean is: " + clean);
			if (clean) {
				clean();
			}
		}
	}
	
	private void clean() {
		try {
			System.out.println("About to clean: " + sandboxDir);
			FileUtils.deleteDirectory(sandboxDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public SandboxExecutor readOutput(boolean readOutput) {
		processExecutor.readOutput(readOutput);
		return this;
	}

	protected List<String> buildCommand() {
		List<String> dockerCommand = getDockerCommand();
		List<String> command = new ArrayList<>(dockerCommand.size() + 5);
		command.addAll(dockerCommand);
		command.add(String.join(" ", userCommand));
		command.add(String.valueOf(timeoutInSeconds));
		command.add(input);
		command.add(output);
		command.add(error);
		return command;
	}

	public String printCommand() {
		StringBuilder result = new StringBuilder(String.join(" ", getDockerCommand()));
		result.append(" ").append("\"" + String.join(" ", userCommand) +"\"");
		result.append(" ").append(String.valueOf(timeoutInSeconds));
		result.append(" ").append(input);
		result.append(" ").append(output);
		result.append(" ").append(error);
		return result.toString();
	}

	
	protected List<String> getDockerCommand() {
		List<String> dockerCommand = new ArrayList<>();
		dockerCommand.add("docker");
		dockerCommand.add("run");
		if (containerName != null) {
			dockerCommand.add("--name");
			dockerCommand.add(containerName);
		}
		dockerCommand.add("--volume");
		dockerCommand.add(sandboxDir + ":/shared/");
		dockerCommand.add("--cpus");
		dockerCommand.add("1");
		dockerCommand.add("--memory");
		dockerCommand.add(memoryInMB + "M");
		dockerCommand.add("--network");
		dockerCommand.add("none");
		dockerCommand.add("--rm");
		dockerCommand.add("pppepito86/judge");
		dockerCommand.add("/scripts/sandbox.sh");		
		return dockerCommand;
	}
	
}
