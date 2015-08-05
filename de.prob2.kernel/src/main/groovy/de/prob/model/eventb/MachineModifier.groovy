package de.prob.model.eventb

import de.prob.model.eventb.Event.EventType
import de.prob.model.representation.BEvent
import de.prob.model.representation.Invariant
import de.prob.model.representation.Machine
import de.prob.model.representation.ModelElementList
import de.prob.model.representation.Variable

/**
 * The {@link MachineModifier} provides an API to programmatically modify or
 * construct {@link EventBMachine}s. Basic elements can be added to the machine
 * via the methods beginning with 'add' (e.g. {@link #addInvariant(String)}).
 * <br>
 * Machines can also be constructed using JavaBuilder syntax which adds an element
 * and returns the {@link MachineModifier} object itself to allow the method calls
 * to be chained together.
 * <br>
 * For example: <br>
 * <code>modifier.var_block("x","x:NAT","x:=0").invariant("x < 10")</code>
 * <br>
 * These methods can then be put together to create a Groovy DSL.
 * <br>
 * <code>
 * modifier.make {<br>
 * <br>
 * var_block name: "x", invariant: "x:NAT", init: "x:=0"<br>
 * <br>
 * event(name: "inc") { action x:=x+1 }<br>
 * <br>
 * }<br>
 * </code>
 * @author Joy Clark
 */
class MachineModifier extends AbstractModifier {
	private final int invctr
	EventBMachine machine
	EventBModel model

	def MachineModifier(EventBMachine machine) {
		this(machine, -1)
	}

	private MachineModifier(EventBMachine machine, int invctr) {
		this.machine = machine
		this.invctr = invctr
	}

	private newMM(EventBMachine machine) {
		new MachineModifier(machine, invctr)
	}

	def MachineModifier addSees(List<Context> seenContexts) {
		newMM(machine.set(Context.class, new ModelElementList<Context>(seenContexts)))
	}

	def MachineModifier addRefines(List<EventBMachine> refines) {
		newMM(machine.set(Machine.class, new ModelElementList<EventBMachine>(refines)))
	}

	def MachineModifier variables(String... variables) {
		MachineModifier mm = this
		variables.each {
			mm = mm.variable(it)
		}
		mm
	}

	/** adds a variable */
	def MachineModifier variable(String varName) {
		newMM(machine.addTo(Variable.class, new EventBVariable(varName, null)))
	}

	def MachineModifier var_block(LinkedHashMap properties) {
		Map validated = validateProperties(properties, [name: String, invariant: Object, init: Object])
		var_block(validated.name, validated.invariant, validated.init)
	}

	def MachineModifier var_block(String name, String invariant, String init) {
		MachineModifier mm = variable(name)
		mm = mm.invariant(invariant)
		mm = mm.initialisation({ action init })
		mm
	}

	def MachineModifier var_block(String name, Map inv, Map init) {
		MachineModifier mm = variable(name)
		mm = mm.invariant(inv)
		mm = mm.initialisation({ action init })
		mm
	}

	def MachineModifier removeVariable(String name) {
		def var = machine.variables.getElement(name)
		var ? removeVariable(var) : this
	}

	def MachineModifier removeVariable(EventBVariable variable) {
		newMM(machine.removeFrom(Variable.class, variable))
	}

	/**
	 * Removes a variable and its typing/initialisation information from the machine
	 * @param block containing the added variable, typing invariant, and initialisation
	 * @return if the removal of all elements from the machine was successful.
	 */
	def boolean removeVariableBlock(VariableBlock block) {
		// proof obligations are invalidated by removeInvariant
		// if we could check whether typingInvariant is in fact only typing,
		// we could remove just selected proof information
		def a = machine.variables.remove(block.getVariable())
		def b = removeInvariant(block.getTypingInvariant())
		def c = machine.events.INITIALISATION.actions.remove(block.getInitialisationAction())
		return a & b & c
	}

	def MachineModifier invariants(Map invariants) {
		MachineModifier mm = this
		invariants.each { k,v ->
			mm = mm.invariant(k,v)
		}
		mm
	}

	def MachineModifier invariants(String... invariants) {
		MachineModifier mm = this
		invariants.each {
			mm = mm.invariant(it)
		}
		mm
	}

	def MachineModifier theorems(Map invariants) {
		MachineModifier mm = this
		invariants.each { k,v ->
			mm = mm.theorem(k,v)
		}
		mm
	}

	def MachineModifier theorems(String... invariants) {
		MachineModifier mm = this
		invariants.each {
			mm = mm.theorem(it)
		}
		mm
	}

	def MachineModifier theorem(LinkedHashMap properties) {
		invariant(properties, true)
	}

	def MachineModifier theorem(String thm) {
		invariant(thm, true)
	}

	def MachineModifier invariant(LinkedHashMap properties, boolean theorem=false) {
		Definition prop = getDefinition(properties)
		return invariant(prop.label, prop.formula, theorem)
	}

	def MachineModifier invariant(String pred, boolean theorem=false) {
		int ctr = invctr + 1
		new MachineModifier(invariant("i$ctr", pred, theorem).getMachine(),ctr)
	}

	def MachineModifier invariant(String name, String predicate, boolean theorem=false) {
		def newproofs = machine.getProofs().findAll { po ->
			!po.getName().endsWith("/INV")
		}

		def invariant = new EventBInvariant(name, predicate, theorem, Collections.emptySet())
		machine = machine.addTo(Invariant.class, invariant)
		machine = machine.set(ProofObligation.class, new ModelElementList<ProofObligation>(newproofs))
		newMM(machine)
	}

	def MachineModifier removeInvariant(String name) {
		def axm = machine.invariants.getElement(name)
		axm ? removeInvariant(axm) : this
	}

	/**
	 * Removes an invariant from the machine.
	 * @param invariant to be removed
	 * @return whether or not the removal was successful
	 */
	def MachineModifier removeInvariant(EventBInvariant invariant) {
		// only variant well-definedness may not use existing invariants in a prove
		// thus, these seem to be the only proof obligations we can keep
		def newproofs = machine.getProofs().findAll { po ->
			po.getName().endsWith("/VWD")
		}

		newMM(machine.removeFrom(Invariant.class, invariant)
				.set(ProofObligation.class, new ModelElementList<ProofObligation>(newproofs)))
	}

	def MachineModifier variant(String expression) {
		def variant = new Variant(expression, Collections.emptySet())
		newMM(machine.set(Variant.class, new ModelElementList([variant])))
	}

	def MachineModifier removeVariant(Variant variant) {
		newMM(machine.removeFrom(Variant.class, variant))
	}

	def MachineModifier initialisation(LinkedHashMap properties) {
		if (properties["extended"] == true) {
			initialisation({},true)
		}
		this
	}

	def MachineModifier initialisation(Closure cls, boolean extended=false) {
		def refines = machine.getRefines().isEmpty() ? [] : [machine.getRefines()[0].events.INITIALISATION]
		event("INITIALISATION", refines, EventType.ORDINARY, extended, cls)
	}

	def MachineModifier refine(LinkedHashMap properties, Closure cls={}) {
		properties["refines"] = properties["name"]
		event(properties, cls)
	}

	def MachineModifier event(LinkedHashMap properties, Closure cls={}) {
		validateProperties(properties, [name: String])

		def refinedEvent = properties["refines"]
		def refinedE  = []
		if (refinedEvent) {
			def props = validateProperties(properties, [refines: String])
			refinedEvent = props["refines"]
			machine.getRefines().each { EventBMachine m ->
				def e = m.getEvent(refinedEvent)
				if (e) {
					refinedE << e
				}
			}
		}
		def type = properties["type"] ?: EventType.ORDINARY

		if (refinedEvent && refinedE.size() != 1) {
			throw new IllegalArgumentException("Tried to refine event $refinedEvent, but found $refinedE events")
		}

		event(properties["name"], refinedE, type, properties["extended"]  ?: false, cls)
	}

	def MachineModifier event(String name, List<Event> refinedEvents, type, boolean extended, Closure cls={}) {
		def mm = removePOsForEvent(name)
		def oldevent = machine.getEvent(name)
		def event = oldevent ? oldevent.changeType(type).toggleExtended(extended) : new Event(name, type, properties["extended"] == true)
		event = event.set(Event.class, new ModelElementList<Event>(refinedEvents))
		def em = new EventModifier(event, "INITIALISATION" == name).make(cls)
		def m = mm.getMachine()
		if (oldevent) {
			m = m.replaceIn(BEvent.class, oldevent, em.getEvent())
		} else {
			m = m.addTo(BEvent.class, em.getEvent())
		}
		newMM(m)
	}

	/**
	 * Generates a new {@link Event} in the machine that is identical to
	 * the specified event for copying. The new {@link Event} object will
	 * have the specified name. If an existing {@link Event} in the machine
	 * has the same name, this will be overwritten.
	 * @param name of the event to be duplicated
	 * @param newName of the cloned event
	 */
	def MachineModifier duplicateEvent(String eventName, String newName) {
		MachineModifier mm = removePOsForEvent(newName)
		Event event = machine.getEvent(eventName)
		if (!event) {
			throw new IllegalArgumentException("Can only duplicate an event that exists! Event with name $eventName was not found.")
		}
		Event event2 = new Event(newName, event.type, event.extended, event.children)
		def oldE = mm.getMachine().events.getElement(newName)
		def m = oldE ? mm.getMachine().replaceIn(BEvent.class, oldE, event2) : mm.getMachine().addTo(BEvent.class, event2)
		return newMM(m)
	}

	def MachineModifier removeEvent(String name) {
		def evt = machine.events.getElement(name)
		evt ? removeEvent(evt) : this
	}

	/**
	 * Removes an event from the machine.
	 * @param event to be removed
	 * @return whether or not the removal was successful
	 */
	def MachineModifier removeEvent(Event event) {
		MachineModifier mm = removePOsForEvent(event.name)
		newMM(mm.getMachine().removeFrom(BEvent.class, event))
	}

	def MachineModifier removePOsForEvent(String name) {
		def proofs = machine.getProofs()
		proofs.each {
			if (it.name.startsWith(name)) {
				proofs = proofs.removeElement(it)
			}
		}
		newMM(machine.set(ProofObligation.class, proofs))
	}

	def MachineModifier make(Closure definition) {
		runClosure definition
	}


}
