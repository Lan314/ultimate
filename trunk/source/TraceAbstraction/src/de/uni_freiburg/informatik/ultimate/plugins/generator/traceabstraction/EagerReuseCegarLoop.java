/*
 * Copyright (C) 2017 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2017 University of Freiburg
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
package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.INwaOutgoingLetterAndTransitionProvider;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.Difference;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.IsIncluded;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.PowersetDeterminizer;
import de.uni_freiburg.informatik.ultimate.automata.nestedword.operations.oldapi.IOpWithDelayedDeadEndRemoval;
import de.uni_freiburg.informatik.ultimate.core.model.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.CfgSmtToolkit;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfg;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfgTransition;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.interpolantautomata.transitionappender.AbstractInterpolantAutomaton;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.PredicateFactory;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TAPreferences;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TraceAbstractionPreferenceInitializer.InterpolationTechnique;

/**
 * Subclass of {@link BasicCegarLoop} in which we initially subtract from the
 * abstraction a set of given Floyd-Hoare automata.
 * 
 * @author Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 *
 */
public class EagerReuseCegarLoop<LETTER extends IIcfgTransition<?>> extends BasicCegarLoop<LETTER> {

	private enum MinimizeInitially {
		NEVER, AFTER_EACH_DIFFERENCE, ONCE_AT_END
	};

	private final MinimizeInitially mMinimize = MinimizeInitially.AFTER_EACH_DIFFERENCE;

	private final List<AbstractInterpolantAutomaton<LETTER>> mFloydHoareAutomataFromOtherErrorLocations;
	private final List<NestedWordAutomaton<String, String>> mRawFloydHoareAutomataFromFile;

	/**
	 * The following can be costly. Enable only for debugging or analyzing our
	 * algorithm
	 */
	private static final boolean IDENTIFY_USELESS_FLOYDHOARE_AUTOMATA = false;

	public EagerReuseCegarLoop(final String name, final IIcfg<?> rootNode, final CfgSmtToolkit csToolkit,
			final PredicateFactory predicateFactory, final TAPreferences taPrefs,
			final Collection<? extends IcfgLocation> errorLocs, final InterpolationTechnique interpolation,
			final boolean computeHoareAnnotation, final IUltimateServiceProvider services,
			final IToolchainStorage storage,
			final List<AbstractInterpolantAutomaton<LETTER>> floydHoareAutomataFromOtherLocations,
			final List<NestedWordAutomaton<String, String>> rawFloydHoareAutomataFromFile) {
		super(name, rootNode, csToolkit, predicateFactory, taPrefs, errorLocs, interpolation, computeHoareAnnotation,
				services, storage);
		mFloydHoareAutomataFromOtherErrorLocations = floydHoareAutomataFromOtherLocations;
		mRawFloydHoareAutomataFromFile = rawFloydHoareAutomataFromFile;
	}

	@Override
	protected void getInitialAbstraction() throws AutomataLibraryException {
		super.getInitialAbstraction();

		final List<INestedWordAutomaton<LETTER, IPredicate>> floydHoareAutomataFromFiles = AutomataReuseUtils.interpretAutomata(
				mRawFloydHoareAutomataFromFile, (INestedWordAutomaton<LETTER, IPredicate>) mAbstraction,
				mPredicateFactoryInterpolantAutomata, mServices, mPredicateFactory, mLogger, mCsToolkit);

		mLogger.info("Reusing " + mFloydHoareAutomataFromOtherErrorLocations.size() + " Floyd-Hoare automata from previous error locations.");
		mLogger.info("Reusing " + floydHoareAutomataFromFiles.size() + " Floyd-Hoare automata from ats files.");

		final List<INwaOutgoingLetterAndTransitionProvider<LETTER, IPredicate>> reuseAutomata = new ArrayList<>();
		reuseAutomata.addAll(mFloydHoareAutomataFromOtherErrorLocations);
		reuseAutomata.addAll(floydHoareAutomataFromFiles);
				
		for (int i = 0; i < reuseAutomata.size(); i++) {
			final int oneBasedi = i+1;
			int internalTransitionsBeforeDifference = 0;
			int internalTransitionsAfterDifference = 0;
			final INwaOutgoingLetterAndTransitionProvider<LETTER, IPredicate> ai = reuseAutomata.get(i);
			if (ai instanceof AbstractInterpolantAutomaton<?>) {
				internalTransitionsBeforeDifference = ((AbstractInterpolantAutomaton<LETTER>)ai).computeNumberOfInternalTransitions();
				((AbstractInterpolantAutomaton<LETTER>)ai).switchToOnDemandConstructionMode();
				if (mPref.dumpAutomata()) {
					writeAutomatonToFile(ai, "ReusedAutomataFromErrorLocation"+oneBasedi);
				}
			} else {
				if (mPref.dumpAutomata()) {
					writeAutomatonToFile(ai, "ReusedAutomataFromFile"+oneBasedi);
				}
			}
			final PowersetDeterminizer<LETTER, IPredicate> psd = new PowersetDeterminizer<>(ai, true,
					mPredicateFactoryInterpolantAutomata);
			IOpWithDelayedDeadEndRemoval<LETTER, IPredicate> diff;
			final boolean explointSigmaStarConcatOfIA = true;
			diff = new Difference<LETTER, IPredicate>(new AutomataLibraryServices(mServices),
					mStateFactoryForRefinement,
					(INwaOutgoingLetterAndTransitionProvider<LETTER, IPredicate>) mAbstraction, ai, psd,
					explointSigmaStarConcatOfIA);
			if (mPref.dumpAutomata()) {
				final String filename = "DiffAfterEagerReuse" + oneBasedi;
				writeAutomatonToFile(diff.getResult(), filename);
			}
			if (ai instanceof AbstractInterpolantAutomaton<?>) {
				((AbstractInterpolantAutomaton<LETTER>)ai).switchToReadonlyMode();
				internalTransitionsAfterDifference = ((AbstractInterpolantAutomaton<LETTER>)ai).computeNumberOfInternalTransitions();
				mLogger.info("Floyd-Hoare automaton" + i + " had " + internalTransitionsAfterDifference
					+ " internal transitions before reuse, on-demand computation of difference added "
					+ (internalTransitionsAfterDifference - internalTransitionsBeforeDifference) + " more.");
			}
			if (REMOVE_DEAD_ENDS) {
				if (mComputeHoareAnnotation) {
					final Difference<LETTER, IPredicate> difference = (Difference<LETTER, IPredicate>) diff;
					mHaf.updateOnIntersection(difference.getFst2snd2res(), difference.getResult());
				}
				diff.removeDeadEnds();
				if (mComputeHoareAnnotation) {
					mHaf.addDeadEndDoubleDeckers(diff);
				}
			}
			if (IDENTIFY_USELESS_FLOYDHOARE_AUTOMATA) {
				final AutomataLibraryServices als = new AutomataLibraryServices(mServices);
				final Boolean noTraceExcluded = new IsIncluded<>(als, mPredicateFactoryResultChecking,
						(INwaOutgoingLetterAndTransitionProvider<LETTER, IPredicate>) mAbstraction, diff.getResult())
								.getResult();
				if (noTraceExcluded) {
					mLogger.warn("Floyd-Hoare automaton" + i
							+ " did not remove an error trace from abstraction and was hence useless for this error location.");
				} else {
					mLogger.info(
							"Floyd-Hoare automaton" + i + " removed at least one error trace from the abstraction.");
				}

			}
			mAbstraction = diff.getResult();

			if (mAbstraction.size() == 0) {
				// stop to compute differences if abstraction is already empty
				break;
			}

			if (mMinimize == MinimizeInitially.AFTER_EACH_DIFFERENCE) {
				minimizeAbstractionIfEnabled();
			}
		}
		if (mMinimize == MinimizeInitially.ONCE_AT_END) {
			minimizeAbstractionIfEnabled();
		}
	}
	
	


}
