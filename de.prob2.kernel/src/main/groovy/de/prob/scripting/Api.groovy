package de.prob.scripting;

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.google.inject.Inject
import com.google.inject.Provider

import de.be4.classicalb.core.parser.exceptions.BException
import de.be4.classicalb.core.parser.node.Start
import de.prob.animator.IAnimator
import de.prob.animator.command.GetVersionCommand
import de.prob.cli.CliVersionNumber
import de.prob.cli.ProBInstance
import de.prob.exception.ProBError
import de.prob.model.eventb.translate.EventBModelTranslator
import de.prob.prolog.output.PrologTermOutput
import de.prob.statespace.StateSpace


public class Api {

	Logger logger = LoggerFactory.getLogger(Api.class);

	private final FactoryProvider modelFactoryProvider;
	private final Provider<IAnimator> animatorProvider;

	/**
	 * This variable specifies whether the variables in the model are
	 * registered by default when loading the model.
	 */
	def loadVariablesByDefault = true;

	def globals = [:]


	@Override
	public String toString() {
		return "ProB Connector";
	}

	/**
	 * A {@link FactoryProvider} and {@link Downloader} are injected into an api
	 * object at startup
	 *
	 * @param modelFactoryProvider
	 * @param downloader
	 */
	@Inject
	public Api(final FactoryProvider modelFactoryProvider,final Provider<IAnimator> animatorProvider) {
		this.animatorProvider =  animatorProvider;
		this.modelFactoryProvider = modelFactoryProvider;
	}

	public Closure getSubscribeClosure(closure) {
		if (loadVariablesByDefault) {
			return closure
		}
		LoadClosures.EMPTY
	}

	/**
	 * Shutdown the specified {@link ProBInstance} object.
	 *
	 * @param x
	 */
	public void shutdown(final ProBInstance x) {
		x.shutdown();
	}

	public StateSpace eventb_load(final String file, final Map<String, String> prefs=Collections.emptyMap()) {
		def fileName = file;
		def loadClosure=getSubscribeClosure(LoadClosures.EVENTB)
		EventBFactory factory = modelFactoryProvider.getEventBFactory();
		if (fileName.endsWith(".eventb")) {
			return factory.loadModelFromEventBFile(file, prefs)
		}
		def extracted = factory.extract(fileName)
		StateSpace s = extracted.load(prefs)
		loadClosure(s)
		return s
	}

	public void eventb_save(final StateSpace s, final String path) {
		EventBModelTranslator translator = new EventBModelTranslator(s.getModel(), s.getMainComponent());

		def fos = new FileOutputStream(path);
		PrologTermOutput pto = new PrologTermOutput(fos,false);

		pto.openTerm("package");
		translator.printProlog(pto);
		pto.closeTerm();
		pto.fullstop();

		pto.flush();
		fos.flush();
		fos.close();
	}

	/**
	 * Loads a {@link ClassicalBModel} from the specified file path.
	 *
	 * @param file
	 * @return classicalBModel
	 * @throws BException
	 * @throws IOException
	 */
	public StateSpace b_load(final String file,
			final Map<String, String> prefs=Collections.emptyMap()) throws IOException, BException {
		ClassicalBFactory bFactory = modelFactoryProvider
				.getClassicalBFactory();
		Closure loadClosure=getSubscribeClosure(LoadClosures.B)
		def extracted = bFactory.extract(file)
		StateSpace s = extracted.load(prefs)
		loadClosure(s)
		return s
	}

	public StateSpace b_load(final Start ast,
			final Map<String, String> prefs=Collections.emptyMap()) throws IOException, BException {
		ClassicalBFactory bFactory = modelFactoryProvider
				.getClassicalBFactory();
		Closure loadClosure=getSubscribeClosure(LoadClosures.B)
		def extracted = bFactory.create(ast)
		StateSpace s = extracted.load(prefs)
		loadClosure(s)
		return s
	}

	public StateSpace tla_load(final String file,
			final Map<String, String> prefs=Collections.emptyMap()) throws IOException, BException {
		TLAFactory tlaFactory = modelFactoryProvider.getTLAFactory();
		Closure loadClosure=getSubscribeClosure(LoadClosures.B)
		def extracted = tlaFactory.extract(file)
		StateSpace s = extracted.load(prefs)
		loadClosure(s)
		return s
	}

	/**
	 * Loads a {@link CSPModel} from the given file. If the user does not have
	 * the cspm parser installed, an Exception is thrown informing the user that
	 * they need to install it.
	 *
	 * @param file
	 * @return {@link CSPModel} that has been loaded from file
	 * @throws Exception
	 */
	public StateSpace csp_load(final String file, final Map<String, String> prefs=Collections.emptyMap())
	throws Exception {
		CSPFactory cspFactory = modelFactoryProvider.getCspFactory();
		StateSpace s = null;
		try {
			def extracted = cspFactory.extract(file)
			s = extracted.load(prefs)
		} catch (ProBError error) {
			throw new Exception(
			"Could not find CSP Parser. Perform 'installCSPM' to install cspm in your ProB lib directory");
		}
		return s;
	}

	public CliVersionNumber getVersion() {
		try {
			IAnimator animator = animatorProvider.get();
			GetVersionCommand versionCommand = new GetVersionCommand();
			animator.execute(versionCommand);
			animator.cli.shutdown()
			return versionCommand.getVersion();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @return Returns a String representation of the currently available
	 *         commands for the Api object. Intended to ease use in the Groovy
	 *         console.
	 */
	public String help() {
		return "Api Commands: \n\n ClassicalBModel b_load(String PathToFile): load .mch files \n"
		+ " CSPModel csp_load(String PathToFile): load .csp files \n"
		+ " toFile(StateSpace s): save StateSpace\n"
		+ " readFile(): reload saved StateSpace\n"
		+ " shutdown(ProBInstance x): shutdown ProBInstance\n"
		+ " help(): print out available commands";
	}
}
