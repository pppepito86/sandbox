import java.io.File;

import org.pesho.judge.SandboxExecutor;
import org.pesho.judge.SandboxResult;

public class Main {

	public static void main(String[] args) throws Exception {
		SandboxResult result = new SandboxExecutor()
				.directory(new File("."))
				.command(args[0])
				.execute();
		System.out.println(result.getResult());
	}

}