/*
 * Copyright (C) 2016 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2016 University of Freiburg
 *
 * This file is part of the ULTIMATE TraceAbstraction plug-in.
 *
 * The ULTIMATE TraceAbstraction plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE TraceAbstraction plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE TraceAbstraction plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE TraceAbstraction plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE TraceAbstraction plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.automataminimization;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.AutomataOperationCanceledException;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.IDoubleDeckerAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomatonSimple;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.RemoveUnreachable;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.minimization.AbstractMinimizeNwa;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.minimization.IMinimizeNwa;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.minimization.IMinimizeNwaDD;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.minimization.MinimizeDfaHopcroftArrays;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.minimization.MinimizeDfaHopcroftLists;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.minimization.MinimizeNwaCombinator;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.minimization.MinimizeNwaCombinator.MinimizationMethods;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.minimization.MinimizeNwaMaxSat2;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.minimization.MinimizeNwaMulti;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.minimization.MinimizeNwaMulti.Strategy;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.minimization.MinimizeNwaOverapproximation;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.minimization.MinimizeNwaPattern;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.minimization.MinimizeNwaPmaxSat;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.minimization.MinimizeSevpa;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.minimization.ShrinkNwa;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.minimization.maxsat.arrays.MinimizeNwaMaxSAT;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.delayed.BuchiReduce;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.delayed.nwa.ReduceNwaDelayedSimulation;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.direct.nwa.ReduceNwaDirectSimulation;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.fair.ReduceBuchiFairDirectSimulation;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.fair.ReduceBuchiFairSimulation;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.multipebble.ReduceNwaDelayedFullMultipebbleSimulation;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.multipebble.ReduceNwaDirectFullMultipebbleSimulation;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.util.nwa.graph.summarycomputationgraph.ReduceNwaDelayedSimulationB;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.simulation.util.nwa.graph.summarycomputationgraph.ReduceNwaDirectSimulationB;
import de.uni_freiburg.informatik.ultimate.automata.statefactory.IStateFactory;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.BoogieIcfgLocation;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.Activator;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.TraceAbstractionUtils;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.IMLPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.ISLPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TraceAbstractionPreferenceInitializer.Minimization;

/**
 *
 * Objects of this class perform a single automata minimization operation and provide the results of this operation.
 *
 * @author Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 *
 * @param <LCS>
 *            local control state, e.g., {@link BoogieIcfgLocation} for sequential programs or a set of
 *            {@link BoogieIcfgLocation}s for parallel programs.
 * @param <LCSP>
 *            local control state provider, e.g., {@link ISLPredicate}, or {@link IMLPredicate}
 */
public class AutomataMinimization<LCS, LCSP extends IPredicate, LETTER> {

	private final IUltimateServiceProvider mServices;
	private final ILogger mLogger;
	private final MinimizationResult mMinimizationResult;
	private final IDoubleDeckerAutomaton<LETTER, IPredicate> mMinimizedAutomaton;
	private final Map<IPredicate, IPredicate> mOldState2newState;
	private final AutomataMinimizationStatisticsGenerator mStatistics;
	private final static long DEFAULT_TIMEOUT_FOR_EXPENSIVE_NWA_MINIMIZATIONS = 5_000;

	public AutomataMinimization(final IUltimateServiceProvider services,
			final INestedWordAutomaton<LETTER, IPredicate> operand, final Minimization minimization,
			final boolean computeOldState2NewStateMapping, final int iteration,
			final IStateFactory<IPredicate> predicateFactoryRefinement, final int minimizeEveryKthIteration,
			final Collection<INestedWordAutomatonSimple<LETTER, IPredicate>> storedRawInterpolantAutomata,
			final INestedWordAutomaton<LETTER, IPredicate> interpolAutomaton, final int minimizationTimeout,
			final IStateFactory<IPredicate> resultCheckPredFac, final Function<LCSP, LCS> lcsProvider,
			final boolean initialPartitionSeparatesFinalsAndNonfinals) throws AutomataMinimizationTimeout {

		mServices = services;
		mLogger = services.getLoggingService().getLogger(Activator.PLUGIN_ID);
		final long startTime = System.nanoTime();

		final Collection<Set<IPredicate>> partition =
				TraceAbstractionUtils.computePartition(operand, mLogger, lcsProvider);

		// output of minimization

		final AutomataLibraryServices autServices = new AutomataLibraryServices(services);
		try {
			mMinimizationResult = doMinimizationOperation(operand, minimization, computeOldState2NewStateMapping,
					iteration, predicateFactoryRefinement, minimizeEveryKthIteration, storedRawInterpolantAutomata,
					interpolAutomaton, minimizationTimeout, partition, autServices,
					initialPartitionSeparatesFinalsAndNonfinals);
			// postprocessing after minimization
			final IDoubleDeckerAutomaton<LETTER, IPredicate> newAbstraction;
			if (mMinimizationResult.wasNewAutomatonBuilt()) {
				// extract result
				try {
					assert mMinimizationResult.getRawMinimizationOutput()
							.checkResult(resultCheckPredFac) : "incorrect minimization result for " + minimization;
				} catch (final AutomataOperationCanceledException e) {
					throw e;
				} catch (final AutomataLibraryException e) {
					throw new AssertionError(e);
				}

				if (mMinimizationResult.getRawMinimizationOutput() instanceof IMinimizeNwaDD) {
					/**
					 * TODO Christian 2016-08-05: remove RemoveUnreachable() call (test thoroughly first!)
					 */
					// DoubleDecker information already present in output
					newAbstraction = (IDoubleDeckerAutomaton<LETTER, IPredicate>) mMinimizationResult
							.getRawMinimizationOutput().getResult();
					// (new RemoveUnreachable<LETTER, IPredicate>(new AutomataLibraryServices(mServices),
					// newAbstractionRaw.getResult())).getResult();
				} else {
					// compute DoubleDecker information
					newAbstraction = (new RemoveUnreachable<>(new AutomataLibraryServices(services),
							mMinimizationResult.getRawMinimizationOutput().getResult())).getResult();
				}

				// extract Hoare annotation
				if (computeOldState2NewStateMapping) {
					if (!(mMinimizationResult.getRawMinimizationOutput() instanceof AbstractMinimizeNwa)) {
						throw new AssertionError("Hoare annotation and " + minimization + " incompatible");
					}
					final AbstractMinimizeNwa<LETTER, IPredicate> minimizeOpCast =
							(AbstractMinimizeNwa<LETTER, IPredicate>) mMinimizationResult.getRawMinimizationOutput();
					mOldState2newState = minimizeOpCast.getOldState2newState();
				} else {
					mOldState2newState = null;
				}

				// use result
				mMinimizedAutomaton = newAbstraction;

			} else {
				mMinimizedAutomaton = null;
				mOldState2newState = null;
			}
		} catch (final AutomataOperationCanceledException aoce) {
			final long automataMinimizationTime = System.nanoTime() - startTime;
			mStatistics = new AutomataMinimizationStatisticsGenerator(automataMinimizationTime, true, false, 0);
			throw new AutomataMinimizationTimeout(aoce, mStatistics);
		}
		final long statesRemovedByMinimization;
		if (mMinimizationResult.wasNewAutomatonBuilt()) {
			statesRemovedByMinimization = operand.size() - mMinimizedAutomaton.size();
		} else {
			statesRemovedByMinimization = 0;
		}
		final long automataMinimizationTime = System.nanoTime() - startTime;
		mStatistics = new AutomataMinimizationStatisticsGenerator(automataMinimizationTime,
				mMinimizationResult.wasMinimizationAttempted(), statesRemovedByMinimization > 0,
				statesRemovedByMinimization);
	}

	private MinimizationResult doMinimizationOperation(final INestedWordAutomaton<LETTER, IPredicate> operand,
			final Minimization minimization, final boolean computeOldState2NewStateMapping, final int iteration,
			final IStateFactory<IPredicate> predicateFactoryRefinement, final int minimizeEveryKthIteration,
			final Collection<INestedWordAutomatonSimple<LETTER, IPredicate>> storedRawInterpolantAutomata,
			final INestedWordAutomaton<LETTER, IPredicate> interpolAutomaton, final int minimizationTimeout,
			final Collection<Set<IPredicate>> partition, final AutomataLibraryServices autServices,
			final boolean initialPartitionSeparatesFinalsAndNonfinals)
			throws AutomataOperationCanceledException, AssertionError {

		final MinimizationResult minimizationResult;
		switch (minimization) {
		case MINIMIZE_SEVPA: {
			minimizationResult = new MinimizationResult(true, true,
					new MinimizeSevpa<>(autServices, operand, partition, predicateFactoryRefinement,
							computeOldState2NewStateMapping, initialPartitionSeparatesFinalsAndNonfinals));
			break;
		}
		case SHRINK_NWA: {
			minimizationResult = new MinimizationResult(true, true,
					new ShrinkNwa<>(autServices, predicateFactoryRefinement, operand, partition,
							computeOldState2NewStateMapping, false, false, ShrinkNwa.SUGGESTED_RANDOM_SPLIT_SIZE, false,
							0, false, false, true, initialPartitionSeparatesFinalsAndNonfinals));
			break;
		}
		case NWA_COMBINATOR_PATTERN: {
			final IMinimizeNwa<LETTER, IPredicate> minNwa = new MinimizeNwaPattern<>(autServices,
					predicateFactoryRefinement, (IDoubleDeckerAutomaton<LETTER, IPredicate>) operand, partition,
					computeOldState2NewStateMapping, iteration);
			// it can happen that no minimization took place
			final boolean newAutomatonWasBuilt = (minNwa != operand);
			minimizationResult = new MinimizationResult(true, newAutomatonWasBuilt, minNwa);
			break;
		}
		case NWA_COMBINATOR_EVERY_KTH: {
			final IMinimizeNwa<LETTER, IPredicate> minNwa = new MinimizeNwaPattern<>(autServices,
					predicateFactoryRefinement, (IDoubleDeckerAutomaton<LETTER, IPredicate>) operand, partition,
					computeOldState2NewStateMapping, minimizeEveryKthIteration, iteration);
			// it can happen that no minimization took place
			final boolean newAutomatonWasBuilt = (minNwa != operand);
			minimizationResult = new MinimizationResult(true, newAutomatonWasBuilt, minNwa);
			throw new UnsupportedOperationException("currently unsupported - check minimizaton attemp");
		}
		case NWA_OVERAPPROXIMATION: {
			assert storedRawInterpolantAutomata != null;
			storedRawInterpolantAutomata.add(interpolAutomaton);
			final IMinimizeNwa<LETTER, IPredicate> minNwa =
					new MinimizeNwaOverapproximation<>(autServices, predicateFactoryRefinement, operand, partition,
							computeOldState2NewStateMapping, minimizationTimeout, storedRawInterpolantAutomata);
			final boolean newAutomatonWasBuilt = (minNwa != operand);
			minimizationResult = new MinimizationResult(true, newAutomatonWasBuilt, minNwa);
			throw new UnsupportedOperationException("currently unsupported - check minimizaton attemp");
		}
		case DFA_HOPCROFT_ARRAYS: {
			minimizationResult = new MinimizationResult(true, true, new MinimizeDfaHopcroftArrays<>(autServices,
					operand, predicateFactoryRefinement, partition, computeOldState2NewStateMapping));
			break;
		}
		case DFA_HOPCROFT_LISTS: {
			minimizationResult = new MinimizationResult(true, true, new MinimizeDfaHopcroftLists<>(autServices, operand,
					predicateFactoryRefinement, partition, computeOldState2NewStateMapping));
			break;
		}
		case NWA_MAX_SAT: {
			minimizationResult = new MinimizationResult(true, true,
					new MinimizeNwaMaxSAT<>(autServices, predicateFactoryRefinement, operand));
			break;
		}
		case NWA_MAX_SAT2: {
			final AutomataLibraryServices autServicesWithTimeout = new AutomataLibraryServices(mServices, 
					DEFAULT_TIMEOUT_FOR_EXPENSIVE_NWA_MINIMIZATIONS);
			MinimizationResult localResult = null;
			try {
				localResult = new MinimizationResult(true, true,
						new MinimizeNwaPmaxSat<>(autServicesWithTimeout, predicateFactoryRefinement,
								(IDoubleDeckerAutomaton<LETTER, IPredicate>) operand, partition,
								new MinimizeNwaMaxSat2.Settings<IPredicate>()
								.setAddMapOldState2NewState(computeOldState2NewStateMapping)));
			} catch (final AutomataOperationCanceledException aoce) {
				// just catch and ignore the exception, probably only a local timeout
				localResult = constructNoopMinimizationResult(true, operand);
			}
			minimizationResult = localResult;
			break;
		}
		case RAQ_DIRECT_SIMULATION: {
			minimizationResult = new MinimizationResult(true, true,
					new ReduceNwaDirectSimulation<>(autServices, predicateFactoryRefinement,
							(IDoubleDeckerAutomaton<LETTER, IPredicate>) operand, false, partition));
			break;
		}
		case RAQ_DIRECT_SIMULATION_B: {
			minimizationResult = new MinimizationResult(true, true, new ReduceNwaDirectSimulationB<>(autServices,
					predicateFactoryRefinement, (IDoubleDeckerAutomaton<LETTER, IPredicate>) operand));
			break;
		}
		case FULLMULTIPEBBLE_DIRECT_SIMULATION: {
			final AutomataLibraryServices autServicesWithTimeout = new AutomataLibraryServices(mServices, 
					DEFAULT_TIMEOUT_FOR_EXPENSIVE_NWA_MINIMIZATIONS);
			MinimizationResult localResult = null;
			try {
				localResult = new MinimizationResult(true, true, 
						new ReduceNwaDirectFullMultipebbleSimulation<LETTER, IPredicate>(autServicesWithTimeout,
								predicateFactoryRefinement, (IDoubleDeckerAutomaton<LETTER, IPredicate>) operand));
			} catch (final AutomataOperationCanceledException aoce) {
				// just catch and ignore the exception, probably only a local timeout
				localResult = constructNoopMinimizationResult(true, operand);
			}
			minimizationResult = localResult;
			break;
		}
		case NWA_COMBINATOR_MULTI_DEFAULT: {
			final IMinimizeNwa<LETTER, IPredicate> minNwa = new MinimizeNwaMulti<>(autServices,
					predicateFactoryRefinement, (IDoubleDeckerAutomaton<LETTER, IPredicate>) operand, partition,
					computeOldState2NewStateMapping);
			final boolean minimizationAttempt =
					((MinimizeNwaCombinator<LETTER, IPredicate>) minNwa).getMode() != MinimizationMethods.NONE;
			minimizationResult = new MinimizationResult(minimizationAttempt, true, minNwa);
			break;
		}
		case NWA_COMBINATOR_MULTI_SIMULATION: {
			final IMinimizeNwa<LETTER, IPredicate> minNwa = new MinimizeNwaMulti<>(autServices,
					predicateFactoryRefinement, (IDoubleDeckerAutomaton<LETTER, IPredicate>) operand, partition,
					computeOldState2NewStateMapping, Strategy.SIMULATION_BASED);
			final boolean minimizationAttempt =
					((MinimizeNwaCombinator<LETTER, IPredicate>) minNwa).getMode() != MinimizationMethods.NONE;
			minimizationResult = new MinimizationResult(minimizationAttempt, true, minNwa);
			break;
		}
		case DELAYED_SIMULATION: {
			minimizationResult = new MinimizationResult(true, true,
					new BuchiReduce<>(autServices, predicateFactoryRefinement, operand));
			break;
		}
		case FAIR_SIMULATION_WITH_SCC: {
			minimizationResult = new MinimizationResult(true, true,
					new ReduceBuchiFairSimulation<>(autServices, predicateFactoryRefinement, operand, true));
			break;
		}
		case FAIR_SIMULATION_WITHOUT_SCC: {
			minimizationResult = new MinimizationResult(true, true,
					new ReduceBuchiFairSimulation<>(autServices, predicateFactoryRefinement, operand, false));
			break;
		}
		case FAIR_DIRECT_SIMULATION: {
			minimizationResult = new MinimizationResult(true, true,
					new ReduceBuchiFairDirectSimulation<>(autServices, predicateFactoryRefinement, operand, true));
			break;
		}
		case RAQ_DELAYED_SIMULATION: {
			minimizationResult = new MinimizationResult(true, true,
					new ReduceNwaDelayedSimulation<>(autServices, predicateFactoryRefinement,
							(IDoubleDeckerAutomaton<LETTER, IPredicate>) operand, false, partition));
			break;
		}
		case RAQ_DELAYED_SIMULATION_B: {
			minimizationResult = new MinimizationResult(true, true, new ReduceNwaDelayedSimulationB<>(autServices,
					predicateFactoryRefinement, (IDoubleDeckerAutomaton<LETTER, IPredicate>) operand));
			break;
		}
		case FULLMULTIPEBBLE_DELAYED_SIMULATION: {
			final AutomataLibraryServices autServicesWithTimeout = new AutomataLibraryServices(mServices, 
					DEFAULT_TIMEOUT_FOR_EXPENSIVE_NWA_MINIMIZATIONS);
			MinimizationResult localResult = null;
			try {
				localResult = new MinimizationResult(true, true, 
						new ReduceNwaDelayedFullMultipebbleSimulation<LETTER, IPredicate>(autServicesWithTimeout,
								predicateFactoryRefinement, (IDoubleDeckerAutomaton<LETTER, IPredicate>) operand));
			} catch (final AutomataOperationCanceledException aoce) {
				// just catch and ignore the exception, probably only a local timeout
				localResult = constructNoopMinimizationResult(true, operand);
			}
			minimizationResult = localResult;
			break;
		}
		case NONE: {
			// no-op minimization
			final boolean minimizationAttempt = false;
			minimizationResult = constructNoopMinimizationResult(minimizationAttempt, operand);
			break;
		}
		default:
			throw new AssertionError("Unknown minimization method.");
		}
		return minimizationResult;
	}

	private MinimizationResult constructNoopMinimizationResult(final boolean minimizationAttempt,
			final INestedWordAutomaton<LETTER, IPredicate> operand) {
		final MinimizationResult minimizationResult;
		minimizationResult = new MinimizationResult(minimizationAttempt, false, new IMinimizeNwa<LETTER, IPredicate>() {
			@Override
			public INestedWordAutomaton<LETTER, IPredicate> getResult() {
				return operand;
			}

			@Override
			public boolean checkResult(final IStateFactory<IPredicate> stateFactory)
					throws AutomataLibraryException {
				return true;
			}
		});
		return minimizationResult;
	}

	public IDoubleDeckerAutomaton<LETTER, IPredicate> getMinimizedAutomaton() {
		return mMinimizedAutomaton;
	}

	public Map<IPredicate, IPredicate> getOldState2newStateMapping() {
		return mOldState2newState;
	}

	public boolean newAutomatonWasBuilt() {
		return mMinimizationResult.wasNewAutomatonBuilt();
	}

	public boolean wasMinimizationAttempted() {
		return mMinimizationResult.wasMinimizationAttempted();
	}

	public AutomataMinimizationStatisticsGenerator getStatistics() {
		return mStatistics;
	}

	private class MinimizationResult {
		private final boolean mNewAutomatonWasBuilt;
		private final boolean mMinimizationAttempt;
		private final IMinimizeNwa<LETTER, IPredicate> mRawMinimizationOutput;

		public MinimizationResult(final boolean minimizationAttempt, final boolean newAutomatonWasBuilt,
				final IMinimizeNwa<LETTER, IPredicate> rawMinimizationOutput) {
			super();
			mNewAutomatonWasBuilt = newAutomatonWasBuilt;
			mMinimizationAttempt = minimizationAttempt;
			mRawMinimizationOutput = rawMinimizationOutput;
		}

		public boolean wasNewAutomatonBuilt() {
			return mNewAutomatonWasBuilt;
		}

		public boolean wasMinimizationAttempted() {
			return mMinimizationAttempt;
		}

		public IMinimizeNwa<LETTER, IPredicate> getRawMinimizationOutput() {
			return mRawMinimizationOutput;
		}
	}

	public static class AutomataMinimizationTimeout extends Exception {
		private static final long serialVersionUID = 1L;
		private final AutomataMinimizationStatisticsGenerator mStatistics;
		private final AutomataOperationCanceledException mAutomataOperationCanceledException;

		public AutomataMinimizationTimeout(final AutomataOperationCanceledException aoce,
				final AutomataMinimizationStatisticsGenerator statistics) {
			mAutomataOperationCanceledException = aoce;
			mStatistics = statistics;
		}

		public AutomataMinimizationStatisticsGenerator getStatistics() {
			return mStatistics;
		}

		public AutomataOperationCanceledException getAutomataOperationCanceledException() {
			return mAutomataOperationCanceledException;
		}
	}

}
