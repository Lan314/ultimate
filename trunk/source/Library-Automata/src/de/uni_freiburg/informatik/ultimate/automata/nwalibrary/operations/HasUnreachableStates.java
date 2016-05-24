/*
 * Copyright (C) 2011-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operations;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryException;
import de.uni_freiburg.informatik.ultimate.automata.AutomataLibraryServices;
import de.uni_freiburg.informatik.ultimate.automata.IOperation;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.DoubleDecker;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonOldApi;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.operationsOldApi.DoubleDeckerVisitor;


/**
 * Check if an NWA contains states which are not reachable in any run.
 * Does not change the input automaton.
 * @author heizmann@informatik.uni-freiburg.de
 *
 * @param <LETTER>
 * @param <STATE>
 */
public class HasUnreachableStates<LETTER,STATE> extends DoubleDeckerVisitor<LETTER,STATE>
										   implements IOperation<LETTER,STATE> {
	private final Set<STATE> mVisitedStates = new HashSet<STATE>();
	private int mUnreachalbeStates = 0;

	
	public HasUnreachableStates(AutomataLibraryServices services,
			INestedWordAutomatonOldApi<LETTER,STATE> operand) throws AutomataLibraryException {
		super(services);
		mTraversedNwa = operand;
		mLogger.info(startMessage());
		traverseDoubleDeckerGraph();
		for (STATE state : mTraversedNwa.getStates()) {
			if (!mVisitedStates.contains(state)) {
				mUnreachalbeStates++;
				mLogger.warn("Unreachalbe state: " + state);
			}
		}
		mLogger.info(exitMessage());
	}

	@Override
	protected Collection<STATE> getInitialStates() {
		mVisitedStates.addAll(mTraversedNwa.getInitialStates());
		return mTraversedNwa.getInitialStates();
	}

	@Override
	protected Collection<STATE> visitAndGetInternalSuccessors(
			DoubleDecker<STATE> doubleDecker) {
		STATE state = doubleDecker.getUp();
		Set<STATE> succs = new HashSet<STATE>();
		for (LETTER letter : mTraversedNwa.lettersInternal(state)) {
			for (STATE succ : mTraversedNwa.succInternal(state, letter)) {
				succs.add(succ);
			}
		}
		mVisitedStates.addAll(succs);
		return succs;
	}
	
	

	@Override
	protected Collection<STATE> visitAndGetCallSuccessors(
			DoubleDecker<STATE> doubleDecker) {
		STATE state = doubleDecker.getUp();
		Set<STATE> succs = new HashSet<STATE>();
		for (LETTER letter : mTraversedNwa.lettersCall(state)) {
			for (STATE succ : mTraversedNwa.succCall(state, letter)) {
				succs.add(succ);
			}
		}
		mVisitedStates.addAll(succs);
		return succs;
	}
	
	

	@Override
	protected Collection<STATE> visitAndGetReturnSuccessors(
			DoubleDecker<STATE> doubleDecker) {
		STATE state = doubleDecker.getUp();
		STATE hier = doubleDecker.getDown();
		Set<STATE> succs = new HashSet<STATE>();
		for (LETTER letter : mTraversedNwa.lettersReturn(state)) {
			for (STATE succ : mTraversedNwa.succReturn(state, hier, letter)) {
				succs.add(succ);
			}
		}
		mVisitedStates.addAll(succs);
		return succs;
	}

	@Override
	public String operationName() {
		return "detectUnreachableStates";
	}

	@Override
	public String startMessage() {
		return "Start " + operationName() + " Operand " + 
			mTraversedNwa.sizeInformation();
	}
	
	
	@Override
	public String exitMessage() {
		return "Finished " + operationName() + " Operand has " + 
			mUnreachalbeStates + " unreachalbe states";
	}
	
	public boolean result() {
		return mUnreachalbeStates != 0;
	}

	@Override
	public boolean checkResult(StateFactory<STATE> stateFactory)
			throws AutomataLibraryException {
		return true;
	}
	

}
