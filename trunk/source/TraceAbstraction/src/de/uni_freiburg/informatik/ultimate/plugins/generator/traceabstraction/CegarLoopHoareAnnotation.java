/*
 * Copyright (C) 2014-2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2014-2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
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

import java.util.Set;

import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.CfgSmtToolkit;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.structure.IcfgLocation;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils.SimplificationTechnique;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils.XnfConversionTechnique;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates.PredicateTransformer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.BoogieIcfgContainer;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.BoogieIcfgLocation;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.PredicateFactory;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.HashRelation;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.HashRelation3;
import de.uni_freiburg.informatik.ultimate.util.datastructures.relation.NestedMap2;

/**
 * Hoare annotation for invariants computed by single CEGAR loop
 * 
 * @author heizmann@informatik.uni-freiburg.de
 * 
 */
public class CegarLoopHoareAnnotation {

	private final IUltimateServiceProvider mServices;
	private final CfgSmtToolkit mCsToolkit;
	private final PredicateFactory mPredicateFactory;
	private final HoareAnnotationFragments mHoareAnnotationFragments;
	
	private final HoareAnnotationStatisticsGenerator mHoareAnnotationStatisticsGenerator;

	/**
	 * What is the precondition for a context? Strongest postcondition or entry
	 * given by automaton?
	 */
	private final boolean mUseEntry = true;
	private final PredicateTransformer mPredicateTransformer;
	
	final NestedMap2<IcfgLocation, IPredicate, IPredicate> mLoc2precond2invariant;

	public CegarLoopHoareAnnotation(final BoogieIcfgContainer rootAnnot, final CfgSmtToolkit csToolkit, final PredicateFactory predicateFactory,
			final HoareAnnotationFragments hoareAnnotationFragments, final IUltimateServiceProvider services, 
			final SimplificationTechnique simplicationTechnique, final XnfConversionTechnique xnfConversionTechnique) {
		mServices = services;
		mCsToolkit = csToolkit;
		mPredicateFactory = predicateFactory;
		mHoareAnnotationFragments = hoareAnnotationFragments;
		mPredicateTransformer = new PredicateTransformer(services, 
				csToolkit.getManagedScript(), simplicationTechnique, xnfConversionTechnique);
		mHoareAnnotationStatisticsGenerator = new HoareAnnotationStatisticsGenerator();
		final HashRelation3<IcfgLocation, IPredicate, IPredicate> loc2precond2invariantSet = constructMapping();
		mLoc2precond2invariant = combine(loc2precond2invariantSet);
	}

	private NestedMap2<IcfgLocation, IPredicate, IPredicate> combine(
			final HashRelation3<IcfgLocation, IPredicate, IPredicate> loc2precond2invariantSet) {
		final NestedMap2<IcfgLocation, IPredicate, IPredicate> loc2precond2invariant = new NestedMap2<>();
		for (final IcfgLocation loc : loc2precond2invariantSet.projectToFst()) {
			for (final IPredicate precond : loc2precond2invariantSet.projectToSnd(loc)) {
				final Set<IPredicate> preds = loc2precond2invariantSet.projectToTrd(loc, precond);
				final IPredicate invariant = or(preds);
				loc2precond2invariant.put(loc, precond, invariant);
			}
		}
		return loc2precond2invariant;
	}

	private IPredicate or(final Set<IPredicate> preds) {
		// TODO Auto-generated method stub
		
//		
//		final Term tvp = mPredicateFactory.or(false, preds);
//		final IPredicate formulaForPP = mPredicateFactory.newPredicate(tvp);
//		addFormulasToLocNodes(loc, precondForContext, formulaForPP);
		return null;
	}

	
	/**
	 * Construct mapping for our three cases: 
	 * - invariants for empty context
	 * - invariants for dead context
	 * - invariants for live context 
	 * 
	 */
	public HashRelation3<IcfgLocation, IPredicate, IPredicate> constructMapping() {
		final HashRelation3<IcfgLocation, IPredicate, IPredicate> loc2precond2invariant = new HashRelation3<>();
		
		final IPredicate precondForEmptyContext = mPredicateFactory.newPredicate(mCsToolkit.getManagedScript().getScript().term("true"));
		addHoareAnnotationForContext(loc2precond2invariant, precondForEmptyContext,	mHoareAnnotationFragments.getProgPoint2StatesWithEmptyContext());

		for (final IPredicate context : mHoareAnnotationFragments.getDeadContexts2ProgPoint2Preds().keySet()) {
			IPredicate precondForContext;
			if (mUseEntry || containsAnOldVar(context)) {
				precondForContext = mHoareAnnotationFragments.getContext2Entry().get(context);
			} else {
				// compute SP
				throw new AssertionError("not implemented");
			}
			precondForContext = TraceAbstractionUtils.renameGlobalsToOldGlobals(precondForContext, 
					mServices, mCsToolkit.getManagedScript(), mPredicateFactory, SimplificationTechnique.SIMPLIFY_DDA);
			final HashRelation<BoogieIcfgLocation, IPredicate> pp2preds = mHoareAnnotationFragments
					.getDeadContexts2ProgPoint2Preds().get(context);
			addHoareAnnotationForContext(loc2precond2invariant, precondForContext, pp2preds);
		}

		for (final IPredicate context : mHoareAnnotationFragments.getLiveContexts2ProgPoint2Preds().keySet()) {
			IPredicate precondForContext;
			if (mUseEntry || containsAnOldVar(context)) {
				precondForContext = mHoareAnnotationFragments.getContext2Entry().get(context);
			} else {
				// compute SP
				throw new AssertionError("not implemented");
			}
			precondForContext = TraceAbstractionUtils.renameGlobalsToOldGlobals(precondForContext, 
					mServices, mCsToolkit.getManagedScript(), mPredicateFactory, SimplificationTechnique.SIMPLIFY_DDA);
			final HashRelation<BoogieIcfgLocation, IPredicate> pp2preds = mHoareAnnotationFragments
					.getLiveContexts2ProgPoint2Preds().get(context);
			addHoareAnnotationForContext(loc2precond2invariant, precondForContext, pp2preds);
		}
		return loc2precond2invariant;
	}

	/**
	 * @param loc2precond2invariant
	 * @param precondForContext
	 * @param pp2preds
	 */
	private void addHoareAnnotationForContext(final HashRelation3<IcfgLocation, IPredicate, IPredicate> loc2precond2invariant, 
			final IPredicate precondForContext,
			final HashRelation<BoogieIcfgLocation, IPredicate> pp2preds) {
		for (final BoogieIcfgLocation loc : pp2preds.getDomain()) {
			final Set<IPredicate> preds = pp2preds.getImage(loc);
			for (final IPredicate pred : preds) {
				loc2precond2invariant.addTriple(loc, precondForContext, pred);
			}

		}
	}


	private boolean containsAnOldVar(final IPredicate p) {
		for (final IProgramVar bv : p.getVars()) {
			if (bv.isOldvar()) {
				return true;
			}
		}
		return false;
	}

	public HoareAnnotationStatisticsGenerator getHoareAnnotationStatisticsGenerator() {
		return mHoareAnnotationStatisticsGenerator;
	}
	
	

}