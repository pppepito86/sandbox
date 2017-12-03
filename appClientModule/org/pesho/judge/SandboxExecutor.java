package org.pesho.judge;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.zeroturnaround.exec.ProcessExecutor;

public class SandboxExecutor {

	protected static AtomicInteger processCounter = new AtomicInteger(0);
	
	//protected File sandboxDir = new File("sandbox_" + processCounter.incrementAndGet());
	protected ProcessExecutor processExecutor = new ProcessExecutor();

	protected File sandboxDir = new File("workdir");
	protected List<String> userCommand = new ArrayList<>();
	protected double timeoutInSeconds = 5.0;
	protected double extraTimeoutInSeconds = 10.0;
	protected int memoryInMB = 256;
	protected String input = "input";
	protected String output = "output";
	protected String error = "error";
	
	//protected ByteArrayOutputStream errorOutput = new ByteArrayOutputStream();

	public SandboxExecutor directory(File directory) {
		sandboxDir = directory;
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
	
	public SandboxResult execute() {
		if (!sandboxDir.exists()) sandboxDir.mkdirs();
		processExecutor.command(buildCommand());
		long sandboxTimeout = (long) (((timeoutInSeconds + extraTimeoutInSeconds))*1000);
		processExecutor.timeout(sandboxTimeout, TimeUnit.MILLISECONDS);
		
		System.out.println("command: " + printCommand());
		try {
			return new SandboxResult(processExecutor.execute(), sandboxDir, timeoutInSeconds);
		} catch (TimeoutException e) {
			return new SandboxResult(e);
		} catch (Exception e) {
			// TODO log e.printStackTrace();
			return new SandboxResult(e);
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

	protected String printCommand() {
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
		dockerCommand.add("--name");
		dockerCommand.add("cont2");
		dockerCommand.add("--volume");
		dockerCommand.add(sandboxDir.getAbsolutePath() + ":/shared/");
		dockerCommand.add("--cpus");
		dockerCommand.add("1");
		dockerCommand.add("--memory");
		dockerCommand.add(memoryInMB + "M");
		dockerCommand.add("--rm");
		dockerCommand.add("pppepito86/judge");
		dockerCommand.add("/scripts/sandbox.sh");		
		return dockerCommand;
	}
	
}
