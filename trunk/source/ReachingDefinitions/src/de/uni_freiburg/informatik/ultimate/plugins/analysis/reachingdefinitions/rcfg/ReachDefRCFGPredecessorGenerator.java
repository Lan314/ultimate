package de.uni_freiburg.informatik.ultimate.plugins.analysis.reachingdefinitions.rcfg;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.uni_freiburg.informatik.ultimate.plugins.analysis.reachingdefinitions.annotations.IAnnotationProvider;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.reachingdefinitions.annotations.ReachDefStatementAnnotation;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.reachingdefinitions.util.Util;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RCFGEdge;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.RCFGNode;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.SequentialComposition;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.StatementSequence;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.util.RCFGEdgeVisitor;

public class ReachDefRCFGPredecessorGenerator extends RCFGEdgeVisitor {

	private final Logger mLogger;
	private final IAnnotationProvider<ReachDefStatementAnnotation> mProvider;

	public ReachDefRCFGPredecessorGenerator(IAnnotationProvider<ReachDefStatementAnnotation> provider, Logger logger) {
		mLogger = logger;
		mProvider = provider;
	}

	private List<ReachDefStatementAnnotation> rtr;

	/**
	 * Returns all preceeding {@link ReachDefStatementAnnotation}s
	 * 
	 * @param e
	 * @return
	 */
	public List<ReachDefStatementAnnotation> process(RCFGNode currentNode) {
		rtr = new ArrayList<ReachDefStatementAnnotation>();
		if (currentNode == null) {
			return rtr;
		}

		for (RCFGEdge pre : currentNode.getIncomingEdges()) {
			visit(pre);
		}

		if (mLogger.isDebugEnabled()) {
			mLogger.debug("Predecessors: "
					+ Util.prettyPrintIterable(currentNode.getIncomingEdges(), Util.<RCFGEdge> createHashCodePrinter()));
		}

		return rtr;
	}

	@Override
	protected void visit(SequentialComposition c) {
		List<CodeBlock> blck = c.getCodeBlocks();
		if (blck == null || blck.isEmpty()) {
			return;
		}
		super.visit(blck.get(blck.size() - 1));
	}

	@Override
	protected void visit(StatementSequence stmtSeq) {
		ReachDefStatementAnnotation annot = mProvider.getAnnotation(stmtSeq.getStatements().get(
				stmtSeq.getStatements().size() - 1));
		if (annot != null) {
			rtr.add(annot);
		}

		super.visit(stmtSeq);
	}
}
