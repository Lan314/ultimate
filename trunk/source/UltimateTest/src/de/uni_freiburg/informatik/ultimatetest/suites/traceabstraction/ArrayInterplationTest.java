/*
 * Copyright (C) 2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Matthias Heizmann (heizmann@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE Test Library.
 * 
 * The ULTIMATE Test Library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE Test Library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE Test Library. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE Test Library, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE Test Library grant you additional permission 
 * to convey the resulting work.
 */
/**
 * 
 */
package de.uni_freiburg.informatik.ultimatetest.suites.traceabstraction;

import java.util.Collection;

import de.uni_freiburg.informatik.ultimate.test.UltimateTestCase;

/**
 * Test for array interpolation
 * @author musab@informatik.uni-freiburg.de, heizmanninformatik.uni-freiburg.de
 *
 */

public class ArrayInterplationTest extends
		AbstractTraceAbstractionTestSuite {
	private static final String[] mDirectories = {
//		"examples/programs/regression",
//		"examples/programs/quantifier/",
//		"examples/programs/quantifier/regression",
//		"examples/programs/recursivePrograms",
//		"examples/programs/toy"
		"examples/svcomp/ldv-regression/"
	};
	
	private static final boolean mTraceAbstractionBoogieWithBackwardPredicates = false;
	private static final boolean mTraceAbstractionBoogieWithForwardPredicates = false;
	private static final boolean mTraceAbstractionBoogieWithFPandBP = false;
	private static final boolean mTraceAbstractionCWithBackwardPredicates = false;
	private static final boolean mTraceAbstractionCWithForwardPredicates = true;		
	private static final boolean mTraceAbstractionCWithFPandBP = false;
	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getTimeout() {
		return 120 * 1000;
	}
	
	@Override
	public Collection<UltimateTestCase> createTestCases() {
		if (mTraceAbstractionBoogieWithForwardPredicates) {
			addTestCase(
					"AutomizerBpl.xml",
					"automizer/ForwardPredicates.epf",
				    mDirectories,
				    new String[] {".bpl"});
		} 
		if (mTraceAbstractionBoogieWithBackwardPredicates) {
			addTestCase(
					"AutomizerBpl.xml",
					"automizer/BackwardPredicates.epf",
				    mDirectories,
				    new String[] {".bpl"});
		}
		if (mTraceAbstractionBoogieWithFPandBP) {
			addTestCase(
					"AutomizerBpl.xml",
					"automizer/ForwardPredicatesAndBackwardPredicates.epf",
				    mDirectories,
				    new String[] {".bpl"});
		}
		if (mTraceAbstractionCWithForwardPredicates) {
			addTestCase(
					"AutomizerC.xml",
					"automizer/arrayInterpolationTest/ForwardPredicates.epf",
				    mDirectories,
				    new String[] {".c", ".i"});
		}
		if (mTraceAbstractionCWithBackwardPredicates) {
			addTestCase(
					"AutomizerC.xml",
					"automizer/BackwardPredicates.epf",
				    mDirectories,
				    new String[] {".c", ".i"});
		}
		if (mTraceAbstractionCWithFPandBP) {
			addTestCase(
					"AutomizerC.xml",
					"automizer/ForwardPredicatesAndBackwardPredicates.epf",
				    mDirectories,
				    new String[] {".c", ".i"});
		}
		return super.createTestCases();
	}

	
}
