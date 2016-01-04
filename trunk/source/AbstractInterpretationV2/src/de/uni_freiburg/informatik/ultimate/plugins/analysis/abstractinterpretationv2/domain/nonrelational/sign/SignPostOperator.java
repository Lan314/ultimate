/*
 * Copyright (C) 2015 Daniel Dietsch (dietsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 Marius Greitschus (greitsch@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
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

package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.nonrelational.sign;

import java.util.List;

import de.uni_freiburg.informatik.ultimate.core.services.model.IUltimateServiceProvider;
import de.uni_freiburg.informatik.ultimate.model.boogie.IBoogieVar;
import de.uni_freiburg.informatik.ultimate.model.boogie.ast.Statement;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.algorithm.rcfg.RcfgStatementExtractor;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.model.IAbstractPostOperator;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.model.IAbstractState;
import de.uni_freiburg.informatik.ultimate.plugins.generator.rcfgbuilder.cfg.CodeBlock;

/**
 * Applies a post operation to an abstract state of the {@link SignDomain}.
 * 
 * @author Marius Greitschus (greitsch@informatik.uni-freiburg.de)
 */
public class SignPostOperator implements IAbstractPostOperator<SignDomainState, CodeBlock, IBoogieVar> {

	private final RcfgStatementExtractor mStatementExtractor;
	private final SignDomainStatementProcessor mStatementProcessor;

	/**
	 * Default constructor.
	 */
	protected SignPostOperator(IUltimateServiceProvider services) {
		mStatementExtractor = new RcfgStatementExtractor();
		mStatementProcessor = new SignDomainStatementProcessor(services);
	}

	/**
	 * Applys the post operator to a given {@link IAbstractState}, according to some Boogie {@link CodeBlock}.
	 * 
	 * @param oldstate
	 *            The current abstract state, the post operator is applied on.
	 * @param codeBlock
	 *            The Boogie code block that is used to apply the post operator.
	 * @return A new abstract state which is the result of applying the post operator to a given abstract state.
	 */
	@Override
	public SignDomainState apply(SignDomainState oldstate, CodeBlock codeBlock) {
		SignDomainState currentState = oldstate;
		final List<Statement> statements = mStatementExtractor.process(codeBlock);
		for (final Statement stmt : statements) {
			final List<SignDomainState> result = mStatementProcessor.process(currentState, stmt);
			for (int i = 0; i < result.size(); i++) {
				if (i == 0) {
					currentState = result.get(i);
					// TODO: Merge all states here!
				}
			}
		}

		return currentState;
	}
}
