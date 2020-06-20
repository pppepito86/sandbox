package org.pesho;

import static org.hamcrest.CoreMatchers.is;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.pesho.sandbox.SandboxExecutor;

public class CommandTest {

//	@Test
	public void test() {
		SandboxExecutor command = new SandboxExecutor()
			.directory(new File("testdir"))
			.input("input")
			.output("output")
			.timeout(1.5)
			.memory(100)
			.command("./solution");
		System.out.println(command.toString());
		String expected = "isolate --run -d /shared=C:\\Users\\Petar\\sts-workspace\\sandbox\\testdir -M metadata -m 25600 -t 1.5 -w 4.0 -i /shared/input -o output -r error -- /shared/./solution";
		Assert.assertThat(command.toString(), is(expected));
	}
	
}
