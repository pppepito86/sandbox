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
	protected int boxId = 0;
	protected double timeoutInSeconds = 5.0;
	protected double extraTimeoutInSeconds = 1.0;
	protected Integer memoryInMB = 256;
	protected String input = null;
	protected String output = "output";
	protected String error = "error";
	protected boolean clean = false;
	protected boolean trusted = false;
	protected boolean showError = false;
	protected double ioTimeoutInSeconds = 0;
	protected String extraMetadata = "extra_metadata";
	
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
	
	public SandboxExecutor ioTimeout(double ioTime) {
		this.ioTimeoutInSeconds = ioTime;
		return this;
	}
	
	public SandboxExecutor memory(Integer memory) {
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

	public SandboxExecutor trusted(boolean trusted) {
		this.trusted = trusted;
		return this;
	}
	
	public SandboxExecutor showError() {
		this.showError = true;
		return this;
	}
	
	public SandboxExecutor clean(boolean clean) {
		this.clean = clean;
		return this;
	}
	
	public SandboxExecutor name(int boxId) {
		this.boxId = boxId;
		return this;
	}
	
	public void createSandbox() throws Exception {
		new ProcessExecutor().command("isolate", "--box-id="+boxId, "--cg", "--init").execute();
	}
	
	public void destroySandbox() throws Exception {
		new ProcessExecutor().command("isolate", "--box-id="+boxId, "--cg", "--cleanup").execute();
	}
	
	public SandboxResult execute() {
		try {
			createSandbox();
			if (!sandboxDir.exists()) sandboxDir.mkdirs();
			new ProcessExecutor("chmod", "-R", "777", sandboxDir.getAbsolutePath()).execute();
			System.out.println("sandbox dir: " + sandboxDir.getAbsolutePath());

			processExecutor.command(buildCommand());
//			processExecutor.directory(sandboxDir);
			long hardTimeout = Math.round((2*timeoutInSeconds+ioTimeoutInSeconds+1+extraTimeoutInSeconds)*1000);
			processExecutor.timeout(hardTimeout, TimeUnit.MILLISECONDS);
			
			System.out.println("command: " + this);
			ProcessResult processResult = processExecutor.execute();
			
//			for (File file: new File("/var/local/lib/isolate/0/box").listFiles()) {
//				System.out.println("file: " + file.getAbsolutePath());
//				FileUtils.copyFile(file, new File(sandboxDir, file.getName()));
//			}
			if (showError) {
				return new SandboxResult(processResult, sandboxDir, timeoutInSeconds, memoryInMB, new File(sandboxDir, error), ioTimeoutInSeconds);
			} else {
				return new SandboxResult(processResult, sandboxDir, timeoutInSeconds, memoryInMB, null, ioTimeoutInSeconds);
			}
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
		
		isolateCommand.add("--box-id="+boxId);
		
		isolateCommand.add("--cg");
		isolateCommand.add("--cg-timing");
		
		isolateCommand.add("--chdir=/tmp");
		
		isolateCommand.add("--dir=/etc");
		isolateCommand.add("--dir=/tmp="+sandboxDir+":rw");
		
		if (input != null) {
			isolateCommand.add("--stdin=/tmp/"+input);
		}
		isolateCommand.add("--stdout=/tmp/"+output);
		isolateCommand.add("--stderr=/tmp/"+error);
		
		isolateCommand.add("--meta="+new File(sandboxDir, "metadata").getAbsolutePath());
		
		isolateCommand.add("--fsize="+(1<<20));
		isolateCommand.add("--processes="+(trusted?1000:1));
		
		if (trusted) {
			isolateCommand.add("-e");
		}
		
		double sandboxTime = timeoutInSeconds + ioTimeoutInSeconds;

		isolateCommand.add("--time="+sandboxTime);
		isolateCommand.add("--wall-time="+(sandboxTime+timeoutInSeconds+1));
		isolateCommand.add("--extra-time="+(sandboxTime+Math.min(timeoutInSeconds/2, 0.5)));

		if (memoryInMB != null) {
			isolateCommand.add("--cg-mem="+(1024 * (memoryInMB+5)));
		}
		
		isolateCommand.add("--run");
		
		isolateCommand.add("--");
		isolateCommand.addAll(userCommand);
		if (ioTimeoutInSeconds != 0) isolateCommand.add(extraMetadata);
		
		return isolateCommand;
	}
	
	@Override
	public String toString() {
		return String.join(" ", getIsolateCommand());
	}
	
}
