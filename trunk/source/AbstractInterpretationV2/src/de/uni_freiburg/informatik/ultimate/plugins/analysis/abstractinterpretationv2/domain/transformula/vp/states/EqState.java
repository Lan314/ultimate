/*
 * Copyright (C) 2017 Alexander Nutz (nutz@informatik.uni-freiburg.de)
 * Copyright (C) 2017 University of Freiburg
 *
 * This file is part of the ULTIMATE AbstractInterpretationV2 plug-in.
 *
 * The ULTIMATE AbstractInterpretationV2 plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE AbstractInterpretationV2 plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE AbstractInterpretationV2 plug-in. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE AbstractInterpretationV2 plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE AbstractInterpretationV2 plug-in grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.states;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.absint.IAbstractState;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.BoogieConst;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfgTransition;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramOldVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVarOrConst;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.equalityanalysis.IEqualityProvidingState;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.elements.EqNode;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.elements.EqNodeAndFunctionFactory;

/**
 *
 * @author Alexander Nutz (nutz@informatik.uni-freiburg.de)
 *
 * @param <ACTION>
 */
public class EqState<ACTION extends IIcfgTransition<IcfgLocation>>
		implements IAbstractState<EqState<ACTION>>, IEqualityProvidingState {

	private static int sNextFreeId = 0;

	private final int mId;

	/**
	 * The variables and constants that this state has "for the abstract interpretation"/"as an IAbstractState". Note
	 * that these should be related but need not be identical to mConstraint.getPvocs(..).
	 */
	private final Set<IProgramVarOrConst> mPvocs;

	private final EqConstraint<ACTION, EqNode> mConstraint;

	private final EqStateFactory<ACTION> mFactory;

	public EqState(final EqConstraint<ACTION, EqNode> constraint,
			final EqNodeAndFunctionFactory eqNodeAndFunctionFactory, final EqStateFactory<ACTION> eqStateFactory,
			final Set<IProgramVarOrConst> variables) {
		mId = sNextFreeId++;
		assert sNextFreeId != Integer.MAX_VALUE;
		mConstraint = constraint;
		mFactory = eqStateFactory;
		mPvocs = new HashSet<>(variables);
		assert mPvocs.containsAll(constraint.getPvocs(mFactory.getSymbolTable()).stream()
				.filter(pvoc -> !(pvoc instanceof IProgramOldVar)).filter(pvoc -> !(pvoc instanceof BoogieConst))
				.collect(Collectors.toSet()));
	}

	@Override
	public EqState<ACTION> addVariable(final IProgramVarOrConst variable) {
		final Set<IProgramVarOrConst> newPvocs = new HashSet<>(mPvocs);
		newPvocs.add(variable);
		return mFactory.getEqState(mConstraint, newPvocs);
	}

	@Override
	public EqState<ACTION> removeVariable(final IProgramVarOrConst variable) {
		return removeVariables(Collections.singleton(variable));
	}

	@Override
	public EqState<ACTION> addVariables(final Collection<IProgramVarOrConst> variables) {
		final Set<IProgramVarOrConst> newPvocs = new HashSet<>(mPvocs);
		newPvocs.addAll(variables);
		return mFactory.getEqState(mConstraint, newPvocs);
	}

	@Override
	public EqState<ACTION> removeVariables(final Collection<IProgramVarOrConst> variables) {
		final Set<TermVariable> termVariablesFromPvocs =
				variables.stream().map(pvoc -> (TermVariable) pvoc.getTerm()).collect(Collectors.toSet());
		final EqConstraint<ACTION, EqNode> projectedConstraint =
				mFactory.getEqConstraintFactory().projectExistentially(termVariablesFromPvocs, mConstraint);

		final Set<IProgramVarOrConst> newVariables = new HashSet<>(mPvocs);
		newVariables.removeAll(variables);

		return mFactory.getEqState(projectedConstraint, newVariables);
	}

	@Override
	public boolean containsVariable(final IProgramVarOrConst var) {
		return mPvocs.contains(var);
	}

	@Override
	public EqState<ACTION> renameVariables(final Map<IProgramVarOrConst, IProgramVarOrConst> old2newVars) {
		throw new UnsupportedOperationException("not yet implemented");
	}

	@Override
	public Set<IProgramVarOrConst> getVariables() {
		return Collections.unmodifiableSet(mPvocs);
	}

	@Override
	public EqState<ACTION> patch(final EqState<ACTION> dominator) {
		final EqState<ACTION> newState = this.removeVariables(dominator.getVariables());
		return newState.intersect(dominator);
	}

	@Override
	public EqState<ACTION> intersect(final EqState<ACTION> other) {
		final EqConstraint<ACTION, EqNode> newConstraint =
				mFactory.getEqConstraintFactory().conjoinFlat(this.getConstraint(), other.getConstraint());

		final Set<IProgramVarOrConst> newVariables = new HashSet<>();
		newVariables.addAll(this.getVariables());
		newVariables.addAll(other.getVariables());

		// return mFactory.getEqState(newConstraint, newConstraint.getPvocs(mFactory.getSymbolTable()));
		return mFactory.getEqState(newConstraint, newVariables);
	}

	@Override
	public EqState<ACTION> union(final EqState<ACTION> other) {
		final EqConstraint<ACTION, EqNode> newConstraint =
				mFactory.getEqConstraintFactory().disjoinFlat(this.getConstraint(), other.getConstraint());

		final Set<IProgramVarOrConst> newVariables = new HashSet<>();
		newVariables.addAll(this.getVariables());
		newVariables.addAll(other.getVariables());

		// return mFactory.getEqState(newConstraint, newConstraint.getPvocs(mFactory.getSymbolTable()));
		return mFactory.getEqState(newConstraint, newVariables);

	}

	@Override
	public boolean isEmpty() {
		return mPvocs.isEmpty();
	}

	@Override
	public boolean isBottom() {
		return mConstraint.isBottom();
	}

	@Override
	public boolean isEqualTo(final EqState<ACTION> other) {
		return this.isSubsetOf(other) == SubsetResult.EQUAL || (this.isSubsetOf(other) == SubsetResult.NON_STRICT
				&& other.isSubsetOf(this) == SubsetResult.NON_STRICT);
	}

	@Override
	public SubsetResult isSubsetOf(final EqState<ACTION> other) {
		if (mConstraint.isTop() && other.mConstraint.isTop()) {
			return SubsetResult.EQUAL;
		}
		if (mConstraint.isBottom() && other.mConstraint.isBottom()) {
			return SubsetResult.EQUAL;
		}
		if (mConstraint.isBottom()) {
			// we know from the case above that other.mConstraint != bottom
			return SubsetResult.STRICT;
		}
		if (other.mConstraint.isTop()) {
			// we know from the case above that !mConstraint.isTop()
			return SubsetResult.STRICT;
		}

		if (this.mConstraint.isStrongerThan(other.mConstraint)) {
			return SubsetResult.NON_STRICT;
		} else {
			return SubsetResult.NONE;
		}
	}

	@SafeVarargs
	private static <E> List<E> toList(final E... elems) {
		return Arrays.asList(elems);
	}

	@Override
	public EqState<ACTION> compact() {
		return mFactory.getEqState(mConstraint, mConstraint.getPvocs(mFactory.getSymbolTable()));
	}

	@Override
	public Term getTerm(final Script script) {
		return mConstraint.getTerm(script);
	}

	@Override
	public String toLogString() {
		return mPvocs.toString() + "\n" + mConstraint.toString();
	}

	@Override
	public String toString() {
		return toLogString();
	}

	public EqConstraint<ACTION, EqNode> getConstraint() {
		return mConstraint;
	}

	public EqPredicate<ACTION> toEqPredicate() {
		return mFactory.stateToPredicate(this);
	}



	public boolean areUnequal(final EqNode node1, final EqNode node2) {
		return mConstraint.areUnequal(node1, node2);
	}

	@Override
	public boolean areEqual(final Term term1, final Term term2) {
//		if (term1.getSort().isArraySort()) {
//			// array case
//			assert term2.getSort().isArraySort();
//			final EqFunction node1 = mFactory.getEqNodeAndFunctionFactory().getExistingFunction(term1);
//			final EqFunction node2 = mFactory.getEqNodeAndFunctionFactory().getExistingFunction(term2);
//			if (node1 == null || node2 == null) {
//				// we did not track at least one of the nodes
//				return false;
//			}
//			return mConstraint.areEqual(node1, node2);
//		}
//		// element case
		final EqNode node1 = mFactory.getEqNodeAndFunctionFactory().getExistingNode(term1);
		final EqNode node2 = mFactory.getEqNodeAndFunctionFactory().getExistingNode(term2);
		if (node1 == null || node2 == null) {
			// we did not track at least one of the nodes
			return false;
		}
		return mConstraint.areEqual(node1, node2);
	}

	@Override
	public boolean areUnequal(final Term term1, final Term term2) {
//		if (term1.getSort().isArraySort()) {
//			// array case
//			assert term2.getSort().isArraySort();
//			final EqFunction func1 = mFactory.getEqNodeAndFunctionFactory().getExistingFunction(term1);
//			final EqFunction func2 = mFactory.getEqNodeAndFunctionFactory().getExistingFunction(term2);
//			if (func1 == null || func2 == null) {
//				// we did not track at least one of the nodes
//				return false;
//			}
//			return mConstraint.areUnequal(func1, func2);
//		}
//		// element case
		final EqNode node1 = mFactory.getEqNodeAndFunctionFactory().getExistingNode(term1);
		final EqNode node2 = mFactory.getEqNodeAndFunctionFactory().getExistingNode(term2);
		if (node1 == null || node2 == null) {
			// we did not track at least one of the nodes
			return false;
		}
		return mConstraint.areUnequal(node1, node2);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		return this.mId == ((EqState) obj).mId;
	}

	@Override
	public int hashCode() {
		// return super.hashCode();
		return mId;
	}

	@Override
	public IEqualityProvidingState union(final IEqualityProvidingState other) {
		return union((EqState<ACTION>) other);
	}

}
