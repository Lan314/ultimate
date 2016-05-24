/*
 * Copyright (C) 2014-2015 Jan Leike (leike@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.lassoranker.termination;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.lassoranker.AffineTerm;
import de.uni_freiburg.informatik.ultimate.lassoranker.LinearInequality;
import de.uni_freiburg.informatik.ultimate.lassoranker.variables.RankVar;
import de.uni_freiburg.informatik.ultimate.logic.Rational;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;


/**
 * The AffineFunctionGenerator creates template instances of affine-linear
 * functions to be used in LinearInequality instances.
 * 
 * A valuation on the generated variables can be used to create an
 * AffineFunction instance.
 * 
 * @author Jan Leike
 */
public class AffineFunctionGenerator implements Serializable {
	private static final long serialVersionUID = 4376363192635730213L;
	
	private final Term mconstant;
	private final Map<RankVar, Term> mcoefficients;
	
	/**
	 * Name of the variable for the affine function's affine constant
	 */
	private static String constName(String prefix) {
		return prefix + "c";
	}
	
	/**
	 * Name of the variable for the affine function's coefficients
	 */
	private static String coeffName(String prefix, RankVar var) {
		return prefix + "_" + SmtUtils.removeSmtQuoteCharacters(
													var.getGloballyUniqueId());
	}
	
	/**
	 * @param script current SMT script
	 * @param variables the set of variables that need coefficients
	 * @param prefix new variables' name prefix
	 */
	public AffineFunctionGenerator(Script script, Collection<RankVar> variables,
			String prefix) {
		// Create variables
		mconstant = SmtUtils.buildNewConstant(script, constName(prefix), 
				"Real");
		mcoefficients = new LinkedHashMap<RankVar, Term>();
		for (RankVar var : variables) {
			mcoefficients.put(var, SmtUtils.buildNewConstant(script,
					coeffName(prefix, var), "Real"));
		}
	}
	
	/**
	 * Generate the linear inequality
	 * @param vars a mapping from Boogie variables to TermVariables to be used
	 * @return Linear inequality corresponding to si(x)
	 */
	public LinearInequality generate(Map<RankVar, Term> vars) {
		LinearInequality li = new LinearInequality();
		li.add(new AffineTerm(mconstant, Rational.ONE));
		for (Map.Entry<RankVar, Term> entry : vars.entrySet()) {
			if (mcoefficients.containsKey(entry.getKey())) {
				li.add(entry.getValue(),
						new AffineTerm(mcoefficients.get(entry.getKey()),
								Rational.ONE));
			}
		}
		return li;
	}
	
	/**
	 * Return all SMT variables used by this template
	 */
	public Collection<Term> getVariables() {
		Collection<Term> vars = new ArrayList<Term>();
		vars.addAll(mcoefficients.values());
		vars.add(mconstant);
		return vars;
	}
	
	/**
	 * Extract the greatest common denominator of all coefficients and the
	 * constant of this affine function.
	 * @param assignment the assignment to the function's coefficients
	 * @return the greatest common denominator
	 */
	public Rational getGcd(Map<Term, Rational> assignment) {
		Rational gcd = assignment.get(mconstant);
		for (Map.Entry<RankVar, Term> entry : mcoefficients.entrySet()) {
			gcd = gcd.gcd(assignment.get(entry.getValue()));
		}
		// use the absolute value of the GCD obtained from Rational.gcd
		// TODO: check with Jochen and Jürgen if negative GCD is a bug
		return gcd.abs();
	}
	
	/**
	 * Extract coefficients from the model and convert them to an AffineFunction
	 * 
	 * The affine function's coefficients must be integers, so we have to divide
	 * them by their greatest common denominator.
	 * This method automatically determines the gcd for the function's
	 * coefficients.
	 * 
	 * @param assignment the assignment to the function's coefficients
	 * @return an instance of the affine function generated by this generator
	 */
	public AffineFunction extractAffineFunction(
			Map<Term, Rational> assignment) {
		return extractAffineFunction(assignment, this.getGcd(assignment));
	}
	
	/**
	 * Extract coefficients from an assignment and convert them to an
	 * AffineFunction
	 * 
	 * The affine function's coefficients must be integers, so we have to divide
	 * them by their greatest common denominator.
	 * 
	 * @param assignment the assignment to the function's coefficients
	 * @param gcd a divisor for all values extracted from the assignment
	 * @return an instance of AffineFunction generated by this generator
	 */
	public AffineFunction extractAffineFunction(Map<Term, Rational> assignment,
			Rational gcd) {
		AffineFunction f = new AffineFunction();
		if (gcd.equals(Rational.ZERO)) {
			// special case: gcd is zero, this happens only if all
			// coefficients are zero.
			Rational c = assignment.get(mconstant);
			assert (c.equals(Rational.ZERO));
			for (Map.Entry<RankVar, Term> entry : mcoefficients.entrySet()) {
				c = assignment.get(entry.getValue());
				assert (c.equals(Rational.ZERO));
				f.put(entry.getKey(), c.numerator());
			}
		} else {
			// Divide all coefficients by the gcd
			Rational c = assignment.get(mconstant).div(gcd);
			assert(c.denominator().equals(BigInteger.ONE));
			f.setConstant(c.numerator());
			for (Map.Entry<RankVar, Term> entry : mcoefficients.entrySet()) {
				c = assignment.get(entry.getValue()).div(gcd);
				assert(c.denominator().equals(BigInteger.ONE));
				f.put(entry.getKey(), c.numerator());
			}
		}
		return f;
	}
}
