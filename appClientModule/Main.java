import org.pesho.judge.SandboxExecutor;
import org.pesho.judge.SandboxResult;

public class Main {

	public static void main(String[] args) throws Exception {
		
		SandboxResult result = new SandboxExecutor().readOutput(true).command(args[0])
                .timeout(Double.valueOf(args[1])).execute();
		System.out.println(result.getResult());
		System.out.println(result.getTime());
	}

}