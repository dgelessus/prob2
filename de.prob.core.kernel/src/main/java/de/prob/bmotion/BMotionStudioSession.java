package de.prob.bmotion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.servlet.AsyncContext;

import org.eclipse.jetty.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.inject.Inject;

import de.be4.classicalb.core.parser.exceptions.BException;
import de.prob.animator.domainobjects.CSP;
import de.prob.animator.domainobjects.ClassicalB;
import de.prob.animator.domainobjects.ComputationNotCompletedResult;
import de.prob.animator.domainobjects.EvalResult;
import de.prob.animator.domainobjects.EventB;
import de.prob.animator.domainobjects.IEvalElement;
import de.prob.animator.domainobjects.IEvalResult;
import de.prob.model.classicalb.ClassicalBModel;
import de.prob.model.eventb.EventBModel;
import de.prob.model.representation.AbstractModel;
import de.prob.model.representation.CSPModel;
import de.prob.scripting.ScriptEngineProvider;
import de.prob.statespace.AnimationSelector;
import de.prob.statespace.IAnimationChangeListener;
import de.prob.statespace.IModelChangedListener;
import de.prob.statespace.StateSpace;
import de.prob.statespace.Trace;
import de.prob.web.AbstractSession;
import de.prob.web.WebUtils;

public class BMotionStudioSession extends AbstractSession implements
		IAnimationChangeListener, IModelChangedListener {

	Logger logger = LoggerFactory.getLogger(BMotionStudioSession.class);

	private Trace currentTrace;

	private final AnimationSelector selector;

	private String template;

	private final ScriptEngine groovy;

	private final Map<String, Object> parameterMap = new HashMap<String, Object>();

	private final Map<String, Object> formulas = new HashMap<String, Object>();

	private final Map<String, String> cachedCSPString = new HashMap<String, String>();

	private final List<Object> jsonData = new ArrayList<Object>();

	private final Observer observer;

	// private String[] eventbExtensions = { "buc", "bcc", "bcm", "bum" };
	// private String[] classicalBExtensions = { "mch" };

	private final List<IBMotionScript> scriptListeners = new ArrayList<IBMotionScript>();

	@Inject
	public BMotionStudioSession(final AnimationSelector selector,
			final ScriptEngineProvider sep) {
		this.selector = selector;
		incrementalUpdate = false;
		currentTrace = selector.getCurrentTrace();
		groovy = sep.get();
		observer = new Observer(this);
		selector.registerAnimationChangeListener(this);
	}

	@Override
	public String html(final String clientid,
			final Map<String, String[]> parameterMap) {
		return null;
	}

	// public Object eval(final Map<String, String[]> params) {
	//
	// String formula = params.get("formula")[0];
	// String callback = params.get("callback")[0];
	//
	// String data = null;
	// String[] dataPara = params.get("data");
	// if (dataPara != null)
	// data = dataPara[0];
	//
	// Object parse = JSON.parse(formula);
	//
	// Map<String, String> wrap = WebUtils.wrap("cmd", callback);
	//
	// if (parse instanceof Object[]) {
	// Map<String, Object> tmp = new HashMap<String, Object>();
	// Object[] oa = (Object[]) parse;
	// for (Object o : oa) {
	// String f = o.toString();
	// Object value = translateValue(registerFormula(f));
	// tmp.put(f, value);
	// }
	// wrap.put("result", WebUtils.toJson(tmp));
	// } else {
	// Object value = translateValue(registerFormula(parse.toString()));
	// wrap.put("result", value.toString());
	//
	// }
	//
	// if (data != null)
	// wrap.put("data", data);
	//
	// return wrap;
	//
	// }

	public Object executeOperation(final Map<String, String[]> params) {
		String op = params.get("op")[0];
		String predicate = params.get("predicate")[0];
		if (predicate.isEmpty()) {
			predicate = "1=1";
		}
		Trace currentTrace = selector.getCurrentTrace();
		try {
			Trace newTrace = currentTrace.add(op, predicate);
			selector.replaceTrace(currentTrace, newTrace);
		} catch (BException e) {
			e.printStackTrace();
		}
		return null;
	}

	public Object setTemplate(final Map<String, String[]> params) {
		String fullTemplatePath = params.get("path")[0];
		submit(WebUtils.wrap("cmd", "bms.setTemplate", "request",
				fullTemplatePath));
		return null;
	}

	@Override
	public void reload(final String client, final int lastinfo,
			final AsyncContext context) {

		// Remove all script listeners and add new observer scriptlistener
		scriptListeners.clear();
		scriptListeners.add(observer);
		// After reload do not resent old messages
		int old = responses.size() + 1;
		// Add dummy message
		submit(WebUtils.wrap("cmd", "extern.skip"));
		// Initialize json data (if not already done)
		initJsonData();
		// Init Groovy scripts
		initGroovy();
		// Trigger an init trace change
		if (currentTrace != null) {
			traceChange(currentTrace);
		}
		// Resent messages from groovy script and init trace change
		if (!responses.isEmpty()) {
			resend(client, old, context);
		}

		super.reload(client, lastinfo, context);

	}

	@Override
	public void traceChange(final Trace trace) {

		currentTrace = trace;

		// Register not yet evaluated formulas (with null value)
		for (Map.Entry<String, Object> entry : formulas.entrySet()) {
			if (entry.getValue() == null) {
				registerFormula(entry.getKey());
			}
		}
		// Collect subscribed values ...
		Map<IEvalElement, IEvalResult> valuesAt = trace.getStateSpace()
				.valuesAt(trace.getCurrentState());
		for (Map.Entry<IEvalElement, IEvalResult> entry : valuesAt.entrySet()) {
			IEvalElement ee = entry.getKey();
			IEvalResult er = entry.getValue();
			if (er instanceof EvalResult) {
				formulas.put(ee.getCode(),
						translateValue(((EvalResult) er).getValue()));
			}
		}
		formulas.putAll(cachedCSPString);

		// Trigger all registered script listeners
		for (IBMotionScript s : scriptListeners) {
			s.traceChange(trace, formulas);
		}

	}

	private void initJsonData() {

		if (template == null) {
			return;
		}

		Map<String, Object> scope = new HashMap<String, Object>();
		scope.put("eval", new EvalExpression());

		String templateFolder = getTemplateFolder();
		Object jsonPaths = parameterMap.get("json");
		if (jsonPaths != null) {
			String[] sp = jsonPaths.toString().split(",");
			for (String s : sp) {
				File f = new File(templateFolder + "/" + s);
				WebUtils.render(f.getPath(), scope);
				String jsonRendered = readFile(f.getPath());
				jsonData.add(JSON.parse(jsonRendered));
			}
		}

	}

	private String readFile(final String filename) {
		String content = null;
		File file = new File(filename); // for ex foo.txt
		try {
			FileReader reader = new FileReader(file);
			char[] chars = new char[(int) file.length()];
			reader.read(chars);
			content = new String(chars);
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}

	private class EvalExpression implements Function<String, Object> {

		@Override
		public Object apply(final String input) {
			String finput = input.replace("\\\\", "\\");
			Object output = translateValue(registerFormula(finput));
			return output;
		}

	}

	public String registerFormula(final String formula) {

		String output = "???";

		formulas.put(formula, null);

		if (currentTrace != null) {
			try {

				IEvalResult evaluationResult = null;
				IEvalElement evalElement = null;

				AbstractModel model = currentTrace.getModel();

				if (model instanceof EventBModel
						|| model instanceof ClassicalBModel) {

					if (model instanceof ClassicalBModel) {
						evalElement = new ClassicalB(formula);
					} else if (model instanceof EventBModel) {
						evalElement = new EventB(formula);
					}

					StateSpace stateSpace = currentTrace.getStateSpace();
					Map<IEvalElement, IEvalResult> valuesAt = stateSpace
							.valuesAt(currentTrace.getCurrentState());
					evaluationResult = valuesAt.get(evalElement);
					if (evaluationResult == null) {
						evaluationResult = currentTrace
								.evalCurrent(evalElement);
						stateSpace.subscribe(this, evalElement);
						// TODO: unscribe!!!
					}

				} else if (model instanceof CSPModel) {
					output = cachedCSPString.get(formula);
					if (output == null) {
						evalElement = new CSP(formula,
								(CSPModel) currentTrace.getModel());
						evaluationResult = currentTrace
								.evalCurrent(evalElement);
					}
				}

				if (evaluationResult != null) {
					if (evaluationResult instanceof ComputationNotCompletedResult) {
						// TODO: do something .....
					} else if (evaluationResult instanceof EvalResult) {
						output = ((EvalResult) evaluationResult).getValue();
						if (model instanceof CSPModel) {
							cachedCSPString.put(formula, output);
						}
						formulas.put(formula, translateValue(output));
					}
				}

			} catch (Exception e) {
				// TODO: do something ...
				// e.printStackTrace();
			}

		}

		return output;

	}

	private Object translateValue(final String val) {
		Object fvalue = val;
		if (val.equalsIgnoreCase("TRUE")) {
			fvalue = true;
		} else if (val.equalsIgnoreCase("FALSE")) {
			fvalue = false;
		}
		return fvalue;
	}

	public void toVisualization(final Object values) {
		submit(WebUtils.wrap("cmd", "bms.update_visualization", "values",
				values));
	}

	public void registerScript(final IBMotionScript script) {
		scriptListeners.add(script);
		if (currentTrace != null) {
			script.traceChange(currentTrace, formulas);
		}
	}

	public List<Object> getJsonData() {
		return jsonData;
	}

	public void setTemplate(final String template) {
		this.template = template;
	}

	public String getTemplate() {
		return template;
	}

	private String getTemplateFolder() {
		if (template != null) {
			return new File(template).getParent();
		}
		return null;
	}

	// private String getFormalism(String machine) {
	// String language = "???";
	// if (machine != null) {
	// int i = machine.lastIndexOf('.');
	// if (i > 0) {
	// language = machine.substring(i + 1);
	// }
	// if (Arrays.asList(eventbExtensions).contains(language)) {
	// language = "eventb";
	// } else if (Arrays.asList(classicalBExtensions).contains(language)) {
	// language = "b";
	// }
	// }
	// return language;
	// }

	public void addParameter(final String key, final Object value) {
		parameterMap.put(key, value);
	}

	// private void animateModel(Object machinePath) {
	//
	// try {
	// Injector injector = ServletContextListener.INJECTOR;
	// Api api = injector.getInstance(Api.class);
	// AnimationSelector selector = injector
	// .getInstance(AnimationSelector.class);
	// Method method = api.getClass().getMethod(
	// getFormalism(machinePath.toString()) + "_load",
	// String.class);
	// AbstractModel m = (AbstractModel) method.invoke(api, machinePath);
	// StateSpace s = m.getStatespace();
	// Trace h = new Trace(s);
	// selector.addNewAnimation(h);
	// } catch (NoSuchMethodException e) {
	// e.printStackTrace();
	// } catch (SecurityException e) {
	// e.printStackTrace();
	// } catch (IllegalAccessException e) {
	// e.printStackTrace();
	// } catch (IllegalArgumentException e) {
	// e.printStackTrace();
	// } catch (InvocationTargetException e) {
	// e.printStackTrace();
	// }
	//
	// }

	@Override
	public void modelChanged(final StateSpace s) {
		// TODO: Reload ........
	}

	private void initGroovy() {

		if (template == null) {
			return;
		}

		try {

			String templateFolder = getTemplateFolder();
			Bindings bindings = groovy.getBindings(ScriptContext.GLOBAL_SCOPE);
			bindings.putAll(parameterMap);
			bindings.put("bms", this);

			// Object machinePath = parameterMap.get("machine");
			// Object load = parameterMap.get("load");
			// if (machinePath != null
			// && (load != null && load.toString().equals("1"))) {
			// animateModel(machinePath);
			// }

			Object scriptPaths = parameterMap.get("script");
			if (scriptPaths != null) {
				String[] sp = scriptPaths.toString().split(",");
				for (String s : sp) {
					FileReader fr = new FileReader(templateFolder + "/" + s);
					groovy.eval(fr, bindings);
				}
			}

		} catch (ScriptException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void animatorStatus(final boolean busy) {
		// TODO Auto-generated method stub

	}

}