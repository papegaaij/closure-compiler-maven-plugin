package com.github.urmuzov.closurecompilermavenplugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;

/**
 * @goal compile
 * @phase generate-sources
 */
public class ClosureCompilerMojo extends AbstractMojo
{

	/**
	 * @parameter expression="WARNING"
	 * @required
	 */
	private String loggingLevel;

	/**
	 * @parameter expression="WHITESPACE_ONLY"
	 * @required
	 */
	private String compilationLevel;

	/**
	 * @parameter expression="VERBOSE"
	 * @required
	 */
	private String warningLevel;

	/**
	 * @parameter expression="null"
	 * @required
	 */
	private String formatting;

	/**
	 * @parameter expression="false"
	 * @required
	 */
	private boolean manageClosureDependencies;

	/**
	 * @parameter expression="false"
	 * @required
	 */
	private boolean generateExports;

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

	/**
	 * @parameter expression="false"
	 * @required
	 */
	private boolean logSourceFiles;

	/**
	 * @parameter expression="false"
	 * @required
	 */
	private boolean logExternFiles;

	/**
	 * @parameter expression="false"
	 * @required
	 */
	private boolean addDefaultExterns;

	/**
	 * @parameter expression="false"
	 * @required
	 */
	private boolean stopOnWarnings;

	/**
	 * @parameter expression="false"
	 * @required
	 */
	private boolean stopOnErrors;

	@Override
	public void execute() throws MojoFailureException
	{
		CompilerOptions compilerOptions = new CompilerOptions();

		CompilationLevel compilationLvl = null;
		try
		{
			compilationLvl = CompilationLevel.valueOf(this.compilationLevel);
			compilationLvl.setOptionsForCompilationLevel(compilerOptions);
		}
		catch (IllegalArgumentException e)
		{
			throw new MojoFailureException("Compilation level invalid (values: "
				+ Arrays.asList(CompilationLevel.values()).toString() + ")", e);
		}

		WarningLevel warningLvl = null;
		try
		{
			warningLvl = WarningLevel.valueOf(this.warningLevel);
			warningLvl.setOptionsForWarningLevel(compilerOptions);
		}
		catch (IllegalArgumentException e)
		{
			throw new MojoFailureException("Warning level invalid (values: "
				+ Arrays.asList(WarningLevel.values()).toString() + ")", e);
		}

		compilerOptions.setManageClosureDependencies(manageClosureDependencies);

		compilerOptions.setGenerateExports(generateExports);

		FormattingOption formattingOption = null;
		if (this.formatting != null && !this.formatting.equals("null"))
		{
			try
			{
				formattingOption = FormattingOption.valueOf(this.formatting);
				formattingOption.applyToOptions(compilerOptions);
			}
			catch (IllegalArgumentException e)
			{
				throw new MojoFailureException("Formatting invalid (values: "
					+ Arrays.asList(FormattingOption.values()).toString() + ")", e);
			}
		}

		Compiler compiler = new Compiler();

		Level loggingLvl = null;
		try
		{
			loggingLvl = Level.parse(this.loggingLevel);
			Compiler.setLoggingLevel(loggingLvl);
		}
		catch (IllegalArgumentException e)
		{
			throw new MojoFailureException(
				"Logging level invalid (values: [ALL, CONFIG, FINE, FINER, FINEST, INFO, OFF, SEVERE, WARNING])",
				e);
		}

		List<JSSourceFile> externs = new ArrayList<JSSourceFile>();
		if (addDefaultExterns)
		{
			try
			{
				externs.addAll(CommandLineRunner.getDefaultExterns());
			}
			catch (IOException ex)
			{
				throw new MojoFailureException("Default externs adding error");
			}
		}
		externs.addAll(listJSSourceFiles(externsSourceDirectory));
		if (logExternFiles)
		{
			getLog().info("Extern files:");
			for (JSSourceFile f : externs)
			{
				getLog().info(f.getOriginalPath());
			}
		}

		List<JSSourceFile> sources = listJSSourceFiles(sourceDirectory);
		if (logSourceFiles)
		{
			getLog().info("Source files:");
			for (JSSourceFile f : sources)
			{
				getLog().info(f.getOriginalPath());
			}
		}

		Result result = compiler.compile(externs, sources, compilerOptions);

		boolean hasWarnings = false;
		for (JSError warning : result.warnings)
		{
			getLog().warn(warning.toString());
			hasWarnings = true;
		}

		boolean hasErrors = false;
		for (JSError error : result.errors)
		{
			getLog().error(error.toString());
			hasErrors = true;
		}
		if (stopOnWarnings && hasWarnings)
		{
			throw new MojoFailureException("Compilation faied: has warnings");
		}
		if (stopOnErrors && hasErrors)
		{
			throw new MojoFailureException("Compilation faied: has errors");
		}

		if (!result.success)
		{
			throw new MojoFailureException("Compilation failure");
		}

		try
		{
			Files.createParentDirs(outputFile);
			Files.touch(outputFile);
			Files.write(compiler.toSource(), outputFile, Charsets.UTF_8);
		}
		catch (IOException e)
		{
			throw new MojoFailureException(outputFile != null ? outputFile.toString()
				: e.getMessage(), e);
		}
	}

	private List<JSSourceFile> listJSSourceFiles(File directory)
	{
		return listJSSourceFiles(new ArrayList<JSSourceFile>(), directory);
	}

	private List<JSSourceFile> listJSSourceFiles(List<JSSourceFile> jsSourceFiles, File directory)
	{
		if (directory != null)
		{
			File[] files = directory.listFiles();
			if (files != null)
			{
				for (File file : directory.listFiles())
				{
					if (file.isFile())
					{
						if (file.getName().endsWith(".js"))
						{
							jsSourceFiles.add(JSSourceFile.fromFile(file));
						}
					}
					else
					{
						listJSSourceFiles(jsSourceFiles, file);
					}
				}
			}
		}
		return jsSourceFiles;
	}

	private static enum FormattingOption
	{

		PRETTY_PRINT,
		PRINT_INPUT_DELIMITER, ;

		private void applyToOptions(CompilerOptions options)
		{
			switch (this)
			{
				case PRETTY_PRINT:
					options.prettyPrint = true;
					break;
				case PRINT_INPUT_DELIMITER:
					options.printInputDelimiter = true;
					break;
				default:
					throw new RuntimeException("Unknown formatting option: " + this);
			}
		}
	}
}
