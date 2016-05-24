/*
 * Copyright (C) 2014-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE LassoRanker Library.
 * 
 * The ULTIMATE LassoRanker Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE LassoRanker Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE LassoRanker Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE LassoRanker Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE LassoRanker Library grant you additional permission 
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.lassoranker.variables;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Util;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.Boogie2SMT;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.IFreshTermVariableConstructor;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.NonTheorySymbol;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.normalForms.Cnf;
import de.uni_freiburg.informatik.ultimate.util.datastructures.UnionFind;
import de.uni_freiburg.informatik.ultimate.util.relation.HashRelation;
import de.uni_freiburg.informatik.ultimate.util.relation.NestedMap2;

/**
 * Split lasso into independent components.
 * 
 * @author Matthias Heizmann
 *
 */
public class LassoPartitioneer {
	
	private final IUltimateServiceProvider mServices;
	private final IFreshTermVariableConstructor mFreshTermVariableConstructor;
	
	private final LassoUnderConstruction mLasso;
	
	private enum Part { STEM, LOOP };
	
	private final NestedMap2<Part, NonTheorySymbol<?>, TransFormulaLR> mSymbol2OriginalTF = 
			new NestedMap2<Part, NonTheorySymbol<?>, TransFormulaLR>();
	private HashRelation<NonTheorySymbol<?>, Term> mSymbol2StemConjuncts;
	/**
	 * NonTheorySymbols of stem that do not occur in any conjunct (only occur as
	 * inVar or outVar in original lasso.
	 */
	private HashSet<NonTheorySymbol<?>> mStemSymbolsWithoutConjuncts;
	private HashRelation<NonTheorySymbol<?>, Term> mSymbol2LoopConjuncts;
	/**
	 * NonTheorySymbols of loop that do not occur in any conjunct (only occur as
	 * inVar or outVar in original lasso.
	 */
	private HashSet<NonTheorySymbol<?>> mLoopSymbolsWithoutConjuncts;
	private List<Term> mStemConjunctsWithoutSymbols;
	private List<Term> mLoopConjunctsWithoutSymbols;
	private final UnionFind<NonTheorySymbol<?>> mEquivalentSymbols = new UnionFind<>();
	private final Set<RankVar> mAllRankVars = new HashSet<RankVar>();
	private final Script mScript;
	private final List<LassoUnderConstruction> mNewLassos = new ArrayList<>();
	private final Boogie2SMT mBoogie2Smt;
	
	
	public LassoPartitioneer(IUltimateServiceProvider services, 
			Boogie2SMT boogie2smt, 
			Script script, LassoUnderConstruction lasso) {
		mServices = services;
		mBoogie2Smt = boogie2smt;
		mFreshTermVariableConstructor = boogie2smt.getVariableManager();
		mScript = script;
		mLasso = lasso;
		doPartition();
//		assert checkStemImplications() : "stem problem";
	}

//	private boolean checkStemImplications() {
//		boolean result = true;
//		for (LassoUnderConstruction newLasso : mNewLassos) {
//			result &= checkStemImplication(newLasso);
//			assert result;
//		}
//		return result;
//	}
//	
//	private boolean checkStemImplication(LassoUnderConstruction newLasso) {
//		boolean result = TransFormulaUtils.implies(mLasso.getStem(), newLasso.getStem(), mScript, 
//				mBoogie2Smt.getBoogie2SmtSymbolTable(), 
//				mBoogie2Smt.getVariableManager()) != LBool.SAT;
//		return result;
//	}

	public List<LassoUnderConstruction> getNewLassos() {
		return mNewLassos;
	}

	private void doPartition() {
		mSymbol2StemConjuncts = new HashRelation<>();
		mSymbol2LoopConjuncts = new HashRelation<>();
		mStemSymbolsWithoutConjuncts = new HashSet<>();
		mLoopSymbolsWithoutConjuncts = new HashSet<>();
		mStemConjunctsWithoutSymbols = new ArrayList<>();
		mLoopConjunctsWithoutSymbols = new ArrayList<>();
		
		extractSymbols(Part.STEM, mLasso.getStem(), mSymbol2StemConjuncts, 
				mStemSymbolsWithoutConjuncts, mStemConjunctsWithoutSymbols);
		extractSymbols(Part.LOOP, mLasso.getLoop(), mSymbol2LoopConjuncts, 
				mLoopSymbolsWithoutConjuncts, mLoopConjunctsWithoutSymbols);
		
		for (RankVar rv : mAllRankVars) {
			Set<NonTheorySymbol<?>> symbols = new HashSet<NonTheorySymbol<?>>();
			extractInVarAndOutVarSymbols(rv, symbols, mLasso.getStem());
			extractInVarAndOutVarSymbols(rv, symbols, mLasso.getLoop()); 
			announceEquivalence(symbols);
		}


		for (NonTheorySymbol<?> equivalenceClassRepresentative : 
								mEquivalentSymbols.getAllRepresentatives()) {
			Set<NonTheorySymbol<?>> symbolEquivalenceClass = 
					mEquivalentSymbols.getEquivalenceClassMembers(equivalenceClassRepresentative);
			Set<Term> equivalentStemConjuncts = new HashSet<Term>();
			Set<Term> equivalentLoopConjuncts = new HashSet<Term>();
			Set<NonTheorySymbol<?>> equivalentStemSymbolsWithoutConjunct = new HashSet<NonTheorySymbol<?>>();
			Set<NonTheorySymbol<?>> equivalentLoopSymbolsWithoutConjunct = new HashSet<NonTheorySymbol<?>>();
			for (NonTheorySymbol<?> tv : symbolEquivalenceClass) {
				if (mSymbol2StemConjuncts.getDomain().contains(tv)) {
					equivalentStemConjuncts.addAll(mSymbol2StemConjuncts.getImage(tv));
				} else if (mStemSymbolsWithoutConjuncts.contains(tv)) {
					equivalentStemSymbolsWithoutConjunct.add(tv);
				} else if (mSymbol2LoopConjuncts.getDomain().contains(tv)) {
					equivalentLoopConjuncts.addAll(mSymbol2LoopConjuncts.getImage(tv));
				} else if (mLoopSymbolsWithoutConjuncts.contains(tv)) {
					equivalentLoopSymbolsWithoutConjunct.add(tv);
				} else {
					throw new AssertionError("unknown variable " + tv);
				}
			}
			if (equivalentStemConjuncts.isEmpty() && equivalentStemSymbolsWithoutConjunct.isEmpty() 
					&& equivalentLoopConjuncts.isEmpty() && equivalentLoopSymbolsWithoutConjunct.isEmpty()) {
				// do nothing
			} else {
				TransFormulaLR stemTransformulaLR = constructTransFormulaLR(Part.STEM, equivalentStemConjuncts, equivalentStemSymbolsWithoutConjunct);
				TransFormulaLR loopTransformulaLR = constructTransFormulaLR(Part.LOOP, equivalentLoopConjuncts, equivalentLoopSymbolsWithoutConjunct);
				mNewLassos.add(new LassoUnderConstruction(stemTransformulaLR, loopTransformulaLR));
			}
		}
		
		if (emptyOrTrue(mStemConjunctsWithoutSymbols) && emptyOrTrue(mLoopConjunctsWithoutSymbols)) {
			// do nothing
		} else {
			TransFormulaLR stemTransformulaLR = constructTransFormulaLR(Part.STEM, mStemConjunctsWithoutSymbols);
			TransFormulaLR loopTransformulaLR = constructTransFormulaLR(Part.LOOP, mLoopConjunctsWithoutSymbols);
			mNewLassos.add(new LassoUnderConstruction(stemTransformulaLR, loopTransformulaLR));
		}
	}



	private boolean emptyOrTrue(List<Term> terms) {
		if (terms.isEmpty()) {
			return true;
		} else {
			Term conjunction = SmtUtils.and(mScript, terms);
			return (conjunction.toString().equals("true"));
		}
	}

	private void extractInVarAndOutVarSymbols(RankVar rv,
			Set<NonTheorySymbol<?>> symbols, TransFormulaLR transFormulaLR) {
		Term inVar = transFormulaLR.getInVars().get(rv);
		if (inVar != null) {
			symbols.add(constructSymbol(inVar));
		}
		Term outVar = transFormulaLR.getOutVars().get(rv);
		if (outVar != null) {
			symbols.add(constructSymbol(outVar));
		}
		assert (inVar == null) == (outVar == null) : "both or none";
	}
	
	private TransFormulaLR constructTransFormulaLR(
			Part part, Set<Term> equivalentConjuncts, Set<NonTheorySymbol<?>> equivalentVariablesWithoutConjunct) {
		TransFormulaLR transformulaLR;
		Term formula = Util.and(mScript, equivalentConjuncts.toArray(new Term[equivalentConjuncts.size()]));
		transformulaLR = new TransFormulaLR(formula);
		for (NonTheorySymbol<?> symbol : NonTheorySymbol.extractNonTheorySymbols(formula)) {
			addInOuAuxVar(part, transformulaLR, symbol);
		}
		for (NonTheorySymbol<?> symbol : equivalentVariablesWithoutConjunct) {
			addInOuAuxVar(part, transformulaLR, symbol);
		}
		return transformulaLR;
	}
	
	private TransFormulaLR constructTransFormulaLR(
			Part part, List<Term> conjunctsWithoutSymbols) {
		TransFormulaLR transformulaLR;
		Term formula = Util.and(mScript, conjunctsWithoutSymbols.toArray(new Term[conjunctsWithoutSymbols.size()]));
		transformulaLR = new TransFormulaLR(formula);
		return transformulaLR;
	}


	private void addInOuAuxVar(Part part, TransFormulaLR transformulaLR, NonTheorySymbol<?> symbol) {
		TransFormulaLR original = mSymbol2OriginalTF.get(part, symbol);
		boolean isConstant;
		Term term;
		if (symbol instanceof NonTheorySymbol.Variable) {
			term = (Term) symbol.getSymbol();
			isConstant = false;
		} else if (symbol instanceof NonTheorySymbol.Constant) {
			term = (Term) symbol.getSymbol();
			isConstant = true;
		} else {
			throw new UnsupportedOperationException("function symbols not yet supported");
		}
		RankVar inVarRankVar = original.getInVarsReverseMapping().get(term);
		RankVar outVarRankVar = original.getOutVarsReverseMapping().get(term);
		boolean isAuxVar = original.getAuxVars().contains(term);
		assert (isConstant || !isAuxVar || (inVarRankVar == null && outVarRankVar == null)) : "auxVar may neither be inVar nor outVar";
		assert (isConstant || !(inVarRankVar == null && outVarRankVar == null) || isAuxVar) : "neither inVar nor outVar may be auxVar";
		if (inVarRankVar != null) {
			transformulaLR.addInVar(inVarRankVar, term);
		}
		if (outVarRankVar != null) {
			transformulaLR.addOutVar(outVarRankVar, term);
		}
		if (isAuxVar) {
			transformulaLR.addAuxVars(Collections.singleton((TermVariable) term));
		}
	}


	private HashRelation<NonTheorySymbol<?>, Term> extractSymbols(
			Part part, TransFormulaLR tf, 
			HashRelation<NonTheorySymbol<?>, Term> symbol2Conjuncts, 
			HashSet<NonTheorySymbol<?>> symbolsWithoutConjuncts,
			List<Term> conjunctsWithoutSymbols) {
		mAllRankVars.addAll(tf.getInVars().keySet());
		mAllRankVars.addAll(tf.getOutVars().keySet());
		//FIXME CNF conversion should be done in advance if desired
		Term cnf = (new Cnf(mScript, mServices, mFreshTermVariableConstructor)).transform(tf.getFormula());
		Term[] conjuncts = SmtUtils.getConjuncts(cnf);
		for (Term conjunct : conjuncts) {
			Set<NonTheorySymbol<?>> allSymbolsOfConjunct = NonTheorySymbol.extractNonTheorySymbols(conjunct);
			if (allSymbolsOfConjunct.isEmpty()) {
				conjunctsWithoutSymbols.add(conjunct);
			} else {
				for (NonTheorySymbol<?> symbol : allSymbolsOfConjunct) {
					TransFormulaLR oldValue = mSymbol2OriginalTF.put(part, symbol, tf);
					assert oldValue == null || oldValue == tf : "may not be modified";
					allSymbolsOfConjunct.add(symbol);
					if (mEquivalentSymbols.find(symbol) == null) {
						mEquivalentSymbols.makeEquivalenceClass(symbol);
					}
					symbol2Conjuncts.addPair(symbol, conjunct);
				}
				announceEquivalence(allSymbolsOfConjunct);
			}
		}
		for (Entry<RankVar, Term> entry : tf.getInVars().entrySet()) {
			addIfNotAlreadyAdded(part, symbolsWithoutConjuncts, tf, entry.getValue(), symbol2Conjuncts);
		}
		for (Entry<RankVar, Term> entry : tf.getOutVars().entrySet()) {
			addIfNotAlreadyAdded(part, symbolsWithoutConjuncts, tf, entry.getValue(), symbol2Conjuncts);
		}
		return symbol2Conjuncts;
	}


	private void addIfNotAlreadyAdded(
			Part part, HashSet<NonTheorySymbol<?>> symbolsWithoutConjuncts, TransFormulaLR tf,
			Term tvOrConstant,
			HashRelation<NonTheorySymbol<?>, Term> symbol2Conjuncts) {
		NonTheorySymbol<?> symbol = constructSymbol(tvOrConstant);
		if (!symbol2Conjuncts.getDomain().contains(symbol) && !symbolsWithoutConjuncts.contains(symbol)) {
			if (mEquivalentSymbols.find(symbol) == null) {
				// check needed because constants may occur in stem and loop.
				mEquivalentSymbols.makeEquivalenceClass(symbol);
			}
			symbolsWithoutConjuncts.add(symbol);
			TransFormulaLR oldValue = mSymbol2OriginalTF.put(part, symbol, tf);
			assert oldValue == null || oldValue == tf : "may not be modified";
		}
	}


	private NonTheorySymbol<?> constructSymbol(Term tvOrConstant) {
		if (tvOrConstant instanceof TermVariable) {
			return new NonTheorySymbol.Variable((TermVariable) tvOrConstant);
		} else {
			if (SmtUtils.isConstant(tvOrConstant)) {
				return new NonTheorySymbol.Constant((ApplicationTerm) tvOrConstant);
			} else {
				throw new IllegalArgumentException();
			}
		}
	}


	private void announceEquivalence(Set<NonTheorySymbol<?>> allSymbolsOfConjunct) {
		NonTheorySymbol<?> last = null;
		for (NonTheorySymbol<?> symbol : allSymbolsOfConjunct) {
			if (last != null) {
				mEquivalentSymbols.union(symbol, last);
			}
			last = symbol;
		}
	}

}
