/*
 * Copyright (C) 2014-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2012-2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE ModelCheckerUtils Library.
 * 
 * The ULTIMATE ModelCheckerUtils Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE ModelCheckerUtils Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE ModelCheckerUtils Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE ModelCheckerUtils Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE ModelCheckerUtils Library grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.partialQuantifierElimination;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import de.uni_freiburg.informatik.ultimate.core.lib.exceptions.ToolchainCanceledException;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.QuantifiedFormula;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.PartialQuantifierElimination;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils.XnfConversionTechnique;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.MultiDimensionalSelect;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.AffineRelation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.AffineRelation.TransformInequality;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.BinaryNumericRelation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.BinaryRelation.NoRelationOfThisKindException;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.linearTerms.NotAffineException;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.managedscript.ManagedScript;

/**
 * Transitive inequality resolution (TIR) for terms in XNF.
 * @author Matthias Heizmann
 */
public class XnfTir extends XjunctPartialQuantifierElimination {
	
	private final XnfConversionTechnique mXnfConversionTechnique;

	public XnfTir(final ManagedScript script, final IUltimateServiceProvider services,
			final XnfConversionTechnique XnfConversionTechnique) {
		super(script, services);
		mXnfConversionTechnique = XnfConversionTechnique;
	}

	@Override
	public String getName() {
		return "transitive inequality resolution";
	}

	@Override
	public String getAcronym() {
		return "TIR";
	}
	
	@Override
	public boolean resultIsXjunction() {
		return false;
	}

	
	public enum BoundType { UPPER, LOWER }

	@Override
	public Term[] tryToEliminate(final int quantifier, final Term[] inputConjuncts,
			final Set<TermVariable> eliminatees) {
		final Term inputConjunction = PartialQuantifierElimination.applyDualFiniteConnective(mScript, quantifier, Arrays.asList(inputConjuncts));
		List<Term> currentDisjuncts = new ArrayList<Term>(Arrays.asList(inputConjunction));
		final Iterator<TermVariable> it = eliminatees.iterator();
		while (it.hasNext()) {
			List<Term> nextDisjuncts = new ArrayList<Term>();
			final TermVariable eliminatee = it.next();
			if (!eliminatee.getSort().isNumericSort()) {
				// this technique is not applicable
				nextDisjuncts = currentDisjuncts;
				continue;
			}
			boolean unableToRemoveEliminatee = false;
			for (final Term oldDisjunct : currentDisjuncts) {
				final List<Term> elimResultDisjuncts = tryToEliminate_singleDisjuct(quantifier, oldDisjunct, eliminatee);
				if (elimResultDisjuncts == null) {
					// unable to eliminate
					unableToRemoveEliminatee = true;
					nextDisjuncts.add(oldDisjunct);
				} else {
					nextDisjuncts.addAll(elimResultDisjuncts);
				}
			}
			if (unableToRemoveEliminatee) {
				// not eliminated :-(
			} else {
				it.remove();
			}
			currentDisjuncts = nextDisjuncts;
		}
		final Term[] resultDisjuncts = currentDisjuncts.toArray(new Term[currentDisjuncts.size()]);
		final Term resultDisjunction =  PartialQuantifierElimination.applyCorrespondingFiniteConnective(mScript, quantifier, resultDisjuncts);
		return new Term[] { resultDisjunction };
	}

	private List<Term> tryToEliminate_singleDisjuct(final int quantifier, final Term disjunct,
			final TermVariable eliminatee) {
		final Term[] conjuncts = PartialQuantifierElimination.getXjunctsInner(quantifier, disjunct);
		final List<Term> result = tryToEliminate_Conjuncts(quantifier, conjuncts, eliminatee);
//		Following lines used for debugging - remove them
//		Term term = SmtUtils.or(mScript, (Collection<Term>) result);
//		term = SmtUtils.simplify(mScript, term, mServices);
//		result = Arrays.asList(PartialQuantifierElimination.getXjunctsOuter(quantifier, term));
//
		return result;
	}
	
	private List<Term> tryToEliminate_Conjuncts(final int quantifier, final Term[] inputAtoms,
			final TermVariable eliminatee) {
		final List<Term> termsWithoutEliminatee = new ArrayList<Term>();
		final List<Bound> upperBounds = new ArrayList<Bound>();
		final List<Bound> lowerBounds = new ArrayList<Bound>();
		final List<Term> antiDer = new ArrayList<Term>();

		
		for (final Term term : inputAtoms) {
			if (!Arrays.asList(term.getFreeVars()).contains(eliminatee)) {
				termsWithoutEliminatee.add(term);
			} else {
				ApplicationTerm eliminateeOnLhs;
				AffineRelation rel;
				try {
					TransformInequality transform;
					if (quantifier == QuantifiedFormula.EXISTS) {
						transform = TransformInequality.STRICT2NONSTRICT;
					} else if (quantifier == QuantifiedFormula.FORALL) {
						transform = TransformInequality.NONSTRICT2STRICT;
					} else {
						throw new AssertionError("unknown quantifier");
					}
					 rel = new AffineRelation(mScript, term, transform);
				} catch (final NotAffineException e) {
					// no chance to eliminate the variable
					return null;
				}
				if (!rel.isVariable(eliminatee)) {
					// eliminatee occurs probably only in select
					return null;
				}
				try {
					eliminateeOnLhs = rel.onLeftHandSideOnly(mScript, eliminatee);
				} catch (final NotAffineException e) {
					// no chance to eliminate the variable
					return null;
				}
				if (!SmtUtils.occursAtMostAsLhs(eliminatee, eliminateeOnLhs)) {
					// eliminatee occurs additionally in rhs e.g., inside a
					// select or modulo term.
					return null;
				}
				try {
					final BinaryNumericRelation bnr = new BinaryNumericRelation(eliminateeOnLhs);
					switch (bnr.getRelationSymbol()) {
					case DISTINCT:
						if (quantifier == QuantifiedFormula.EXISTS) {
							 antiDer.add(bnr.getRhs());
						} else {
							assert occursInsideSelectTerm(term, eliminatee) : "should have been removed by DER";
							// no chance to eliminate the variable
						}
						break;
					case EQ:
						if (quantifier == QuantifiedFormula.FORALL) {
							 antiDer.add(bnr.getRhs());
						} else {
							assert occursInsideSelectTerm(term, eliminatee) : "should have been removed by DER";
							// no chance to eliminate the variable
						}
						break;
					case GEQ:
						lowerBounds.add(new Bound(false, bnr.getRhs()));
						break;
					case GREATER:
						lowerBounds.add(new Bound(true, bnr.getRhs()));
						break;
					case LEQ:
						upperBounds.add(new Bound(false, bnr.getRhs()));
						break;
					case LESS:
						upperBounds.add(new Bound(true, bnr.getRhs()));
						break;
					default:
						throw new AssertionError();
					}
				} catch (final NoRelationOfThisKindException e) {
					throw new AssertionError();
				}
			}
		}
		final BuildingInstructions bi = new BuildingInstructions(quantifier,
				eliminatee.getSort(),
				termsWithoutEliminatee,
				upperBounds,
				lowerBounds,
				antiDer);
		final List<Term> resultAtoms = new ArrayList<Term>();
		for (final Bound lowerBound : lowerBounds) {
			for (final Bound upperBound : upperBounds) {
				resultAtoms.add(buildInequality(quantifier, lowerBound, upperBound));
			}
		}
		resultAtoms.addAll(termsWithoutEliminatee);
		final List<Term> resultDisjunctions;
		if (antiDer.isEmpty()) {
			resultDisjunctions = new ArrayList<Term>();
			final Term tmp = PartialQuantifierElimination.applyDualFiniteConnective(mScript, quantifier, resultAtoms);
			assert !Arrays.asList(tmp.getFreeVars()).contains(eliminatee) : "not eliminated";
			resultDisjunctions.add(tmp);
		} else {
			resultAtoms.add(bi.computeAdDisjunction());
			final Term tmp = PartialQuantifierElimination.applyDualFiniteConnective(mScript, quantifier, resultAtoms);
			Term disjunction;
			if (quantifier == QuantifiedFormula.EXISTS) {
				disjunction = SmtUtils.toDnf(mServices, mMgdScript, tmp, mXnfConversionTechnique);
			} else if (quantifier == QuantifiedFormula.FORALL) {
				disjunction = SmtUtils.toCnf(mServices, mMgdScript, tmp, mXnfConversionTechnique);
			} else {
				throw new AssertionError("unknown quantifier");
			}
			assert !Arrays.asList(disjunction.getFreeVars()).contains(eliminatee) : "not eliminated";
			resultDisjunctions = Arrays.asList(PartialQuantifierElimination.getXjunctsOuter(quantifier, disjunction));
		}
		return resultDisjunctions;
	}

	/**
	 * 	@return true iff tv is subterm of some select term in term.
	 */
	private boolean occursInsideSelectTerm(final Term term, final TermVariable tv) {
		final List<MultiDimensionalSelect> selectTerms = MultiDimensionalSelect.extractSelectShallow(term, true);
		for (final MultiDimensionalSelect mds : selectTerms) {
			for (final Term index : mds.getIndex()) {
				if (Arrays.asList(index.getFreeVars()).contains(tv)) {
					return true;
				}
			}
			if (Arrays.asList(mds.getSelectTerm().getFreeVars()).contains(tv)) {
				return true;
			}
			if (Arrays.asList(mds.getArray().getFreeVars()).contains(tv)) {
				return true;
			}
		}
		return false;
	}

	private Term buildInequality(final String symbol, final Term lhs, final Term rhs) {
		final Term term = mScript.term(symbol, lhs, rhs);
		AffineRelation rel;
		try {
			rel = new AffineRelation(mScript, term);
		} catch (final NotAffineException e) {
			throw new AssertionError("should be affine");
		}
		return rel.positiveNormalForm(mScript);
	}
	
	private Term buildInequality(final int quantifier, final Bound lowerBound, final Bound upperBound) {
		final boolean isStrict;
		if (quantifier == QuantifiedFormula.EXISTS) {
			isStrict = lowerBound.isIsStrict() || upperBound.isIsStrict();
			assert !(lowerBound.isIsStrict() && upperBound.isIsStrict()) ||
			!lowerBound.getTerm().getSort().getName().equals("Int") : "unsound if int and both are strict";
		} else if (quantifier == QuantifiedFormula.FORALL) {
			isStrict = lowerBound.isIsStrict() && upperBound.isIsStrict();
			assert !(!lowerBound.isIsStrict() && !upperBound.isIsStrict()) ||
			!lowerBound.getTerm().getSort().getName().equals("Int") : "unsound if int and both are non-strict";
		} else {
			throw new AssertionError("unknown quantifier");
		}
		final String symbol = (isStrict ? "<" : "<=");
		final Term term = mScript.term(symbol, lowerBound.getTerm(), upperBound.getTerm());
		AffineRelation rel;
		try {
			rel = new AffineRelation(mScript, term);
		} catch (final NotAffineException e) {
			throw new AssertionError("should be affine");
		}
		return rel.positiveNormalForm(mScript);
	}


	private class BuildingInstructions {
		private final int mquantifier;
		private final Sort mSort;
		private final List<Term> mtermsWithoutEliminatee;
		private final List<Bound> mUpperBounds;
		private final List<Bound> mLowerBounds;
		private final List<Term> mantiDer;
		public BuildingInstructions(final int quantifier, final Sort sort,
				final List<Term> termsWithoutEliminatee, final List<Bound> upperBounds,
				final List<Bound> lowerBounds, final List<Term> antiDer) {
			super();
			mquantifier = quantifier;
			mSort = sort;
			mtermsWithoutEliminatee = termsWithoutEliminatee;
			mUpperBounds = upperBounds;
			mLowerBounds = lowerBounds;
			mantiDer = antiDer;
		}
		

		Term computeAdDisjunction() {
			final ArrayList<Term> resultXJuncts = new ArrayList<Term>();
			for (int i=0; i<Math.pow(2,mantiDer.size()); i++) {
				final ArrayList<Term> resultAtoms = new ArrayList<Term>();
				final ArrayList<Bound> adLowerBounds = new ArrayList<Bound>();
				final ArrayList<Bound> adUpperBounds = new ArrayList<Bound>();
				for (int k=0; k<mantiDer.size(); k++) {
					// zero means lower -  one means upper
					if (BigInteger.valueOf(i).testBit(k)) {
						final Bound upperBound = computeBound(mantiDer.get(k),
								mquantifier, BoundType.UPPER);
						adUpperBounds.add(upperBound);
					} else {
						final Bound lowerBound = computeBound(mantiDer.get(k),
								mquantifier, BoundType.LOWER);
						adLowerBounds.add(lowerBound);

					}
				}
				
				for (final Bound adLower : adLowerBounds) {
					for (final Bound adUpper : adUpperBounds) {
						resultAtoms.add(buildInequality(mquantifier, adLower, adUpper));
					}
					for (final Bound upperBound : mUpperBounds) {
						resultAtoms.add(buildInequality(mquantifier, adLower, upperBound));
					}
				}
				for (final Bound adUpper : adUpperBounds) {
					for (final Bound lowerBound : mLowerBounds) {
						resultAtoms.add(buildInequality(mquantifier, lowerBound, adUpper));
					}
				}
				resultXJuncts.add(PartialQuantifierElimination.applyDualFiniteConnective(mScript, mquantifier, resultAtoms));
				if (!mServices.getProgressMonitorService().continueProcessing()) {
					throw new ToolchainCanceledException(this.getClass(),
							"building " + Math.pow(2,mantiDer.size()) + " xjuncts");
				}
			}
			return PartialQuantifierElimination.applyCorrespondingFiniteConnective(mScript, mquantifier, resultXJuncts.toArray(new Term[resultXJuncts.size()]));
		}

		private Bound computeBound(final Term term,
				final int quantifier, final BoundType boundType) {
			final Bound result;
			if (term.getSort().getName().equals("Real")) {
				if (quantifier == QuantifiedFormula.EXISTS) {
					return new Bound(true, term);
				} else if (quantifier == QuantifiedFormula.FORALL) {
					return new Bound(false, term);
				} else {
					throw new AssertionError("unknown quantifier");
				}
			} else if (term.getSort().getName().equals("Int")) {
				final Term one = mScript.numeral(BigInteger.ONE);
				if (quantifier == QuantifiedFormula.EXISTS) {
					// transform terms such that
					//     lower < x /\ x < upper
					// becomes
					//     lower+1 <= x /\ x <= upper-1
					if (boundType == BoundType.LOWER) {
						result = new Bound(false, mScript.term("+", term, one));
					} else if (boundType == BoundType.UPPER) {
						result = new Bound(false, mScript.term("-", term, one));
					} else {
						throw new AssertionError("unknown BoundType" + boundType);
					}
				} else if (quantifier == QuantifiedFormula.FORALL) {
					// transform terms such that
					// lower <= x \/ x <= upper becomes
					// lower-1 < x \/ x < upper+1
					if (boundType == BoundType.LOWER) {
						result = new Bound(true, mScript.term("-", term, one));
					} else if (boundType == BoundType.UPPER) {
						result = new Bound(true, mScript.term("+", term, one));
					} else {
						throw new AssertionError("unknown BoundType" + boundType);
					}
				} else {
					throw new AssertionError("unknown quantifier");
				}
			} else {
				throw new AssertionError("unknown sort " + term.getSort());
			}
			return result;
		}


		private String computeRelationSymbol(final int quantifier, final Sort sort) {
			if (quantifier == QuantifiedFormula.FORALL) {
				return "<=";
			} else {
				switch (mSort.getName()) {
				case "Int":
					return "<=";
				case "Real":
					return "<";
				default:
					throw new UnsupportedOperationException("unknown Sort");
				}
			}
		}
		


		/**
		 * Add Term summand2
		 * @param adUpperBounds
		 * @param term
		 * @return
		 */
		private ArrayList<Term> add(final ArrayList<Term> terms, final Term summand) {
			assert summand.getSort().getName().equals("Int");
			final ArrayList<Term> result = new ArrayList<Term>();
			for (final Term term : terms) {
				assert term.getSort().getName().equals("Int");
				result.add(mScript.term("+", term, summand));
			}
			return result;
		}

		
		
	}
	
	
	private static class Bound {
		private final boolean mIsStrict;
		private final Term mTerm;
		public Bound(final boolean isStrict, final Term term) {
			super();
			mIsStrict = isStrict;
			mTerm = term;
		}
		public boolean isIsStrict() {
			return mIsStrict;
		}
		public Term getTerm() {
			return mTerm;
		}
		@Override
		public String toString() {
			return "Bound [mIsStrict=" + mIsStrict + ", mTerm=" + mTerm
					+ "]";
		}
	}
	

	


}
