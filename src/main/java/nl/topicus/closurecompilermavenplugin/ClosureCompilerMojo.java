package nl.topicus.closurecompilermavenplugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.DirectoryScanner;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
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
	 * @parameter expression="SIMPLE_OPTIMIZATIONS"
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

	// @formatter:off
	/**
	 * @parameter expression="${project.build.directory}/${project.artifactId}-${project.version}/js/${project.artifactId}.js"
	 * @required
	 */
	private File outputFile;
	// @formatter:on

	/**
	 * @parameter expression=true
	 * @required
	 */
	private boolean merge;

	/**
	 * @parameter expression="${project.build.outputDirectory}"
	 * @required
	 */
	private File outputDirectory;

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
	 * @parameter expression="true"
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

		parseCompilationLevel(compilerOptions);
		parseWarningLevel(compilerOptions);
		compilerOptions.setManageClosureDependencies(manageClosureDependencies);
		compilerOptions.setGenerateExports(generateExports);
		parseFormattingOptions(compilerOptions);
		parseLoggingLevel();

		List<JSSourceFile> externs = parseExterns();
		List<String> sources = parseSources();

		try
		{
			if (merge)
			{
				String source =
					compile(compilerOptions, externs,
						filenamesToSourceFiles(sourceDirectory, sources));
				Files.createParentDirs(outputFile);
				Files.touch(outputFile);
				Files.write(source, outputFile, Charsets.UTF_8);
			}
			else
			{
				for (String curSourceFile : sources)
				{
					String source =
						compile(compilerOptions, externs, ImmutableList.of(JSSourceFile
							.fromFile(new File(sourceDirectory, curSourceFile))));
					File curOuputFile = sourceToDest(curSourceFile);
					Files.createParentDirs(curOuputFile);
					Files.touch(curOuputFile);
					Files.write(source, curOuputFile, Charsets.UTF_8);
				}
			}
		}
		catch (IOException e)
		{
			throw new MojoFailureException(e.getMessage(), e);
		}
	}

	private File sourceToDest(String sourceFile)
	{
		return new File(outputDirectory, sourceFile.substring(0, sourceFile.length() - 3)
			+ ".min.js");
	}

	protected String compile(CompilerOptions compilerOptions, List<JSSourceFile> externs,
			List<JSSourceFile> sources) throws MojoFailureException
	{
		Compiler compiler = new Compiler();
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
		String source = compiler.toSource();
		return source;
	}

	protected void parseCompilationLevel(CompilerOptions compilerOptions)
			throws MojoFailureException
	{
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
	}

	protected void parseWarningLevel(CompilerOptions compilerOptions) throws MojoFailureException
	{
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
	}

	protected void parseFormattingOptions(CompilerOptions compilerOptions)
			throws MojoFailureException
	{
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
	}

	protected void parseLoggingLevel() throws MojoFailureException
	{
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
	}

	protected List<JSSourceFile> parseExterns() throws MojoFailureException
	{
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
		externs.addAll(filesToSourceFiles(listFiles(externsSourceDirectory)));
		if (logExternFiles)
		{
			getLog().info("Extern files:");
			for (JSSourceFile f : externs)
			{
				getLog().info(f.getOriginalPath());
			}
		}
		return externs;
	}

	protected List<String> parseSources()
	{
		DirectoryScanner scanner = new DirectoryScanner();
		scanner.setBasedir(sourceDirectory);
		scanner.setIncludes(new String[] {"**/*.js"});
		scanner.addDefaultExcludes();
		scanner.scan();

		List<String> sources = ImmutableList.copyOf(scanner.getIncludedFiles());
		if (logSourceFiles)
		{
			getLog().info("Source files:");
			for (String f : sources)
			{
				getLog().info(f);
			}
		}
		return sources;
	}

	private List<JSSourceFile> filesToSourceFiles(List<File> files)
	{
		return Lists.transform(files, new Function<File, JSSourceFile>()
		{
			@Override
			public JSSourceFile apply(File input)
			{
				return JSSourceFile.fromFile(input);
			}
		});
	}

	private List<JSSourceFile> filenamesToSourceFiles(final File path, List<String> filenames)
	{
		return Lists.transform(filenames, new Function<String, JSSourceFile>()
		{
			@Override
			public JSSourceFile apply(String input)
			{
				return JSSourceFile.fromFile(new File(path, input));
			}
		});
	}

	private List<File> listFiles(File directory)
	{
		return listFiles(new ArrayList<File>(), directory);
	}

	private List<File> listFiles(List<File> jsSourceFiles, File directory)
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
							jsSourceFiles.add(file);
						}
					}
					else
					{
						listFiles(jsSourceFiles, file);
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
