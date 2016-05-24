/*
 * Copyright (C) 2013-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE BuchiAutomizer plug-in.
 * 
 * The ULTIMATE BuchiAutomizer plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE BuchiAutomizer plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE BuchiAutomizer plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE BuchiAutomizer plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE BuchiAutomizer plug-in grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.generator.buchiautomizer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.boogie.BoogieNonOldVar;
import de.uni_freiburg.informatik.ultimate.boogie.BoogieOldVar;
import de.uni_freiburg.informatik.ultimate.boogie.BoogieVar;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Util;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Boogie2SMT;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.ModifiableGlobalVariableManager;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.TransFormula.Infeasibility;

public class BuchiModGlobalVarManager extends ModifiableGlobalVariableManager {
	private final BoogieNonOldVar mUnseeded;
	private final BoogieNonOldVar[] mOldRank;
	private final BoogieOldVar mUnseededOldVar;
	private final BoogieOldVar[] mOldRankOldVar;
	private final Boogie2SMT mBoogie2smt;
	private final Script mScript;
	
	private final Map<String, TransFormula> mProc2OldVarsAssignment;
	private final Map<String, TransFormula> mProc2GlobalVarsAssignment;

	public BuchiModGlobalVarManager(BoogieNonOldVar unseeded, BoogieNonOldVar[] oldRank,
			ModifiableGlobalVariableManager modifiableGlobalVariableManager, 
			Boogie2SMT boogie2Smt) {
		super(modifiableGlobalVariableManager);
		mBoogie2smt = boogie2Smt;
		mUnseeded = unseeded;
		mUnseededOldVar = unseeded.getOldVar();
		assert mUnseededOldVar != null : "oldVar missing";
		mOldRank = oldRank;
		mOldRankOldVar = new BoogieOldVar[oldRank.length];
		for (int i=0; i<oldRank.length; i++) {
			mOldRankOldVar[i] = oldRank[i].getOldVar();
			assert mOldRankOldVar[i] != null : "oldVar missing";
		}
		mScript  = boogie2Smt.getScript();
		mProc2OldVarsAssignment = new HashMap<String, TransFormula>();
		mProc2GlobalVarsAssignment = new HashMap<String, TransFormula>();
	}

	
	@Override
	public TransFormula getOldVarsAssignment(String proc) {
		TransFormula oldVarsAssignment = mProc2OldVarsAssignment.get(proc);
		if (oldVarsAssignment == null) {
			oldVarsAssignment = constructOldVarsAssignment(proc);
			mProc2OldVarsAssignment.put(proc, oldVarsAssignment);
		}
		return mProc2OldVarsAssignment.get(proc);
	}
	
	

	@Override
	public TransFormula getGlobalVarsAssignment(String proc) {
		TransFormula globalVarsAssignment = mProc2GlobalVarsAssignment.get(proc);
		if (globalVarsAssignment == null) {
			globalVarsAssignment = constructGlobalVarsAssignment(proc);
			mProc2GlobalVarsAssignment.put(proc, globalVarsAssignment);
		}
		return mProc2GlobalVarsAssignment.get(proc);
	}
	
	
	private TransFormula constructOldVarsAssignment(String proc) {
		TransFormula without = super.getOldVarsAssignment(proc);
		
		Term formula = without.getFormula();
		Map<BoogieVar, TermVariable> inVars = 
				new HashMap<BoogieVar, TermVariable>(without.getInVars());
		Map<BoogieVar, TermVariable> outVars = 
				new HashMap<BoogieVar, TermVariable>(without.getOutVars());
		Set<TermVariable> auxVars = without.getAuxVars();
		Set<TermVariable> branchEncoders = without.getBranchEncoders();
		assert branchEncoders.isEmpty();
		Infeasibility infeasibility = without.isInfeasible();
		assert infeasibility == Infeasibility.UNPROVEABLE;
		formula = Util.and(mScript, formula, oldVarEquality(mUnseeded, mUnseededOldVar));
		inVars.put(mUnseeded, mUnseeded.getTermVariable());
		outVars.put(mUnseeded, mUnseeded.getTermVariable());
		outVars.put(mUnseededOldVar, mUnseededOldVar.getTermVariable());
		for (int i=0; i<mOldRank.length; i++) {
			formula = Util.and(mScript, formula, oldVarEquality(mOldRank[i], mOldRankOldVar[i]));
			inVars.put(mOldRank[i], mOldRank[i].getTermVariable());
			outVars.put(mOldRank[i], mOldRank[i].getTermVariable());
			outVars.put(mOldRankOldVar[i], mOldRankOldVar[i].getTermVariable());
		}
		Term closedFormula = TransFormula.computeClosedFormula(
				formula, inVars, outVars, auxVars, false, mBoogie2smt);
		TransFormula result = new TransFormula(formula, inVars, outVars, 
				auxVars, branchEncoders, infeasibility, closedFormula);
		return result;
	}
	
	
	private TransFormula constructGlobalVarsAssignment(String proc) {
		TransFormula without = super.getGlobalVarsAssignment(proc);
		
		Term formula = without.getFormula();
		Map<BoogieVar, TermVariable> inVars = 
				new HashMap<BoogieVar, TermVariable>(without.getInVars());
		Map<BoogieVar, TermVariable> outVars = 
				new HashMap<BoogieVar, TermVariable>(without.getOutVars());
		Set<TermVariable> auxVars = without.getAuxVars();
		Set<TermVariable> branchEncoders = without.getBranchEncoders();
		assert branchEncoders.isEmpty();
		Infeasibility infeasibility = without.isInfeasible();
		assert infeasibility == Infeasibility.UNPROVEABLE;
		formula = Util.and(mScript, formula, oldVarEquality(mUnseeded, mUnseededOldVar));
		inVars.put(mUnseededOldVar, mUnseededOldVar.getTermVariable());
		outVars.put(mUnseededOldVar, mUnseededOldVar.getTermVariable());
		outVars.put(mUnseeded, mUnseeded.getTermVariable());
		for (int i=0; i<mOldRank.length; i++) {
			formula = Util.and(mScript, formula, oldVarEquality(mOldRank[i], mOldRankOldVar[i]));
			inVars.put(mOldRankOldVar[i], mOldRankOldVar[i].getTermVariable());
			outVars.put(mOldRankOldVar[i], mOldRankOldVar[i].getTermVariable());
			outVars.put(mOldRank[i], mOldRank[i].getTermVariable());
		}
		Term closedFormula = TransFormula.computeClosedFormula(
				formula, inVars, outVars, auxVars, false, mBoogie2smt);
		TransFormula result = new TransFormula(formula, inVars, outVars, 
				auxVars, branchEncoders, infeasibility, closedFormula);
		return result;
	}


	
	public Term oldVarEquality(BoogieVar var, BoogieVar oldVar) {
		return mScript.term("=", var.getTermVariable(), oldVar.getTermVariable());
	}


	@Override
	public Map<String, BoogieNonOldVar> getGlobals() {
		HashMap<String, BoogieNonOldVar> result = 
				new HashMap<String, BoogieNonOldVar>(super.getGlobals());
		for (int i=0; i<mOldRank.length; i++) {
			result.put(mOldRank[i].getIdentifier(),mOldRank[i]);
		}
		result.put(mUnseeded.getIdentifier(),mUnseeded);
		return result;
	}

}
