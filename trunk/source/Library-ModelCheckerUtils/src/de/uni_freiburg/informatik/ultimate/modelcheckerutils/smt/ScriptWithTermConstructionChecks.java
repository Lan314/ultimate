/*
 * Copyright (C) 2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.logic.Annotation;
import de.uni_freiburg.informatik.ultimate.logic.Assignments;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.Model;
import de.uni_freiburg.informatik.ultimate.logic.QuotedObject;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Theory;

/**
 * Script that wraps an existing Script but has additional checks for the construction of Terms. Whenever a Term is
 * constructed we check if all params have the same Theory. This is useful to detect the common mistake that Terms are
 * combined that have been constructed using different Scripts. This is not a perfect solution, should be considered as
 * a workaround and used only for debugging.
 *
 * @author Matthias Heizmann
 */
public class ScriptWithTermConstructionChecks implements Script {

	private final Script mScript;

	public ScriptWithTermConstructionChecks(final Script script) {
		mScript = script;
	}

	@Override
	public void setLogic(final String logic) {
		mScript.setLogic(logic);
	}

	@Override
	public void setLogic(final Logics logic) {
		mScript.setLogic(logic);
	}

	@Override
	public void setOption(final String opt, final Object value) {
		mScript.setOption(opt, value);
	}

	@Override
	public void setInfo(final String info, final Object value) {
		mScript.setInfo(info, value);
	}

	@Override
	public void declareSort(final String sort, final int arity) {
		mScript.declareSort(sort, arity);
	}

	@Override
	public void defineSort(final String sort, final Sort[] sortParams, final Sort definition) {
		mScript.defineSort(sort, sortParams, definition);
	}

	@Override
	public void declareFun(final String fun, final Sort[] paramSorts, final Sort resultSort) {
		mScript.declareFun(fun, paramSorts, resultSort);
	}

	@Override
	public void defineFun(final String fun, final TermVariable[] params, final Sort resultSort, final Term definition) {
		mScript.defineFun(fun, params, resultSort, definition);
	}

	@Override
	public void push(final int levels) {
		mScript.push(levels);
	}

	@Override
	public void pop(final int levels) {
		mScript.pop(levels);
	}

	@Override
	public LBool assertTerm(final Term term) {
		return mScript.assertTerm(term);
	}

	@Override
	public LBool checkSat() {
		return mScript.checkSat();
	}

	@Override
	public Term[] getAssertions() {
		return mScript.getAssertions();
	}

	@Override
	public Term getProof() {
		return mScript.getProof();
	}

	@Override
	public Term[] getUnsatCore() {
		return mScript.getUnsatCore();
	}

	@Override
	public Map<Term, Term> getValue(final Term[] terms) {
		return mScript.getValue(terms);
	}

	@Override
	public Assignments getAssignment() {
		return mScript.getAssignment();
	}

	@Override
	public Object getOption(final String opt) {
		return mScript.getOption(opt);
	}

	@Override
	public Object getInfo(final String info) {
		return mScript.getInfo(info);
	}

	@Override
	public void exit() {
		mScript.exit();
	}

	@Override
	public Sort sort(final String sortname, final Sort... params) {
		return mScript.sort(sortname, params);
	}

	@Override
	public Sort sort(final String sortname, final BigInteger[] indices, final Sort... params) {
		return mScript.sort(sortname, indices, params);
	}

	@Override
	public Sort[] sortVariables(final String... names) {
		return mScript.sortVariables(names);
	}

	@Override
	public Term term(final String funcname, final Term... params) {
		checkIfsomeParamUsesDifferentTheory(params);
		return mScript.term(funcname, params);
	}

	private void checkIfsomeParamUsesDifferentTheory(final Term[] params) {
		for (final Term param : params) {
			final Theory paramTheory = getTheory(param);
			if (paramTheory != getThisScriptsTheory()) {
				throw new IllegalArgumentException("Param was constructed with different Script: " + param);
			}
		}
	}

	private static Theory getTheory(final Term param) {
		return param.getSort().getTheory();
	}

	private Theory getThisScriptsTheory() {
		return SmtSortUtils.getBoolSort(mScript).getTheory();
	}

	@Override
	public Term term(final String funcname, final BigInteger[] indices, final Sort returnSort, final Term... params) {
		checkIfsomeParamUsesDifferentTheory(params);
		return mScript.term(funcname, indices, returnSort, params);
	}

	@Override
	public TermVariable variable(final String varname, final Sort sort) {
		return mScript.variable(varname, sort);
	}

	@Override
	public Term quantifier(final int quantor, final TermVariable[] vars, final Term body, final Term[]... patterns) {
		return mScript.quantifier(quantor, vars, body, patterns);
	}

	@Override
	public Term let(final TermVariable[] vars, final Term[] values, final Term body) {
		return mScript.let(vars, values, body);
	}

	@Override
	public Term annotate(final Term t, final Annotation... annotations) {
		return mScript.annotate(t, annotations);
	}

	@Override
	public Term numeral(final String num) {
		return mScript.numeral(num);
	}

	@Override
	public Term numeral(final BigInteger num) {
		return mScript.numeral(num);
	}

	@Override
	public Term decimal(final String decimal) {
		return mScript.decimal(decimal);
	}

	@Override
	public Term decimal(final BigDecimal decimal) {
		return mScript.decimal(decimal);
	}

	@Override
	public Term hexadecimal(final String hex) {
		return mScript.hexadecimal(hex);
	}

	@Override
	public Term binary(final String bin) {
		return mScript.binary(bin);
	}

	@Override
	public Term string(final String str) {
		return mScript.string(str);
	}

	@Override
	public Term simplify(final Term term) {
		return mScript.simplify(term);
	}

	@Override
	public void reset() {
		mScript.reset();
	}

	@Override
	public Term[] getInterpolants(final Term[] partition) {
		return mScript.getInterpolants(partition);
	}

	@Override
	public Term[] getInterpolants(final Term[] partition, final int[] startOfSubtree) {
		return mScript.getInterpolants(partition, startOfSubtree);
	}

	@Override
	public Model getModel() {
		return mScript.getModel();
	}

	@Override
	public Iterable<Term[]> checkAllsat(final Term[] predicates) {
		return mScript.checkAllsat(predicates);
	}

	@Override
	public Term[] findImpliedEquality(final Term[] x, final Term[] y) {
		return mScript.findImpliedEquality(x, y);
	}

	@Override
	public QuotedObject echo(final QuotedObject msg) {
		return mScript.echo(msg);
	}

	@Override
	public LBool checkSatAssuming(final Term... assumptions) {
		throw new UnsupportedOperationException("Introduced in SMTInterpol 2.1-324-ga0525a0, not yet supported");
	}

	@Override
	public Term[] getUnsatAssumptions() {
		throw new UnsupportedOperationException("Introduced in SMTInterpol 2.1-324-ga0525a0, not yet supported");
	}

	@Override
	public void resetAssertions() {
		throw new UnsupportedOperationException("Introduced in SMTInterpol 2.1-324-ga0525a0, not yet supported");
	}
}
