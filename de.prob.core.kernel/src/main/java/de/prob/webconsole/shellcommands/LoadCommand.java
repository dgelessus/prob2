package de.prob.webconsole.shellcommands;

import groovy.lang.Binding;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jline.FileNameCompletor;

import com.google.common.base.Joiner;

import de.prob.webconsole.GroovyExecution;

public class LoadCommand extends AbstractShellCommand {

	private final static Logger LOGGER = LoggerFactory
			.getLogger(LoadCommand.class);

	@Override
	public Object perform(List<String> m, GroovyExecution exec)
			throws IOException {

		if (m.size() != 2) {
			String msg = "Load commend takes exactly one parameter, a filename.";
			LOGGER.error(msg);
			return "error: " + msg;
		}

		String filename = m.get(1);

		if (filename.startsWith("~")) {
			filename = filename.replaceFirst("~",
					System.getProperty("user.home"));
		}

		int lastDot = filename.lastIndexOf('.');

		File f = new File(filename);
		if (!f.exists()) {
			String msg = "File '" + filename + "' does not exist.";
			LOGGER.error(msg);
			return "error: " + msg;
		}

		String extension = filename.substring(lastDot, filename.length());

		if (extension.equals(".mch") || extension.equals(".ref")
				|| extension.equals(".imp")) {
			String name = freshVar(exec, "model_");
			exec.evaluate(name + " = api.b_load('" + filename + "')");
			Object model = exec.getBindings().getVariable(name);
			if (model != null) {
				return name + " = " + model;
			}
			return model;
		}
		if (extension.equals(".eventb")) {
			String name = freshVar(exec, "model_");
			exec.evaluate(name + " = api.eb_load('" + filename + "')");
			Object model = exec.getBindings().getVariable(name);
			if (model != null) {
				return name + " = " + model;
			}
			return model;
		}
		String msg = "Unknown filetype: " + extension;
		LOGGER.error(msg);
		return "error: " + msg;
	}

	private synchronized String freshVar(GroovyExecution exec, String string) {
		String v;
		Binding bindings = exec.getBindings();
		do {
			v = string + exec.nextCounter();
		} while (bindings.hasVariable(v));
		bindings.setVariable(v, null);
		return v;
	}

	@Override
	public List<String> complete(List<String> args, int pos) {
		ArrayList<String> suggestions = new ArrayList<String>();
		ArrayList<String> s = new ArrayList<String>();
		FileNameCompletor completor = new FileNameCompletor();
		String input = Joiner.on(" ").join(args);
		completor.complete(input, pos, suggestions);
		if (suggestions.isEmpty())
			return suggestions;
		int lastSlash = input.lastIndexOf(File.separator);
		if (lastSlash > -1) {
			String prefix = input.substring(0, lastSlash);
			String suffix = input.substring(lastSlash+1);
			String commonPrefix = findLongestCommonPrefix(suggestions);
			if (commonPrefix.isEmpty() || commonPrefix.equals(suffix)) {
				for (String string : suggestions) {
					s.add(string);
				}
			} else
				s.add(prefix + File.separator +commonPrefix);
		}
		if (s.size() == 1) {
			return Collections.singletonList("load " + s.get(0));
		}

		return s;
	}

	private String greatestCommonPrefix(String a, String b) {
		int minLength = Math.min(a.length(), b.length());
		for (int i = 0; i < minLength; i++) {
			if (a.charAt(i) != b.charAt(i)) {
				return a.substring(0, i);
			}
		}
		return a.substring(0, minLength);
	}

	private String findLongestCommonPrefix(ArrayList<String> suggestions) {
		if (suggestions.size() == 1)
			return suggestions.get(0);
		String res = greatestCommonPrefix(suggestions.get(0),
				suggestions.get(1));
		for (int i = 2; i < suggestions.size(); i++) {
			res = greatestCommonPrefix(res, suggestions.get(i));
		}
		return res;
	}
}