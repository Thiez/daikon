package daikon.inv.binary.twoScalar;

import daikon.*;
import daikon.inv.*;
import daikon.inv.Invariant.OutputFormat;
import utilMDE.*;
import java.io.Serializable;
import org.apache.log4j.Category;

public final class LinearBinaryCore
  implements Serializable, Cloneable
{
  // We are Serializable, so we specify a version to allow changes to
  // method signatures without breaking serialization.  If you add or
  // remove fields, you should change this number to the current date.
  static final long serialVersionUID = 20020122L;

  /**
   * Debug tracer
   **/

  public static final Category debug =
    Category.getInstance("daikon.inv.binary.twoScalar.LinearBinaryCore");

  // y == ax + b; first argument is x, second is y
  public double a, b;
  // The above form rules out vertical lines.  We could also state
  // this invariant like "ax + by = 1".  This would make swapping
  // easier and allow for vertical lines; however, it would rule out
  // 45-degree ("x == y") lines.  For now, I guess we leave it as-is,
  // but presumably we can arrange to not use this invariant on equal
  // variables, whereas we might use it on constant variables.

  public Invariant wrapper;

  public int values_seen = 0;

  // XXX Why do we have to wait for 4 datapoints?  Aren't two points
  // enough to form a line?  Code would be much simpler without this.
  final static int MINPAIRS = 4;

  long[] x_cache = new long[MINPAIRS];
  long[] y_cache = new long[MINPAIRS];

  public LinearBinaryCore(Invariant wrapper) {
    this.wrapper = wrapper;
  }

  public Object clone() {
    try {
      LinearBinaryCore result = (LinearBinaryCore) super.clone();
      if (x_cache != null)
	result.x_cache = (long[]) x_cache.clone();
      if (y_cache != null)
	result.y_cache = (long[]) y_cache.clone();
      return result;
    } catch (CloneNotSupportedException e) {
      throw new Error(); // can't happen
    }
  }

  public void swap() {
    // was a swap
    if (a == 0) {
      // can't swap horizontal line into vertical
      Assert.assert(b == 0);
    } else {
      a = 1 / a;   // a' =  1/a
      b = -b * a;  // b' = -b/a
    }

    long[] tmp = x_cache;
    x_cache = y_cache;
    y_cache = tmp;
  }

  public void add_modified(long x, long y, int count) {
    if (values_seen < MINPAIRS) {
      // We delay computation of a and b until we have seen several pairs
      // so that we can compute a and b based on a far-separated pair.  If
      // the points in a pair are nearby, then roundoff errors in the
      // computation of the slope can be non-negligible.

      for (int i=0; i<values_seen; i++)
	if ((x_cache[i] == x) && (y_cache[i] == y))
	  return;
      x_cache[values_seen] = x;
      y_cache[values_seen] = y;
      values_seen++;
      if (values_seen == MINPAIRS) {
	// Find the most separated pair
        // Do I really need to check in two dimensions, or would one be enough?
        // indices of the most-separated pair
	int max_i = -1;
	int max_j = -1;
        // (square of the) distance between the most separated pair
	double max_separation = 0;
	for (int i=0; i<MINPAIRS-1; i++) {
	  for (int j=i+1; j<MINPAIRS; j++) {
            // not long, lest we get wraparound
	    double xsep = ((double)x_cache[i] - x_cache[j]);
	    double ysep = ((double)y_cache[i] - y_cache[j]);
	    double separation = xsep*xsep + ysep*ysep;
            // Assert.assert(separation > 0);
	    if (separation > max_separation) {
	      max_separation = separation;
	      max_i = i;
	      max_j = j;
	    }
	  }
	}
	// Set a and b based on that pair
	boolean ok =
	  set_bi_linear(x_cache[max_i], x_cache[max_j], y_cache[max_i], y_cache[max_j]);
	if (a == 0) {
	  ok = false;
	  debug.debug("Suppressing " + "LinearBinaryCore (" + wrapper.format() + ") because a == 0");
	}
	// Check all values against a and b.
	for (int i=0; ok && i<MINPAIRS; i++) {
	  // I should permit a fudge factor here.
	  if (y_cache[i] != a*x_cache[i]+b) {
	    if (debug.isDebugEnabled()) {
	      debug.debug("Suppressing " + "LinearBinaryCore (" + wrapper.format() + ") at index " + i + ": "
			  + y_cache[i] + " != " + a + "*" + x_cache[i] + "+" + b);
	      debug.debug("    ");
	    }
	    ok = false;
          }
        }
        if (! ok) {
	  values_seen--;
	  wrapper.flowClone();
	  wrapper.destroy();
	  return;
	} else {
	  x_cache = null;
	  y_cache = null;
	}
      }
    } else {
      // Check the new value against a and b.
      if (y != a*x+b) {
        if (debug.isDebugEnabled()) {
          debug.debug("Suppressing " + "LinearBinaryCore (" + wrapper.format() + ") at new value: "
                             + y + " != " + a + "*" + x + "+" + b);
        }
	wrapper.flowThis();
        wrapper.destroy();
        return;
      }
    }
  }

  // Given ((x0,y0),(x1,y1)), set a and b such that y = ax + b.
  // @return true if such an (a,b) exists
  boolean set_bi_linear(long x0, long x1, long y0, long y1) {
    if (x0 == x1) {
      // x being constant would have been discovered elsewhere (and this
      // invariant would not have been instantiated).
      if (debug.isDebugEnabled()) {
        debug.debug("Suppressing " + "LinearBinaryCore" + " due to equal x values: (" + x0 + "," + y0 + "), (" + x1 + "," + y1 + ")");
      }
      return false;
    }

    a = (y1-y0)/(x1-x0);
    b = (y0*x1-x0*y1)/(x1-x0);
    return true;
  }

  public boolean enoughSamples() {
    return values_seen >= MINPAIRS;
  }

  public double computeProbability() {
    if (wrapper.falsified)
      return Invariant.PROBABILITY_NEVER;
    return Invariant.prob_is_ge(values_seen, MINPAIRS);
  }

  public String repr() {
    return "LinearBinaryCore" + wrapper.varNames() + ": "
      + "a=" + a
      + ",b=" + b
      + ",values_seen=" + values_seen;
  }

  // Format one term of an equation.
  // Variable "first" indicates whether this is the leading term
  // Variable "var" is the name of the variable; may be null for the constant term.
  public static String formatTerm(OutputFormat format,
				  double coeff,
				  VarInfoName var,
				  boolean first)
  {
    if (coeff == 0)
      return "";
    String sign;
    if (coeff < 0) {
      if (first) {
        sign = "- ";
      } else {
        sign = " - ";
      }
      coeff = -coeff;
    } else if (first) {
      sign = "";
    } else {
      sign = " + ";
    }
    String coeff_string = (coeff == (int)coeff) ? "" + (int)coeff : "" + coeff;
    if (var == null)
      return sign + coeff_string;
    if (coeff == 1)
      return sign + var.name_using(format);
    else
      return sign + coeff_string + " * " + var.name_using(format);
  }

  public static String format_using(OutputFormat format,
				    VarInfoName x, VarInfoName y,
				    double a, double b)
  {
    String xname = x.name_using(format);
    String yname = y.name_using(format);

    if ((a == 0) && (b == 0)) {
      String result = yname + " == ? * " + xname + " + ?";
      if (format == OutputFormat.IOA) result += " ***";
      return result;
    }

    if ((format == OutputFormat.DAIKON)
	|| (format == OutputFormat.JAVA)
	|| (format == OutputFormat.ESCJAVA)
	|| (format == OutputFormat.JML)
	|| (format == OutputFormat.IOA))
    {
      String eq = " == ";
      if (format == OutputFormat.IOA) eq = " = ";
      return yname + eq
	+ formatTerm(format, a, x, true)
	+ formatTerm(format, b, null, false);
    }

    if (format == OutputFormat.SIMPLIFY) {
      int ia = (int) a;
      int ib = (int) b;

      //      no data            or      non-integral
      if (((ia == 0) && (ib == 0)) || (ia != a) || (ib != b)) {
	return null;
      }

      // y == a x + b
      String str_ax = (a == 1) ? xname : "(* " + a + " " + xname + ")";
      String str_axpb = (b == 0) ? str_ax : "(+ " + str_ax + " " + b + ")";
      return "(EQ " + yname + " " + str_axpb + ")";
    }

    return null;
  }

  public String format_using(OutputFormat format,
			     VarInfoName x, VarInfoName y)
  {
    String result = format_using(format, x, y, a, b);
    if (result != null) {
      return result;
    } else {
      return wrapper.format_unimplemented(format);
    }
  }

  // Format as "x = cy+d" instead of as "y = ax+b".
  public String format_reversed_using(OutputFormat format,
				      VarInfoName x, VarInfoName y)
  {
    Assert.assert(a == 1 || a == -1);
    return format_using(format, y, x, a, -b/a);
  }

  public boolean isSameFormula(LinearBinaryCore other)
  {
    boolean thisMeaningless = values_seen < MINPAIRS;
    boolean otherMeaningless = other.values_seen < MINPAIRS;

    if (thisMeaningless && otherMeaningless) {
      return true;
    } else {
      return
        (values_seen >= MINPAIRS) &&
        (other.values_seen >= MINPAIRS) &&
        (a == other.a) &&
        (b == other.b);
    }
  }

  public boolean isExclusiveFormula(LinearBinaryCore other)
  {
    if ((values_seen < MINPAIRS) ||
        (other.values_seen < MINPAIRS)) {
      return false;
    }

    return ((a == other.a)
            && (b != other.b));
  }

}
