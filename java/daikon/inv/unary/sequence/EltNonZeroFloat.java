// ***** This file is automatically generated from EltNonZero.java.jpp

package daikon.inv.unary.sequence;

import daikon.*;
import daikon.inv.*;
import daikon.inv.binary.twoSequence.*;

import utilMDE.*;

import org.apache.log4j.Category;

import java.util.*;

// States that the value is one of the specified values.

// This subsumes an "exact" invariant that says the value is always exactly
// a specific value.  Do I want to make that a separate invariant
// nonetheless?  Probably not, as this will simplify implication and such.

public final class EltNonZeroFloat
  extends SingleFloatSequence
{
  /**
   * Debug tracer.
   **/
  public static final Category debug =
    Category.getInstance("daikon.inv.unary.sequence.EltNonZeroFloat");

  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  // Variables starting with dkconfig_ should only be set via the
  // daikon.config.Configuration interface.
  /**
   * Boolean.  True iff EltNonZero invariants should be considered.
   **/
  public static boolean dkconfig_enabled = true;

  double min = Double.MAX_VALUE;
  double max = Double.MIN_VALUE;

  // If nonzero, use this as the range instead of the actual range.
  // This lets one use a specified probability of nonzero (say, 1/10
  // for pointers).
  int override_range = 0;
  boolean pointer_type = false;

  EltNonZeroFloat (PptSlice ppt) {
    super(ppt);
  }

  public static EltNonZeroFloat instantiate(PptSlice ppt) {
    if (!dkconfig_enabled) return null;
    EltNonZeroFloat result = new EltNonZeroFloat(ppt);
    if (! ppt.var_infos[0].type.baseIsFloat()) {
      result.pointer_type = true;
      result.override_range = 3;
      if (!result.var().aux.getFlag(VarInfoAux.HAS_NULL)) {
        // If it's not a number and null doesn't have special meaning...
        return null;
     }

    }

    if (debug.isDebugEnabled()) {
      System.out.println("EltNonZeroFloat.instantiate: " + result.format());
    }
    return result;
  }

  public String repr() {
    return "EltNonZeroFloat" + varNames() + ": "
      + !falsified + ",min=" + min + ",max=" + max;
  }

  public String format_using(OutputFormat format) {
    if (format == OutputFormat.DAIKON) return format_daikon();
    if (format == OutputFormat.ESCJAVA) return format_esc();
    if (format == OutputFormat.IOA) return format_ioa();
    if (format == OutputFormat.SIMPLIFY) return format_simplify();
    if (format == OutputFormat.JML) return format_jml();

    return format_unimplemented(format);
  }

  public String format_daikon() {
    return var().name.name() + " elements != " + (pointer_type ? "null" : "0");
  }

  // We are a special case where a ghost field can actually talk about
  // array contents.
  public boolean isValidEscExpression() {
    return true;
  }

  public String format_esc() {
    // If this is an entire array or Collection (not VarInfoName.Slice), then
    //  * for arrays: use \nonnullelements(A)
    //  * for Collections: use collection.containsNull == false
    //    (the latter also requires that ghost field to get set)

    VarInfoName name = var().name;
    if (name instanceof VarInfoName.Elements) {
      VarInfoName term = ((VarInfoName.Elements) name).term;
      if (var().type.isArray()) {
        return "\\nonnullelements(" + term.esc_name() + ")";
      } else {
        return term.esc_name() + ".containsNull == false";
      }
    }

    // If this is just part of an array or Collection (name instanceof
    // VarInfoName.Slice), then calling name.esc_name() will always throw
    // an exception, since var() is certainly a sequence.  So use the
    // standard quantification.

    String[] form =
      VarInfoName.QuantHelper.format_esc(new VarInfoName[]
        { var().name });
    return form[0] + "(" + form[1] + " != " + (pointer_type ? "null" : "0") + ")" + form[2];
  }

  public String format_jml() {
    // If this is an entire array or Collection (not VarInfoName.Slice), then
    //  * for arrays: use \nonnullelements(A)
    //  * for Collections: use collection.containsNull == false
    //    (the latter also requires that ghost field to get set)

    VarInfoName name = var().name;
    if (name instanceof VarInfoName.Elements) {
      VarInfoName term = ((VarInfoName.Elements) name).term;
      if (var().type.isArray()) {
        return "\\nonnullelements(" + term.jml_name() + ")";
      } else {
        return term.jml_name() + ".containsNull == false";
      }
    }

    // If this is just part of an array or Collection (name instanceof
    // VarInfoName.Slice), then calling name.jml_name() will always throw
    // an exception, since var() is certainly a sequence.  So use the
    // standard quantification.

    String[] form =
      VarInfoName.QuantHelper.format_jml(new VarInfoName[]
        { var().name });
    return form[0] + form[1] + " != " + (pointer_type ? "null" : "0") + form[2];
  }

  /* IOA */
  public String format_ioa() {
    VarInfoName.QuantHelper.IOAQuantification quant = new VarInfoName.QuantHelper.IOAQuantification (var ());
    String result = quant.getQuantifierExp() + quant.getVarName(0).ioa_name() + " \\in " +
      var().name.ioa_name() + " => " + quant.getVarIndexed(0) + "~=";
    if (pointer_type) {
      return result + "null" + quant.getClosingExp();
    } else {
      return result + "0" + quant.getClosingExp();
    }
  }

  public String format_simplify() {
    String[] form =
      VarInfoName.QuantHelper.format_simplify(new VarInfoName[]
        { var().name });
    return form[0] + "(NEQ " + form[1] + " " + (pointer_type ? "null" : "0") + ")" + form[2];
  }

  public void add_modified(double[] a, int count) {
    for (int ai=0; ai<a.length; ai++) {
      double v = a[ai];

      // The min and max tests will simultaneoulsy succeed exactly once (for
      // the first value).
      if (v == 0) {
        destroyAndFlow();
        return;
      }
      // XXX; uh oh -- flowing these is bad stuff; maybe search for
      // upper / lower bound instead when computing probability
      if (v < min) min = v;
      if (v > max) max = v;
    }
  }

  protected double computeProbability() {
    Assert.assertTrue(! falsified);
    // Maybe just use 0 as the min or max instead, and see what happens:
    // see whether the "nonzero" invariant holds anyway.  (Perhaps only
    // makes sense to do if the {Lower,Upper}Bound invariant doesn't imply
    // the non-zeroness.)  In that case, do still check for no values yet
    // received.
    if ((override_range == 0) && ((min > 0) || (max < 0)))
      return Invariant.PROBABILITY_UNJUSTIFIED;
    else {
      double range;
      if (override_range != 0) {
        range = override_range;
      } else {
        int modulus = 1;

        // I need to come back and make this work.
        // {
        //   for (Iterator itor = ppt.invs.iterator(); itor.hasNext();) {
        //     Invariant inv = (Invariant) itor.next();
        //     if ((inv instanceof Modulus) && inv.enoughSamples()) {
        //       modulus = ((Modulus) inv).modulus;
        //       break;
        //     }
        //   }
        // }

        // Perhaps I ought to check that it's possible (given the modulus
        // constraints) for the value to be zero; otherwise, the modulus
        // constraint implies non-zero.
        range = (max - min + 1) / modulus;
      }
      double probability_one_elt_nonzero = 1 - 1.0/range;
      // This could underflow; so consider doing
      //   double log_probability = self.samples*math.log(probability);
      // then calling Math.exp (if the value is in the range that wouldn't
      // cause underflow).
      return Math.pow(probability_one_elt_nonzero, ppt.num_mod_non_missing_samples());
    }
  }

  public boolean isSameFormula(Invariant other)
  {
    Assert.assertTrue(other instanceof EltNonZeroFloat);
    return true;
  }

  public boolean isExclusiveFormula(Invariant other)
  {
    if (other instanceof EltOneOfFloat) {
      EltOneOfFloat eoo = (EltOneOfFloat) other;
      if ((eoo.num_elts() == 1) && (((Double)eoo.elt()).doubleValue() == 0)) {
        return true;
      }
    }
    return false;
  }

  public boolean isObviousImplied() {
    // For every other EltNonZero at this program point, see if there is a
    // subsequence relationship between that array and this one.

    PptTopLevel parent = ppt.parent;
    for (Iterator itor = parent.invariants_iterator(); itor.hasNext(); ) {
      Invariant inv = (Invariant) itor.next();
      if ((inv instanceof EltNonZeroFloat) && (inv != this) && inv.enoughSamples()) {
        VarInfo v1 = var();
        VarInfo v2 = inv.ppt.var_infos[0];
        if (SubSequence.isObviousDerived(v1, v2)) {
          // System.out.println("obvious: " + format() + "   because of " + inv.format());
          return true;
        }

        boolean this_var_first = (v1.varinfo_index < v2.varinfo_index);
        if (! this_var_first) { VarInfo temp = v1; v1 = v2; v2 = temp; }
        Assert.assertTrue(v1.varinfo_index < v2.varinfo_index);
        PptSlice2 slice_2seq = parent.findSlice(v1, v2);
        if (slice_2seq == null) {
          // System.out.println("EltNonZeroFloat.isObviousImplied: no slice for " + v1.name + ", " + v2.name);
        } else  {
          // slice_2seq != null
          SubSequence ss = SubSequence.find(slice_2seq);
          if (ss == null) {
            // System.out.println("EltNonZeroFloat.isObviousImplied: no SubSequence for " + v1.name + ", " + v2.name);
          } else {
            // System.out.println("EltNonZeroFloat.isObviousImplied: found SubSequence: " + ss.repr());
            if (this_var_first
                ? ss.var1_in_var2
                : ss.var2_in_var1) {
              return true;
            }
          }
        }
      }
    }

    return false;
  }

  // Look up a previously instantiated invariant.
  public static EltNonZeroFloat find(PptSlice ppt) {
    Assert.assertTrue(ppt.arity == 1);
    for (Iterator itor = ppt.invs.iterator(); itor.hasNext(); ) {
      Invariant inv = (Invariant) itor.next();
      if (inv instanceof EltNonZeroFloat)
        return (EltNonZeroFloat) inv;
    }
    return null;
  }

}
