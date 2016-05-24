/*
 * Copyright (C) 2014-2015 Jan Leike (leike@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.lassoranker.AffineTerm;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearInequality;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearInequality.PossibleMotzkinCoefficients;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.AffineFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.AffineFunctionGenerator;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.PiecewiseRankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.RankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.RankVar;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Term;


/**
 * The piecewise template finds ranking functions that are piecewise defined
 * functions.
 * 
 * Template:
 * <pre>
 *    /\ δ > 0
 * /\ /\_i /\_j g_i(x) < 0 \/ g_j(x') < 0 \/ f_j(x') < f_i(x) - delta
 * /\ /\_i f_i(x) > 0
 * /\ ( \/_i g_i(x) >= 0 )
 * </pre>
 * 
 * The functions f_i define the pieces.  Each function has a discriminatory
 * predicate g_i() >= 0.  We use this piece iff the corresponding predicate is
 * satisfied.
 * 
 * @author Jan Leike
 */
public class PiecewiseTemplate extends RankingTemplate {
	
	public final int size;
	
	private static final String s_name_delta = "delta";
	private static final String s_name_function = "rank";
	private static final String s_name_pred = "pred";
	
	private Term mdelta;
	private AffineFunctionGenerator[] mfgens; // functions
	private AffineFunctionGenerator[] mpgens; // discriminatory predicates
	
	/**
	 * @param numfunctions number of pieces
	 */
	public PiecewiseTemplate(int numpieces) {
		assert(numpieces >= 2);
		size = numpieces;
		mfgens = new AffineFunctionGenerator[size];
		mpgens = new AffineFunctionGenerator[size];
	}
	
	@Override
	protected void _init() {
		mdelta = newDelta(s_name_delta);
		for (int i = 0; i < size; ++i) {
			mfgens[i] = new AffineFunctionGenerator(mscript, mvariables,
					s_name_function + i);
			mpgens[i] = new AffineFunctionGenerator(mscript, mvariables,
					s_name_pred + i);
		}
	}
	
	@Override
	public String getName() {
		return size + "-piece";
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(size);
		sb.append("-piece template:\n");
		sb.append("   delta > 0\n");
		for (int i = 0; i < size; ++i) {
			for (int j = 0; j < size; ++j) {
				sb.append("/\\ ( g_" + i + "(x) < 0 \\/ ");
				sb.append("g_" + j + "(x') < 0 \\/ ");
				sb.append("f_" + j + "(x') < f_" + i + "(x) - delta )\n");
			}
		}
		for (int i = 0; i < size; ++i) {
			sb.append("/\\ f_" + i + "(x) > 0\n");
		}
		sb.append("/\\ ( ");
		for (int i = 0; i < size; ++i) {
			sb.append("g_" + i + "(x) >= 0");
			if (i < size - 1) {
				sb.append(" \\/ ");
			}
		}
		sb.append(" )");
		return sb.toString();
	}
	
	@Override
	public List<List<LinearInequality>> getConstraints(
			Map<RankVar, Term> inVars, Map<RankVar, Term> outVars) {
		checkInitialized();
		List<List<LinearInequality>> conjunction =
				new ArrayList<List<LinearInequality>>();
		
		// /\_i /\_j g_i(x) < 0 \/ g_j(x') < 0 \/ f_j(x') < f_i(x) - delta
		for (int i = 0; i < size; ++i) {
			for (int j = 0; j < size; ++j) {
				List<LinearInequality> disjunction =
						new ArrayList<LinearInequality>();
				
				LinearInequality li1 = mpgens[i].generate(inVars);
				li1.negate();
				li1.setStrict(true);
				li1.motzkin_coefficient = sBlueAtoms && i == j ?
						PossibleMotzkinCoefficients.ZERO_AND_ONE
						: PossibleMotzkinCoefficients.ANYTHING;
				disjunction.add(li1);
				
				LinearInequality li2 = mpgens[j].generate(outVars);
				li2.negate();
				li2.setStrict(true);
				li2.motzkin_coefficient = PossibleMotzkinCoefficients.ANYTHING;
				disjunction.add(li2);
				
				LinearInequality li3 = mfgens[i].generate(inVars);
				LinearInequality li4 = mfgens[j].generate(outVars);
				li4.negate();
				li3.add(li4);
				AffineTerm a = new AffineTerm(mdelta, Rational.MONE);
				li3.add(a);
				li3.setStrict(true);
				li3.motzkin_coefficient = sRedAtoms ?
						PossibleMotzkinCoefficients.ZERO_AND_ONE
						: PossibleMotzkinCoefficients.ANYTHING;
				disjunction.add(li3);
				conjunction.add(disjunction);
			}
		}
		
		// /\_i f_i(x) > 0
		for (int i = 0; i < size; ++i) {
			LinearInequality li = mfgens[i].generate(inVars);
			li.setStrict(true);
			li.motzkin_coefficient = sRedAtoms ? PossibleMotzkinCoefficients.ONE
					: PossibleMotzkinCoefficients.ANYTHING;
			conjunction.add(Collections.singletonList(li));
		}
		
		// \/_i g_i(x) >= 0
		List<LinearInequality> disjunction = new ArrayList<LinearInequality>();
		for (int i = 0; i < size; ++i) {
			LinearInequality li = mpgens[i].generate(inVars);
			li.setStrict(false);
			li.motzkin_coefficient = sRedAtoms && i == 0 ?
					PossibleMotzkinCoefficients.ZERO_AND_ONE
					: PossibleMotzkinCoefficients.ANYTHING;
			disjunction.add(li);
		}
		conjunction.add(disjunction);
		
		// delta > 0 is assured by RankingFunctionTemplate.newDelta
		return conjunction;
	}

	@Override
	public Collection<Term> getVariables() {
		Collection<Term> list = new ArrayList<Term>();
		list.add(mdelta);
		for (int i = 0; i < size; ++i) {
			list.addAll(mfgens[i].getVariables());
			list.addAll(mpgens[i].getVariables());
		}
		return list;
	}

	@Override
	public RankingFunction extractRankingFunction(Map<Term, Rational> val)
			throws SMTLIBException {
		// The ranking pieces need a common gcd
		Rational gcd_f = mfgens[0].getGcd(val);
		for (int i = 1; i < size; ++i) {
			gcd_f = gcd_f.gcd(mfgens[i].getGcd(val));
		}
		
		AffineFunction[] fs = new AffineFunction[size];
		AffineFunction[] gs = new AffineFunction[size];
		for (int i = 0; i < size; ++i) {
			fs[i] = mfgens[i].extractAffineFunction(val, gcd_f);
			gs[i] = mpgens[i].extractAffineFunction(val);
		}
		return new PiecewiseRankingFunction(fs, gs);
	}
	
	@Override
	public List<String> getAnnotations() {
		List<String> annotations = new ArrayList<String>();
		for (int i = 0; i < size; ++i) {
			for (int j = 0; j < size; ++j) {
				annotations.add("transition from piece " + i + " to piece " + j);
			}
		}
		for (int i = 0; i < size; ++i) {
			annotations.add("rank f" + i + " is bounded");
		}
		annotations.add("case split is exhaustive");
		return annotations;
	}

	@Override
	public int getDegree() {
		assert(size > 0);
		return 2*size*size - 1;
	}
}
