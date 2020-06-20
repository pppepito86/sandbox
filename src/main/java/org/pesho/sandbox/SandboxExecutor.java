package org.pesho.sandbox;

import java.io.File;
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
	protected double extraTimeoutInSeconds = 1.0;
	protected int memoryInMB = 256;
	protected String input = null;
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
		if (command[0].equals("g++")) command[0] = "/usr/bin/g++";
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
	
	public void createSandbox() throws Exception {
		new ProcessExecutor().command("isolate", "--init").execute();
	}
	public void destroySandbox() throws Exception {
		new ProcessExecutor().command("isolate", "--cleanup").execute();
	}
	
	public SandboxResult execute() {
		try {
			createSandbox();
			if (!sandboxDir.exists()) sandboxDir.mkdirs();
			System.out.println("sandbox dir: " + sandboxDir.getAbsolutePath());
			for (File file: sandboxDir.listFiles()) {
				System.out.println("file: " + file.getAbsolutePath());
				FileUtils.copyFile(file, new File("/var/local/lib/isolate/0/box/" + file.getName()));
			}
			processExecutor.command(buildCommand());
			long hardTimeout = Math.round((2*timeoutInSeconds+1+extraTimeoutInSeconds)*1000);
			processExecutor.timeout(hardTimeout, TimeUnit.MILLISECONDS);
			
			System.out.println("command: " + this);
			ProcessResult processResult = processExecutor.execute();
			
			for (File file: new File("/var/local/lib/isolate/0/box").listFiles()) {
				System.out.println("file: " + file.getAbsolutePath());
				FileUtils.copyFile(file, new File(sandboxDir, file.getName()));
			}
			SandboxResult sandboxResult = new SandboxResult(processResult, sandboxDir, timeoutInSeconds, new File(sandboxDir, error));
			System.out.println(sandboxResult.getResult().getReason());
			return sandboxResult;
		} catch (TimeoutException e) {
			return new SandboxResult(e);
		} catch (Exception e) {
			e.printStackTrace();
			return new SandboxResult(e);
		} finally {
			try {
				destroySandbox();
				if (clean) {
					FileUtils.deleteQuietly(sandboxDir);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public SandboxExecutor readOutput(boolean readOutput) {
		processExecutor.readOutput(readOutput);
		return this;
	}

	protected List<String> buildCommand() {
		List<String> isolateCommand = getIsolateCommand();
		return isolateCommand;
	}

	protected List<String> getIsolateCommand() {
		List<String> isolateCommand = new ArrayList<>();
		isolateCommand.add("isolate");
		isolateCommand.add("--run");
		if (containerName != null) {
			isolateCommand.add("-b");
			isolateCommand.add(containerName);
		}
		isolateCommand.add("-e");
		isolateCommand.add("-p");
		isolateCommand.add("-d");
		isolateCommand.add("etc");
//		isolateCommand.add("-d");
//		isolateCommand.add("/shared="+sandboxDir);
		isolateCommand.add("-M");
		isolateCommand.add(new File(sandboxDir, "metadata").getAbsolutePath());
		isolateCommand.add("-m");
		isolateCommand.add(String.valueOf(256 * memoryInMB));
		isolateCommand.add("-t");
		isolateCommand.add(String.valueOf(timeoutInSeconds));
		isolateCommand.add("-w");
		isolateCommand.add(String.valueOf(2*timeoutInSeconds+1));
		if (input != null) {
			isolateCommand.add("-i");
			isolateCommand.add(input);
		}
		isolateCommand.add("-o");
		isolateCommand.add(output);
		isolateCommand.add("-r");
		isolateCommand.add(error);
		isolateCommand.add("--");
		isolateCommand.addAll(userCommand);
		return isolateCommand;
	}
	
	@Override
	public String toString() {
		return String.join(" ", getIsolateCommand());
	}
	
}
