package daikon;

import java.util.*;
import java.io.*;
import gnu.getopt.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import utilMDE.Assert;
import utilMDE.UtilMDE;
import daikon.inv.*;

public class MergeInvariants {

  public static final String lineSep = Global.lineSep;

  public static final Logger debug = Logger.getLogger("daikon.MergeInvariants");

  public static final Logger debugProgress
                        = Logger.getLogger("daikon.MergeInvariants.progress");

  public static File output_inv_file;


  private static String usage =
    UtilMDE.join(new String[] {
      "Usage: java daikon.PrintInvariants [OPTION]... FILE",
      "  -h, --" + Daikon.help_SWITCH,
      "      Display this usage message",
      "  --" + Daikon.config_option_SWITCH,
      "      Specify a configuration option ",
      "  --" + Daikon.debug_SWITCH,
      "      Specify a logger to enable",
      "  --" + Daikon.noinvariantguarding_SWITCH,
      "      Disable invariant guarding, which is normally on for "
             + "JML and ESC output formats.",
      "   -o ",
      "      Specify an output inv file.  If not specified, the results "
             + "are printed"},
      lineSep);

  public static void main(String[] args) throws FileNotFoundException,
  StreamCorruptedException, OptionalDataException, IOException,
  ClassNotFoundException {
    LongOpt[] longopts = new LongOpt[] {
      new LongOpt(Daikon.config_option_SWITCH, LongOpt.REQUIRED_ARGUMENT,
                  null, 0),
      new LongOpt(Daikon.debugAll_SWITCH, LongOpt.NO_ARGUMENT, null, 0),
      new LongOpt(Daikon.debug_SWITCH, LongOpt.REQUIRED_ARGUMENT, null, 0),
    };

    Getopt g = new Getopt("daikon.MergeInvariants", args, "ho:", longopts);
    int c;
    while ((c = g.getopt()) != -1) {
      switch(c) {

      // long option
      case 0:
        String option_name = longopts[g.getLongind()].getName();
        if (Daikon.help_SWITCH.equals(option_name)) {
          System.out.println(usage);
          System.exit(1);

        } else if (Daikon.config_option_SWITCH.equals(option_name)) {
          String item = g.getOptarg();
          daikon.config.Configuration.getInstance().apply(item);
          break;

        } else if (Daikon.debugAll_SWITCH.equals(option_name)) {
          Global.debugAll = true;

        } else if (Daikon.debug_SWITCH.equals(option_name)) {
          LogHelper.setLevel(g.getOptarg(), LogHelper.FINE);
        } else {
          throw new RuntimeException("Unknown long option received: " +
                                     option_name);
        }
        break;

      case 'h':
        System.out.println(usage);
        System.exit(1);
        break;

      case 'o':
        if (output_inv_file != null)
          throw new Error("multiple serialization output files "
                         + "supplied on command line");

        String output_inv_filename = g.getOptarg();
        output_inv_file = new File(output_inv_filename);

        if (! UtilMDE.canCreateAndWrite(output_inv_file)) {
          throw new Error("Cannot write to file " + output_inv_file);
        }
        break;

      case '?':
        break; // getopt() already printed an error

      default:
        System.out.println("getopt() returned " + c);
        break;
      }
    }

    daikon.LogHelper.setupLogs(Global.debugAll ? LogHelper.FINE
                               : LogHelper.INFO);

    List inv_files = new ArrayList();
    File decl_file = null;

    // Get each file specified
    for (int i = g.getOptind(); i < args.length; i++) {
      File file = new File (args[i]);
      if (! file.exists()) {
        throw new Error("File " + file + " not found.");
      }
      if (file.toString().indexOf (".inv") != -1)
        inv_files.add (file);
      else if (file.toString().indexOf (".decls") != -1) {
        if (decl_file != null)
          throw new Error ("Only one decl file may be specified");
        decl_file = file;
      } else {
        throw new Error ("unexpected file: " + file);
      }
    }

    // Make sure at least two files were specified
    if (inv_files.size() < 2)
      throw new Error ("Must specify at least two inv files");

    // Read in each of the specified maps
    List pptmaps = new ArrayList();
    for (int i = 0; i < inv_files.size(); i++) {
      File file = (File) inv_files.get(i);
      debugProgress.fine ("Processing " + file);
      PptMap ppts = FileIO.read_serialized_pptmap (file, true);
      ppts.repCheck();
      pptmaps.add (ppts);
    }

    // Merged ppt map (result of merging each specified inv file)
    PptMap merge_ppts = null;

    // if no decls file was specified
    if (decl_file == null) {

      // Read in the first map again to serve as a template
      File file = (File) inv_files.get(0);
      debugProgress.fine ("Reading " + file + " as merge template");
      merge_ppts = FileIO.read_serialized_pptmap (file, true);

      // Remove all of the slices, equality sets, to start
      debugProgress.fine ("Cleaning ppt map in preparation for merge");
      for (Iterator i = merge_ppts.pptIterator(); i.hasNext(); ) {
        PptTopLevel ppt = (PptTopLevel) i.next();
        ppt.clean_for_merge();
      }

    } else {

      // Build the result ppmap from the specific decls file
      debugProgress.fine ("Building result ppt map from decls file");
      List  decl_files = new ArrayList();
      decl_files.add (decl_file);
      merge_ppts = FileIO.read_declaration_files(decl_files);
      Dataflow.init_partial_order (merge_ppts);
      merge_ppts.trimToSize();
      PptRelation.init_hierarchy (merge_ppts);
    }

    // Create a hierarchy between the merge exitNN points and the
    // corresponding points in each of the specified maps.  This
    // should only be created at the exitNN points (ie, the leaves)
    // so that the normal processing will create the invariants at
    // upper points.
    debugProgress.fine ("Building hierarchy between leaves of the maps");
    for (Iterator i = merge_ppts.pptIterator(); i.hasNext(); ) {
      PptTopLevel ppt = (PptTopLevel) i.next();
      if (!ppt.ppt_name.isExitPoint())
        continue;
      if (ppt.ppt_name.isCombinedExitPoint())
        continue;
      for (int j = 0; j < pptmaps.size(); j++ ) {
        PptMap pmap = (PptMap) pptmaps.get (j);
        PptTopLevel child = pmap.get (ppt.ppt_name);
        if ((decl_file == null) && (child == null))
          throw new Error ("Can't find " + ppt.ppt_name + " in "
                           + inv_files.get(j));
        if (child == null)
          continue;
        if (child.num_samples() == 0)
          continue;
        if (child.equality_view == null)
          System.out.println ("equality_view == null in child ppt: "
                              + child.ppt_name + " (" + inv_files.get(j) + ")");
        else if (child.equality_view.invs == null)
          System.out.println ("equality_view.invs == null in child ppt: "
                              + child.ppt_name + " (" + inv_files.get(j) + ")"
                              + " samples = " + child.num_samples());
        PptRelation rel = PptRelation.newMergeChildRel (ppt, child);
      }
    }

    // Debug print the hierarchy is a more readable manner
    if (debug.isLoggable(Level.FINE)) {
      debug.fine ("PPT Hierarchy");
      for (Iterator i = merge_ppts.pptIterator(); i.hasNext(); ) {
        PptTopLevel ppt = (PptTopLevel) i.next();
        if (ppt.parents.size() == 0)
          ppt.debug_print_tree (debug, 0, null);
      }
    }

    // Merge the invariants
    debugProgress.fine ("Merging invariants");
    Dataflow.createUpperPpts (merge_ppts);

    // Equality post processing
    debugProgress.fine ("Equality Post Processing");
    for (Iterator itor = merge_ppts.pptIterator() ; itor.hasNext() ; ) {
      PptTopLevel ppt = (PptTopLevel) itor.next();
      ppt.postProcessEquality();
    }

    // Suppression
    debugProgress.fine ("Suppression processing");
    Daikon.use_suppression_optimization = true;
    for (Iterator itor = merge_ppts.pptIterator() ; itor.hasNext() ; ) {
      PptTopLevel ppt = (PptTopLevel) itor.next();
      ppt.suppressAll (false);
    }

    // Remove the PptRelation links so that when the file is written
    // out it only includes the new information
    for (Iterator i = merge_ppts.pptIterator(); i.hasNext(); ) {
      PptTopLevel ppt = (PptTopLevel) i.next();
      if (!ppt.ppt_name.isExitPoint())
        continue;
      if (ppt.ppt_name.isCombinedExitPoint())
        continue;
      ppt.children.clear();
    }

    // Write serialized output - must be done before guarding invariants
    debugProgress.fine ("Writing Output");
    if (output_inv_file != null) {
      try {
        FileIO.write_serialized_pptmap(merge_ppts, output_inv_file);
      } catch (IOException e) {
        throw new RuntimeException("Error while writing .inv file "
                                + "'" + output_inv_file + "': " + e.toString());
      }
    } else {
      // Print the invariants
      PrintInvariants.print_invariants (merge_ppts);
    }

  }

}
