package daikon.inv.scalar;

import daikon.*;
import daikon.inv.*;

import utilMDE.*;

import java.util.*;

// States that the value is one of the specified values.

// This subsumes an "exact" invariant that says the value is always exactly
// a specific value.  Do I want to make that a separate invariant
// nonetheless?  Probably not, as this will simplify implication and such.

// Similar to OneOfSequence; if I change one, change the other.
public class OneOfScalar extends SingleScalar implements OneOf {
  final static int LIMIT = 5;	// maximum size for the one_of list
  // probably needs to keep its own list of the values

  private int[] elts;
  private int num_elts;

  OneOfScalar(PptSlice ppt_) {
    super(ppt_);
    elts = new int[LIMIT];
    num_elts = 0;
  }

  public static OneOfScalar instantiate(PptSlice ppt) {
    return new OneOfScalar(ppt);
  }

  public int num_elts() {
    return num_elts;
  }

  public Object elt() {
    if (num_elts != 1)
      throw new Error("Represents " + num_elts + " elements");
    // Not sure whether interning is necessary, but just in case...
    return Intern.internedInteger(elts[0]);
  }

  private String subarray_rep() {
    // Not so efficient an implementation, but simple;
    // and how often will we need to print this anyway?
    int num_good_elts = Math.min(num_elts, LIMIT);
    Arrays.sort(elts, 0, num_good_elts);
    String asarray = ArraysMDE.toString(ArraysMDE.subarray(elts, 0, num_good_elts));
    return "{" + asarray.substring(1, asarray.length()-1) + "}";
  }

  public String repr() {
    double probability = getProbability();
    return "OneOfScalar(" + var().name + "): "
      + "no_invariant=" + no_invariant
      + ", num_elts=" + num_elts
      + ", elts=" + subarray_rep();
  }

  public String format() {
    if (no_invariant || (num_elts == 0) || (! justified()))
      return null;
    if (num_elts == 1)
      return var().name + " = " + elts[0];
    else
      return var().name + " one of " + subarray_rep();
  }


  public void add_modified(int v, int count) {
    for (int i=0; i<num_elts; i++)
      if (elts[i] == v)
        return;
    if (num_elts == LIMIT) {
      destroy();
      return;
    }
    elts[num_elts] = v;
    num_elts++;
  }

  protected double computeProbability() {
    // This is wrong; fix it
    return 0;
  }

}
