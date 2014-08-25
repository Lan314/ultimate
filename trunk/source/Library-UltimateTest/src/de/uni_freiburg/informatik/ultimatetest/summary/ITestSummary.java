package de.uni_freiburg.informatik.ultimatetest.summary;

import de.uni_freiburg.informatik.ultimate.core.services.IResultService;
import de.uni_freiburg.informatik.ultimatetest.UltimateRunDefinition;
import de.uni_freiburg.informatik.ultimatetest.UltimateTestCase;
import de.uni_freiburg.informatik.ultimatetest.UltimateTestSuite;
import de.uni_freiburg.informatik.ultimatetest.decider.ITestResultDecider;
import de.uni_freiburg.informatik.ultimatetest.decider.ITestResultDecider.TestResult;

/**
 * This interface describes test summaries that can be used to create a summary
 * log file of the results of a whole test suite.
 * 
 * As our test suites have typically a lot of tests, it is more convenient to
 * write a summary file to see which test failed why and group the tests
 * according to some criteria. This interface describes classes that can be used
 * to do this.
 * 
 * @author dietsch
 * 
 */
public interface ITestSummary {

	/**
	 * Produces the actual content of the summary.
	 * 
	 * @return A (multi-line) String that will be written to the
	 *         surefire-reports directory of your local Ultimate installation
	 *         with a name specified by {@link #getSummaryLogFileName()}
	 */
	public String getSummaryLog();

	/**
	 * Class of the UltimateTestSuite for which this summary was constructed.
	 */
	public Class<? extends UltimateTestSuite> getUltimateTestSuite();
	
	/**
	 * Description of this type of summary, e.g., "AutomataScriptSummary", 
	 * "TraceAbstractionBenchmarks". This String is part of the filename
	 * to which this summary is written.
	 */
	public String getSummaryTypeDescription();
	
	/**
	 * Filename extension of the log file that will be written. E.g., ".log", or
	 * ".csv"
	 */
	public String getFilenameExtension();

	/**
	 * This method is called after the execution of each
	 * {@link UltimateTestCase} and reports the result to the
	 * {@link ITestSummary} instance of the active {@link UltimateTestSuite test
	 * suite}.
	 * 
	 * @param threeValuedResult
	 *            The actual result of the test case.
	 * @param category
	 *            The category of this test result as specified by
	 *            {@link ITestResultDecider#getResultCategory()}
	 * @param ultimateRunDefinition
	 *            Input file, settings file and toolchain file.
	 * @param message
	 *            A message for this specific result and this specific input
	 *            file as specified by
	 *            {@link ITestResultDecider#getResultMessage()}
	 * @param resultService
	 * 	          All IResults produced during the run of Ultimate.
	 *            The results are given as a map which maps plugin IDs to a 
	 *            the list of results produced by that plugin.
	 *            
	 */
	public void addResult(TestResult threeValuedResult, String category, 
			UltimateRunDefinition ultimateRunDefinition, String message, 
			IResultService resultService);
}