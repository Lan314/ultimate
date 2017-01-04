package de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.elements;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.VPDomainHelpers;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.VPDomainSymmetricPair;
import de.uni_freiburg.informatik.ultimate.plugins.analysis.abstractinterpretationv2.domain.transformula.vp.states.VPTfState;

public class SelectTermWrapper implements IElementWrapper {
	
	IArrayWrapper mArray;

	List<IElementWrapper> mIndices; // this is a list because we may have a multidimensional array

//	NodeIdWithSideCondition mSideCondition;
	
	

//	public SelectTermWrapper(IArrayWrapper mArray, List<IElementWrapper> mIndices,
//			WrapperSideCondition mSideCondition) {
//		super();
//		this.mArray = mArray;
//		this.mIndices = mIndices;
//		this.mSideCondition = mSideCondition;
//	}



	public SelectTermWrapper(IArrayWrapper array, List<IElementWrapper> indices) {
		this.mArray = array;
		this.mIndices = indices;
	}



	@Override
	public Set<NodeIdWithSideCondition> getNodeIdWithSideConditions(VPTfState tfState) {
		
		// TODO: the method should probably given the index (or indices) of this select term so
		//  it can prepare the anonymous array better
		Set<ArrayWithSideCondition> arrayWscs = mArray.getArrayWithSideConditions(tfState);

		List<Set<NodeIdWithSideCondition>> indexNiwscs = mIndices.stream()
				.map(elWr -> elWr.getNodeIdWithSideConditions(tfState))
				.collect(Collectors.toList());
		Set<List<NodeIdWithSideCondition>> combinedIndices = VPDomainHelpers.computeCrossProduct(indexNiwscs);
		
		Set<NodeIdWithSideCondition> result = new HashSet<>();

		for (ArrayWithSideCondition arrayWsc : arrayWscs) {
			for (List<NodeIdWithSideCondition> indexVector : combinedIndices) {
				VPTfNodeIdentifier valueAtIndex = arrayWsc.getIndexToValue().get(indexVector);
				
				// compute the new sidecondition
				Set<VPDomainSymmetricPair<VPTfNodeIdentifier>> resultEqualities = 
						new HashSet<>(arrayWsc.getEqualities());
				Set<VPDomainSymmetricPair<VPTfNodeIdentifier>> resultDisEqualities = 
						new HashSet<>(arrayWsc.getDisEqualities());
				for (NodeIdWithSideCondition i : indexVector) {
					resultEqualities.addAll(i.getEqualities());
					resultDisEqualities.addAll(i.getDisEqualities());
				}
				// TODO filter out inconsistent nodes
				
				if (valueAtIndex != null) {
					// the arrayWsc gives us a value for the given index --> return that
					result.add(
							new NodeIdWithSideCondition(
									valueAtIndex, 
									resultEqualities, 
									resultDisEqualities));
				} else {
					// we don't know the array's value at the given index
					// --> we can still return the condition under which the index is indeterminate
					
					
				}
			}
		}
		
		return result;
		
//		/*
//		 * Cases:
//		 * - a select-over-store introduces a case split:
//		 *   given (select (store a i x) j), we get the cases "i = j" and "i != j"
//		 * - 
//		 */
//		
//		if (mArray instanceof VPTfArrayIdentifier) {
//			
//			return null;
//		} else if (mArray instanceof StoreTermWrapper) {
//
//			return null;
//		} else {
//			assert false : "missed a case?";
//			return null;
//		}
		
	}



//	@Override
//	public Set<ISingleElementWrapper> getElements() {
////		return Collections.singleton(this); // TODO: not sure about this
//		assert false;
//		return null;
//	}
}