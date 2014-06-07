package com.byond.maven.plugin.compiler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.cli.Commandline;

public final class BYONDCompiler extends AbstractCompiler {

	public BYONDCompiler() {
		super(CompilerOutputStyle.ONE_OUTPUT_FILE_FOR_ALL_INPUT_FILES, ".dm", ".dmb", null);
	}
	
	@Override
	public String[] createCommandLine(CompilerConfiguration config) throws CompilerException {
		String[] sourceFiles = getSourceFiles(config);
		return null;
	}
	
	public String getOutputFile(CompilerConfiguration config) throws CompilerException {
		return config.getOutputFileName() + getOutputFileEnding(config);
	}
	
	public CompilerResult performCompile(CompilerConfiguration config) throws CompilerException {
		File destinationDir = new File(config.getOutputLocation());
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }
        
        String[] sourceFiles = getSourceFiles(config);
        
        if (sourceFiles.length == 0) {
            return new CompilerResult();
        }
        
        String[] args = createCommandLine(config);
        
        compileOutOfProcess(config.getWorkingDirectory(), config.getBuildDirectory(), config.getOutputFileName(), findExecutable(config), args);
        
        return null;
	}
	
	private String findExecutable(CompilerConfiguration config) {
        String executable = config.getExecutable();
        if (executable != null && !executable.isEmpty()) {
            return executable;
        }
        if (Os.isFamily("windows")) {
            return "dm.exe";
        }
        return "DreamMaker";
    }
	
	private List<CompilerMessage> compileOutOfProcess(File workingDirectory, File target, String targetName, String executable, String[] args) throws CompilerException {
		File file;
		PrintWriter output = null;

		try {
			file = new File(target, targetName + ".dme");
			output = new PrintWriter(new FileWriter(file));
			for (String arg : args) {
				output.println(arg);
	        }
		} catch (IOException e) {
			throw new CompilerException("Error DME file.", e);
	    } finally {
	    	IOUtil.close(output);
	    }
		
		Commandline cli = new Commandline();
		cli.setWorkingDirectory(workingDirectory.getAbsolutePath());
        cli.setExecutable(executable);
        cli.createArg().setValue(file.getAbsolutePath());
        
		return null;
	}
}
