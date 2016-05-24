/*
 * Copyright (C) 2015 Betim Musa (musab@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
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

import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ProgramPoint;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.ISLPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TAPreferences;

/**
 * 
 * @author musab@informatik.uni-freiburg.de
 *
 */
public class PredicateFactoryForInterpolantConsolidation extends PredicateFactoryForInterpolantAutomata {
	
	private Map<IPredicate, Set<IPredicate>> mLocationsToSetOfPredicates;
	private Map<IPredicate, AbstractMap.SimpleEntry<IPredicate, IPredicate>> mIntersectedPredicateToArgumentPredicates;
	private Map<AbstractMap.SimpleEntry<IPredicate, IPredicate>, IPredicate> mArgumentPredicatesToIntersectedPredicate;
	public PredicateFactoryForInterpolantConsolidation(SmtManager smtManager,
			TAPreferences taPrefs) {
		super(smtManager, taPrefs);
		mLocationsToSetOfPredicates = new HashMap<IPredicate, Set<IPredicate>>();
		mIntersectedPredicateToArgumentPredicates = new HashMap<IPredicate, AbstractMap.SimpleEntry<IPredicate, IPredicate>>();
		mArgumentPredicatesToIntersectedPredicate = new HashMap<AbstractMap.SimpleEntry<IPredicate, IPredicate>, IPredicate>();
	}

	public Map<IPredicate, Set<IPredicate>> getLocationsToSetOfPredicates() {
		return mLocationsToSetOfPredicates;
	}
	
	/**
	 * Remove the predicates from the set of the consolidated predicates which only lead to a final state in the difference automaton. 
	 * @param badPredicates - a set of states from the difference automaton
	 */
	public void removeBadPredicates(Set<IPredicate> badPredicates) {
		for (IPredicate p : badPredicates) {
			AbstractMap.SimpleEntry<IPredicate, IPredicate> argumentPredicates = mIntersectedPredicateToArgumentPredicates.get(p);
			Set<IPredicate> consolidatePredicates = mLocationsToSetOfPredicates.get(argumentPredicates.getKey());
			consolidatePredicates.remove(argumentPredicates.getValue());
		}
	}
	
	public IPredicate getIntersectedPredicate(IPredicate argumentPredicate1, IPredicate argumentPredicate2) {
		AbstractMap.SimpleEntry<IPredicate, IPredicate> predicateArguments = new AbstractMap.SimpleEntry<IPredicate, IPredicate>(argumentPredicate1, argumentPredicate2);
		return mArgumentPredicatesToIntersectedPredicate.get(predicateArguments);
	}
	
	@Override
	public IPredicate intersection(IPredicate p1, IPredicate p2) {
		// 1. Do the intersection
		assert (p1 instanceof ISLPredicate);
		
		ProgramPoint pp = ((ISLPredicate) p1).getProgramPoint();
		
		Term conjunction = super.mSmtManager.getPredicateFactory().and(p1, p2);
		IPredicate result = super.mSmtManager.getPredicateFactory().newSPredicate(pp, conjunction);
		
		if (mIntersectedPredicateToArgumentPredicates.containsKey(result)) {
			throw new AssertionError("States of difference automaton are not unique!");
		}
		AbstractMap.SimpleEntry<IPredicate, IPredicate> predicateArguments = new AbstractMap.SimpleEntry<IPredicate, IPredicate>(p1, p2);
		mIntersectedPredicateToArgumentPredicates.put(result, predicateArguments);
		mArgumentPredicatesToIntersectedPredicate.put(predicateArguments, result);
		
		// 2. Store the predicates in the map
		if (mLocationsToSetOfPredicates.containsKey(p1)) {
			Set<IPredicate> predicates = mLocationsToSetOfPredicates.get(p1);
			predicates.add(p2);
		} else {
			Set<IPredicate> predicatesForThisLocation = new HashSet<IPredicate>();
			predicatesForThisLocation.add(p2);
			mLocationsToSetOfPredicates.put(p1, predicatesForThisLocation);
		}
		return result;
	}

	public void removeConsolidatedPredicatesOnDifferentLevels(Map<IPredicate, Integer> stateToLevel) {
		int maxLevel = Collections.max(stateToLevel.values());
		for (IPredicate loc : mLocationsToSetOfPredicates.keySet()) {
			Set<IPredicate> consolidatedPreds = mLocationsToSetOfPredicates.get(loc);
			if (!consolidatedPreds.isEmpty()) {
				Set<IPredicate> predsToRemove = new HashSet<IPredicate>();
				int[] levelOccurrencesOfPredicates = new int[maxLevel];
				for (IPredicate p : consolidatedPreds) {
					IPredicate diffAutomatonState = getIntersectedPredicate(loc, p);
					int lvlOfState = stateToLevel.get(diffAutomatonState);
					levelOccurrencesOfPredicates[lvlOfState-1]++;
				}
				int lvlThatOccursMost = getIndexOfMaxValue(levelOccurrencesOfPredicates);
				if (levelOccurrencesOfPredicates[lvlThatOccursMost-1] <= 1) predsToRemove = consolidatedPreds;
				else {
					for (IPredicate p : consolidatedPreds) {
						IPredicate diffAutomatonState = getIntersectedPredicate(loc, p);
						int lvlOfState = stateToLevel.get(diffAutomatonState);
						if (lvlOfState != lvlThatOccursMost) {
							predsToRemove.add(p);
						}
					}
				}
				// Remove states that occur on different levels than lvlThatOccursMost from consolidated predicates
				consolidatedPreds.removeAll(predsToRemove);
			}
		}
	}

	private int getIndexOfMaxValue(int[] levelOccurrencesOfPredicates) {
		int indexOfMaxValue = 0;
		for (int i = 1; i < levelOccurrencesOfPredicates.length; i++) {
			if (levelOccurrencesOfPredicates[i] > levelOccurrencesOfPredicates[indexOfMaxValue]) indexOfMaxValue = i;
		}
		return indexOfMaxValue;
	}
}
