//#define ELTWISEINTCOMPARISON EltwiseIntComparison

// ***** This file is automatically generated from SingleSequenceFactory.java.jpp

package daikon.inv.unary.sequence;

import daikon.*;

import utilMDE.*;

import java.util.*;

public final class SingleScalarSequenceFactory {

  // Adds the appropriate new Invariant objects to the specified Invariants
  // collection.
  public static Vector instantiate(PptSlice ppt, boolean excludeEquality) {

    VarInfo var = ppt.var_infos[0];
    Assert.assertTrue(var.rep_type == ProglangType.INT_ARRAY);
    Assert.assertTrue(var.type.pseudoDimensions() > 0);

    Vector result = new Vector();
    { // previously (pass == 1)
      result.add(OneOfSequence.instantiate(ppt));
      result.add(EltOneOf.instantiate(ppt));
    }
    { // previously (pass == 2)
      // EltOneOf eoo = EltOneOf.find(ppt);
      // if (!((eoo != null) && (eoo.num_elts() == 1)))
      {
        result.add(EltNonZero.instantiate(ppt));
        result.add(NoDuplicates.instantiate(ppt));
        result.add(CommonSequence.instantiate(ppt));
        if (var.type.elementIsIntegral()) {
          // result.add(ELTWISEINTCOMPARISON.instantiate(ppt));

          result.add(EltLowerBound.instantiate(ppt));
          result.add(EltUpperBound.instantiate(ppt));
          result.add(SeqIndexComparison.instantiate(ppt));
          result.add(SeqIndexNonEqual.instantiate(ppt));
        }

        // Must now instantiate one of each type... perhaps this functionality can
        // be moved to their superclass EltwiseIntComparison

        result.add(EltwiseIntEqual.instantiate(ppt));
        result.add(EltwiseIntNonEqual.instantiate(ppt));
        result.add(EltwiseIntLessThan.instantiate(ppt));
        result.add(EltwiseIntLessEqual.instantiate(ppt));
        result.add(EltwiseIntGreaterThan.instantiate(ppt));
        result.add(EltwiseIntGreaterEqual.instantiate(ppt));

      }
    }
    return result;
  }

  private SingleScalarSequenceFactory() {
  }

}
