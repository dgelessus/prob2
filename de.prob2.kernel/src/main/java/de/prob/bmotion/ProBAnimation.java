package de.prob.bmotion;

import de.prob.model.representation.AbstractModel;
import de.prob.statespace.*;
import de.prob.ui.api.ITool;
import de.prob.ui.api.ToolRegistry;

public abstract class ProBAnimation implements ITool, IAnimationChangeListener, IModelChangedListener {

	private AbstractModel model;

	protected Trace trace;
	protected final ToolRegistry toolRegistry;
	protected final AnimationSelector animations;
	protected final String toolId;
    public static final String TRIGGER_MODEL_CHANGED = "ModelChanged";

	public ProBAnimation(final String sessionId, final AbstractModel model,
			final AnimationSelector animations, final ToolRegistry toolRegistry) {
		this(sessionId, animations, toolRegistry);
		this.model = model;
		trace = new Trace(model);
	}

	public ProBAnimation(final String toolId,
			final AnimationSelector animations, final ToolRegistry toolRegistry) {
		this.toolId = toolId;
		this.animations = animations;
		this.toolRegistry = toolRegistry;
		animations.registerAnimationChangeListener(this);
	}

	public AbstractModel getModel() {
		return model;
	}

	public void setModel(final AbstractModel model) {
		this.model = model;
		trace = new Trace(model);
	}

	public Trace getTrace() {
		return trace;
	}

	public StateSpace getStateSpace() {
		return trace != null ? trace.getStateSpace() : null;
	}

	public ToolRegistry getToolRegistry() {
		return toolRegistry;
	}

	@Override
	public void traceChange(final Trace currentTrace,
			final boolean currentAnimationChanged) {
		Trace oldtrace = trace;
		trace = currentTrace;
		if (oldtrace != null && !currentTrace.equals(oldtrace)) {
			toolRegistry.notifyToolChange(BMotion.TRIGGER_ANIMATION_CHANGED,this);
		}
	}

    @Override
    public void modelChanged(StateSpace s) {
        toolRegistry.notifyToolChange(ProBAnimation.TRIGGER_MODEL_CHANGED,this);
    }

	@Override
	public String getCurrentState() {
		return trace != null ? trace.getCurrentState().getId() : null;
	}

	@Override
	public String getName() {
		return toolId;
	}

	@Override
	public boolean canBacktrack() {
		return true;
	}

}
