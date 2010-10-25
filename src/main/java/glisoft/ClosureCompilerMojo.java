package glisoft;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.jscomp.Result;

/**
 * @goal compile
 * @phase generate-sources
 */
public class ClosureCompilerMojo extends AbstractMojo {

	/**
	 * @parameter expression="WHITESPACE_ONLY"
	 * @required
	 */
	private String compilationLevel;

	/**
	 * @parameter expression="src/main/webapp/js"
	 * @required
	 */
	private File externsSourceDirectory;

	/**
	 * @parameter expression="src/main/js"
	 * @required
	 */
	private File sourceDirectory;

	/**
	 * @parameter expression=
	 *            "${project.build.directory}/${project.artifactId}-${project.version}/js/${project.artifactId}.js"
	 * @required
	 */
	private File outputFile;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		CompilationLevel compilationLevel = null;
		try {
			compilationLevel = CompilationLevel.valueOf(this.compilationLevel);
		} catch (IllegalArgumentException e) {
			throw new MojoFailureException("Compilation level invalid !", e);
		}

		CompilerOptions compilerOptions = new CompilerOptions();
		compilationLevel.setOptionsForCompilationLevel(compilerOptions);

		Compiler compiler = new Compiler();
		Result result = compiler.compile(listJSSourceFiles(externsSourceDirectory), listJSSourceFiles(sourceDirectory), compilerOptions);
		if (!result.success) {
			throw new MojoFailureException(result.debugLog);
		}

		try {
			Files.createParentDirs(outputFile);
			Files.touch(outputFile);
			Files.write(compiler.toSource(), outputFile, Charsets.UTF_8);
		} catch (IOException e) {
			throw new MojoFailureException(outputFile != null ? outputFile.toString() : e.getMessage(), e);
		}
	}

	private List<JSSourceFile> listJSSourceFiles(File directory) {
		return listJSSourceFiles(new ArrayList<JSSourceFile>(), directory);
	}

	private List<JSSourceFile> listJSSourceFiles(List<JSSourceFile> jsSourceFiles, File directory) {
		if (directory != null) {
			File[] files = directory.listFiles();
			if (files != null) {
				for (File file : directory.listFiles()) {
					if (file.isFile()) {
						if (file.getName().endsWith(".js")) {
							jsSourceFiles.add(JSSourceFile.fromFile(file));
						}
					} else {
						listJSSourceFiles(jsSourceFiles, file);
					}
				}
			}
		}
		return jsSourceFiles;
	}

}
