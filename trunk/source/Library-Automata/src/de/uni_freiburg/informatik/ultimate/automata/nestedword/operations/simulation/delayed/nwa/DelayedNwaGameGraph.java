/*
 * Copyright (C) 2015-2016 Daniel Tischner
 * Copyright (C) 2009-2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE Automata Library.
 * 
 * The ULTIMATE Automata Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE Automata Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Automata Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Automata Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE Automata Library grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.delayed.nwa;

import java.util.Collection;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.IDoubleDeckerAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.RemoveUnreachable;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.ESimulationType;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.delayed.DelayedGameGraph;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.performance.EMultipleDataOption;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.performance.ETimeMeasure;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.performance.SimulationPerformance;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.util.DuplicatorVertex;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.util.SpoilerVertex;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.util.nwa.ETransitionType;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.util.nwa.graph.INwaGameGraph;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.util.nwa.graph.NwaGameGraphGeneration;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.util.nwa.graph.SummarizeEdge;
import de.uni_freiburg.informatik.ultimate.automata.statefactory.IStateFactory;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IProgressAwareTimer;

/**
 * Game graph that realizes <b>delayed simulation</b> for NWA automata.<br/>
 * In delayed simulation each time <i>Spoiler</i> visits a final state
 * <i>Duplicator</i> must at least visit one in the future for coverage.<br/>
 * To reflect <i>Duplicator</i>s coverage the delayed game graph uses vertices
 * that have an extra bit.<br/>
 * <br/>
 * 
 * If its impossible for <i>Spoiler</i> to build a word such that
 * <i>Duplicator</i> can not fulfill its condition we say <b>q1 delayed
 * simulates q0</b> where q0 was the starting state of <i>Spoiler</i> and q1 of
 * <i>Duplicator</i>.
 * 
 * @author Daniel Tischner {@literal <zabuza.dev@gmail.com>}
 *
 * @param <LETTER>
 *            Letter class of nwa
 * @param <STATE>
 *            State class of nwa
 */
public final class DelayedNwaGameGraph<LETTER, STATE> extends DelayedGameGraph<LETTER, STATE>
		implements INwaGameGraph<LETTER, STATE> {
	/**
	 * Utility object for generating game graphs based on nwa automata.
	 */
	private final NwaGameGraphGeneration<LETTER, STATE> mGeneration;
	/**
	 * The underlying nwa, as double decker automaton, from which the
	 * game graph gets generated.
	 */
	private final IDoubleDeckerAutomaton<LETTER, STATE> mNwa;

	/**
	 * Creates a new delayed nwa game graph by using the given nwa.
	 * 
	 * @param services
	 *            Service provider of Ultimate framework
	 * @param progressTimer
	 *            Timer used for responding to timeouts and operation
	 *            cancellation.
	 * @param logger
	 *            ILogger of the Ultimate framework.
	 * @param nwa
	 *            The underlying nwa from which the game graph gets
	 *            generated.
	 * @param stateFactory
	 *            State factory used for state creation
	 * @param possibleEquivalenceClasses
	 *            A collection of sets which contains states of an
	 *            automaton that may be merge-able. States which are not in the
	 *            same set are definitely not merge-able which is used as an
	 *            optimization for the game graph
	 * @throws AutomataOperationCanceledException
	 *             If the operation was canceled, for example from the Ultimate
	 *             framework.
	 */
	public DelayedNwaGameGraph(final AutomataLibraryServices services,
			final IProgressAwareTimer progressTimer,
			final ILogger logger, final IDoubleDeckerAutomaton<LETTER, STATE> nwa,
			final IStateFactory<STATE> stateFactory,
			final Collection<Set<STATE>> possibleEquivalenceClasses)
					throws AutomataOperationCanceledException {
		super(services, progressTimer, logger, nwa, stateFactory);
		// To derive down states of automaton ensure it
		// is a double decker automaton
		final INestedWordAutomaton<LETTER, STATE> preparedNwa = getAutomaton();
		if (preparedNwa instanceof IDoubleDeckerAutomaton<?, ?>) {
			mNwa = (IDoubleDeckerAutomaton<LETTER, STATE>) preparedNwa;
		} else {
			mNwa = new RemoveUnreachable<LETTER, STATE>(services, preparedNwa).getResult();
		}
		mGeneration = new NwaGameGraphGeneration<LETTER, STATE>(services, getProgressTimer(), getLogger(), mNwa, this,
				ESimulationType.DELAYED, possibleEquivalenceClasses, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.
	 * simulation.delayed.DelayedGameGraph#generateAutomatonFromGraph()
	 */
	@Override
	public INestedWordAutomaton<LETTER, STATE> generateAutomatonFromGraph()
			throws AutomataOperationCanceledException {
		final SimulationPerformance performance = getSimulationPerformance();
		if (performance != null) {
			performance.startTimeMeasure(ETimeMeasure.BUILD_RESULT);
		}

		final INestedWordAutomaton<LETTER, STATE> result = mGeneration.generateAutomatonFromGraph(false);

		// Log performance
		if (performance != null) {
			performance.stopTimeMeasure(ETimeMeasure.BUILD_RESULT);
			performance.addAllMeasures(mGeneration.getSimulationPerformance());
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.
	 * simulation.delayed.DelayedGameGraph#generateGameGraphFromAutomaton()
	 */
	@Override
	public void generateGameGraphFromAutomaton() throws AutomataOperationCanceledException {
		mGeneration.generateGameGraphFromAutomaton();

		// Set values for compatibility with non nwa graph
		final SimulationPerformance performance = mGeneration.getSimulationPerformance();
		setGraphBuildTime(performance.getTimeMeasureResult(ETimeMeasure.BUILD_GRAPH, EMultipleDataOption.ADDITIVE));
	}

	/**
	 * Unsupported operation. Use
	 * {@link #getDuplicatorVertex(Object, Object, Object, boolean, ETransitionType, SummarizeEdge, Sink)}
	 * instead.
	 * 
	 * @throws UnsupportedOperationException
	 *             Operation is not supported.
	 */
	@Override
	public DuplicatorVertex<LETTER, STATE> getDuplicatorVertex(final STATE q0, final STATE q1, final LETTER a,
			final boolean bit) {
		throw new UnsupportedOperationException(
				"Use getDuplicatorVertex(q0, q1, a, bit, transType, summarizeEdge, sink) instead.");
	}

	/**
	 * Unsupported operation. Use
	 * {@link #getSpoilerVertex(Object, Object, boolean, SummarizeEdge, Sink)}
	 * instead.
	 * 
	 * @throws UnsupportedOperationException
	 *             Operation is not supported.
	 */
	@Override
	public SpoilerVertex<LETTER, STATE> getSpoilerVertex(final STATE q0, final STATE q1, final boolean bit) {
		throw new UnsupportedOperationException("Use getSpoilerVertex(q0, q1, a, bit, summarizeEdge, sink) instead.");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.
	 * simulation.util.nwa.INwaGameGraph#undoRemovedReturnBridgesChanges()
	 */
	@Override
	public void undoRemovedReturnBridgesChanges() {
		undoChanges(mGeneration.getRemovedReturnBridgesChanges());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations.
	 * simulation.AGameGraph#verifyAutomatonValidity(de.uni_freiburg.informatik.
	 * ultimate.automata.nwalibrary.INestedWordAutomaton)
	 */
	@Override
	public void verifyAutomatonValidity(final INestedWordAutomaton<LETTER, STATE> automaton) {
		// Do noting to accept nwa
	}
}