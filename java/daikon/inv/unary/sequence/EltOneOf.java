package daikon.inv.unary.sequence;

import daikon.*;
import daikon.inv.*;
import daikon.derive.unary.*;
import daikon.inv.unary.scalar.*;
import daikon.inv.unary.sequence.*;
import daikon.inv.binary.sequenceScalar.*;

import utilMDE.*;

import java.util.*;
import java.io.*;

// *****
// Do not edit this file directly:
// it is automatically generated from OneOf.java.jpp
// *****

// States that the value is one of the specified values.

// This subsumes an "exact" invariant that says the value is always exactly
// a specific value.  Do I want to make that a separate invariant
// nonetheless?  Probably not, as this will simplify implication and such.

public final class EltOneOf 
  extends SingleSequence 
  implements OneOf
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  public static boolean dkconfig_enabled = true;
  public static int dkconfig_size = 3;

  // Probably needs to keep its own list of the values, and number of each seen.
  // (That depends on the slice; maybe not until the slice is cleared out.
  // But so few values is cheap, so this is quite fine for now and long-term.)

  private long [] elts;
  private int num_elts;

  private boolean is_boolean;
  private boolean is_hashcode;

  EltOneOf (PptSlice ppt) {
    super(ppt);

    elts = new long [dkconfig_size];

    num_elts = 0;

    Assert.assert(var().type.isPseudoArray(),
		  "ProglangType must be pseudo-array for EltOneOf or OneOfSequence");
    is_boolean = (var().file_rep_type.elementType() == ProglangType.BOOLEAN);
    is_hashcode = (var().file_rep_type.elementType() == ProglangType.HASHCODE);
    // System.out.println("is_hashcode=" + is_hashcode + " for " + format()
    //                    + "; file_rep_type=" + var().file_rep_type.format());

  }

  public static EltOneOf  instantiate(PptSlice ppt) {
    if (!dkconfig_enabled) return null;
    return new EltOneOf (ppt);
  }

  protected Object clone() {
    EltOneOf  result = (EltOneOf ) super.clone();
    result.elts = (long []) elts.clone();

    return result;
  }

  public int num_elts() {
    return num_elts;
  }

  public Object elt() {
    if (num_elts != 1)
      throw new Error("Represents " + num_elts + " elements");

    // Not sure whether interning is necessary (or just returning an Integer
    // would be sufficient), but just in case...
    return Intern.internedLong(elts[0]);

  }

  private void sort_rep() {
    Arrays.sort(elts, 0, num_elts  );
  }

  public Object min_elt() {
    if (num_elts == 0)
      throw new Error("Represents no elements");
    sort_rep();

    // Not sure whether interning is necessary (or just returning an Integer
    // would be sufficient), but just in case...
    return Intern.internedLong(elts[0]);

  }

  public Object max_elt() {
    if (num_elts == 0)
      throw new Error("Represents no elements");
    sort_rep();

    // Not sure whether interning is necessary (or just returning an Integer
    // would be sufficient), but just in case...
    return Intern.internedLong(elts[num_elts-1]);

  }

  // Assumes the other array is already sorted
  public boolean compare_rep(int num_other_elts, long [] other_elts) {
    if (num_elts != num_other_elts)
      return false;
    sort_rep();
    for (int i=0; i < num_elts; i++)
      if (elts[i] != other_elts[i]) // elements are interned
        return false;
    return true;
  }

  private String subarray_rep() {
    // Not so efficient an implementation, but simple;
    // and how often will we need to print this anyway?
    sort_rep();
    StringBuffer sb = new StringBuffer();
    sb.append("{ ");
    for (int i=0; i<num_elts; i++) {
      if (i != 0)
        sb.append(", ");
      sb.append(((( elts[i]  == 0) && (var().file_rep_type == ProglangType.HASHCODE_ARRAY)) ? "null" : ((Integer.MIN_VALUE <=  elts[i]  &&  elts[i]  <= Integer.MAX_VALUE) ? String.valueOf( elts[i] ) : (String.valueOf( elts[i] ) + "L"))) );
    }
    sb.append(" }");
    return sb.toString();
  }

  public String repr() {
    return "EltOneOf"  + varNames() + ": "
      + "no_invariant=" + no_invariant
      + ", num_elts=" + num_elts
      + ", elts=" + subarray_rep();
  }

  public String format() {
    String varname = var().name.name() + " elements" ;
    if (num_elts == 1) {

      if (is_boolean) {
        Assert.assert((elts[0] == 0) || (elts[0] == 1));
        return varname + " == " + ((elts[0] == 0) ? "false" : "true");
      } else if (is_hashcode) {
        if (elts[0] == 0) {
          return varname + " == null";
        } else {
          return varname + " has only one value"
            // + " (hashcode=" + elts[0] + ")"
            ;
        }
      } else {
        return varname + " == " + ((( elts[0]  == 0) && (var().file_rep_type == ProglangType.HASHCODE_ARRAY)) ? "null" : ((Integer.MIN_VALUE <=  elts[0]  &&  elts[0]  <= Integer.MAX_VALUE) ? String.valueOf( elts[0] ) : (String.valueOf( elts[0] ) + "L"))) ;
      }

    } else {
      return varname + " one of " + subarray_rep();
    }
  }

    public String format_java() {
       StringBuffer sb = new StringBuffer();
       for (int i = 0; i < num_elts; i++) {
	 sb.append (" || (" + var().name.java_name()  + " == " +  ((( elts[i]  == 0) && (var().file_rep_type == ProglangType.HASHCODE_ARRAY)) ? "null" : ((Integer.MIN_VALUE <=  elts[i]  &&  elts[i]  <= Integer.MAX_VALUE) ? String.valueOf( elts[i] ) : (String.valueOf( elts[i] ) + "L")))   );
	 sb.append (")");
       }
       // trim off the && at the beginning for the first case
       return sb.toString().substring (4);
    }

  /* IOA */
  public String format_ioa() {

    VarInfoName.QuantHelper.IOAQuantification quant =
      new VarInfoName.QuantHelper.IOAQuantification (new VarInfo[] {var()});
    String varname = quant.getVarName(0);

    String result;

    if (is_boolean) {
      Assert.assert(num_elts == 1);
      Assert.assert((elts[0] == 0) || (elts[0] == 1));
      result = varname + " = " + ((elts[0] == 0) ? "false" : "true");
    } else if (is_hashcode) {
      Assert.assert(num_elts == 1);
      if (elts[0] == 0) {
        result = varname + " = null";
      } else {
        result = varname + " = {one value}";
      }
    } else {
      result = "";
      for (int i=0; i<num_elts; i++) {
        if (i != 0) { result += " \\/ ("; }
        result += varname + " = " + ((( elts[i]  == 0) && (var().file_rep_type == ProglangType.HASHCODE_ARRAY)) ? "null" : ((Integer.MIN_VALUE <=  elts[i]  &&  elts[i]  <= Integer.MAX_VALUE) ? String.valueOf( elts[i] ) : (String.valueOf( elts[i] ) + "L")))  + ")";
      }
      result += ")";
    }

    result = quant.getQuantifierExp() + quant.getMembershipRestriction(0) + " => " + result + quant.getClosingExp();

    return result;
  }

  public String format_esc() {

    String[] form = VarInfoName.QuantHelper.format_esc(new VarInfoName[] { var().name } );
    String varname = form[1];

    String result;

    if (is_boolean) {
      Assert.assert(num_elts == 1);
      Assert.assert((elts[0] == 0) || (elts[0] == 1));
      result = varname + " == " + ((elts[0] == 0) ? "false" : "true");
    } else if (is_hashcode) {
      Assert.assert(num_elts == 1);
      if (elts[0] == 0) {
        result = varname + " == null";
      } else {
        result = varname + " != null";
	  // varname + " has only one value"
          // + " (hashcode=" + elts[0] + ")"
          ;
      }
    } else {
      result = "";
      for (int i=0; i<num_elts; i++) {
        if (i != 0) { result += " || "; }
        result += varname + " == " + ((( elts[i]  == 0) && (var().file_rep_type == ProglangType.HASHCODE_ARRAY)) ? "null" : ((Integer.MIN_VALUE <=  elts[i]  &&  elts[i]  <= Integer.MAX_VALUE) ? String.valueOf( elts[i] ) : (String.valueOf( elts[i] ) + "L"))) ;
      }
    }

    result = form[0] + "(" + result + ")" + form[2];

    return result;
  }

  public String format_simplify() {

    String[] form = VarInfoName.QuantHelper.format_simplify(new VarInfoName[] { var().name } );
    String varname = form[1];

    String result;

    if (is_boolean) {
      Assert.assert(num_elts == 1);
      Assert.assert((elts[0] == 0) || (elts[0] == 1));
      result = "(EQ " + varname + " " + ((elts[0] == 0) ? "|@false|" : "|@true|") + ")";
    } else if (is_hashcode) {
      Assert.assert(num_elts == 1);
      result = "(EQ " + varname + " " + ((elts[0] == 0) ? "null" : ("hash_" + elts[0])) + ")";
    } else {
      result = "";
      for (int i=0; i<num_elts; i++) {
        result += " (EQ " + varname + " " + ((( elts[i]  == 0) && (var().file_rep_type == ProglangType.HASHCODE_ARRAY)) ? "null" : ((Integer.MIN_VALUE <=  elts[i]  &&  elts[i]  <= Integer.MAX_VALUE) ? String.valueOf( elts[i] ) : (String.valueOf( elts[i] ) + "L")))  + ")";
      }
      if (num_elts > 1) {
	result = "(OR" + result + ")";
      } else {
	// chop leading space
	result = result.substring(1);
      }
    }

    result = form[0] + result + form[2];

    return result;
  }

  public void add_modified(long [] a, int count) {
  OUTER:
    for (int ai=0; ai<a.length; ai++) {
      long  v = a[ai];

    for (int i=0; i<num_elts; i++)
      if (elts[i] == v) {

        continue OUTER;

      }
    if (num_elts == dkconfig_size) {
      flowThis();
      destroy();
      return;
    }

    if ((is_boolean && (num_elts == 1))
        || (is_hashcode && (num_elts == 2))) {
      flowThis();
      destroy();
      return;
    }
    if (is_hashcode && (num_elts == 1)) {
      // Permit two object values only if one of them is null
      if ((elts[0] != 0) && (v != 0)) {
	flowThis();
        destroy();
        return;
      }
    }

    if (num_elts > 0) {
      // We are significantly changing our state (not just zeroing in on
      // a constant), so we have to flow a copy before we do so.
      flowClone();
    }

    elts[num_elts] = v;
    num_elts++;

    }
  }

  protected double computeProbability() {
    // This is not ideal.
    if (num_elts == 0) {
      return Invariant.PROBABILITY_UNJUSTIFIED;

    } else if (is_hashcode && (num_elts > 1)) {
      // This should never happen
      return Invariant.PROBABILITY_UNJUSTIFIED;

    } else {
      return Invariant.PROBABILITY_JUSTIFIED;
    }
  }

  public boolean isSameFormula(Invariant o)
  {
    EltOneOf  other = (EltOneOf ) o;
    if (num_elts != other.num_elts)
      return false;

    // Add case for SEQUENCE eventually

    // If the invariants are both hashcodes and non-null, consider
    // them to have the same formula
    if (num_elts == 1 && other.num_elts == 1) {
      if (is_hashcode && elts[0] != 0 && other.elts[0] != 0) {
        return true;
      }
    }

    sort_rep();
    other.sort_rep();
    for (int i=0; i < num_elts; i++)
      if (elts[i] != other.elts[i]) // elements are interned
	return false;

    return true;
  }

  public boolean isExclusiveFormula(Invariant o)
  {
    if (o instanceof EltOneOf ) {
      EltOneOf  other = (EltOneOf ) o;

      for (int i=0; i < num_elts; i++) {
        for (int j=0; j < other.num_elts; j++) {
          if (elts[i] == other.elts[j]) // elements are interned
            return false;
        }
      }
      return true;
    }

    // Many more checks can be added here:  against nonzero, modulus, etc.
    if ((o instanceof EltNonZero ) && (num_elts == 1) && (elts[0] == 0)) {
      return true;
    }
    long elts_min = Long.MAX_VALUE;
    long elts_max = Long.MIN_VALUE;
    for (int i=0; i < num_elts; i++) {
      elts_min = Math.min(elts_min, elts[i]);
      elts_max = Math.max(elts_max, elts[i]);
    }
    if ((o instanceof LowerBound) && (elts_max < ((LowerBound)o).core.min1))
      return true;
    if ((o instanceof UpperBound) && (elts_min > ((UpperBound)o).core.max1))
      return true;

    return false;
  }

  // OneOf invariants that indicate a small set of possible values are
  // uninteresting.  OneOf invariants that indicate exactly one value
  // are interesting.
  public boolean isInteresting() {
    if (num_elts() > 1) {
      return false;
    } else {
      return true;
    }
  }

  // Look up a previously instantiated invariant.
  public static EltOneOf  find(PptSlice ppt) {
    Assert.assert(ppt.arity == 1);
    for (Iterator itor = ppt.invs.iterator(); itor.hasNext(); ) {
      Invariant inv = (Invariant) itor.next();
      if (inv instanceof EltOneOf )
        return (EltOneOf ) inv;
    }
    return null;
  }

  // Interning is lost when an object is serialized and deserialized.
  // Manually re-intern any interned fields upon deserialization.
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    for (int i=0; i < num_elts; i++)
      elts[i] = Intern.intern(elts[i]);
  }

}

