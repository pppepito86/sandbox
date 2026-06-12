package org.pesho.sandbox;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.commons.io.FileUtils;
import org.zeroturnaround.exec.ProcessExecutor;

/**
 * Builds a minimal, curated /etc to bind into the compile sandbox instead of the host /etc.
 *
 * The compile sandbox returns the compiler's diagnostics to the contestant, so binding the whole
 * host /etc lets a submission do  #include "/etc/&lt;file&gt;"  and exfiltrate that file's contents
 * through the compile-error message. Here we copy only the entries the C/C++ toolchain genuinely
 * needs (dynamic-linker config plus the update-alternatives symlinks that resolve /usr/bin/g++), so
 * that an #include of anything else under /etc resolves to nothing. None of the whitelisted entries
 * contain secrets.
 *
 * If the curated copy cannot be built, {@link #getDir()} returns {@code null} and the caller falls
 * back to the host /etc (fail-open: compilation keeps working, but the disclosure is NOT mitigated).
 * The fallback is logged loudly so an operator can notice and fix the environment. Switch to
 * fail-closed (return an empty dir) if you would rather block compilation than risk the leak.
 */
public final class MinimalEtc {

	// Entries copied from /etc. None of these contain secrets.
	private static final String[] WHITELIST = {
		"ld.so.cache",   // dynamic-linker cache, consulted while linking
		"ld.so.conf",    // linker search-path config
		"ld.so.conf.d",  // linker search-path config fragments
		"alternatives",  // update-alternatives symlinks: /usr/bin/g++ -> /etc/alternatives/g++ -> /usr/bin/g++-NN
		"localtime",     // harmless; avoids TZ warnings from the JVM/toolchain
		"nsswitch.conf", // harmless name-service config some tools stat()
		"locale.alias",  // glibc locale alias table, consulted by setlocale()
	};

	private static volatile String dir = null;
	private static volatile boolean attempted = false;

	/** Absolute path of the curated /etc, or {@code null} if it could not be built (use host /etc). */
	public static String getDir() {
		if (attempted) return dir;
		return build();
	}

	private static synchronized String build() {
		if (attempted) return dir;
		attempted = true;
		try {
			Path src = Paths.get("/etc");
			// Unique per JVM so two processes on one host cannot clobber each other's copy.
			String unique = ManagementFactory.getRuntimeMXBean().getName().replaceAll("[^a-zA-Z0-9]", "_");
			Path dst = Paths.get(System.getProperty("java.io.tmpdir", "/tmp"), "bos-sandbox-etc-" + unique);
			FileUtils.deleteQuietly(dst.toFile());
			Files.createDirectories(dst);

			int copied = 0;
			for (String name : WHITELIST) {
				Path from = src.resolve(name);
				if (!Files.exists(from, LinkOption.NOFOLLOW_LINKS)) continue;
				copyTree(from, dst.resolve(name));
				copied++;
			}
			if (copied == 0) throw new IOException("no whitelisted /etc entries found under " + src);

			// Let the unprivileged sandbox user traverse/read the copy. chmod -R does not dereference
			// symlinks found during the walk, so the alternatives symlinks (and their host targets
			// under /usr) are left untouched.
			new ProcessExecutor().command("chmod", "-R", "a+rX", dst.toAbsolutePath().toString()).execute();

			dir = dst.toAbsolutePath().toString();
			System.out.println("Sandbox compile /etc: using curated " + dir);
			return dir;
		} catch (Exception e) {
			dir = null;
			System.err.println("WARNING: could not build curated sandbox /etc; falling back to host /etc. "
				+ "Compile-time file disclosure (#include \"/etc/...\") is NOT mitigated: " + e.getMessage());
			e.printStackTrace();
			return null;
		}
	}

	private static void copyTree(Path from, Path to) throws IOException {
		if (Files.isSymbolicLink(from)) {
			Files.createDirectories(to.getParent());
			Path target = Files.readSymbolicLink(from);
			Files.deleteIfExists(to);
			Files.createSymbolicLink(to, target);
		} else if (Files.isDirectory(from, LinkOption.NOFOLLOW_LINKS)) {
			Files.createDirectories(to);
			try (DirectoryStream<Path> ds = Files.newDirectoryStream(from)) {
				for (Path child : ds) copyTree(child, to.resolve(child.getFileName()));
			}
		} else {
			Files.createDirectories(to.getParent());
			Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
		}
	}

	private MinimalEtc() {}
}
