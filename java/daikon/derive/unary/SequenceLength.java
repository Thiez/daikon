package daikon.derive.unary;
import daikon.*;
import daikon.derive.*;

// originally from pass1.
public class SequenceLength extends UnaryDerivation {

  public SequenceLength(VarInfo vi) {
    super(vi);
  }

  public static boolean applicable(VarInfo vi) {
    if (!vi.rep_type.isArray())
      return false;
    return true;
  }

  public ValueAndModified computeValueAndModified(ValueTuple vt) {
    int source_mod = var_info.getModified(vt);
    if (source_mod == ValueTuple.MISSING)
      return ValueAndModified.MISSING;
    Object val = var_info.getValue(vt);
    if (val == null)
      return ValueAndModified.MISSING;
    else {
      int len;
      // Can't use "==" because ProglangType objecs are not interned.
      if (var_info.rep_type.equals(ProglangType.INT_ARRAY)) {
        len = ((int[])val).length;
      } else {
        len = ((Object[])val).length;
      }
      return new ValueAndModified(new Integer(len), source_mod);
    }
  }

  VarInfo this_var_info;

  public VarInfo makeVarInfo() {
    if (this_var_info != null)
      return this_var_info;

    String name = "size(" + var_info.name + ")";
    ProglangType ptype = ProglangType.INT;
    ProglangType rtype = ProglangType.INT;
    ExplicitVarComparability comp = var_info.comparability.indexType(0);
    this_var_info = new VarInfo(name, ptype, rtype, comp);

    var_info.derivees.add(this);
    this_var_info.derived = this;
    return this_var_info;
  }

}
