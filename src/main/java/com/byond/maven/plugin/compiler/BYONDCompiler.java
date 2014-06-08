package com.byond.maven.plugin.compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.plexus.compiler.AbstractCompiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.compiler.CompilerMessage;
import org.codehaus.plexus.compiler.CompilerMessage.Kind;
import org.codehaus.plexus.compiler.CompilerOutputStyle;
import org.codehaus.plexus.compiler.CompilerResult;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.StreamConsumer;
import org.codehaus.plexus.util.cli.WriterStreamConsumer;

public final class BYONDCompiler extends AbstractCompiler {

	private static final Pattern COMPILER_MESSAGE_EXPRESSION = Pattern.compile("^([^:]+):(\\d+):(error|warning):\\w+(.+)$");
	
	public BYONDCompiler() {
		super(CompilerOutputStyle.ONE_OUTPUT_FILE_FOR_ALL_INPUT_FILES, ".dm", ".dmb", null);
	}
	
	@Override
	public String[] createCommandLine(CompilerConfiguration config) throws CompilerException { 
		return getSourceFiles(config);
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
        
        List<CompilerMessage> messages = compileOutOfProcess(config.getWorkingDirectory(), config.getBuildDirectory(), config.getOutputFileName(), findExecutable(config), args);
        CompilerResult result = new CompilerResult(true, messages);        
        return result;
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
		List<CompilerMessage> messages = new ArrayList<CompilerMessage>();

		try {
			file = new File(target, targetName + ".dme");
			output = new PrintWriter(new FileWriter(file));
			for (String arg : args) {
				output.println("#include \"" + arg + "\"");
	        }
		} catch (IOException e) {
			throw new CompilerException("Error creating DME file.", e);
	    } finally {
	    	IOUtil.close(output);
	    }
		
		Commandline cli = new Commandline();
		cli.setWorkingDirectory(workingDirectory.getAbsolutePath());
        cli.setExecutable(executable);
        cli.createArg().setValue(file.getAbsolutePath());
        
        Writer stringWriter = new StringWriter();
        StreamConsumer out = new WriterStreamConsumer(stringWriter);
        StreamConsumer err = new WriterStreamConsumer(stringWriter);
        
        try {
			CommandLineUtils.executeCommandLine(cli, out, err);
			messages = parseCompilerOutput(new BufferedReader(new StringReader(stringWriter.toString())));
		} catch (CommandLineException e) {
			messages.add(new CompilerMessage("Could not execute DM compiler process: " + e.getLocalizedMessage(), Kind.ERROR));
		}
        
		return messages;
	}

	private List<CompilerMessage> parseCompilerOutput(BufferedReader bufferedReader) {
		List<CompilerMessage> messages = new ArrayList<CompilerMessage>();
		String line = null;
		try {
			while ((line = bufferedReader.readLine()) != null) {
				Matcher matcher = COMPILER_MESSAGE_EXPRESSION.matcher(line);
				if (matcher.matches()) {
					int lineNo = Integer.parseInt(matcher.group(2));
					CompilerMessage message = new CompilerMessage(matcher.group(1), convertMessageType(matcher.group(3)), lineNo, 0, lineNo, 0, matcher.group(4));
					messages.add(message);
				}
			}
		} catch (IOException e) {
			messages.add(new CompilerMessage("Could not read DM compiler process output: " + e.getLocalizedMessage(), Kind.ERROR));
		}
		return messages;
	}
	
	private Kind convertMessageType(String type) {
		if (type.equalsIgnoreCase("warning")) {
			return Kind.WARNING;
		}
		if (type.equalsIgnoreCase("error")) {
			return Kind.ERROR;
		}
		return Kind.OTHER;
	}
}
