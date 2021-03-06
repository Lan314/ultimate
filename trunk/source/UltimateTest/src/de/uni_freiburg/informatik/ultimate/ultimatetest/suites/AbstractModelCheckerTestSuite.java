/*
 * Copyright (C) 2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
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
package de.uni_freiburg.informatik.ultimate.ultimatetest.suites;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.uni_freiburg.informatik.ultimate.test.UltimateRunDefinition;
import de.uni_freiburg.informatik.ultimate.test.UltimateTestCase;
import de.uni_freiburg.informatik.ultimate.test.UltimateTestSuite;
import de.uni_freiburg.informatik.ultimate.test.decider.ITestResultDecider;
import de.uni_freiburg.informatik.ultimate.test.util.DirectoryFileEndingsPair;
import de.uni_freiburg.informatik.ultimate.test.util.UltimateRunDefinitionGenerator;

/**
 * 
 * @author Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 *
 */
public abstract class AbstractModelCheckerTestSuite extends UltimateTestSuite {

	protected final List<UltimateTestCase> mTestCases;

	public AbstractModelCheckerTestSuite() {
		mTestCases = new ArrayList<>();
	}

	/**
	 * Timeout of each test case.
	 * 
	 * @return A timeout for each test case in ms. The value 0 means that there is no timeout. Negative values are
	 *         forbidden. This will override the timeout that is specified in the settings files.
	 */
	protected abstract long getTimeout();

	protected abstract ITestResultDecider constructITestResultDecider(UltimateRunDefinition ultimateRunDefinition);

	@Override
	public Collection<UltimateTestCase> createTestCases() {
		mTestCases.sort(null);
		return mTestCases;
	}

	protected void addTestCases(final File toolchainFile, final File settingsFile, final Collection<File> inputFiles) {
		final long timeout = getTimeout();
		for (final File inputFile : inputFiles) {
			addTestCase(new UltimateRunDefinition(inputFile, settingsFile, toolchainFile, timeout));
		}
	}

	protected void addTestCase(final Collection<UltimateRunDefinition> urds) {
		for (final UltimateRunDefinition urd : urds) {
			mTestCases.add(buildTestCase(urd, constructITestResultDecider(urd)));
		}
	}

	protected void addTestCase(final UltimateRunDefinition urd) {
		mTestCases.add(buildTestCase(urd, constructITestResultDecider(urd)));
	}

	protected void addTestCase(final String toolchain, final String settings, final String input) {
		addTestCase(UltimateRunDefinitionGenerator.getRunDefinitionFromTrunk(input, settings, toolchain, getTimeout()));
	}

	protected void addTestCase(final String toolchain, final String settings, final String[] directories,
			final String[] fileEndings) {
		addTestCase(UltimateRunDefinitionGenerator.getRunDefinitionFromTrunk(directories, fileEndings, settings,
				toolchain, getTimeout()));
	}

	protected void addTestCase(final String toolchain, final String settings,
			final DirectoryFileEndingsPair[] directoryFileEndingsPairs) {
		addTestCase(UltimateRunDefinitionGenerator.getRunDefinitionFromTrunk(toolchain, settings,
				directoryFileEndingsPairs, getTimeout()));
	}
}
