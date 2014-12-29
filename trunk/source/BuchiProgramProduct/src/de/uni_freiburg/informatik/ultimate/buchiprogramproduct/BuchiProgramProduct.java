package de.uni_freiburg.informatik.ultimate.buchiprogramproduct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.access.IObserver;
import de.uni_freiburg.informatik.ultimate.core.preferences.UltimatePreferenceInitializer;
import de.uni_freiburg.informatik.ultimate.core.services.IToolchainStorage;
import de.uni_freiburg.informatik.ultimate.core.services.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.core.util.CoreUtil;
import de.uni_freiburg.informatik.ultimate.ep.interfaces.IGenerator;
import de.uni_freiburg.informatik.ultimate.model.GraphType;
import de.uni_freiburg.informatik.ultimate.model.IElement;
import de.uni_freiburg.informatik.ultimate.result.CounterExampleResult;

/**
 * This plugin implements the product algorithm described in the Masterthesis
 * "Automatische Generierungvon Buchi-Programmen".
 * 
 * 
 * @author Langenfeld
 * 
 * 
 */
public class BuchiProgramProduct implements IGenerator {

	private static final boolean UseSBE = !false;

	protected static Logger mLogger;
	protected List<String> mFileNames;

	private BuchiProductObserver mBuchiProductObserver;
	private boolean mUseBuchiProductObserver;
	private boolean mPreviousToolFoundErrors;
	private IUltimateServiceProvider mServices;
	private int mUseful;
	private boolean mModelIsRCFG;

	@Override
	public GraphType getOutputDefinition() {
		if (mPreviousToolFoundErrors) {
			return null;
		}

		List<String> filenames = new ArrayList<String>();
		filenames.add("LTL+Program Product");
		return new GraphType(Activator.PLUGIN_ID, GraphType.Type.OTHER, filenames);
	}

	@Override
	public boolean isGuiRequired() {
		return false;
	}

	@Override
	public QueryKeyword getQueryKeyword() {
		if (mPreviousToolFoundErrors) {
			return QueryKeyword.LAST;
		}
		return QueryKeyword.ALL;
	}

	@Override
	public void setInputDefinition(GraphType graphType) {
		switch (graphType.getCreator()) {
		case "de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder":
			mModelIsRCFG = true;
		case "de.uni_freiburg.informatik.ultimate.ltl2aut":
			mUseBuchiProductObserver = true;
			mUseful++;
			break;
		default:
			mUseBuchiProductObserver = false;
			mModelIsRCFG = false;
			break;
		}
	}

	@Override
	public List<IObserver> getObservers() {
		ArrayList<IObserver> observers = new ArrayList<IObserver>();
		if (!mPreviousToolFoundErrors) {
			if (mModelIsRCFG && UseSBE) {
				observers.add(new SmallBlockEncoder(mLogger, mServices));
			}

			if (mUseBuchiProductObserver) {
				if (mBuchiProductObserver == null) {
					mBuchiProductObserver = new BuchiProductObserver(mLogger, mServices);
				}
				observers.add(mBuchiProductObserver);
			}
		}
		return observers;
	}

	@Override
	public void init() {
		mUseBuchiProductObserver = false;
		mModelIsRCFG = false;
		mFileNames = new ArrayList<String>();
		mUseful = 0;
	}

	@Override
	public String getPluginName() {
		return Activator.PLUGIN_NAME;
	}

	@Override
	public String getPluginID() {
		return Activator.PLUGIN_ID;
	}

	@Override
	public IElement getModel() {
		if (mBuchiProductObserver.getProduct() != null) {
			return mBuchiProductObserver.getProduct().getProductRCFG();
		} else {
			return null;
		}
	}

	@Override
	public List<String> getDesiredToolID() {
		return null;
	}

	@Override
	public UltimatePreferenceInitializer getPreferences() {
		return null;
	}

	@Override
	public void setToolchainStorage(IToolchainStorage services) {

	}

	@SuppressWarnings("rawtypes")
	@Override
	public void setServices(IUltimateServiceProvider services) {
		mServices = services;
		mLogger = mServices.getLoggingService().getLogger(Activator.PLUGIN_ID);
		Collection<CounterExampleResult> cex = CoreUtil.filterResults(services.getResultService().getResults(),
				CounterExampleResult.class);
		mPreviousToolFoundErrors = !cex.isEmpty();
	}

	@Override
	public void finish() {
		if (!mPreviousToolFoundErrors && mUseful == 0) {
			throw new IllegalStateException("Was used in a toolchain were it did nothing");
		}
		if (mPreviousToolFoundErrors) {
			mLogger.info("Another plugin discovered errors, skipping...");
		}
	}

}
