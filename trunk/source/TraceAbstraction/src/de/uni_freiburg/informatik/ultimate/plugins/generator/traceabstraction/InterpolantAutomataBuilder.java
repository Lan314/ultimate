package de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.automata.IAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.IRun;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.INestedWordAutomatonSimple;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedRun;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWord;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.NestedWordAutomaton;
import de.uni_freiburg.informatik.ultimate.automata.nwalibrary.StateFactory;
import de.uni_freiburg.informatik.ultimate.core.api.UltimateServices;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.ProgramPoint;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.Return;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.IPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.ISLPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SPredicate;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.predicates.SmtManager;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.preferences.TAPreferences.InterpolantAutomaton;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.PredicateUnifier;
import de.uni_freiburg.informatik.ultimate.plugins.generator.traceabstraction.singleTraceCheck.TraceChecker;

/**
 * Construct interpolant automaton.
 * The automaton will have selfloops with every statement at its final state.
 * Only automata without backedges and a canonical like interpolant automaton
 * are supported. If you want the eager automaton use one of the determinization
 * algorithms. 
 * @author heizmann@informatik.uni-freiburg.de
 */
public class InterpolantAutomataBuilder {

	private static Logger s_Logger = 
		UltimateServices.getInstance().getLogger(Activator.s_PLUGIN_ID);
	
	private final NestedWord<CodeBlock> m_NestedWord;
	private ArrayList<IPredicate> m_StateSequence;
	private final IPredicate[] m_Interpolants;
	NestedWordAutomaton<CodeBlock, IPredicate> m_IA;
	private final PredicateUnifier m_PredicateUnifier;
	
	private final InterpolantAutomaton m_AdditionalEdges;
	private final boolean m_SelfloopAtInitial;
	private final boolean m_SelfloopAtFinal = true;

	private final SmtManager m_SmtManager;
	
	private final Map<ProgramPoint, List<Integer>> m_ProgramPoint2Occurence = 
		new HashMap<ProgramPoint, List<Integer>>();
	
	private final Map<Integer,Set<IPredicate>> m_AlternativeCallPredecessors
			= new HashMap<Integer,Set<IPredicate>>();
	private int m_Unsat;
	private int m_Sat;
	private int m_Unknown;
	private int m_Trivial;
	
	private final IPredicate m_TruePredicate;
	private final IPredicate m_FalsePredicate;


	

	public InterpolantAutomataBuilder(
			IRun<CodeBlock,IPredicate> nestedRun,
			TraceChecker traceChecker,
			InterpolantAutomaton additionalEdges,
			boolean selfloopAtInitial,
			SmtManager smtManager) {
		this.m_Interpolants = traceChecker.getInterpolants();
		m_NestedWord = NestedWord.nestedWord(nestedRun.getWord());
		if (nestedRun instanceof NestedRun) {
			m_StateSequence = ((NestedRun<CodeBlock,IPredicate>) nestedRun).getStateSequence();
		}
		else {
			if (additionalEdges != InterpolantAutomaton.SINGLETRACE) {
				throw new UnsupportedOperationException("Additional edges" +
						" allowed only for automata runs");
			}
		}
		m_PredicateUnifier = traceChecker.getPredicateUnifier();
		m_AdditionalEdges = additionalEdges;
		m_SmtManager = smtManager;
		m_SelfloopAtInitial = selfloopAtInitial;
		m_TruePredicate = traceChecker.getPrecondition();
		m_FalsePredicate = traceChecker.getPostcondition();
	}
	
	
	public NestedWordAutomaton<CodeBlock, IPredicate> 
	buildInterpolantAutomaton(IAutomaton<CodeBlock, IPredicate> abstraction,
			StateFactory<IPredicate> tAContentFactory) {

		Set<CodeBlock> internalAlphabet = abstraction.getAlphabet();
		Set<CodeBlock> callAlphabet = new HashSet<CodeBlock>(0);
		Set<CodeBlock> returnAlphabet = new HashSet<CodeBlock>(0);

		if (abstraction instanceof INestedWordAutomatonSimple) {
			INestedWordAutomatonSimple<CodeBlock, IPredicate> nwa = (INestedWordAutomatonSimple<CodeBlock, IPredicate>) abstraction;
			callAlphabet = nwa.getCallAlphabet();
			returnAlphabet = nwa.getReturnAlphabet();
		}
		
		assert(m_NestedWord.length()-1==m_Interpolants.length);
		String interpolantAutomatonType;
		switch (m_AdditionalEdges) {
		case SINGLETRACE:
			interpolantAutomatonType = 
				"Constructing interpolant automaton without backedges";
			break;
		case CANONICAL:
			interpolantAutomatonType = 
				"Constructing canonical interpolant automaton";
			break;
		default:
			throw new AssertionError("Unsupported kind of interpolant automaton");
			
		}
		if (m_SelfloopAtInitial) {
			interpolantAutomatonType += ", with selfloop in true state";
		}
		if (m_SelfloopAtFinal) {
			interpolantAutomatonType += ", with selfloop in false state";
		}
		s_Logger.info(interpolantAutomatonType);

		m_IA = new NestedWordAutomaton<CodeBlock, IPredicate>(
				internalAlphabet,
				callAlphabet,
				returnAlphabet,
				tAContentFactory);
		{
			m_IA.addState(true, false, m_TruePredicate);
//			List<Integer> occurrence = new ArrayList<Integer>();
//			occurrence.add(0);
//			ProgramPoint pp = getProgramPointAtPosition(0);
//			m_ProgramPoint2Occurence.put(pp,occurrence);
		}
		m_IA.addState(false, true, m_FalsePredicate);
		
		for (int i=0; i<m_NestedWord.length(); i++) {
			boolean isFinal = isFalsePredicate(getInterpolant(i));
			if (!m_IA.getStates().contains(getInterpolant(i))) {
				m_IA.addState(false, isFinal, getInterpolant(i));
			}

			addTransition(i-1, i, i);
			
			
			if(m_AdditionalEdges == InterpolantAutomaton.CANONICAL) {
				ProgramPoint pp = getProgramPointAtPosition(i-1);
				List<Integer> previousOccurrences = m_ProgramPoint2Occurence.get(pp);
				if (previousOccurrences == null) {
					previousOccurrences = new ArrayList<Integer>();
					m_ProgramPoint2Occurence.put(pp, previousOccurrences);
				}
				else {
					for (int previousOccurrence : previousOccurrences) {
						int currentPosition = i-1;
						assert currentPosition > previousOccurrence;
						IPredicate currentPredicate = getInterpolant(currentPosition);
						IPredicate previousPredicate = getInterpolant(previousOccurrence);
						if (currentPredicate == previousPredicate) {
							// trivially covered and backedges already contained
							m_Trivial++;
						} else {
							LBool lbool = m_PredicateUnifier.getCoverageRelation().isCovered(
														currentPredicate, previousPredicate);
							LBool isSat = m_SmtManager.isCovered(m_Interpolants[currentPosition],
																	m_Interpolants[previousOccurrence]);
							assert lbool == isSat;
							switch (isSat) {
							case UNSAT:
								m_Unsat++;
								addTransition(currentPosition-1, currentPosition, previousOccurrence);
								addTransition(currentPosition, previousOccurrence+1, previousOccurrence+1);
								break;
							case SAT:
								m_Sat++;
								break;
							case UNKNOWN:
								m_Unknown++;
								break;
							default:
								throw new AssertionError();
							}
						}
					}
				}
				previousOccurrences.add(i-1);
			}
		}
		
		
		if (m_SelfloopAtInitial) {
			for (CodeBlock symbol : internalAlphabet) {
				m_IA.addInternalTransition(
								getInterpolant(-1), symbol, getInterpolant(-1));
			}
			for (CodeBlock symbol : callAlphabet) {
				m_IA.addCallTransition(
								getInterpolant(-1), symbol, getInterpolant(-1));
			}
			for (CodeBlock symbol : returnAlphabet) {
				m_IA.addReturnTransition(
				  getInterpolant(-1),getInterpolant(-1),symbol,getInterpolant(-1));
				for (Integer pos : m_AlternativeCallPredecessors.keySet()) {
					for (IPredicate hier : 
									m_AlternativeCallPredecessors.get(pos)) {
						m_IA.addReturnTransition(
							getInterpolant(-1), hier, symbol, getInterpolant(-1));
					}
				}

			}
			
		}
		
		if (m_SelfloopAtFinal) {
			for (CodeBlock symbol : internalAlphabet) {
				m_IA.addInternalTransition(m_FalsePredicate, symbol, m_FalsePredicate);
			}
			for (CodeBlock symbol : callAlphabet) {
				m_IA.addCallTransition(m_FalsePredicate, symbol, m_FalsePredicate);
			}
			for (CodeBlock symbol : returnAlphabet) {
				m_IA.addReturnTransition(
						m_FalsePredicate, m_FalsePredicate, symbol, m_FalsePredicate);
				for (Integer pos : m_AlternativeCallPredecessors.keySet()) {
					for (IPredicate hier : 
									m_AlternativeCallPredecessors.get(pos)) {
						m_IA.addReturnTransition(
								m_FalsePredicate, hier, symbol, m_FalsePredicate);
					}
				}
			}
		}
		
		s_Logger.info("Checked inductivity of " +
				(m_Unsat+m_Sat+m_Unknown+m_Trivial) +	" backedges. " + 
				m_Unsat + " proven. " + 
				m_Sat + " refuted. " + 
				m_Unknown + " times theorem prover too weak." +
				m_Trivial + " trivial.");
		
		if (m_AdditionalEdges == InterpolantAutomaton.TOTALINTERPOLATION) {
			throw new UnsupportedOperationException();
		}
		return m_IA;
	}
	


	private IPredicate getInterpolant(int i) {
		if (i == -1) {
			return m_TruePredicate;
		} else if (i == m_Interpolants.length) {
			return m_FalsePredicate;
		} else {
			return m_Interpolants[i];
		}
	}
	

	private boolean isFalsePredicate(IPredicate p) {
		if (p == m_FalsePredicate) {
			return true;
		} else {
			assert SmtManager.isDontCare(p) || p.getFormula() != m_SmtManager.getScript().term("false");
			return false;
		}
	}
	
	
	private ProgramPoint getProgramPointAtPosition(int i) {
		if (i==-1) {
			return null;
		} else if (i == m_Interpolants.length) {
			return null;
		} else {
			// workaround for the concurrent model checker, where emptiness check
			// does not yet return places
			if (m_StateSequence == null) {
				return new ProgramPoint("dummy", "dummy", false, null);
			}
			if (m_StateSequence.get(i) == null) {
				return new ProgramPoint("dummy", "dummy", false, null);
			}
			return ((ISLPredicate) m_StateSequence.get(i)).getProgramPoint();
		}
	}
	
	
	
	
	private void surveyBackedge(int newOccurrence, int oldOccurrence) {
		LBool isSat = m_SmtManager.isCovered(m_Interpolants[newOccurrence],
												m_Interpolants[oldOccurrence]);
		switch (isSat) {
		case UNSAT:
			m_Unsat++;
			addTransition(newOccurrence-1, newOccurrence, oldOccurrence);
			addTransition(newOccurrence, oldOccurrence+1, oldOccurrence+1);
			break;
		case SAT:
			m_Sat++;
			break;
		case UNKNOWN:
			m_Unknown++;
			break;
		default:
			throw new AssertionError();
		}	
	}
	
	
	
	private void addTransition(int prePos, int symbolPos, int succPos) {
		IPredicate pred = getInterpolant(prePos);
		IPredicate succ = getInterpolant(succPos);
		CodeBlock symbol = m_NestedWord.getSymbol(symbolPos);
		if (m_NestedWord.isCallPosition(symbolPos)) {
			m_IA.addCallTransition(pred, symbol, succ);
			if (getInterpolant(prePos) != getInterpolant(symbolPos)) {
				addAlternativeCallPredecessor(symbolPos, getInterpolant(prePos));
			}
		}
		else if (m_NestedWord.isReturnPosition(symbolPos)) {
			int callPos= m_NestedWord.getCallPosition(symbolPos);
			IPredicate hier = getInterpolant(callPos-1);
			m_IA.addReturnTransition(pred, hier, symbol, succ);
			if(m_AdditionalEdges == InterpolantAutomaton.CANONICAL) {
				addAlternativeReturnTransitions(pred, callPos, symbol, succ);
			}
		}
		else {
			m_IA.addInternalTransition(pred, symbol,  succ);
		}
	}
	
	private void addAlternativeCallPredecessor(int symbolPos,
			IPredicate alternativeCallPredecessor) {
		Set<IPredicate> alts = m_AlternativeCallPredecessors.get(symbolPos);
		if (alts == null) {
			alts = new HashSet<IPredicate>();
			m_AlternativeCallPredecessors.put(symbolPos, alts);
		}
		alts.add(alternativeCallPredecessor);
	}


	private void addAlternativeReturnTransitions(IPredicate pred,
			int callPos, CodeBlock symbol, IPredicate succ) {
		if (m_AlternativeCallPredecessors.get(callPos) == null) {
			return;
		}
		for(IPredicate hier : m_AlternativeCallPredecessors.get(callPos)) {
			LBool isInductive = m_SmtManager.isInductiveReturn(
										pred, hier, (Return) symbol, succ);
			s_Logger.debug("Trying to add alternative call Predecessor");
			if (isInductive == Script.LBool.UNSAT) {
				m_IA.addReturnTransition(pred, hier, symbol, succ);
				s_Logger.debug("Added return from alternative call Pred");
			}
		}
	}
	
	
	class BackwardCoveringInformation {
		private int m_PotentialBackwardCoverings;
		private int m_SuccessfullBackwardCoverings;
		public BackwardCoveringInformation() {
			super();
			m_PotentialBackwardCoverings = 0;
			m_SuccessfullBackwardCoverings = 0;
		}
		public int getPotentialBackwardCoverings() {
			return m_PotentialBackwardCoverings;
		}
		public int getSuccessfullBackwardCoverings() {
			return m_SuccessfullBackwardCoverings;
		}
		
		public void incrementPotentialBackwardCoverings() {
			m_PotentialBackwardCoverings++;
		}
		
		public void incrementSuccessfullBackwardCoverings() {
			m_SuccessfullBackwardCoverings++;
		}
		
	}


}