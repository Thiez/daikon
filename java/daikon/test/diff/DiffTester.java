package daikon.test.diff;

import junit.framework.*;
import daikon.*;
import daikon.config.*;
import daikon.diff.*;
import daikon.inv.*;
import daikon.inv.unary.scalar.*;
import daikon.inv.unary.string.*;
import daikon.inv.unary.sequence.*;
import daikon.inv.unary.stringsequence.*;
import daikon.inv.binary.twoScalar.*;
import daikon.inv.ternary.threeScalar.*;
import daikon.split.*;
import daikon.split.misc.*;
import daikon.test.*;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;


public class DiffTester extends TestCase {

  private Diff diffSome;
  private Diff diffAll;

  private PptMap empty;

  private PptMap ppts1;
  private PptMap ppts2;
  private PptMap ppts3;
  private PptMap ppts4;

  // ppts1 plus conditional program points
  private PptMap pptsCond;

  private PptMap invs1;
  private PptMap invs2;
  private PptMap invs3;
  private PptMap imps1;
  private PptMap imps2;

  public static void main(String[] args) {
    daikon.Logger.setupLogs (Logger.INFO);
    junit.textui.TestRunner.run(new TestSuite(DiffTester.class));
  }

  public static VarInfo newIntVarInfo(String name) {
    VarInfo result = new VarInfo(VarInfoName.parse(name),
                                 ProglangType.INT,
                                 ProglangType.INT,
                                 (VarComparability)null, // null Comparability
				 VarInfoAux.getDefault());
    // INCR: equal_to no longer exists!
    // // VarInfo.isCanonical() insists that equal_to be non-null.
    // result.equal_to = result;
    return result;
  }

  public DiffTester(String name) throws Exception {
    super(name);

    diffSome = new Diff();
    diffAll = new Diff(true);

    empty = new PptMap();

    ppts1 = new PptMap();
    ppts1.add(newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]));
    ppts1.add(newPptTopLevel("Foo.Bar(int):::ENTER", new VarInfo[0]));
    ppts1.add(newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]));

    ppts2 = new PptMap();
    ppts2.add(newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]));
    ppts2.add(newPptTopLevel("Foo.Bar(int):::ENTER", new VarInfo[0]));

    // Permutation of ppts1
    ppts3 = new PptMap();
    ppts3.add(newPptTopLevel("Foo.Bar(int):::ENTER", new VarInfo[0]));
    ppts3.add(newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]));
    ppts3.add(newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]));

    {
      ppts4 = new PptMap();
      ppts4.add(newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]));
      PptTopLevel ppt1 =
        newPptTopLevel("Foo.Bar(int):::EXIT19", new VarInfo[0]);
      PptTopLevel ppt2 =
	newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]);
      // ppt1.combined_exit = ppt2; // [INCR]
      ppts4.add(ppt1);
      ppts4.add(ppt2);
    }

    {
      pptsCond = new PptMap();
      PptTopLevel ppt1 =
        newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]);
      Splitter split = new ReturnTrueSplitter();
      PptConditional pptCond = new PptConditional(ppt1, split, false);
      ppt1.views_cond.addElement(pptCond);
      pptsCond.add(ppt1);
      pptsCond.add(newPptTopLevel("Foo.Bar(int):::ENTER", new VarInfo[0]));
      pptsCond.add(newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]));
    }

    // Invoke private method using reflection
    Method mAddViews = PptTopLevel.class.getDeclaredMethod
      ("addViews", new Class[] {Vector.class});
    mAddViews.setAccessible(true);

    {
      invs1 = new PptMap();
      VarInfo[] vars = { newIntVarInfo("x"), newIntVarInfo("y"),
                         newIntVarInfo("z")};
      PptTopLevel ppt = newPptTopLevel("Foo.Baa(int):::ENTER", vars);
      PptSlice slicex = new PptSlice1(ppt, new VarInfo[] {vars[0]});
      Invariant invx = LowerBound.instantiate(slicex);
      slicex.addInvariant(invx);
      PptSlice slicey = new PptSlice1(ppt, new VarInfo[] {vars[1]});
      Invariant invy = LowerBound.instantiate(slicey);
      slicey.addInvariant(invy);
      PptSlice slicez = new PptSlice1(ppt, new VarInfo[] {vars[2]});
      Invariant invz = LowerBound.instantiate(slicez);
      slicez.addInvariant(invz);
      Vector v = new Vector();
      v.add(slicex);
      v.add(slicey);
      v.add(slicez);
      mAddViews.invoke(ppt, new Object[] {v});
      invs1.add(ppt);
    }

    {
      // Permutation of invs1
      invs2 = new PptMap();
      VarInfo[] vars = { newIntVarInfo("x"), newIntVarInfo("y"),
                         newIntVarInfo("z")};
      PptTopLevel ppt = newPptTopLevel("Foo.Baa(int):::ENTER", vars);
      PptSlice slicey = new PptSlice1(ppt, new VarInfo[] {vars[1]});
      Invariant invy = LowerBound.instantiate(slicey);
      slicey.addInvariant(invy);
      PptSlice slicex = new PptSlice1(ppt, new VarInfo[] {vars[0]});
      Invariant invx = LowerBound.instantiate(slicex);
      slicex.addInvariant(invx);
      PptSlice slicez = new PptSlice1(ppt, new VarInfo[] {vars[2]});
      Invariant invz = LowerBound.instantiate(slicez);
      slicez.addInvariant(invz);
      Vector v = new Vector();
      v.add(slicey);
      v.add(slicex);
      v.add(slicez);
      mAddViews.invoke(ppt, new Object[] {v});
      invs2.add(ppt);
    }

    {
      invs3 = new PptMap();
      VarInfo[] vars = { newIntVarInfo("x"), newIntVarInfo("y"),
                         newIntVarInfo("z")};
      PptTopLevel ppt = newPptTopLevel("Foo.Baa(int):::ENTER", vars);
      PptSlice slicex = new PptSlice1(ppt, new VarInfo[] {vars[0]});
      Invariant invx = LowerBound.instantiate(slicex);
      slicex.addInvariant(invx);
      PptSlice slicey = new PptSlice1(ppt, new VarInfo[] {vars[1]});
      Invariant invy = UpperBound.instantiate(slicey);
      slicex.addInvariant(invy);
      PptSlice slicez = new PptSlice1(ppt, new VarInfo[] {vars[2]});
      Invariant invz = LowerBound.instantiate(slicez);
      slicez.addInvariant(invz);
      Vector v = new Vector();
      v.add(slicex);
      v.add(slicey);
      v.add(slicez);
      mAddViews.invoke(ppt, new Object[] {v});
      invs3.add(ppt);
    }

    {
      // Ensure that Modulus is enabled
      Configuration.getInstance().
        apply(daikon.inv.unary.scalar.Modulus.class,
              "enabled",
              "true");

      imps1 = new PptMap();
      VarInfo[] vars = { newIntVarInfo("x") };
      PptTopLevel ppt = newPptTopLevel("Foo.Baa(int):::ENTER", vars);
      PptSlice slicex = new PptSlice1(ppt, new VarInfo[] {vars[0]});
      Invariant inv1 = LowerBound.instantiate(slicex);
      Invariant inv2 = Modulus.instantiate(slicex);
      Invariant inv3 = UpperBound.instantiate(slicex);
      Implication imp1 = Implication.makeImplication(ppt, inv1, inv2, false);
      Implication imp2 = Implication.makeImplication(ppt, inv1, inv3, false);
      Implication imp3 = Implication.makeImplication(ppt, inv2, inv1, false);
      Implication imp4 = Implication.makeImplication(ppt, inv2, inv3, false);
      Implication imp5 = Implication.makeImplication(ppt, inv3, inv1, false);
      Implication imp6 = Implication.makeImplication(ppt, inv3, inv2, false);
      imps1.add(ppt);
    }


    // Permutation of the nullary invariants in invs1
    {
      imps2 = new PptMap();
      VarInfo[] vars = { newIntVarInfo("x") };
      PptTopLevel ppt = newPptTopLevel("Foo.Baa(int):::ENTER", vars);
      PptSlice slicex = new PptSlice1(ppt, new VarInfo[] {vars[0]});
      Invariant inv1 = LowerBound.instantiate(slicex);
      Invariant inv2 = Modulus.instantiate(slicex);
      Invariant inv3 = UpperBound.instantiate(slicex);
      Implication imp3 = Implication.makeImplication(ppt, inv2, inv1, false);
      Implication imp2 = Implication.makeImplication(ppt, inv1, inv3, false);
      Implication imp4 = Implication.makeImplication(ppt, inv2, inv3, false);
      Implication imp5 = Implication.makeImplication(ppt, inv3, inv1, false);
      Implication imp6 = Implication.makeImplication(ppt, inv3, inv2, false);
      Implication imp1 = Implication.makeImplication(ppt, inv1, inv2, false);
      imps2.add(ppt);
    }

  }

  public void testEmptyEmpty() {
    RootNode diff = diffSome.diffPptMap(empty, empty);
    RootNode ref = new RootNode();
    Assert.assertEquals(printTree(ref), printTree(diff));
  }

  public void testEmptyPpts1() {
    RootNode diff = diffSome.diffPptMap(empty, ppts1);

    RootNode ref = new RootNode();
    PptNode node;
    node = new PptNode
      (null,
       newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]));
    ref.add(node);
    node = new PptNode
      (null,
       newPptTopLevel("Foo.Bar(int):::ENTER", new VarInfo[0]));
    ref.add(node);
    node = new PptNode
      (null,
       newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]));
    ref.add(node);

    Assert.assertEquals(printTree(ref), printTree(diff));
  }

  public void testPpts1Empty() {
    RootNode diff = diffSome.diffPptMap(ppts1, empty);

    RootNode ref = new RootNode();
    PptNode node;
    node = new PptNode
      (newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]),
       null);
    ref.add(node);
    node = new PptNode
      (newPptTopLevel("Foo.Bar(int):::ENTER", new VarInfo[0]),
       null);
    ref.add(node);
    node = new PptNode
      (newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]),
       null);
    ref.add(node);

    Assert.assertEquals(printTree(ref), printTree(diff));
  }

  /* [INCR] This needs to be reinstated, but right now there is a problem
     with both EXIT and EXIT19 being considered a main exit point.
  public void testPpts4Empty() {
    RootNode diff = diffSome.diffPptMap(ppts4, empty);

    RootNode ref = new RootNode();
    PptNode node;
    node = new PptNode
      (newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]),
       null);
    ref.add(node);
    node = new PptNode
      (newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]),
       null);
    ref.add(node);

    Assert.assertEquals(printTree(ref), printTree(diff));
  }


  public void testPpts4EmptyAll() {
    RootNode diff = diffAll.diffPptMap(ppts4, empty);

    RootNode ref = new RootNode();
    PptNode node;
    node = new PptNode
      (newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]),
       null);
    ref.add(node);
    node = new PptNode
      (newPptTopLevel("Foo.Bar(int):::EXIT19", new VarInfo[0]),
       null);
    ref.add(node);
    node = new PptNode
      (newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]),
       null);
    ref.add(node);

    Assert.assertEquals(printTree(ref), printTree(diff));
  }
  */ // INCR

  public void testPpts1Ppts1() {
    RootNode diff = diffSome.diffPptMap(ppts1, ppts1);

    RootNode ref = new RootNode();
    PptNode node;
    node = new PptNode
      (newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]),
       newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]));
    ref.add(node);
    node = new PptNode
      (newPptTopLevel("Foo.Bar(int):::ENTER", new VarInfo[0]),
       newPptTopLevel("Foo.Bar(int):::ENTER", new VarInfo[0]));
    ref.add(node);
    node = new PptNode
      (newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]),
       newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]));
    ref.add(node);

    Assert.assertEquals(printTree(ref), printTree(diff));
  }

  /* [INCR] This needs to be reinstated, but right now there is a problem
     with both EXIT and EXIT19 being considered a main exit point.
  public void testPpts4Ppts4() {
    RootNode diff = diffSome.diffPptMap(ppts4, ppts4);

    RootNode ref = new RootNode();
    PptNode node;
    node = new PptNode
      (newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]),
       newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]));
    ref.add(node);
    node = new PptNode
      (newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]),
       newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]));
    ref.add(node);

    Assert.assertEquals(printTree(ref), printTree(diff));
  }
  */ // INCR

  public void testPpts1Ppts2() {
    RootNode diff = diffSome.diffPptMap(ppts1, ppts2);

    RootNode ref = new RootNode();
    PptNode node;
    node = new PptNode
      (newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]),
       newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]));
    ref.add(node);
    node = new PptNode
      (newPptTopLevel("Foo.Bar(int):::ENTER", new VarInfo[0]),
       newPptTopLevel("Foo.Bar(int):::ENTER", new VarInfo[0]));
    ref.add(node);
    node = new PptNode
      (newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]),
       null);
    ref.add(node);

    Assert.assertEquals(printTree(ref), printTree(diff));
  }

  public void testPpts1Ppts3() {
    RootNode diff = diffSome.diffPptMap(ppts1, ppts3);

    RootNode ref = new RootNode();
    PptNode node;
    node = new PptNode
      (newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]),
       newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]));
    ref.add(node);
    node = new PptNode
      (newPptTopLevel("Foo.Bar(int):::ENTER", new VarInfo[0]),
       newPptTopLevel("Foo.Bar(int):::ENTER", new VarInfo[0]));
    ref.add(node);
    node = new PptNode
      (newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]),
       newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]));
    ref.add(node);

    Assert.assertEquals(printTree(ref), printTree(diff));
  }


  public void testInvs1Empty() {
    RootNode diff = diffSome.diffPptMap(invs1, empty);

    RootNode ref = new RootNode();

    VarInfo[] vars = { newIntVarInfo("x"), newIntVarInfo("y"),
                       newIntVarInfo("z") };
    PptTopLevel ppt = newPptTopLevel("Foo.Baa(int):::ENTER", vars);
    PptSlice slicex = new PptSlice1(ppt, new VarInfo[] {vars[0]});
    Invariant invx = LowerBound.instantiate(slicex);
    slicex.addInvariant(invx);
    PptSlice slicey = new PptSlice1(ppt, new VarInfo[] {vars[1]});
    Invariant invy = LowerBound.instantiate(slicey);
    slicey.addInvariant(invy);
    PptSlice slicez = new PptSlice1(ppt, new VarInfo[] {vars[2]});
    Invariant invz = LowerBound.instantiate(slicez);
    slicez.addInvariant(invz);

    PptNode pptNode;
    pptNode = new PptNode(ppt, null);
    InvNode invNode;
    invNode = new InvNode(invx, null);
    pptNode.add(invNode);
    invNode = new InvNode(invy, null);
    pptNode.add(invNode);
    invNode = new InvNode(invz, null);
    pptNode.add(invNode);
    ref.add(pptNode);

    Assert.assertEquals(printTree(ref), printTree(diff));
  }

  public void testInvs1Invs1() {
    RootNode diff = diffSome.diffPptMap(invs1, invs1);

    RootNode ref = new RootNode();

    VarInfo[] vars = { newIntVarInfo("x"), newIntVarInfo("y"),
                       newIntVarInfo("z") };
    PptTopLevel ppt = newPptTopLevel("Foo.Baa(int):::ENTER", vars);
    PptSlice slicex = new PptSlice1(ppt, new VarInfo[] {vars[0]});
    Invariant invx = LowerBound.instantiate(slicex);
    slicex.addInvariant(invx);
    PptSlice slicey = new PptSlice1(ppt, new VarInfo[] {vars[1]});
    Invariant invy = LowerBound.instantiate(slicey);
    slicey.addInvariant(invy);
    PptSlice slicez = new PptSlice1(ppt, new VarInfo[] {vars[2]});
    Invariant invz = LowerBound.instantiate(slicez);
    slicez.addInvariant(invz);

    PptNode pptNode;
    pptNode = new PptNode(ppt, ppt);
    InvNode invNode;
    invNode = new InvNode(invx, invx);
    pptNode.add(invNode);
    invNode = new InvNode(invy, invy);
    pptNode.add(invNode);
    invNode = new InvNode(invz, invz);
    pptNode.add(invNode);
    ref.add(pptNode);

    Assert.assertEquals(printTree(ref), printTree(diff));
  }

  public void testInvs1Invs2() {
    RootNode diff = diffSome.diffPptMap(invs1, invs2);

    RootNode ref = new RootNode();

    VarInfo[] vars = { newIntVarInfo("x"), newIntVarInfo("y"),
                       newIntVarInfo("z") };
    PptTopLevel ppt = newPptTopLevel("Foo.Baa(int):::ENTER", vars);
    PptSlice slicex = new PptSlice1(ppt, new VarInfo[] {vars[0]});
    Invariant invx = LowerBound.instantiate(slicex);
    slicex.addInvariant(invx);
    PptSlice slicey = new PptSlice1(ppt, new VarInfo[] {vars[1]});
    Invariant invy = LowerBound.instantiate(slicey);
    slicey.addInvariant(invy);
    PptSlice slicez = new PptSlice1(ppt, new VarInfo[] {vars[2]});
    Invariant invz = LowerBound.instantiate(slicez);
    slicez.addInvariant(invz);

    PptNode pptNode;
    pptNode = new PptNode(ppt, ppt);
    InvNode invNode;
    invNode = new InvNode(invx, invx);
    pptNode.add(invNode);
    invNode = new InvNode(invy, invy);
    pptNode.add(invNode);
    invNode = new InvNode(invz, invz);
    pptNode.add(invNode);
    ref.add(pptNode);

    Assert.assertEquals(printTree(ref), printTree(diff));
  }

  public void testInvs1Invs3() {
    RootNode diff = diffSome.diffPptMap(invs1, invs3);

    RootNode ref = new RootNode();

    VarInfo[] vars = { newIntVarInfo("x"), newIntVarInfo("y"),
                       newIntVarInfo("z") };
    PptTopLevel ppt = newPptTopLevel("Foo.Baa(int):::ENTER", vars);
    PptSlice slicex = new PptSlice1(ppt, new VarInfo[] {vars[0]});
    Invariant invx = LowerBound.instantiate(slicex);
    PptSlice slicey = new PptSlice1(ppt, new VarInfo[] {vars[1]});
    Invariant invy1 = LowerBound.instantiate(slicey);
    Invariant invy2 = UpperBound.instantiate(slicey);
    PptSlice slicez = new PptSlice1(ppt, new VarInfo[] {vars[2]});
    Invariant invz = LowerBound.instantiate(slicez);

    PptNode pptNode;
    pptNode = new PptNode(ppt, ppt);
    InvNode invNode;
    invNode = new InvNode(invx, invx);
    pptNode.add(invNode);
    invNode = new InvNode(invy1, null);
    pptNode.add(invNode);
    invNode = new InvNode(invz, invz);
    pptNode.add(invNode);
    invNode = new InvNode(null, invy2);
    pptNode.add(invNode);
    ref.add(pptNode);

    Assert.assertEquals(printTree(ref), printTree(diff));
  }

  public void testNullaryInvs() {
    RootNode root = diffSome.diffPptMap(imps1, imps2);
  }

  public void testNonModulus() {
    // Ensure that NonModulus is enabled
    Configuration.getInstance().
      apply(daikon.inv.unary.scalar.NonModulus.class,
            "enabled",
            "true");

    PptMap map = new PptMap();
    VarInfo[] vars = { newIntVarInfo("x") };
    PptTopLevel ppt = newPptTopLevel("Foo.Baa(int):::ENTER", vars);
    PptSlice slice = new PptSlice1(ppt, vars);
    Invariant inv = NonModulus.instantiate(slice);
    slice.addInvariant(inv);
    Vector v = new Vector();
    v.add(slice);
    map.add(ppt);

    RootNode diff = diffSome.diffPptMap(map, map);
  }

  // Runs diff on a PptMap containing a PptConditional, with
  // examineAllPpts set to false.  The PptConditional should be
  // ignored.
  public void testConditionalPptsFalse() {
    RootNode diff = diffSome.diffPptMap(ppts1, pptsCond);

    RootNode ref = new RootNode();
    PptNode node;
    node = new PptNode
      (newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]),
       newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]));
    ref.add(node);
    node = new PptNode
      (newPptTopLevel("Foo.Bar(int):::ENTER", new VarInfo[0]),
       newPptTopLevel("Foo.Bar(int):::ENTER", new VarInfo[0]));
    ref.add(node);
    node = new PptNode
      (newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]),
       newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]));
    ref.add(node);

    Assert.assertEquals(printTree(ref), printTree(diff));
  }

  // Runs diff on a PptMap containing a PptConditional, with
  // examineAllPpts set to true.  The PptConditional should be
  // ignored.
  public void testConditionalPptsTrue() {
    RootNode diff = diffAll.diffPptMap(ppts1, pptsCond);

    RootNode ref = new RootNode();
    PptNode node;
    node = new PptNode
      (newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]),
       newPptTopLevel("Foo.Baa(int):::ENTER", new VarInfo[0]));
    ref.add(node);
    node = new PptNode(null, newPptTopLevel
      ("Foo.Baa(int):::ENTER;condition=\"return == true\"", new VarInfo[0]));
    ref.add(node);
    node = new PptNode
      (newPptTopLevel("Foo.Bar(int):::ENTER", new VarInfo[0]),
       newPptTopLevel("Foo.Bar(int):::ENTER", new VarInfo[0]));
    ref.add(node);
    node = new PptNode
      (newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]),
       newPptTopLevel("Foo.Bar(int):::EXIT", new VarInfo[0]));
    ref.add(node);

    Assert.assertEquals(printTree(ref), printTree(diff));
  }

  private static String printTree(RootNode root) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(baos);
    PrintAllVisitor v = new PrintAllVisitor(ps, false, true);
    root.accept(v);
    return baos.toString();
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Helper functions
  ///

  PptTopLevel newPptTopLevel(String pptname, VarInfo[] vars) {
    return Common.makePptTopLevel(pptname, vars);
  }

}
