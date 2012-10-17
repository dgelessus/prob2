package de.prob.statespace;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Singleton;

import de.prob.model.representation.IEntity;

@Singleton
public class AnimationSelector implements IAnimationListener {

	List<IHistoryChangeListener> listeners = new ArrayList<IHistoryChangeListener>();
	List<History> histories = new ArrayList<History>();
	History currentHistory = null;

	public void registerHistoryChangeListener(
			final IHistoryChangeListener listener) {
		listeners.add(listener);
		if (currentHistory != null) {
			notifyHistoryChange(currentHistory);
		}
	}

	@Override
	public void currentStateChanged(final History oldHistory,
			final History newHistory) {
		if (oldHistory.equals(currentHistory)) {
			notifyHistoryChange(newHistory);
		}
		histories.set(histories.indexOf(oldHistory), newHistory);
		currentHistory = newHistory;
	}

	public void changeCurrentHistory(final History history) {
		currentHistory = history;
		notifyHistoryChange(history);
	}

	public void addNewHistory(final History history) {
		if (histories.contains(history)) {
			return;
		}
		histories.add(history);
		history.registerAnimationListener(this);
		currentHistory = history;
		notifyHistoryChange(history);
	}

	public void notifyHistoryChange(final History history) {
		for (final IHistoryChangeListener listener : listeners) {
			listener.historyChange(history);
		}
	}

	public History getCurrentHistory() {
		return currentHistory;
	}

	public List<History> getHistories() {
		return histories;
	}

	public IEntity getModel(final History history) {
		return history.getModel();
	}
}