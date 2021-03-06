/*
 * Copyright (C) 2016 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2016 University of Freiburg
 *
 * This file is part of the ULTIMATE ModelCheckerUtilsTest Library.
 *
 * The ULTIMATE ModelCheckerUtilsTest Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The ULTIMATE ModelCheckerUtilsTest Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE ModelCheckerUtilsTest Library. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE ModelCheckerUtilsTest Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP),
 * containing parts covered by the terms of the Eclipse Public License, the
 * licensors of the ULTIMATE ModelCheckerUtilsTest Library grant you additional permission
 * to convey the resulting work.
 */
package de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.predicates;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.uni_freiburg.informatik.ultimate.core.coreplugin.services.ToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.model.services.ILogger;
import de.uni_freiburg.informatik.ultimate.core.model.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.BoogieNonOldVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.boogie.BoogieOldVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.ProgramVarUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtSortUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.SmtUtils;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.smt.managedscript.ManagedScript;
import de.uni_freiburg.informatik.ultimate.smtsolver.external.Scriptor;
import de.uni_freiburg.informatik.ultimate.test.mocks.UltimateMocks;

/**
 *
 * @author Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 *
 */
public class PredicateTreeTest {

	private IUltimateServiceProvider mServices;
	private Script mScript;
	private ManagedScript mMgdScript;
	private ILogger mLogger;

	@Before
	public void setUp() {
		mServices = UltimateMocks.createUltimateServiceProviderMock();
		mLogger = mServices.getLoggingService().getLogger(getClass());
		try {
			mScript = new Scriptor("z3 SMTLIB2_COMPLIANT=true -memory:2024 -smt2 -in", mLogger, mServices,
					new ToolchainStorage(), "z3");
		} catch (final IOException e) {
			throw new AssertionError(e);
		}
		mMgdScript = new ManagedScript(mServices, mScript);
		mScript.setLogic(Logics.ALL);

	}

	@Test
	public void test() {

		final PredicateTree<TestPredicate> ptree = new PredicateTree<>(mMgdScript);
		mMgdScript.lock(this);
		final Set<IProgramVar> vars = new HashSet<>();
		final BoogieNonOldVar a = constructProgramVar("a");
		final BoogieNonOldVar b = constructProgramVar("b");
		vars.add(a);
		vars.add(b);
		final TestPredicate pred1 =
				new TestPredicate(mScript.term("=", a.getTermVariable(), mScript.numeral("1")), vars, mScript);
		final TestPredicate pred2 =
				new TestPredicate(mScript.term("=", a.getTermVariable(), mScript.numeral("1")), vars, mScript);
		final TestPredicate pred3 =
				new TestPredicate(mScript.term("=", a.getTermVariable(), mScript.numeral("2")), vars, mScript);
		final TestPredicate pred4 =
				new TestPredicate(mScript.term(">", a.getTermVariable(), mScript.numeral("0")), vars, mScript);
		final TestPredicate pred5 =
				new TestPredicate(mScript.term(">", a.getTermVariable(), mScript.numeral("1")), vars, mScript);
		final TestPredicate pred6 =
				new TestPredicate(mScript.term("=", b.getTermVariable(), mScript.numeral("0")), vars, mScript);
		final TestPredicate pred7 =
				new TestPredicate(SmtUtils.and(mScript, pred1.getFormula(), pred6.getFormula()), vars, mScript);
		mMgdScript.unlock(this);

		Assert.assertTrue(pred1 != pred2);
		final TestPredicate upred1 = ptree.unifyPredicate(pred1);
		Assert.assertTrue(upred1 == pred1);
		final TestPredicate upred2 = ptree.unifyPredicate(pred2);
		Assert.assertTrue(upred2 == pred1);
		final TestPredicate upred3 = ptree.unifyPredicate(pred3);
		Assert.assertTrue(upred3 == pred3);
		final TestPredicate upred4 = ptree.unifyPredicate(pred4);
		Assert.assertTrue(upred4 == pred4);
		final TestPredicate upred5 = ptree.unifyPredicate(pred5);
		Assert.assertTrue(upred5 == pred5);
		final TestPredicate upred7 = ptree.unifyPredicate(pred7);
		Assert.assertTrue(upred7 == pred7);
		final TestPredicate upred6 = ptree.unifyPredicate(pred6);
		Assert.assertTrue(upred6 == pred6);
		mLogger.info("\n" + ptree.toLogString());
	}

	@After
	public void tearDown() {
		mScript.exit();
	}

	private BoogieNonOldVar constructProgramVar(final String identifier) {
		BoogieOldVar oldVar;
		final Sort sort = SmtSortUtils.getIntSort(mMgdScript);
		{
			final boolean isOldVar = true;
			final String name = ProgramVarUtils.buildBoogieVarName(identifier, null, true, isOldVar);
			final TermVariable termVariable = mMgdScript.variable(name, sort);
			final ApplicationTerm defaultConstant =
					ProgramVarUtils.constructDefaultConstant(mMgdScript, this, sort, name);
			final ApplicationTerm primedConstant =
					ProgramVarUtils.constructPrimedConstant(mMgdScript, this, sort, name);

			oldVar = new BoogieOldVar(identifier, null, termVariable, defaultConstant, primedConstant);
		}
		BoogieNonOldVar nonOldVar;
		{
			final boolean isOldVar = false;
			final String name = ProgramVarUtils.buildBoogieVarName(identifier, null, true, isOldVar);
			final TermVariable termVariable = mMgdScript.variable(name, sort);
			final ApplicationTerm defaultConstant =
					ProgramVarUtils.constructDefaultConstant(mMgdScript, this, sort, name);
			final ApplicationTerm primedConstant =
					ProgramVarUtils.constructPrimedConstant(mMgdScript, this, sort, name);

			nonOldVar = new BoogieNonOldVar(identifier, null, termVariable, defaultConstant, primedConstant, oldVar);
		}
		oldVar.setNonOldVar(nonOldVar);
		return nonOldVar;
	}

	private static final class TestPredicate implements IPredicate {

		private final Set<IProgramVar> mVars;
		private final Term mClosedFormula;
		private final Term mFormula;

		public TestPredicate(final Term formula, final Set<IProgramVar> vars, final Script script) {
			mVars = vars;
			mFormula = formula;
			mClosedFormula = PredicateUtils.computeClosedFormula(formula, vars, script);
		}

		@Override
		public String[] getProcedures() {
			return new String[0];
		}

		@Override
		public Set<IProgramVar> getVars() {
			return mVars;
		}

		@Override
		public Term getFormula() {
			return mFormula;
		}

		@Override
		public Term getClosedFormula() {
			return mClosedFormula;
		}

		@Override
		public String toString() {
			return getFormula().toStringDirect();
		}

	}
}
