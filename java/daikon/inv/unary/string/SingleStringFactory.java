package daikon.inv.unary.string;

import daikon.*;
import utilMDE.*;

import java.util.*;

public final class SingleStringFactory {

  // Adds the appropriate new Invariant objects to the specified Invariants
  // collection.
  public static Vector instantiate(PptSlice ppt) {

    VarInfo var = ppt.var_infos[0];
    Assert.assert(var.rep_type == ProglangType.STRING);

    Vector result = new Vector();
    result.add(OneOfString.instantiate(ppt));
    return result;
  }

  private SingleStringFactory() {
  }

}
