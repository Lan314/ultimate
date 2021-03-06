package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.states;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IIcfgTransition;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.arrays.MultiDimensionalSort;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.IEqNodeIdentifier;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.VPStatistics;
import de.uni_freiburg.informatik.ultimate.util.datastructures.CongruenceClosure;
import de.uni_freiburg.informatik.ultimate.util.datastructures.Doubleton;
import de.uni_freiburg.informatik.ultimate.util.datastructures.EqualityStatus;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.HashRelation;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.Pair;

public class WeqCongruenceClosure<ACTION extends IIcfgTransition<IcfgLocation>, NODE extends IEqNodeIdentifier<NODE>>
		extends CongruenceClosure<NODE> {

	private final WeakEquivalenceGraph<ACTION, NODE> mWeakEquivalenceGraph;
	private final EqConstraintFactory<ACTION, NODE> mFactory;

//	private final LiteralManager<NODE> mLiteralManager;
//	private final Collection<NODE> mAllLiterals;

	private final HashRelation<Object, NODE> mNodeToDependents;

	/**
	 * Create an empty ("True"/unconstrained) WeqCC.
	 *
	 * @param factory
	 */
	public WeqCongruenceClosure(final EqConstraintFactory<ACTION, NODE> factory) {
		super();
		assert factory != null;
		mWeakEquivalenceGraph = new WeakEquivalenceGraph<>(this, factory);
		mFactory = factory;
//		mLiteralManager = mFactory.getLiteralManager();
//		mAllLiterals = new HashSet<>();
		mNodeToDependents = new HashRelation<>();
		assert sanityCheck();
	}

	/**
	 * Create an inconsistent ("False") WeqCC.
	 *
	 * @param isInconsistent
	 */
	public WeqCongruenceClosure(final boolean isInconsistent) {
		super(true);
		if (!isInconsistent) {
			throw new IllegalArgumentException("use other constructor!");
		}
		mWeakEquivalenceGraph = null;
		mFactory = null;
//		mLiteralManager = null;
//		mAllLiterals = null;
		mNodeToDependents = null;
	}

	/**
	 * Create a WeqCC using the given CongruenceClosure as ground partial
	 * arrangement (gpa) and an empty WeakEquivalenceGraph.
	 *
	 * @param original
	 * @param factory
	 */
	public WeqCongruenceClosure(final CongruenceClosure<NODE> original,
			final EqConstraintFactory<ACTION, NODE> factory) {
		super(original);
		assert factory != null;
		mWeakEquivalenceGraph = new WeakEquivalenceGraph<>(this, factory);
		mFactory = factory;
//		mLiteralManager = mFactory.getLiteralManager();
//		mAllLiterals = original.getAllElementRepresentatives().stream().filter(elem -> mLiteralManager.isLiteral(elem))
//				.collect(Collectors.toCollection(HashSet::new));

		mNodeToDependents = new HashRelation<>();
		initializeNodeToDependents(original);
		assert sanityCheck();
	}

	/**
	 * Create a WeqCC using the given CongruenceClosure as ground partial
	 * arrangement (gpa) and the given WeakEquivalenceGraph.
	 *
	 * @param original
	 */
	public WeqCongruenceClosure(final CongruenceClosure<NODE> original,
			final WeakEquivalenceGraph<ACTION, NODE> weqGraph, final EqConstraintFactory<ACTION, NODE> factory) {
		super(original);
		assert factory != null;
		if (original.isInconsistent()) {
			throw new IllegalArgumentException("use other constructor!");
		}
		mFactory = factory;
		mNodeToDependents = new HashRelation<>();
		initializeNodeToDependents(original);

		// we need a fresh instance of WeakEquivalenceGraph here, because we cannot set the link in the weq
		// graph to the right cc instance..
		mWeakEquivalenceGraph = new WeakEquivalenceGraph<>(this, weqGraph);

		assert sanityCheck();
	}

	/**
	 * copy constructor
	 *
	 * @param original
	 */
	public WeqCongruenceClosure(final WeqCongruenceClosure<ACTION, NODE> original) {
		super(original);
		assert original.mFactory != null;
		mFactory = original.mFactory;
//		mLiteralManager = mFactory.getLiteralManager();
		mWeakEquivalenceGraph = new WeakEquivalenceGraph<>(this, original.mWeakEquivalenceGraph);
//		mAllLiterals = new HashSet<>(original.mAllLiterals);
		mNodeToDependents = new HashRelation<>(original.mNodeToDependents);
		assert sanityCheck();
	}

	private void initializeNodeToDependents(final CongruenceClosure<NODE> original) {
		for (final NODE e : original.getAllElements()) {
			if (!e.isDependent()) {
				continue;
			}
			for (final NODE supp : e.getSupportingNodes()) {
				mNodeToDependents.addPair(supp, e);
			}
		}
	}

	public Term getTerm(final Script script) {
		final List<Term> allConjuncts = new ArrayList<>();
		allConjuncts.addAll(EqConstraint.partialArrangementToCube(script, this));

		final List<Term> weakEqConstraints = mWeakEquivalenceGraph.getWeakEquivalenceConstraintsAsTerms(script);
		allConjuncts.addAll(weakEqConstraints);

		final Term result = SmtUtils.and(script, allConjuncts.toArray(new Term[allConjuncts.size()]));
		return result;
	}

	@Override
	public boolean addElement(final NODE elem) {
		final boolean result = addElementRec(elem);

		executeFloydWarshallAndReportResult();
		reportAllArrayEqualitiesFromWeqGraph();
		assert weqGraphFreeOfArrayEqualities();

		assert sanityCheck();
		return result;
	}

	@Override
	protected boolean addElementRec(final NODE elem) {
		final boolean elemIsNew = super.addElementRec(elem);
		if (!elemIsNew) {
			return false;
		}

//		if (mLiteralManager.isLiteral(elem)) {
//			for (final NODE other : mLiteralManager.getDisequalities(elem, getAllLiteralElements())) {
//				reportDisequalityRec(elem, other);
//			}
//			mAllLiterals.add(elem);
//		}

//		executeFloydWarshallAndReportResult();
//		reportAllArrayEqualitiesFromWeqGraph();

//		assert weqGraphFreeOfArrayEqualities();
		return true;
	}

//	private Collection<NODE> getAllLiteralElements() {
//		return mAllLiterals;
//	}

	@Override
	protected CongruenceClosure<NODE> alignElementsAndFunctions(final CongruenceClosure<NODE> otherCC) {
		assert !this.isInconsistent() && !otherCC.isInconsistent();
		if (!(otherCC instanceof WeqCongruenceClosure)) {
			return super.alignElementsAndFunctions(otherCC);
		}
		final WeqCongruenceClosure<ACTION, NODE> other = (WeqCongruenceClosure<ACTION, NODE>) otherCC;

		final WeqCongruenceClosure<ACTION, NODE> result = new WeqCongruenceClosure<>(this);
		assert result.sanityCheck();

		other.getAllElements().stream().forEach(elem -> result.addElement(elem));

		assert result.sanityCheck();
		return result;
	}

	public void renameVariables(final Map<Term, Term> substitutionMapping) {
		transformElementsAndFunctions(node -> node.renameVariables(substitutionMapping));
		mWeakEquivalenceGraph.renameVariables(substitutionMapping);
	}

	public void reportWeakEquivalence(final NODE array1, final NODE array2, final NODE storeIndex) {
		assert array1.isFunction() && array2.isFunction();
		assert array1.hasSameTypeAs(array2);

		getRepresentativeAndAddElementIfNeeded(storeIndex);
		assert sanityCheck();

		final CongruenceClosure<NODE> newConstraint = computeWeqConstraintForIndex(
				Collections.singletonList(storeIndex));
		reportWeakEquivalence(array1, array2, Collections.singletonList(newConstraint));
		assert sanityCheck();
	}

	public void reportWeakEquivalence(final NODE array1, final NODE array2,
			final List<CongruenceClosure<NODE>> edgeLabel) {
		if (isInconsistent()) {
			return;
		}

		while (true) {
			boolean madeChanges = false;
			madeChanges |= reportWeakEquivalenceDoOnlyRoweqPropagations(array1, array2, edgeLabel);
			if (!madeChanges) {
				break;
			}

			madeChanges = false;
			madeChanges |= executeFloydWarshallAndReportResult();
			if (!madeChanges) {
				break;
			}
		}
		assert sanityCheck();

		/*
		 * ext propagations
		 */
		reportAllArrayEqualitiesFromWeqGraph();
		assert sanityCheck();
	}

	private boolean executeFloydWarshallAndReportResult() {
		if (isInconsistent()) {
			return false;
		}
		boolean fwmc = false;
		final Map<Doubleton<NODE>, WeakEquivalenceGraph<ACTION, NODE>.WeakEquivalenceEdgeLabel> fwResult = mWeakEquivalenceGraph
				.close();
		for (final Entry<Doubleton<NODE>, WeakEquivalenceGraph<ACTION, NODE>.WeakEquivalenceEdgeLabel> fwEdge : fwResult
				.entrySet()) {
			fwmc |= reportWeakEquivalenceDoOnlyRoweqPropagations(fwEdge.getKey().getOneElement(),
					fwEdge.getKey().getOtherElement(), fwEdge.getValue().getLabelContents());
		}
		return fwmc;
	}

	private boolean reportWeakEquivalenceDoOnlyRoweqPropagations(final NODE array1, final NODE array2,
			final List<CongruenceClosure<NODE>> edgeLabel) {
		if (isInconsistent()) {
			return false;
		}

		boolean madeChanges = false;
		madeChanges |= addElementRec(array1);
		madeChanges |= addElementRec(array2);

		final NODE array1Rep = mElementTVER.getRepresentative(array1);
		final NODE array2Rep = mElementTVER.getRepresentative(array2);

		if (array1Rep == array2Rep) {
			// no need to have a weq edge from the node to itself
			return madeChanges;
		}

		madeChanges |= mWeakEquivalenceGraph.reportWeakEquivalence(array1Rep, array2Rep, edgeLabel);

		if (!madeChanges) {
			// nothing to propagate
			return false;
		}

		List<CongruenceClosure<NODE>> strengthenedEdgeLabelContents = mWeakEquivalenceGraph
				.getEdgeLabelContents(array1Rep, array2Rep);

		if (strengthenedEdgeLabelContents == null) {
			// edge became "false";
			strengthenedEdgeLabelContents = Collections.emptyList();
		}

		/*
		 * roweq propagations
		 *
		 * look for fitting c[i], d[j] with i ~ j, array1 ~ c, array2 ~ d
		 */
		final Collection<NODE> ccps1 = mAuxData.getAfCcPars(array1Rep);
		final Collection<NODE> ccps2 = mAuxData.getAfCcPars(array2Rep);
		for (final NODE ccp1 : ccps1) {
//			final NODE ccp1Replaced = replaceFuncAppArgsWOtherRepIfNecAndPoss(ccp1);
			final NODE ccp1Replaced = replace(ccp1, mElementCurrentlyBeingRemoved);
			if (ccp1Replaced == null) {
				continue;
			}
			if (!hasElements(ccp1Replaced, ccp1Replaced.getArgument(), ccp1Replaced.getAppliedFunction())) {
				continue;
			}
			for (final NODE ccp2 : ccps2) {
				if (isInconsistent()) {
					return true;
				}
//				final NODE ccp2Replaced = replaceFuncAppArgsWOtherRepIfNecAndPoss(ccp2);
				final NODE ccp2Replaced = replace(ccp2, mElementCurrentlyBeingRemoved);
				if (ccp2Replaced == null) {
					continue;
				}

				if (!hasElements(ccp2Replaced, ccp2Replaced.getArgument(), ccp2Replaced.getAppliedFunction())) {
					continue;
				}

				if (getEqualityStatus(ccp1Replaced.getArgument(), ccp2Replaced.getArgument()) != EqualityStatus.EQUAL) {
					continue;
				}
				/*
				 * i ~ j holds propagate array1[i] -- -- array2[j] (note that this adds the
				 * arrayX[Y] nodes, possibly -- EDIT: not..)
				 */

				final List<CongruenceClosure<NODE>> projectedLabel = mWeakEquivalenceGraph.projectEdgeLabelToPoint(
						strengthenedEdgeLabelContents, ccp1Replaced.getArgument(), getAllWeqVarsNodeForFunction(array1));

				// recursive call
				reportWeakEquivalenceDoOnlyRoweqPropagations(ccp1Replaced, ccp2Replaced, projectedLabel);
			}
		}

		/*
		 * roweq-1 propagations
		 */
		if (array1.isFunctionApplication() && array2.isFunctionApplication()
				&& hasElements(array1.getArgument(), array2.getArgument())
				&& getEqualityStatus(array1.getArgument(), array2.getArgument()) == EqualityStatus.EQUAL) {

			final List<CongruenceClosure<NODE>> shiftedLabelWithException = mWeakEquivalenceGraph
					.shiftLabelAndAddException(strengthenedEdgeLabelContents, array1.getArgument(),
							getAllWeqVarsNodeForFunction(array1.getAppliedFunction()));

			// recursive call
			reportWeakEquivalenceDoOnlyRoweqPropagations(array1.getAppliedFunction(), array2.getAppliedFunction(),
					shiftedLabelWithException);
		}

//		assert sanityCheck();
		return true;
	}

	/**
	 * Given a (multidimensional) index, compute the corresponding annotation for a
	 * weak equivalence edge.
	 *
	 * Example: for (i1, .., in), this should return (q1 = i1, ..., qn = in) as a
	 * list of CongruenceClosures. (where qi is the variable returned by
	 * getWeqVariableForDimension(i))
	 *
	 * @param nodes
	 * @return
	 */
	private CongruenceClosure<NODE> computeWeqConstraintForIndex(final List<NODE> nodes) {
		final CongruenceClosure<NODE> result = new CongruenceClosure<>();
		for (int i = 0; i < nodes.size(); i++) {
			final NODE ithNode = nodes.get(i);
			result.reportEquality(mFactory.getWeqVariableNodeForDimension(i, ithNode.getTerm().getSort()), ithNode);
		}
		return result;
	}

	@Override
	public boolean reportEquality(final NODE node1, final NODE node2) {
		final boolean result = reportEqualityRec(node1, node2);
		executeFloydWarshallAndReportResult();
		assert sanityCheck();
		return result;
	}

	@Override
	protected boolean reportEqualityRec(final NODE node1, final NODE node2) {
		assert node1.hasSameTypeAs(node2);
		if (isInconsistent()) {
			throw new IllegalStateException();
		}

		boolean freshElem = false;
		freshElem |= addElementRec(node1);
		freshElem |= addElementRec(node2);
		assert atMostOneLiteralPerEquivalenceClass();

		if (getEqualityStatus(node1, node2) == EqualityStatus.EQUAL) {
			// nothing to do
			return freshElem;
		}
		if (getEqualityStatus(node1, node2) == EqualityStatus.NOT_EQUAL) {
			// report it to tver so that it is in an inconsistent state
			mElementTVER.reportEquality(node1, node2);
			// not so nice, but needed for literals where TVER does not know they are unequal otherwise
			if (!mElementTVER.isInconsistent()) {
				mElementTVER.reportDisequality(node1, node2);
			}
			assert mElementTVER.isInconsistent();
			return true;
		}


		// old means "before the merge", here..
		final NODE node1OldRep = getRepresentativeElement(node1);
		final NODE node2OldRep = getRepresentativeElement(node2);
		final CongruenceClosure<NODE>.CcAuxData oldAuxData = new CcAuxData(mAuxData, true);

		mWeakEquivalenceGraph.collapseEdgeAtMerge(node1OldRep, node2OldRep);

		/*
		 * cannot just du a super.reportEquality here, because we want to reestablish some class invariants (checked
		 * through sanityCheck()) before doing the recursive calls for the fwcc and bwcc propagations)
		 * in particular we need to do mWeakEquivalenceGraph.updateforNewRep(..)
		 */
//		madeChanges |= super.reportEquality(node1, node2);
		final Pair<HashRelation<NODE, NODE>, HashRelation<NODE, NODE>> propInfo = doMergeAndComputePropagations(node1,
				node2);
		if (propInfo == null) {
			// this became inconsistent through the merge
			return true;
		}


		final NODE newRep = getRepresentativeElement(node1);
		mWeakEquivalenceGraph.updateForNewRep(node1OldRep, node2OldRep, newRep);

		doFwccAndBwccPropagationsFromMerge(propInfo);

		doRoweqPropagationsOnMerge(node1, node2, node1OldRep, node2OldRep, oldAuxData);

//		executeFloydWarshallAndReportResult();

		/*
		 * ext
		 */
		reportGpaChangeToWeqGraphAndPropagateArrayEqualities(
				(final CongruenceClosure<NODE> cc) -> cc.reportEquality(node1, node2));

		return true;
	}

	private void doRoweqPropagationsOnMerge(final NODE node1, final NODE node2, final NODE node1OldRep,
			final NODE node2OldRep, final CcAuxData oldAuxData) {
//			final Collection<NODE> oldArgCcPars1, final Collection<NODE> oldArgCcPars2,
//			final HashRelation<NODE, NODE> oldCcChildren1, final HashRelation<NODE, NODE> oldCcChildren2) {
		if (isInconsistent()) {
			return;
		}

		boolean goOn = false;
		/*
		 * there are three types of propagations related to weak equivalences,
		 * corresponding to the rules ext, roweq and roweq-1
		 */

		/*
		 * the merge may collapse two nodes in the weak equivalence graph (which may
		 * trigger propagations)
		 */
		// (recursive call)
		// EDIT: adding an edge between nodes that are being merged is problematic algorithmically
		// instead do the rule roweqMerge (which models the consequence of the below a -- false -- b edge, together
		//  with fwcc), doing it in an extra procedure..
		//	goOn |= reportWeakEquivalenceDoOnlyRoweqPropagations(node1OldRep, node2OldRep, Collections.emptyList());
		// we will treat roweqMerge during the other propagations below as it need similar matching..

		/*
		 * roweqMerge (1)
		 */
//		if (node1.isFunctionApplication() && node2.isFunctionApplication()
//				&& getEqualityStatus(node1.getArgument(), node2.getArgument()) == EqualityStatus.EQUAL) {
//			// case node1 = a[i], node2 = b[j]
//			final NODE firstWeqVar = getAllWeqVarsNodeForFunction(node1.getAppliedFunction()).get(0);
//			final CongruenceClosure<NODE> qUnequalI = new CongruenceClosure<>();
//			qUnequalI.reportDisequality(firstWeqVar, node1.getArgument());
//			goOn |= reportWeakEquivalenceDoOnlyRoweqPropagations(node1.getAppliedFunction(), node2.getAppliedFunction(),
//					Collections.singletonList(qUnequalI));
//		}
		for (final Entry<NODE, NODE> ccc1 : oldAuxData.getCcChildren(node1OldRep)) {
			// don't propagate something that uses the currently removed element
			final NODE ccc1AfReplaced = replaceWithOtherRepIfNecessaryAndPossible(ccc1.getKey());
			final NODE ccc1ArgReplaced = replaceWithOtherRepIfNecessaryAndPossible(ccc1.getValue());
			if (ccc1AfReplaced == null || ccc1ArgReplaced == null) {
				continue;
			}

			for (final Entry<NODE, NODE> ccc2 : oldAuxData.getCcChildren(node2OldRep)) {

				// don't propagate something that uses the currently removed element
				final NODE ccc2AfReplaced = replaceWithOtherRepIfNecessaryAndPossible(ccc2.getKey());
				final NODE ccc2ArgReplaced = replaceWithOtherRepIfNecessaryAndPossible(ccc2.getValue());
				if (ccc2AfReplaced == null || ccc2ArgReplaced == null) {
					continue;
				}

				assert hasElements(ccc1AfReplaced, ccc1ArgReplaced, ccc2AfReplaced, ccc2ArgReplaced);

				// case ccc1 = (a,i), ccc2 = (b,j)
				if (getEqualityStatus(ccc1ArgReplaced, ccc2ArgReplaced) != EqualityStatus.EQUAL) {
					// not i = j --> cannot propagate
					continue;
				}
				// i = j

				final NODE firstWeqVar = getAllWeqVarsNodeForFunction(ccc1AfReplaced).get(0);
				final CongruenceClosure<NODE> qUnequalI = new CongruenceClosure<>();
				qUnequalI.reportDisequality(firstWeqVar, ccc1ArgReplaced);
				goOn |= reportWeakEquivalenceDoOnlyRoweqPropagations(ccc1AfReplaced, ccc2AfReplaced,
						Collections.singletonList(qUnequalI));
			}
		}


		/*
		 * roweq, roweq-1 (1)
		 */
		// node1 = i, node2 = j in the rule
		// for (final NODE ccp1 : mAuxData.getArgCcPars(node1)) {
//		for (final NODE ccp1 : oldArgCcPars1) {
		for (final NODE ccp1 : oldAuxData.getArgCcPars(node1OldRep)) {
			// for (final NODE ccp2 : mAuxData.getArgCcPars(node2)) {
			for (final NODE ccp2 : oldAuxData.getArgCcPars(node2OldRep)) {
				// ccp1 = a[i], ccp2 = b[j] in the rule

				if (!ccp1.getSort().equals(ccp2.getSort())) {
					continue;
				}

				/*
				 * roweq:
				 */
				final List<CongruenceClosure<NODE>> aToBLabel = mWeakEquivalenceGraph
						.getEdgeLabelContents(ccp1.getAppliedFunction(), ccp2.getAppliedFunction());
				final List<CongruenceClosure<NODE>> projectedLabel = mWeakEquivalenceGraph.projectEdgeLabelToPoint(
						aToBLabel, ccp1.getArgument(), getAllWeqVarsNodeForFunction(ccp1.getAppliedFunction()));
				// recursive call
				goOn |= reportWeakEquivalenceDoOnlyRoweqPropagations(ccp1, ccp2, projectedLabel);

				/*
				 * roweq-1:
				 */
				final List<CongruenceClosure<NODE>> aiToBjLabel = mWeakEquivalenceGraph.getEdgeLabelContents(ccp1,
						ccp2);
				final List<CongruenceClosure<NODE>> shiftedLabelWithException = mWeakEquivalenceGraph
						.shiftLabelAndAddException(aiToBjLabel, node1,
								getAllWeqVarsNodeForFunction(ccp1.getAppliedFunction()));
				// recursive call
				goOn |= reportWeakEquivalenceDoOnlyRoweqPropagations(ccp1.getAppliedFunction(),
						ccp2.getAppliedFunction(), shiftedLabelWithException);

				/*
				 * roweqMerge
				 */
				if (getEqualityStatus(ccp1, ccp2) == EqualityStatus.EQUAL) {
					// we have node1 = i, node2 = j, ccp1 = a[i], ccp2 = b[j]
					final NODE firstWeqVar = getAllWeqVarsNodeForFunction(ccp1.getAppliedFunction()).get(0);
					assert getAllWeqVarsNodeForFunction(ccp1.getAppliedFunction())
						.equals(getAllWeqVarsNodeForFunction(ccp2.getAppliedFunction()));
					assert getEqualityStatus(ccp2.getArgument(), ccp1.getArgument()) == EqualityStatus.EQUAL :
						" propagation is only allowed if i = j";

					final CongruenceClosure<NODE> qUnequalI = new CongruenceClosure<>();
					qUnequalI.reportDisequality(firstWeqVar, ccp1.getArgument());

					goOn |= reportWeakEquivalenceDoOnlyRoweqPropagations(ccp1.getAppliedFunction(),
							ccp2.getAppliedFunction(), Collections.singletonList(qUnequalI));
				}
			}

		}
//		assert sanityCheck();

		/*
		 * roweq-1(2)
		 *
		 * a somewhat more intricate case:
		 *
		 * the added equality may trigger the pattern matching on the weak equivalence
		 * condition of the roweq-1 rule
		 */
		goOn |= otherRoweqPropOnMerge(node1OldRep, oldAuxData);
		goOn |= otherRoweqPropOnMerge(node2OldRep, oldAuxData);
		// otherRoweqPropOnMerge(node1, mAuxData.getCcChildren(node1));
		// otherRoweqPropOnMerge(node2, mAuxData.getCcChildren(node2));
	}



	private boolean otherRoweqPropOnMerge(final NODE nodeOldRep, final CcAuxData oldAuxData) {
//			final HashRelation<NODE, NODE> oldCcChildren1) {
		boolean madeChanges = false;
//		for (final Entry<NODE, NODE> ccc : oldCcChildren1) {
		for (final Entry<NODE, NODE> ccc : oldAuxData.getCcChildren(nodeOldRep)) {
			// ccc = (b,j) , as in b[j]
			for (final Entry<NODE, WeakEquivalenceGraph<ACTION, NODE>.WeakEquivalenceEdgeLabel> edgeAdjacentToNode
					: mWeakEquivalenceGraph .getAdjacentWeqEdges(nodeOldRep).entrySet()) {
				final NODE n = edgeAdjacentToNode.getKey();
				final WeakEquivalenceGraph<ACTION, NODE>.WeakEquivalenceEdgeLabel phi = edgeAdjacentToNode.getValue();

				// TODO is it ok here to use that auxData from after the merge??
//				if (!mAuxData.getArgCcPars(ccc.getValue()).contains(edgeAdjacentToNode.getKey())) {
				if (!oldAuxData.getArgCcPars(ccc.getValue()).contains(edgeAdjacentToNode.getKey())) {
					continue;
				}
				// n in argccp(j)

				// TODO is it ok here to use tha auxData from after the merge??
//				for (final Entry<NODE, NODE> aj : mAuxData.getCcChildren(edgeAdjacentToNode.getKey())) {
				for (final Entry<NODE, NODE> aj : oldAuxData.getCcChildren(edgeAdjacentToNode.getKey())) {
					// aj = (a,j), as in a[j]

					// propagate b -- q != j, Phi+ -- a

					final List<CongruenceClosure<NODE>> shiftedLabelWithException = mWeakEquivalenceGraph
							.shiftLabelAndAddException(phi.getLabelContents(), ccc.getValue(),
									getAllWeqVarsNodeForFunction(ccc.getKey()));
					// recursive call
					madeChanges |= reportWeakEquivalenceDoOnlyRoweqPropagations(ccc.getKey(), aj.getKey(),
							shiftedLabelWithException);
				}

			}

			/*
			 * roweqMerge rule:
			 *  not necessary here as we used ccpar in do doRoweqPropagationsOnMerge
			 */
		}
		return madeChanges;
	}

	private void reportAllArrayEqualitiesFromWeqGraph() {
		while (mWeakEquivalenceGraph.hasArrayEqualities()) {
			final Entry<NODE, NODE> aeq = mWeakEquivalenceGraph.pollArrayEquality();
			reportEquality(aeq.getKey(), aeq.getValue());
			if (isInconsistent()) {
				return;
			}
		}
	}

	@Override
	public boolean reportDisequality(final NODE node1, final NODE node2) {
		final boolean result = reportDisequalityRec(node1, node2);
		assert sanityCheck();
		return result;
	}

	@Override
	protected boolean reportDisequalityRec(final NODE node1, final NODE node2) {
		boolean madeChanges = false;

		madeChanges |= super.reportDisequalityRec(node1, node2);

		if (!madeChanges) {
			return false;
		}

		if (isInconsistent()) {
			// no need for further propagations
			return true;
		}

		reportGpaChangeToWeqGraphAndPropagateArrayEqualities(
				(final CongruenceClosure<NODE> cc) -> cc.reportDisequality(node1, node2));

		assert weqGraphFreeOfArrayEqualities();
		return true;
	}

	/**
	 * Updates the weq-graph wrt. a change in the ground partial arrangement.
	 * Immediately propagates array equalities if some have occurred.
	 *
	 * @param reporter
	 * @return
	 */
	private boolean reportGpaChangeToWeqGraphAndPropagateArrayEqualities(
			final Predicate<CongruenceClosure<NODE>> reporter) {
		if (isInconsistent()) {
			return false;
		}
		boolean madeChanges = false;
		madeChanges |= mWeakEquivalenceGraph.reportChangeInGroundPartialArrangement(reporter);
		reportAllArrayEqualitiesFromWeqGraph();
//		assert sanityCheck();
		return madeChanges;
	}

	@Override
	protected boolean supports(final NODE elem, final NODE elem2) {
		if (mNodeToDependents.getImage(elem).contains(elem2)) {
			return true;
		}
		return super.supports(elem, elem2);
	}

	private List<NODE> getAllWeqVarsNodeForFunction(final NODE func) {
		if (!func.getSort().isArraySort()) {
			return Collections.emptyList();
		}
		final MultiDimensionalSort mdSort = new MultiDimensionalSort(func.getSort());
		final List<Sort> indexSorts = mdSort.getIndexSorts();
		final List<NODE> result = new ArrayList<>(mdSort.getDimension());
		for (int i = 0; i < mdSort.getDimension(); i++) {
			result.add(mFactory.getWeqVariableNodeForDimension(i, indexSorts.get(i)));
		}
		return result;
	}

	@Override
	public boolean isTautological() {
		// TODO: literal disequalities don't prevent being tautological --> account for that!
		return super.isTautological() && mWeakEquivalenceGraph.isEmpty();
	}

	@Override
	public boolean isStrongerThan(final CongruenceClosure<NODE> other) {
		if (!(other instanceof WeqCongruenceClosure<?, ?>)) {
			throw new IllegalArgumentException();
		}
		if (!super.isStrongerThan(other)) {
			return false;
		}

		final WeqCongruenceClosure<ACTION, NODE> otherWeqCc = (WeqCongruenceClosure<ACTION, NODE>) other;

		if (!mWeakEquivalenceGraph.isStrongerThan(otherWeqCc.mWeakEquivalenceGraph)) {
			return false;
		}
		return true;
	}

	@Override
	public boolean removeSimpleElement(final NODE elem) {
		if (elem.isFunctionApplication()) {
			throw new IllegalArgumentException();
		}
		if (!hasElement(elem)) {
			removeDependents(elem);
			return false;
		}
		final CongruenceClosure<NODE> copy = new CongruenceClosure<>(this);


		if (mElementCurrentlyBeingRemoved == null) {
			mElementCurrentlyBeingRemoved = new RemovalInfo(elem, getOtherEquivalenceClassMember(elem));
		} else {
			// this may happen if elem is a dependent element, check that through the assert..
			assert mNodeToDependents.entrySet().stream()
				.map(en -> en.getValue()).filter(e -> e.equals(elem)).findAny().isPresent();
		}
//		addNodesEquivalentToNodesWithRemovedElement(elem);

		final Collection<NODE> nodesToAdd = collectNodesToAddAtFunctionRemoval(elem);
		for (final NODE node : nodesToAdd) {
			addElement(node);
		}

//		final Set<NODE> oldAfParents = new HashSet<>(mFaAuxData.getAfParents(elem));
//		final Set<NODE> oldArgParents = new HashSet<>(mFaAuxData.getArgParents(elem));

//		final NODE newRep = updateElementTverAndAuxDataOnRemoveElement(elem);
		final Map<NODE, NODE> removedElemsToNewReps = super.removeSimpleElementTrackNewReps(elem);

		mWeakEquivalenceGraph.projectFunction(elem, copy, removedElemsToNewReps);

//		removeParents(oldAfParents, oldArgParents);

		removeDependents(elem);

		mAllLiterals.remove(elem);

		if (mElementCurrentlyBeingRemoved.getElem().equals(elem)) {
			mElementCurrentlyBeingRemoved = null;
		}
		assert sanityCheck();
//		assert elementIsFullyRemoved(elem);
		return true;
//		return removeElement(elem, copy);
	}

	protected void removeDependents(final NODE elem) {
		for (final NODE dependent : new HashSet<>(mNodeToDependents.getImage(elem))) {
			removeSimpleElement(dependent);
		}
		mNodeToDependents.removeDomainElement(elem);
	}

//	@Override
//	protected boolean removeFuncAppElement(final NODE elem) {
//		// TODO Auto-generated method stub
//		return super.removeFuncAppElement(elem);
//	}

//	private boolean removeElement(final NODE elem, final CongruenceClosure<NODE> copy) {
//		if (hasElement(elem)) {
//
//			addNodesEquivalentToNodesWithRemovedElement(elem);
//
//			final Collection<NODE> nodesToAdd = collectNodesToAddAtFunctionRemoval(elem);
//			for (final NODE node : nodesToAdd) {
//				addElement(node);
//			}
//
//			final Set<NODE> oldAfParents = new HashSet<>(mFaAuxData.getAfParents(elem));
//			final Set<NODE> oldArgParents = new HashSet<>(mFaAuxData.getArgParents(elem));
//
//			final NODE newRep = updateElementTverAndAuxDataOnRemoveElement(elem);
//
//			/*
//			 * Project func from the weak equivalence graph. We need to make a copy of the
//			 * ground partial arrangement, because ..
//			 */
//			mWeakEquivalenceGraph.projectFunction(elem, newRep, copy);
//			assert projectedFunctionIsGoneFromWeqGraph(elem, mWeakEquivalenceGraph);
//
//			removeParents(oldAfParents, oldArgParents);
//
////			for (final NODE dependent : new HashSet<>(mNodeToDependents.getImage(elem))) {
////				removeElement(dependent, copy);
////			}
////			mNodeToDependents.removeDomainElement(elem);
//
//			assert weqGraphFreeOfArrayEqualities();
//			assert projectedFunctionIsGoneFromWeqGraph(elem, mWeakEquivalenceGraph);
//
//			mAllLiterals.remove(elem);
//		}
//
//		for (final NODE dependent : new HashSet<>(mNodeToDependents.getImage(elem))) {
//			removeElement(dependent, copy);
//		}
//		mNodeToDependents.removeDomainElement(elem);
//		mNodeToDependents.removeRangeElement(elem);
//
//		assert elementIsFullyRemoved(elem);
//		return true;
//	}

	/**
	 * When removing a function we will also remove all function nodes that depend
	 * on it. In this first step we attempt to conserve information about those
	 * nodes if possible by adding nodes with other functions but the same
	 * arguments.
	 *
	 * Conditions to add a node b[i1, ..., in]: (a is the function we are about to
	 * remove)
	 * <li>a[i1, ..., in] is present in this weqCc and is part of a non-tautological
	 * constraint
	 * <li>the current weqCc allows us to conclude a[i1, .., in] = b[i1, ..,in]
	 * <p>
	 * that is the case if one of the following conditions holds
	 * <li>the strong equivalence a = b is implied by this weqCc (it is enough to
	 * propagate for one other function in the equivalence class of a)
	 * <li>there is a weak equivalence edge between a and b, and it allows weak
	 * congruence propagation of the above equality
	 *
	 * @param elem the node that is to be removed
	 * @param eqcMember a member of the equivalence class that elem was in before removal (may be null)
	 * @param elemAfParents
	 */
	private Collection<NODE> collectNodesToAddAtFunctionRemoval(final NODE elem) {

		final Collection<NODE> nodesToAdd = new ArrayList<>();

		/*
		 * collect nodes that are applications with newRep as a function symbol, and that have some constraint on them
		 */
		boolean goOn = true;
		final Set<NODE> transitiveAfParents = new HashSet<>();
		Set<NODE> currentLayer = new HashSet<>(mFaAuxData.getAfParents(elem));
		while (goOn) {
			goOn = false;
			final Set<NODE> nextLayer = new HashSet<>();
			for (final NODE afp : currentLayer) {
				final Set<NODE> transAfp = mFaAuxData.getAfParents(afp);
				goOn |= !transAfp.isEmpty();
				nextLayer.addAll(transAfp);
			}
			transitiveAfParents.addAll(currentLayer);
			currentLayer = nextLayer;
		}
		final Set<NODE> constrainedFuncAppNodes = transitiveAfParents.stream().filter(this::isConstrained)
				.collect(Collectors.toSet());
//		final Set<NODE> constrainedFuncAppNodes = mAuxData.getAfCcPars(newRep).stream().filter(this::isConstrained)
//				.collect(Collectors.toSet());

		for (final NODE fan : constrainedFuncAppNodes) {

			for (final Entry<NODE, WeakEquivalenceGraph<ACTION, NODE>.WeakEquivalenceEdgeLabel> weqEdge
					: mWeakEquivalenceGraph.getAdjacentWeqEdges(elem).entrySet()) {
				if (weqEdge.getValue().impliesEqualityOnThatPosition(Collections.singletonList(fan.getArgument()))) {
					final NODE nodeWithWequalFunc = mFactory.getEqNodeAndFunctionFactory()
							.getOrConstructFuncAppElement(weqEdge.getKey(), fan.getArgument());
					nodesToAdd.add(nodeWithWequalFunc);
				}
			}
		}
		return nodesToAdd;
	}

	@Override
	public boolean isConstrained(final NODE elem) {
		if (super.isConstrained(elem)) {
			return true;
		}
		if (mWeakEquivalenceGraph.isConstrained(elem)) {
			return true;
		}
		return false;
	}

	@Override
	protected void registerNewElement(final NODE elem) {
		super.registerNewElement(elem);

		if (elem.isDependent()) {
			for (final NODE supp : elem.getSupportingNodes()) {
				mNodeToDependents.addPair(supp, elem);
			}
		}

		if (!elem.isFunctionApplication()) {
			// nothing to do
//			assert sanityCheck();
			return;
		}

//		assert sanityCheck();


		boolean madeChanges = false;
		/*
		 * roweq
		 */
		// say elem = a[i], then we attempt to discover all b[j] in exp such that i = j, these are the argccpar of i
		for (final NODE ccp : mAuxData.getArgCcPars(getRepresentativeElement(elem.getArgument()))) {
			if (!ccp.hasSameTypeAs(elem)) {
				// TODO: nicer would be to have argCcPars contain only elements of fitting sort..
				continue;
			}



			/*
			 * don't propagate something that uses the currently removed element
			 */
//			final NODE ccpReplaced = replaceFuncAppArgsWOtherRepIfNecAndPoss(ccp);
			final NODE ccpReplaced = ccp;

			if (ccpReplaced == null) {
				continue;
			}

			assert hasElements(ccpReplaced, ccpReplaced.getAppliedFunction(), ccpReplaced.getArgument());

			// ccp = b[j], look for a weq edge between a and b
			if (getEqualityStatus(elem.getAppliedFunction(), ccpReplaced.getAppliedFunction()) == EqualityStatus.EQUAL) {
				// a = b, strong, not weak equivalence, nothing to do here (propagations done by fwcc)
				continue;
			}
//			final NODE ccpAfRep = getRepresentativeElement(ccp.getAppliedFunction());
//			for (final Entry<NODE, WeakEquivalenceGraph<ACTION, NODE>.WeakEquivalenceEdgeLabel> weqEdge
//					: mWeakEquivalenceGraph.getAdjacentWeqEdges(ccpAfRep).entrySet()) {

			// get label of edge between a and b
			final List<CongruenceClosure<NODE>> weqEdgeLabelContents =
					mWeakEquivalenceGraph.getEdgeLabelContents(ccpReplaced.getAppliedFunction(), elem.getAppliedFunction());

			final List<CongruenceClosure<NODE>> projectedLabel = mWeakEquivalenceGraph.projectEdgeLabelToPoint(
					//						weqEdge.getValue().getLabelContents(),
					weqEdgeLabelContents,
					ccpReplaced.getArgument(),
					getAllWeqVarsNodeForFunction(ccpReplaced.getAppliedFunction()));

			madeChanges |= reportWeakEquivalenceDoOnlyRoweqPropagations(elem,
					ccpReplaced,
					projectedLabel);
//			}
		}

		if (madeChanges) {
			final Map<Doubleton<NODE>, WeakEquivalenceGraph<ACTION, NODE>.WeakEquivalenceEdgeLabel> props =
					mWeakEquivalenceGraph.close();
			for (final Entry<Doubleton<NODE>, WeakEquivalenceGraph<ACTION, NODE>.WeakEquivalenceEdgeLabel> prop
					: props.entrySet()) {
				reportWeakEquivalenceDoOnlyRoweqPropagations(prop.getKey().getOneElement(),
						prop.getKey().getOtherElement(),
						prop.getValue().getLabelContents());
			}
		}

//		assert sanityCheck();
	}

	@Override
	public void transformElementsAndFunctions(final Function<NODE, NODE> elemTransformer) {
		super.transformElementsAndFunctions(elemTransformer);

		for (final Entry<Object, NODE> en : new HashRelation<>(mNodeToDependents).entrySet()) {
			mNodeToDependents.removePair(en.getKey(), en.getValue());
			if (en.getKey() instanceof IEqNodeIdentifier<?>) {
				mNodeToDependents.addPair(elemTransformer.apply((NODE) en.getKey()),
						elemTransformer.apply(en.getValue()));
			} else {
				throw new AssertionError();
			}
		}
	}

	@Override
	protected boolean elementIsFullyRemoved(final NODE elem) {
		for (final Entry<Object, NODE> en : mNodeToDependents.entrySet()) {
			if (en.getKey().equals(elem) || en.getValue().equals(elem)) {
				assert false;
				return false;
			}
		}
		return super.elementIsFullyRemoved(elem);
	}

	@Override
	public WeqCongruenceClosure<ACTION, NODE> join(final CongruenceClosure<NODE> otherCC) {
		if (!(otherCC instanceof WeqCongruenceClosure)) {
			throw new IllegalArgumentException();
		}
		if (otherCC.isInconsistent()) {
			return new WeqCongruenceClosure<>(this);
		}

		final WeqCongruenceClosure<ACTION, NODE> other = (WeqCongruenceClosure<ACTION, NODE>) otherCC;

		return new WeqCongruenceClosure<>(super.join(other), mWeakEquivalenceGraph.join(other.mWeakEquivalenceGraph),
				mFactory);
	}

	@Override
	public WeqCongruenceClosure<ACTION, NODE> meet(final CongruenceClosure<NODE> other) {

		final WeqCongruenceClosure<ACTION, NODE> result = meetRec(other);

		result.executeFloydWarshallAndReportResult();
		if (result.isInconsistent()) {
			return new WeqCongruenceClosure<>(true);
		}
		result.reportAllArrayEqualitiesFromWeqGraph();
		if (result.isInconsistent()) {
			return new WeqCongruenceClosure<>(true);
		}

		assert result.sanityCheck();
		return result;
	}

	@Override
	public WeqCongruenceClosure<ACTION, NODE> meetRec(final CongruenceClosure<NODE> other) {
		if (!(other instanceof WeqCongruenceClosure)) {
			throw new IllegalArgumentException();
		}

		final CongruenceClosure<NODE> gPaMeet = super.meetRec(other);
		if (gPaMeet.isInconsistent()) {
			return new WeqCongruenceClosure<>(true);
		}
		assert gPaMeet.atMostOneLiteralPerEquivalenceClass();
		assert !this.mWeakEquivalenceGraph.hasArrayEqualities();

		/*
		 * strategy: conjoin all weq edges of otherCC to a copy of this's weq graph
		 */

		final WeqCongruenceClosure<ACTION, NODE> newWeqCc = (WeqCongruenceClosure<ACTION, NODE>) gPaMeet;

		final WeqCongruenceClosure<ACTION, NODE> otherWeqCc = (WeqCongruenceClosure<ACTION, NODE>) other;

		// report all weq edges from other
		for (final Entry<Doubleton<NODE>, WeakEquivalenceGraph<ACTION, NODE>.WeakEquivalenceEdgeLabel> edge
				: otherWeqCc.mWeakEquivalenceGraph.getEdges().entrySet()) {
//			newWeqCc.reportWeakEquivalence(edge.getKey().getOneElement(), edge.getKey().getOtherElement(),
			newWeqCc.reportWeakEquivalenceDoOnlyRoweqPropagations(edge.getKey().getOneElement(),
					edge.getKey().getOtherElement(),
					edge.getValue().getLabelContents());
		}


//		newWeqCc.executeFloydWarshallAndReportResult();
//		if (newWeqCc.isInconsistent()) {
//			return new WeqCongruenceClosure<>(true);
//		}
//		newWeqCc.reportAllArrayEqualitiesFromWeqGraph();
//		if (newWeqCc.isInconsistent()) {
//			return new WeqCongruenceClosure<>(true);
//		}

		return newWeqCc;
	}

	@Override
	public boolean sanityCheck() {
		boolean res = super.sanityCheck();
		if (mWeakEquivalenceGraph != null) {
			res &= mWeakEquivalenceGraph.sanityCheck();
		}
		return res;
	}

	@Override
	public String toString() {
		if (isTautological()) {
			return "True";
		}
		if (isInconsistent()) {
			return "False";
		}
		final StringBuilder sb = new StringBuilder();
		sb.append("Partial arrangement:\n");
		sb.append(super.toString());
		sb.append("\n");
		sb.append("Weak equivalences:\n");
		sb.append(mWeakEquivalenceGraph);
		return sb.toString();
	}

	/**
	 * for sanity checking
	 * @return
	 */
	public boolean weqGraphFreeOfArrayEqualities() {
		if (mWeakEquivalenceGraph.hasArrayEqualities()) {
			assert false;
			return false;
		}
		return true;
	}

	private boolean projectedFunctionIsGoneFromWeqGraph(final NODE func,
			final WeakEquivalenceGraph<ACTION, NODE> weakEquivalenceGraph) {
		for (final Entry<Doubleton<NODE>, WeakEquivalenceGraph<ACTION, NODE>.WeakEquivalenceEdgeLabel> edge : weakEquivalenceGraph
				.getEdges().entrySet()) {
			if (edge.getValue().getAppearingNodes().contains(func)) {
				assert false;
				return false;
			}
		}
		return true;
	}

	public Integer getStatistics(final VPStatistics stat) {
		switch (stat) {
		case MAX_WEQGRAPH_SIZE:
			return mWeakEquivalenceGraph.getNumberOfEdgesStatistic();
		case MAX_SIZEOF_WEQEDGELABEL:
			return mWeakEquivalenceGraph.getMaxSizeOfEdgeLabelStatistic();
		case NO_SUPPORTING_DISEQUALITIES:
			// we have to eliminate symmetric entries
			final HashRelation<NODE, NODE> cleanedDeqs = new HashRelation<>();
			for (final Entry<NODE, NODE> deq : mElementTVER.getDisequalities()) {
				if (cleanedDeqs.containsPair(deq.getValue(), deq.getKey())) {
					continue;
				}
				cleanedDeqs.addPair(deq.getKey(), deq.getValue());
			}
			return cleanedDeqs.size();
//			return mElementTVER.getDisequalities().entrySet().stream()
//					.filter(en -> (!mElementTVER.getDisequalities().containsPair(en.getValue(), en.getKey())
//							&& en.getKey().hashCode() < en.getValue().hashCode()))
//					.collect(Collectors.counting()).intValue();
		case NO_SUPPORTING_EQUALITIES:
			return getSupportingElementEqualities().size();
		default :
			throw new UnsupportedOperationException();
		}
	}

}