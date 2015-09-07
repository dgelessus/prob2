package de.prob.model.eventb

import org.eventb.core.ast.extension.IFormulaExtension

import de.prob.animator.domainobjects.EvalElementType
import de.prob.model.eventb.Event.EventType
import de.prob.model.representation.Action
import de.prob.model.representation.ElementComment
import de.prob.model.representation.Guard
import de.prob.model.representation.ModelElementList





public class EventModifier extends AbstractModifier {
	private final actctr
	private final grdctr
	def Event event
	boolean initialisation

	public EventModifier(Event event, boolean initialisation=false, Set<IFormulaExtension> typeEnvironment=Collections.emptySet()) {
		super(typeEnvironment)
		this.initialisation = initialisation
		this.actctr = extractCounter("act",event.actions)
		this.event = event
		this.grdctr = extractCounter("grd",event.guards)
	}

	private EventModifier newEM(Event event) {
		return new EventModifier(event, initialisation)
	}

	def EventModifier refines(String name) {
		return newEM(event.set(Event.class, new ModelElementList<Event>([
			new Event(name, EventType.ORDINARY, false)
		])))
	}

	def EventModifier when(Map g) {
		guards(g)
	}

	def EventModifier when(String... g) {
		guards(g)
	}

	def EventModifier where(Map g) {
		guards(g)
	}

	def EventModifier where(String... g) {
		guards(g)
	}

	def EventModifier guards(Map guards) {
		EventModifier em = this
		guards.each { k,v ->
			em = em.guard(k,v)
		}
		em
	}

	def EventModifier guards(String... grds) {
		EventModifier em = this
		grds.each {
			em = em.guard(it)
		}
		em
	}

	def EventModifier theorem(LinkedHashMap properties) {
		guard(properties, true)
	}

	def EventModifier theorem(String pred) {
		guard(pred, true)
	}

	def EventModifier guard(String pred, boolean theorem=false) {
		def ctr = grdctr + 1
		guard("grd$ctr", pred, theorem)
	}

	def EventModifier guard(LinkedHashMap properties, boolean theorem=false) {
		Definition prop = getDefinition(properties)
		return guard(prop.label, prop.formula, theorem)
	}

	def EventModifier guard(String name, String pred, boolean theorem=false, String comment="") {
		if (initialisation) {
			throw new IllegalArgumentException("Cannot at a guard to INITIALISATION")
		}
		def guard = new EventBGuard(name, parsePredicate(pred), theorem, comment)
		newEM(event.addTo(Guard.class, guard))
	}

	def EventModifier removeGuard(String name) {
		def grd = event.guards.getElement(name)
		removeGuard(grd)
	}

	/**
	 * Remove a guard from an event
	 * @param guard to be removed
	 * @return whether or not the removal was successful
	 */
	def EventModifier removeGuard(EventBGuard guard) {
		newEM(event.removeFrom(Guard.class, guard))
	}

	def EventModifier then(Map acts) {
		actions(acts)
	}

	def EventModifier then(String... acts) {
		actions(acts)
	}

	def EventModifier actions(Map actions) {
		EventModifier em = this
		actions.each { k,v ->
			em = em.action(k,v)
		}
		em
	}

	def EventModifier actions(String... actions) {
		EventModifier em = this
		actions.each {
			em = em.action(it)
		}
		em
	}

	def EventModifier action(LinkedHashMap properties) {
		Definition prop = getDefinition(properties)
		return action(prop.label, prop.formula)
	}

	def EventModifier action(String actionString) {
		int ctr = actctr + 1
		action("act$ctr", actionString)
	}

	def EventModifier action(String name, String action, String comment="") {
		def a = new EventBAction(name, parseFormula(action, EvalElementType.ASSIGNMENT), comment)
		newEM(event.addTo(Action.class, a))
	}

	def EventModifier removeAction(String name) {
		def act = event.actions.getElement(name)
		removeAction(act)
	}

	/**
	 * Remove an action from an event
	 * @param action to be removed
	 * @return whether or not the removal was successful
	 */
	def EventModifier removeAction(EventBAction action) {
		newEM(event.removeFrom(Action.class, action))
	}

	def EventModifier parameters(String... parameters) {
		EventModifier em = this
		parameters.each {
			em = em.parameter(it)
		}
		em
	}

	def EventModifier parameter(String parameter, String comment="") {
		if (initialisation) {
			throw new IllegalArgumentException("Cannot add parameter to initialisation.")
		}
		parseIdentifier(parameter)
		def param = new EventParameter(parameter, comment)
		newEM(event.addTo(EventParameter.class, param))
	}

	def EventModifier removeParameter(String name) {
		def param = event.parameters.getElement(name)
		param ? removeParameter(param) : this
	}

	def EventModifier removeParameter(EventParameter parameter) {
		newEM(event.removeFrom(EventParameter.class, parameter))
	}

	def EventModifier witness(Map properties) {
		Map validated = validateProperties(properties, [for: String, with: String])
		witness(validated.for, validated.with)
	}

	def EventModifier witness(String name, String code, String comment="") {
		def w = new Witness(name, parsePredicate(code), comment)
		newEM(event.addTo(Witness.class, w))
	}

	def EventModifier removeWitness(Witness w) {
		newEM(event.removeFrom(Witness.class, w))
	}

	def EventModifier setType(EventType type) {
		newEM(event.changeType(type))
	}

	def EventModifier addComment(String comment) {
		newEM(event.addTo(ElementComment.class, new ElementComment(comment)))
	}

	def EventModifier make(Closure definition) {
		runClosure definition
	}
}
