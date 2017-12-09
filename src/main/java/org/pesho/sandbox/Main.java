package  org.pesho.sandbox;

import java.io.File;

public class Main {

	public static void main(String[] args) throws Exception {
		SandboxResult result = new SandboxExecutor()
				.command(args[0])
				.execute();
		System.out.println(result.getResult());
	}

}