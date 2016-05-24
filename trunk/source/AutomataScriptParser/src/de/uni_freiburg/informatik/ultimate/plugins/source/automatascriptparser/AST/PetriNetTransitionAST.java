/*
 * Copyright (C) 2013-2015 Betim Musa (musab@informatik.uni-freiburg.de)
 * Copyright (C) 2015 University of Freiburg
 * 
 * This file is part of the ULTIMATE AutomataScriptParser plug-in.
 * 
 * The ULTIMATE AutomataScriptParser plug-in is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * The ULTIMATE AutomataScriptParser plug-in is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with the ULTIMATE AutomataScriptParser plug-in. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7:
 * If you modify the ULTIMATE AutomataScriptParser plug-in, or any covered work, by linking
 * or combining it with Eclipse RCP (or a modified version of Eclipse RCP), 
 * containing parts covered by the terms of the Eclipse Public License, the 
 * licensors of the ULTIMATE AutomataScriptParser plug-in grant you additional permission 
 * to convey the resulting work.
 */
/**
 * 
 */
package de.uni_freiburg.informatik.ultimate.plugins.source.automatascriptparser.AST;

import java.util.List;

import de.uni_freiburg.informatik.ultimate.core.model.models.ILocation;
import de.uni_freiburg.informatik.ultimate.plugins.source.automatascriptparser.AtsASTNode;

/**
 * @author musab@informatik.uni-freiburg.de
 *
 */
public class PetriNetTransitionAST extends AtsASTNode {


	/**
	 * 
	 */
	private static final long serialVersionUID = -1676272287026669953L;

	private String msymbol;

	
	private IdentifierListAST mpredeccesors;
	private IdentifierListAST msuccessors;
	

	public PetriNetTransitionAST(ILocation loc, IdentifierListAST from, String symbol, IdentifierListAST to) {
		super(loc);
		mpredeccesors = from;
		msymbol = symbol;
		msuccessors = to;
	}
	

	public IdentifierListAST getPreviousMarking() {
		return mpredeccesors;
	}


	public IdentifierListAST getNextMarking() {
		return msuccessors;
	}


	public String getSymbol() {
		return msymbol;
	}
	
	/**
	 * Returns the list of predecessor of this transition
	 * @return
	 */
	public List<String> getPreds() {
		return mpredeccesors.getIdentifierList();
	}
	
	/**
	 * Returns the list of successors of this transition
	 * @return
	 */
	public List<String> getSuccs() {
		return msuccessors.getIdentifierList();
	}


	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("{");
		for (String pred : mpredeccesors.getIdentifierList()) {
			builder.append(pred + " ");
		}
		builder.deleteCharAt(builder.length() - 1);
		builder.append("}" + msymbol + "{");
		for (String succ : msuccessors.getIdentifierList()) {
			builder.append(succ + " ");
		}
		builder.deleteCharAt(builder.length() - 1);
		builder.append("}");
		return builder.toString();
	}


	
	
}
